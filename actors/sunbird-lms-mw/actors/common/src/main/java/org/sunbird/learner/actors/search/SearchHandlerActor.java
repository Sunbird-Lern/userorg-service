package org.sunbird.learner.actors.search;

import akka.dispatch.Mapper;
import akka.pattern.Patterns;
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
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.common.responsecode.ResponseMessage;
import org.sunbird.dto.SearchDTO;
import org.sunbird.learner.util.UserUtility;
import org.sunbird.learner.util.Util;
import org.sunbird.models.organisation.Organisation;
import org.sunbird.telemetry.util.TelemetryWriter;
import scala.concurrent.Future;

import java.util.*;

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

  private OrganisationClient orgClient = new OrganisationClientImpl();
  private ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public void onReceive(Request request) throws Throwable {
    request.toLower();
    Util.initializeContext(request, TelemetryEnvKey.USER);
    if (request.getOperation().equalsIgnoreCase(ActorOperations.COMPOSITE_SEARCH.getValue())) {
      Map<String, Object> searchQueryMap = request.getRequest();
      Object objectType =
          ((Map<String, Object>) searchQueryMap.get(JsonKey.FILTERS)).get(JsonKey.OBJECT_TYPE);
      String filterObjectType = "";
      if (objectType != null && objectType instanceof List) {
        List<String> types = (List) objectType;
          filterObjectType = types.get(0);
      }
      ((Map<String, Object>) searchQueryMap.get(JsonKey.FILTERS)).remove(JsonKey.OBJECT_TYPE);
      if (EsType.organisation.getTypeName().equalsIgnoreCase(filterObjectType)) {
        SearchDTO searchDto = Util.createSearchDto(searchQueryMap);
        handleOrgSearchAsyncRequest(
            EsType.organisation.getTypeName(),
            searchDto,request.getContext());
      } else if (EsType.user.getTypeName().equalsIgnoreCase(filterObjectType)) {
          handleUserSearch(request, searchQueryMap, filterObjectType);
      }
    } else {
      onReceiveUnsupportedOperation(request.getOperation());
    }
  }

    private void handleUserSearch(Request request, Map<String, Object> searchQueryMap, String filterObjectType) throws Exception {
        UserUtility.encryptUserSearchFilterQueryData(searchQueryMap);
        extractOrFilter(searchQueryMap);
        SearchDTO searchDto = Util.createSearchDto(searchQueryMap);
        searchDto.setExcludedFields(Arrays.asList(ProjectUtil.excludes));
        Future<Map<String, Object>> resultF = esService.search(searchDto, filterObjectType);
        Map<String, Object>  result = (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
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
          String requestedFields = (String) request.getContext().get(JsonKey.FIELDS);
          updateUserDetailsWithOrgName(requestedFields, userMapList);
        }
        if (result == null) {
            result = new HashMap<>();
        }
        response.put(JsonKey.RESPONSE, result);
        sender().tell(response, self());
        // create search telemetry event here ...
        generateSearchTelemetryEvent(searchDto, filterObjectType, result, request.getContext());
    }

    private void handleOrgSearchAsyncRequest(
      String indexType, SearchDTO searchDto, Map<String,Object> context) {
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
    ProjectLogger.log("SearchHandlerActor:handleOrgSearchAsyncRequest: Telemetry disabled for search org api.",LoggerEnum.INFO.name());
    Request telemetryReq = new Request();
    telemetryReq.getRequest().put("context",context);
    telemetryReq.getRequest().put("searchFResponse",response);
    telemetryReq.getRequest().put("indexType",indexType);
    telemetryReq.getRequest().put("searchDto",searchDto);
    telemetryReq.setOperation("generateSearchTelemetry");
    tellToAnother(telemetryReq);
  }

  @SuppressWarnings("unchecked")
  private void updateUserDetailsWithOrgName(
      String requestedFields, List<Map<String, Object>> userMapList) {
    Map<String, Organisation> orgMap = null;
    if (StringUtils.isNotBlank(requestedFields)) {
      try {
        List<String> fields = Arrays.asList(requestedFields.toLowerCase().split(","));
        List<String> filteredRequestedFields = new ArrayList<>();
        List<String> supportedFields = Arrays.asList(JsonKey.ID, JsonKey.ORG_NAME);
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
      SearchDTO searchDto, String type, Map<String, Object> result, Map<String, Object> context) {

    Map<String, Object> params = new HashMap<>();
    params.put(JsonKey.TYPE, type);
    params.put(JsonKey.QUERY, searchDto.getQuery());
    params.put(JsonKey.FILTERS, searchDto.getAdditionalProperties().get(JsonKey.FILTERS));
    params.put(JsonKey.SORT, searchDto.getSortBy());
    params.put(JsonKey.SIZE, result.get(JsonKey.COUNT));
    params.put(JsonKey.TOPN, generateTopnResult(result)); // need to get topn value from
    // response
    Request req = new Request();
    req.setRequest(telemetryRequestForSearch(context, params));
    TelemetryWriter.write(req);
  }

  private List<Map<String, Object>> generateTopnResult(Map<String, Object> result) {

    List<Map<String, Object>> userMapList = (List<Map<String, Object>>) result.get(JsonKey.CONTENT);
    Integer topN = Integer.parseInt(PropertiesCache.getInstance().getProperty(JsonKey.SEARCH_TOP_N));

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
