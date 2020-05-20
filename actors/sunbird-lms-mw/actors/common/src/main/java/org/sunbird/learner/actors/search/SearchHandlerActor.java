package org.sunbird.learner.actors.search;

import akka.dispatch.Mapper;
import akka.pattern.Patterns;
import java.util.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.actorutil.org.OrganisationClient;
import org.sunbird.actorutil.org.impl.OrganisationClientImpl;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.*;
import org.sunbird.common.models.util.ProjectUtil.EsType;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.common.responsecode.ResponseMessage;
import org.sunbird.dto.SearchDTO;
import org.sunbird.learner.util.UserUtility;
import org.sunbird.learner.util.Util;
import org.sunbird.models.organisation.Organisation;
import org.sunbird.telemetry.util.TelemetryLmaxWriter;
import org.sunbird.telemetry.util.TelemetryUtil;
import scala.concurrent.Await;
import scala.concurrent.Future;

/**
 * This class will handle search operation for all different type of index and types
 *
 * @author Manzarul
 */
@ActorConfig(
  tasks = {"compositeSearch"},
  asyncTasks = {}
)
public class SearchHandlerActor extends BaseActor {

  private List<String> supportedFields = Arrays.asList(JsonKey.ID, JsonKey.ORG_NAME);
  private String topn = PropertiesCache.getInstance().getProperty(JsonKey.SEARCH_TOP_N);
  private OrganisationClient orgClient = new OrganisationClientImpl();
  private ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public void onReceive(Request request) throws Throwable {
    request.toLower();
    Util.initializeContext(request, TelemetryEnvKey.USER);
    // set request id fto thread loacl...
    ExecutionContext.setRequestId(request.getRequestId());
    String requestedFields = (String) request.getContext().get(JsonKey.FIELDS);

    if (request.getOperation().equalsIgnoreCase(ActorOperations.COMPOSITE_SEARCH.getValue())) {
      Map<String, Object> searchQueryMap = request.getRequest();
      Object objectType =
          ((Map<String, Object>) searchQueryMap.get(JsonKey.FILTERS)).get(JsonKey.OBJECT_TYPE);
      String[] types = null;
      if (objectType != null && objectType instanceof List) {
        List<String> list = (List) objectType;
        types = list.toArray(new String[list.size()]);
      }
      ((Map<String, Object>) searchQueryMap.get(JsonKey.FILTERS)).remove(JsonKey.OBJECT_TYPE);
      String filterObjectType = "";
      for (String type : types) {
        if (EsType.user.getTypeName().equalsIgnoreCase(type)) {
          filterObjectType = EsType.user.getTypeName();
          UserUtility.encryptUserSearchFilterQueryData(searchQueryMap);
        } else if (EsType.organisation.getTypeName().equalsIgnoreCase(type)) {
          filterObjectType = EsType.organisation.getTypeName();
        }
      }
      extractOrFilter(searchQueryMap);
      SearchDTO searchDto = Util.createSearchDto(searchQueryMap);
      if (filterObjectType.equalsIgnoreCase(EsType.user.getTypeName())) {
        searchDto.setExcludedFields(Arrays.asList(ProjectUtil.excludes));
      }
      Map<String, Object> result = null;
      if (EsType.organisation.getTypeName().equalsIgnoreCase(filterObjectType)) {
        handleOrgSearchAsyncRequest(
            ProjectUtil.EsIndex.sunbird.getIndexName(),
            EsType.organisation.getTypeName(),
            searchDto);
      } else {
        Future<Map<String, Object>> resultF = esService.search(searchDto, types[0]);
        result = (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
        Response response = new Response();
        // this fuzzy search Logic
        if (((List<Map<String, Object>>) result.get(JsonKey.CONTENT)).size() != 0
            && isFuzzySearchRequired(searchQueryMap)) {
          List<Map<String, Object>> responseList =
              getResponseOnFuzzyRequest(
                  getFuzzyFilterMap(searchQueryMap),
                  (List<Map<String, Object>>) result.get(JsonKey.CONTENT));
          if (responseList.size() != 0) {
            result.replace(JsonKey.COUNT, responseList.size());
            result.replace(JsonKey.CONTENT, responseList);
          } else {
            throw new ProjectCommonException(
                ResponseCode.PARTIAL_SUCCESS_RESPONSE.getErrorCode(),
                String.format(ResponseMessage.Message.PARAM_NOT_MATCH, JsonKey.NAME.toUpperCase()),
                ResponseCode.PARTIAL_SUCCESS_RESPONSE.getResponseCode());
          }
        }
        // Decrypt the data
        if (EsType.user.getTypeName().equalsIgnoreCase(filterObjectType)) {
          List<Map<String, Object>> userMapList =
              (List<Map<String, Object>>) result.get(JsonKey.CONTENT);
          for (Map<String, Object> userMap : userMapList) {
            UserUtility.decryptUserDataFrmES(userMap);
            userMap.remove(JsonKey.ENC_EMAIL);
            userMap.remove(JsonKey.ENC_PHONE);
          }
          updateUserDetailsWithOrgName(requestedFields, userMapList);
        }
        if (result != null) {
          response.put(JsonKey.RESPONSE, result);
        } else {
          result = new HashMap<>();
          response.put(JsonKey.RESPONSE, result);
        }
        sender().tell(response, self());
        // create search telemetry event here ...
        generateSearchTelemetryEvent(searchDto, types, result);
      }
    } else {
      onReceiveUnsupportedOperation(request.getOperation());
    }
  }

  private void handleOrgSearchAsyncRequest(
      String indexName, String indexType, SearchDTO searchDto) {
    Future<Map<String, Object>> futureResponse = esService.search(searchDto, indexType);
    Future<Response> response =
        futureResponse.map(
            new Mapper<Map<String, Object>, Response>() {
              @Override
              public Response apply(Map<String, Object> responseMap) {
                ProjectLogger.log(
                    "SearchHandlerActor:handleOrgSearchAsyncRequest org search call ",
                    LoggerEnum.INFO);
                Response response = new Response();
                response.put(JsonKey.RESPONSE, responseMap);
                return response;
              }
            },
            getContext().dispatcher());
    Patterns.pipe(response, getContext().dispatcher()).to(sender());
    Response orgSearchResponse = null;
    try {
      orgSearchResponse = Await.result(response, BaseActor.timeout.duration());
      String[] types = new String[] {indexType};
      Map<String, Object> contentMap = new HashMap<>();
      List<Object> contentList = new ArrayList<>();
      if (orgSearchResponse != null
          && MapUtils.isNotEmpty(orgSearchResponse.getResult())
          && MapUtils.isNotEmpty(
              (Map<String, Object>) orgSearchResponse.getResult().get(JsonKey.RESPONSE))) {
        HashMap<String, Object> contentListMap =
            (HashMap<String, Object>) orgSearchResponse.getResult().get(JsonKey.RESPONSE);
        contentList.add(contentListMap.get(JsonKey.CONTENT));
        if (CollectionUtils.isNotEmpty(contentList)) {
          contentMap.put(JsonKey.CONTENT, contentList.get(0));
          contentMap.put(
              JsonKey.COUNT,
              contentListMap.get(JsonKey.COUNT) != null ? contentListMap.get(JsonKey.COUNT) : 0);
          generateSearchTelemetryEvent(searchDto, types, contentMap);
        }
      }
    } catch (Exception e) {
      ProjectLogger.log(
          "SearchHandlerActor:handelOrgSearchAsyncRequest: Error occured in generating Telemetry for orgSearch  ",
          e,
          LoggerEnum.ERROR.name());
    }
  }

  @SuppressWarnings("unchecked")
  private void updateUserDetailsWithOrgName(
      String requestedFields, List<Map<String, Object>> userMapList) {
    Map<String, Organisation> orgMap = null;
    if (StringUtils.isNotBlank(requestedFields)) {
      try {
        List<String> fields = Arrays.asList(requestedFields.toLowerCase().split(","));
        List<String> filteredRequestedFields = new ArrayList<>();
        fields
            .stream()
            .forEach(
                rField -> {
                  for (String sField : supportedFields) {
                    if (sField.equalsIgnoreCase(rField)) {
                      filteredRequestedFields.add(sField);
                      break;
                    }
                  }
                });
        if (filteredRequestedFields.isEmpty()) {
          return;
        }
        if (!filteredRequestedFields.contains(JsonKey.ID)) {
          filteredRequestedFields.add(JsonKey.ID);
        }
        orgMap = fetchOrgDetails(userMapList, filteredRequestedFields);
        if (fields.contains(JsonKey.ORG_NAME.toLowerCase())) {
          Map<String, Organisation> filteredOrg = new HashMap<>(orgMap);
          userMapList
              .stream()
              .forEach(
                  userMap -> {
                    String rootOrgId = (String) userMap.get(JsonKey.ROOT_ORG_ID);
                    if (StringUtils.isNotBlank(rootOrgId)) {
                      Organisation org = filteredOrg.get(rootOrgId);
                      if (null != org) {
                        userMap.put(JsonKey.ROOT_ORG_NAME, org.getOrgName());
                      }
                    }
                    List<Map<String, Object>> userOrgList =
                        (List<Map<String, Object>>) userMap.get(JsonKey.ORGANISATIONS);
                    if (CollectionUtils.isNotEmpty(userOrgList)) {
                      userOrgList
                          .stream()
                          .forEach(
                              userOrg -> {
                                String userOrgId = (String) userOrg.get(JsonKey.ORGANISATION_ID);
                                if (StringUtils.isNotBlank(userOrgId)) {
                                  Organisation org = filteredOrg.get(userOrgId);
                                  if (null != org) {
                                    userOrg.put(JsonKey.ORG_NAME, org.getOrgName());
                                  }
                                }
                              });
                    }
                  });
        }
      } catch (Exception ex) {
        ProjectLogger.log(
            "SearchHandlerActor:updateUserDetailsWithOrgName: Exception occurred with error message = "
                + ex.getMessage(),
            ex);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Organisation> fetchOrgDetails(
      List<Map<String, Object>> userMapList, List<String> filteredRequestedFileds) {
    Set<String> orgIdList = new HashSet<>();
    userMapList
        .stream()
        .forEach(
            userMap -> {
              String rootOrgId = (String) userMap.get(JsonKey.ROOT_ORG_ID);
              if (StringUtils.isNotBlank(rootOrgId)) {
                orgIdList.add(rootOrgId);
              }
              List<Map<String, Object>> userOrgList =
                  (List<Map<String, Object>>) userMap.get(JsonKey.ORGANISATIONS);
              if (CollectionUtils.isNotEmpty(userOrgList)) {
                userOrgList
                    .stream()
                    .forEach(
                        userOrg -> {
                          String userOrgId = (String) userOrg.get(JsonKey.ORGANISATION_ID);
                          if (StringUtils.isNotBlank(userOrgId)) {
                            orgIdList.add(userOrgId);
                          }
                        });
              }
            });

    List<String> orgIds = new ArrayList<>(orgIdList);

    List<Organisation> organisations = orgClient.esSearchOrgByIds(orgIds, filteredRequestedFileds);
    Map<String, Organisation> orgMap = new HashMap<>();
    organisations
        .stream()
        .forEach(
            org -> {
              orgMap.put(org.getId(), org);
            });
    return orgMap;
  }

  private void generateSearchTelemetryEvent(
      SearchDTO searchDto, String[] types, Map<String, Object> result) {

    Map<String, Object> telemetryContext = TelemetryUtil.getTelemetryContext();

    Map<String, Object> params = new HashMap<>();
    params.put(JsonKey.TYPE, String.join(",", types));
    params.put(JsonKey.QUERY, searchDto.getQuery());
    params.put(JsonKey.FILTERS, searchDto.getAdditionalProperties().get(JsonKey.FILTERS));
    params.put(JsonKey.SORT, searchDto.getSortBy());
    params.put(JsonKey.SIZE, result.get(JsonKey.COUNT));
    params.put(JsonKey.TOPN, generateTopnResult(result)); // need to get topn value from
    // response
    Request req = new Request();
    req.setRequest(telemetryRequestForSearch(telemetryContext, params));
    TelemetryLmaxWriter.getInstance().submitMessage(req);
  }

  private List<Map<String, Object>> generateTopnResult(Map<String, Object> result) {

    List<Map<String, Object>> userMapList = (List<Map<String, Object>>) result.get(JsonKey.CONTENT);
    Integer topN = Integer.parseInt(topn);

    List<Map<String, Object>> list = new ArrayList<>();
    if (topN < userMapList.size()) {
      for (int i = 0; i < topN; i++) {
        Map<String, Object> m = new HashMap<>();
        m.put(JsonKey.ID, userMapList.get(i).get(JsonKey.ID));
        list.add(m);
      }
    } else {

      for (int i = 0; i < userMapList.size(); i++) {
        Map<String, Object> m = new HashMap<>();
        m.put(JsonKey.ID, userMapList.get(i).get(JsonKey.ID));
        list.add(m);
      }
    }
    return list;
  }

  private static Map<String, Object> telemetryRequestForSearch(
      Map<String, Object> telemetryContext, Map<String, Object> params) {
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.CONTEXT, telemetryContext);
    map.put(JsonKey.PARAMS, params);
    map.put(JsonKey.TELEMETRY_EVENT_TYPE, "SEARCH");
    return map;
  }

  /**
   * this method will convert the Sunbird required
   * fields(source,externalId,userName,provider,loginId,email) into lower case and encrypt PI
   * attributes well
   *
   * @param searchQueryMap
   * @throws Exception
   */
  private void extractOrFilter(Map<String, Object> searchQueryMap) throws Exception {
    Map<String, Object> ORFilterMap =
        (Map<String, Object>)
            ((Map<String, Object>) (searchQueryMap.get(JsonKey.FILTERS)))
                .get(JsonKey.ES_OR_OPERATION);
    if (MapUtils.isNotEmpty(ORFilterMap)) {
      Arrays.asList(
              ProjectUtil.getConfigValue(JsonKey.SUNBIRD_API_REQUEST_LOWER_CASE_FIELDS).split(","))
          .stream()
          .forEach(
              field -> {
                if (StringUtils.isNotBlank((String) ORFilterMap.get(field))) {
                  ORFilterMap.put(field, ((String) ORFilterMap.get(field)).toLowerCase());
                }
              });
      UserUtility.encryptUserData(ORFilterMap);
    }
  }

  private boolean isFuzzySearchRequired(Map<String, Object> searchQueryMap) {
    Map<String, Object> fuzzyFilterMap = getFuzzyFilterMap(searchQueryMap);
    if (MapUtils.isEmpty(fuzzyFilterMap)) {
      return false;
    }
    return true;
  }

  private List<Map<String, Object>> getResponseOnFuzzyRequest(
      Map<String, Object> fuzzyFilterMap, List<Map<String, Object>> searchMap) {
    return FuzzySearchManager.getInstance(fuzzyFilterMap, searchMap).startFuzzySearch();
  }

  private Map<String, Object> getFuzzyFilterMap(Map<String, Object> searchQueryMap) {
    return (Map<String, Object>)
        ((Map<String, Object>) (searchQueryMap.get(JsonKey.FILTERS))).get(JsonKey.SEARCH_FUZZY);
  }
}
