package org.sunbird.learner.actors.search;

import akka.dispatch.Mapper;
import akka.pattern.Patterns;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.stream.Collectors;
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
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.common.responsecode.ResponseMessage;
import org.sunbird.dto.SearchDTO;
import org.sunbird.learner.util.UserUtility;
import org.sunbird.learner.util.Util;
import org.sunbird.models.organisation.Organisation;
import org.sunbird.telemetry.util.TelemetryWriter;
import scala.concurrent.Future;

/**
 * This class will handle search operation for all different type of index and types
 *
 * @author Manzarul
 */
@ActorConfig(
  tasks = {"userSearch", "orgSearch"},
  asyncTasks = {},
  dispatcher = "most-used-one-dispatcher"
)
public class SearchHandlerActor extends BaseActor {

  private OrganisationClient orgClient = new OrganisationClientImpl();
  private ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);
  private ObjectMapper mapper = new ObjectMapper();

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public void onReceive(Request request) throws Throwable {
    request.toLower();
    Util.initializeContext(request, TelemetryEnvKey.USER);
    Map<String, Object> searchQueryMap = request.getRequest();
    if (MapUtils.isNotEmpty(searchQueryMap)
        && MapUtils.isNotEmpty(((Map<String, Object>) searchQueryMap.get(JsonKey.FILTERS)))) {
      ((Map<String, Object>) searchQueryMap.get(JsonKey.FILTERS)).remove(JsonKey.OBJECT_TYPE);
    }
    if (request.getOperation().equalsIgnoreCase(ActorOperations.USER_SEARCH.getValue())) {
      handleUserSearch(request, searchQueryMap, EsType.user.getTypeName());
    } else if (request.getOperation().equalsIgnoreCase(ActorOperations.ORG_SEARCH.getValue())) {
      SearchDTO searchDto = Util.createSearchDto(searchQueryMap);
      handleOrgSearchAsyncRequest(EsType.organisation.getTypeName(), searchDto, request);
    } else {
      onReceiveUnsupportedOperation(request.getOperation());
    }
  }

  private void backwardCompatibility(Map<String, Object> searchQueryMap) {

    Map<String, Object> filterMap =
        (Map<String, Object>)
            searchQueryMap.get(
                JsonKey.FILTERS); // checks if profileuser details is passed or not and calling
    // encryption method accordingly
    if (MapUtils.isNotEmpty(filterMap)) {
      if (StringUtils.isNotBlank((CharSequence) filterMap.get(JsonKey.USER_TYPE))) {
        filterMap.put(
            JsonKey.PROFILE_USERTYPE + "." + JsonKey.TYPE, filterMap.get(JsonKey.USER_TYPE));
        filterMap.remove(JsonKey.USER_TYPE);
      }
      if (StringUtils.isNotBlank((CharSequence) filterMap.get(JsonKey.USER_SUB_TYPE))) {
        filterMap.put(
            JsonKey.PROFILE_USERTYPE + "." + JsonKey.SUB_TYPE,
            filterMap.get(JsonKey.USER_SUB_TYPE));
        filterMap.remove(JsonKey.USER_SUB_TYPE);
      }
      if (StringUtils.isNotBlank((CharSequence) filterMap.get(JsonKey.LOCATION_IDS))) {
        filterMap.put(
            JsonKey.PROFILE_LOCATION + "." + JsonKey.ID, filterMap.get(JsonKey.LOCATION_IDS));
        filterMap.remove(JsonKey.LOCATION_IDS);
      }
    }
  }

  private void handleUserSearch(
      Request request, Map<String, Object> searchQueryMap, String filterObjectType)
      throws Exception {
    // checking for Backword compatibility
    backwardCompatibility(searchQueryMap);
    UserUtility.encryptUserSearchFilterQueryData(searchQueryMap);
    extractOrFilter(searchQueryMap);
    SearchDTO searchDto = Util.createSearchDto(searchQueryMap);
    searchDto.setExcludedFields(Arrays.asList(ProjectUtil.excludes));
    Future<Map<String, Object>> resultF =
        esService.search(searchDto, filterObjectType, request.getRequestContext());
    Map<String, Object> result =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
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
        Map<String, Object> userTypeDetail = new HashMap<>();
        if (MapUtils.isNotEmpty((Map<String, Object>) userMap.get(JsonKey.PROFILE_USERTYPE))) {
          userTypeDetail = (Map<String, Object>) userMap.get(JsonKey.PROFILE_USERTYPE);
          userMap.put(JsonKey.USER_TYPE, userTypeDetail.get(JsonKey.TYPE));
          userMap.put(JsonKey.USER_SUB_TYPE, userTypeDetail.get(JsonKey.SUB_TYPE));
        } else {
          userMap.put(JsonKey.USER_TYPE, null);
          userMap.put(JsonKey.USER_SUB_TYPE, null);
        }
        userMap.put(JsonKey.PROFILE_USERTYPE, userTypeDetail);
        List<String> locationIds = new ArrayList<>();
        List<Map<String, String>> userLocList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(
            (List<Map<String, String>>) userMap.get(JsonKey.PROFILE_LOCATION))) {
          userLocList = (List<Map<String, String>>) userMap.get(JsonKey.PROFILE_LOCATION);
          locationIds =
              userLocList.stream().map(m -> m.get(JsonKey.ID)).collect(Collectors.toList());
        }
        userMap.put(JsonKey.PROFILE_LOCATION, userLocList);
        userMap.put(JsonKey.LOCATION_IDS, locationIds);
        userMap.putAll(Util.getUserDefaultValue());
      }
      String requestedFields = (String) request.getContext().get(JsonKey.FIELDS);
      updateUserDetailsWithOrgName(requestedFields, userMapList, request.getRequestContext());
    }
    response.put(JsonKey.RESPONSE, result);
    sender().tell(response, self());
    generateSearchTelemetryEvent(searchDto, filterObjectType, result, request.getContext());
  }

  private void handleOrgSearchAsyncRequest(String indexType, SearchDTO searchDto, Request request) {
    Future<Map<String, Object>> futureResponse =
        esService.search(searchDto, indexType, request.getRequestContext());
    Future<Response> response =
        futureResponse.map(
            new Mapper<Map<String, Object>, Response>() {
              @Override
              public Response apply(Map<String, Object> responseMap) {
                logger.info(
                    request.getRequestContext(),
                    "SearchHandlerActor:handleOrgSearchAsyncRequest org search call ");
                Response response = new Response();
                response.put(JsonKey.RESPONSE, responseMap);
                responseMap.putAll(Util.getOrgDefaultValue());
                return response;
              }
            },
            getContext().dispatcher());
    Patterns.pipe(response, getContext().dispatcher()).to(sender());
    Request telemetryReq = new Request();
    telemetryReq.getRequest().put("context", request.getContext());
    telemetryReq.getRequest().put("searchFResponse", response);
    telemetryReq.getRequest().put("indexType", indexType);
    telemetryReq.getRequest().put("searchDto", searchDto);
    telemetryReq.setOperation("generateSearchTelemetry");
    tellToAnother(telemetryReq);
  }

  @SuppressWarnings("unchecked")
  private void updateUserDetailsWithOrgName(
      String requestedFields, List<Map<String, Object>> userMapList, RequestContext context) {
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
        orgMap = fetchOrgDetails(userMapList, filteredRequestedFields, context);
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
        logger.error(
            context,
            "SearchHandlerActor:updateUserDetailsWithOrgName: Exception occurred with error message = "
                + ex.getMessage(),
            ex);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Organisation> fetchOrgDetails(
      List<Map<String, Object>> userMapList,
      List<String> filteredRequestedFileds,
      RequestContext context) {
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
    List<Organisation> organisations =
        orgClient.esSearchOrgByIds(orgIds, filteredRequestedFileds, context);
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
    Integer topN =
        Integer.parseInt(PropertiesCache.getInstance().getProperty(JsonKey.SEARCH_TOP_N));

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
