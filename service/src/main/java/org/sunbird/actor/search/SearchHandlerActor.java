package org.sunbird.actor.search;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.dispatch.Mapper;
import org.apache.pekko.pattern.Patterns;
import java.util.*;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.organisation.validator.OrgTypeValidator;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.dto.SearchDTO;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.exception.ResponseMessage;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.organisation.OrgService;
import org.sunbird.service.organisation.impl.OrgServiceImpl;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.telemetry.dto.TelemetryEnvKey;
import org.sunbird.telemetry.util.TelemetryWriter;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.PropertiesCache;
import org.sunbird.util.UserUtility;
import org.sunbird.util.Util;
import org.sunbird.util.search.FuzzySearchManager;
import scala.concurrent.Future;

public class SearchHandlerActor extends BaseActor {

  private final OrgService orgService = OrgServiceImpl.getInstance();
  private final UserService userService = UserServiceImpl.getInstance();

  @Inject
  @Named("search_telemetry_actor")
  private ActorRef searchTelemetryGenerator;

  @Override
  public void onReceive(Request request) throws Throwable {
    request.toLower();
    Util.initializeContext(request, TelemetryEnvKey.USER);
    Map<String, Object> searchQueryMap = request.getRequest();
    if (MapUtils.isNotEmpty(searchQueryMap)
        && MapUtils.isNotEmpty(((Map<String, Object>) searchQueryMap.get(JsonKey.FILTERS)))) {
      ((Map<String, Object>) searchQueryMap.get(JsonKey.FILTERS)).remove(JsonKey.OBJECT_TYPE);
    }
    String operation = request.getOperation();
    switch (operation) {
      case "userSearch":
      case "userSearchV2":
      case "userSearchV3":
        handleUserSearch(request, searchQueryMap);
        break;
      case "orgSearch":
      case "orgSearchV2":
        handleOrgSearchAsyncRequest(searchQueryMap, request);
        break;
      default:
        onReceiveUnsupportedOperation();
    }
  }

  private void backwardCompatibility(Map<String, Object> searchQueryMap) {

    Map<String, Object> filterMap =
        (Map<String, Object>)
            searchQueryMap.get(
                JsonKey.FILTERS); // checks if profile user details is passed or not and calling
    // encryption method accordingly
    if (MapUtils.isNotEmpty(filterMap)) {
      if (StringUtils.isNotBlank((CharSequence) filterMap.get(JsonKey.USER_TYPE))) {
        filterMap.put(
            JsonKey.PROFILE_USERTYPES + "." + JsonKey.TYPE, filterMap.get(JsonKey.USER_TYPE));
        filterMap.remove(JsonKey.USER_TYPE);
      }
      if (StringUtils.isNotBlank((CharSequence) filterMap.get(JsonKey.USER_SUB_TYPE))) {
        filterMap.put(
            JsonKey.PROFILE_USERTYPES + "." + JsonKey.SUB_TYPE,
            filterMap.get(JsonKey.USER_SUB_TYPE));
        filterMap.remove(JsonKey.USER_SUB_TYPE);
      }
      if (filterMap.get(JsonKey.LOCATION_IDS) != null
          && filterMap.get(JsonKey.LOCATION_IDS) instanceof String) {
        if (StringUtils.isNotEmpty((CharSequence) filterMap.get(JsonKey.LOCATION_IDS))) {
          filterMap.put(
              JsonKey.PROFILE_LOCATION + "." + JsonKey.ID, filterMap.get(JsonKey.LOCATION_IDS));
          filterMap.remove(JsonKey.LOCATION_IDS);
        }
      } else if (filterMap.get(JsonKey.LOCATION_IDS) != null
          && filterMap.get(JsonKey.LOCATION_IDS) instanceof List) {
        if (CollectionUtils.isNotEmpty((List) filterMap.get(JsonKey.LOCATION_IDS))) {
          filterMap.put(
              JsonKey.PROFILE_LOCATION + "." + JsonKey.ID, filterMap.get(JsonKey.LOCATION_IDS));
          filterMap.remove(JsonKey.LOCATION_IDS);
        }
      }
    }
  }

  private void handleUserSearch(Request request, Map<String, Object> searchQueryMap)
      throws Exception {
    String searchVersion = request.getOperation();
    if (searchVersion.equalsIgnoreCase(ActorOperations.USER_SEARCH.getValue())) {
      // checking for Backward compatibility
      backwardCompatibility(searchQueryMap);
    }
    UserUtility.encryptUserSearchFilterQueryData(searchQueryMap);
    extractOrFilter(searchQueryMap);
    modifySearchQueryReqForNewRoleStructure(searchQueryMap);
    SearchDTO searchDto = ElasticSearchHelper.createSearchDTO(searchQueryMap);
    searchDto.setExcludedFields(Arrays.asList(ProjectUtil.excludes));
    Map<String, Object> result = userService.searchUser(searchDto, request.getRequestContext());
    Response response = new Response();
    // this fuzzy search Logic
    if (((List<Map<String, Object>>) result.get(JsonKey.CONTENT)).size() != 0
        && isFuzzySearchRequired(searchQueryMap)) {
      List<Map<String, Object>> responseList =
          getResponseOnFuzzyRequest(
              getFuzzyFilterMap(searchQueryMap),
              (List<Map<String, Object>>) result.get(JsonKey.CONTENT));
      if (responseList.isEmpty()) {
        throw new ProjectCommonException(
            ResponseCode.PARTIAL_SUCCESS_RESPONSE,
            String.format(ResponseMessage.Message.PARAM_NOT_MATCH, JsonKey.NAME.toUpperCase()),
            ResponseCode.PARTIAL_SUCCESS_RESPONSE.getResponseCode());
      }
      result.replace(JsonKey.COUNT, responseList.size());
      result.replace(JsonKey.CONTENT, responseList);
    }
    // Decrypt the data
    List<Map<String, Object>> userMapList = (List<Map<String, Object>>) result.get(JsonKey.CONTENT);
    List<String> fields = (List<String>) searchQueryMap.get(JsonKey.FIELDS);
    Map<String, Object> userDefaultFieldValue = Util.getUserDefaultValue();
    getDefaultValues(userDefaultFieldValue, fields);
    for (Map<String, Object> userMap : userMapList) {
      UserUtility.decryptUserDataFrmES(userMap);
      if (!searchVersion.equalsIgnoreCase(ActorOperations.USER_SEARCH_V3.getValue())) {
        updateUserSearchResponseWithOrgLevelRole(userMap, request.getRequestContext());
      }
      userMap.remove(JsonKey.ENC_EMAIL);
      userMap.remove(JsonKey.ENC_PHONE);
      Map<String, Object> userTypeDetail;
      List<String> locationIds;
      List<Map<String, String>> userLocList;
      if (request.getOperation().equalsIgnoreCase(ActorOperations.USER_SEARCH.getValue())) {
        if (userMap.containsKey(JsonKey.PROFILE_USERTYPE)) {
          if (MapUtils.isNotEmpty((Map<String, Object>) userMap.get(JsonKey.PROFILE_USERTYPE))) {
            userTypeDetail = (Map<String, Object>) userMap.get(JsonKey.PROFILE_USERTYPE);
            userMap.put(JsonKey.USER_TYPE, userTypeDetail.get(JsonKey.TYPE));
            userMap.put(JsonKey.USER_SUB_TYPE, userTypeDetail.get(JsonKey.SUB_TYPE));
          } else {
            userMap.put(JsonKey.USER_TYPE, null);
            userMap.put(JsonKey.USER_SUB_TYPE, null);
          }
        }
        if (userMap.containsKey(JsonKey.PROFILE_LOCATION)) {
          if (CollectionUtils.isNotEmpty(
              (List<Map<String, String>>) userMap.get(JsonKey.PROFILE_LOCATION))) {
            userLocList = (List<Map<String, String>>) userMap.get(JsonKey.PROFILE_LOCATION);
            locationIds =
                userLocList.stream().map(m -> m.get(JsonKey.ID)).collect(Collectors.toList());
            userMap.put(JsonKey.LOCATION_IDS, locationIds);
          } else {
            userMap.put(JsonKey.LOCATION_IDS, null);
          }
        }
        userMap.putAll(userDefaultFieldValue);
      } else {
        userMap.remove(JsonKey.USER_TYPE);
        userMap.remove(JsonKey.USER_SUB_TYPE);
        userMap.remove(JsonKey.LOCATION_IDS);
        Util.getUserDefaultValue().keySet().stream().forEach(key -> userMap.remove(key));
      }
    }
    String requestedFields = (String) request.getContext().get(JsonKey.FIELDS);
    updateUserDetailsWithOrgName(requestedFields, userMapList, request.getRequestContext());
    response.put(JsonKey.RESPONSE, result);
    sender().tell(response, self());
    generateSearchTelemetryEvent(
        searchDto, ProjectUtil.EsType.user.getTypeName(), result, request.getContext());
  }

  private void updateUserSearchResponseWithOrgLevelRole(
      Map<String, Object> userMap, RequestContext context) {
    try {
      List<Map<String, Object>> roles = (List<Map<String, Object>>) userMap.remove(JsonKey.ROLES);
      List<Map<String, Object>> organisations =
          (List<Map<String, Object>>) userMap.get(JsonKey.ORGANISATIONS);
      Map<String, Map<String, Object>> userOrgIdMap = new HashMap<>();
      organisations
          .stream()
          .forEach(
              org -> {
                org.put(JsonKey.ROLES, new ArrayList<String>());
                userOrgIdMap.put((String) org.get(JsonKey.ORGANISATION_ID), org);
              });
      if (CollectionUtils.isNotEmpty(roles)) {
        roles
            .stream()
            .forEach(
                role -> {
                  String userRole = (String) role.get(JsonKey.ROLE);
                  List<Map<String, String>> scopes =
                      (List<Map<String, String>>) role.get(JsonKey.SCOPE);
                  scopes
                      .stream()
                      .forEach(
                          scope -> {
                            String orgId = scope.get(JsonKey.ORGANISATION_ID);
                            Map<String, Object> userOrg = userOrgIdMap.get(orgId);
                            if (MapUtils.isNotEmpty(userOrg)) {
                              ((List) userOrg.get(JsonKey.ROLES)).add(userRole);
                            }
                          });
                });
      }
    } catch (Exception ex) {
      logger.error(
          context,
          "SearchHandlerActor:updateUserSearchResponseWithOrgLevelRole: Exception occurred with error message = "
              + ex.getMessage(),
          ex);
    }
    userMap.put(JsonKey.ROLES, new ArrayList<>());
  }

  private void modifySearchQueryReqForNewRoleStructure(Map<String, Object> searchQueryMap) {
    Map<String, Object> filterMap = (Map<String, Object>) searchQueryMap.get(JsonKey.FILTERS);
    Object roles = filterMap.remove(JsonKey.ORGANISATIONS + "." + JsonKey.ROLES);
    if (null != roles) {
      filterMap.put(JsonKey.ROLES + "." + JsonKey.ROLE, roles);
    }
  }

  private void handleOrgSearchAsyncRequest(Map<String, Object> searchQueryMap, Request request) {
    List<String> fields = (List<String>) searchQueryMap.get(JsonKey.FIELDS);
    Map<String, Object> filterMap = (Map<String, Object>) searchQueryMap.get(JsonKey.FILTERS);
    if (filterMap.containsKey(JsonKey.IS_SCHOOL)
        && BooleanUtils.isTrue((Boolean) filterMap.remove(JsonKey.IS_SCHOOL))) {
      filterMap.put(JsonKey.ORGANISATION_TYPE, 2);
    }

    if (ActorOperations.ORG_SEARCH.getValue().equalsIgnoreCase(request.getOperation())
        && filterMap.containsKey(JsonKey.IS_ROOT_ORG)
        && BooleanUtils.isTrue((Boolean) filterMap.remove(JsonKey.IS_ROOT_ORG))) {
      filterMap.put(JsonKey.IS_TENANT, true);
    }
    SearchDTO searchDto = ElasticSearchHelper.createSearchDTO(searchQueryMap);
    Future<Map<String, Object>> futureResponse =
        orgService.searchOrg(searchDto, request.getRequestContext());
    Future<Response> response =
        futureResponse.map(
            new Mapper<>() {
              @Override
              public Response apply(Map<String, Object> responseMap) {
                logger.debug(
                    request.getRequestContext(),
                    "SearchHandlerActor:handleOrgSearchAsyncRequest org search call ");
                Response response = new Response();
                Map<String, Object> orgDefaultFieldValue = new HashMap<>(Util.getOrgDefaultValue());
                getDefaultValues(orgDefaultFieldValue, fields);
                List<Map<String, Object>> contents =
                    (List<Map<String, Object>>) responseMap.get(JsonKey.CONTENT);
                contents
                    .stream()
                    .forEach(
                        org -> {
                          if (request
                              .getOperation()
                              .equalsIgnoreCase(ActorOperations.ORG_SEARCH_V2.getValue())) {
                            Util.getOrgDefaultValue()
                                .keySet()
                                .stream()
                                .forEach(key -> org.remove(key));
                            org.remove(JsonKey.LOCATION_IDS);
                          } else {
                            // Put all default value for backward compatibility
                            org.putAll(orgDefaultFieldValue);
                            org.put(JsonKey.IS_ROOT_ORG, org.get(JsonKey.IS_TENANT));
                          }
                          if ((CollectionUtils.isNotEmpty(fields)
                                  && fields.contains(JsonKey.HASHTAGID))
                              || (CollectionUtils.isEmpty(fields))) {
                            org.put(JsonKey.HASHTAGID, org.get(JsonKey.ID));
                          }
                          if (null != org.get(JsonKey.ORGANISATION_TYPE)) {
                            int orgType = (int) org.get(JsonKey.ORGANISATION_TYPE);
                            boolean isSchool =
                                (orgType
                                        == OrgTypeValidator.getInstance().getValueByType(JsonKey.ORG_TYPE_SCHOOL))
                                    ? true
                                    : false;
                            org.put(JsonKey.IS_SCHOOL, isSchool);
                          }
                        });
                response.put(JsonKey.RESPONSE, responseMap);
                return response;
              }
            },
            getContext().dispatcher());
    Patterns.pipe(response, getContext().dispatcher()).to(sender());
    Request telemetryReq = new Request();
    telemetryReq.getRequest().put("context", request.getContext());
    telemetryReq.getRequest().put("searchFResponse", response);
    telemetryReq.getRequest().put("indexType", ProjectUtil.EsType.organisation.getTypeName());
    telemetryReq.getRequest().put("searchDto", searchDto);
    telemetryReq.setOperation("generateSearchTelemetry");
    try {
      searchTelemetryGenerator.tell(telemetryReq, self());
    } catch (Exception ex) {
      logger.error("Exception while saving telemetry", ex);
    }
  }

  private void getDefaultValues(Map<String, Object> orgDefaultFieldValue, List<String> fields) {
    if (CollectionUtils.isNotEmpty(fields)) {
      Iterator<Map.Entry<String, Object>> iterator = orgDefaultFieldValue.entrySet().iterator();
      while (iterator.hasNext()) {
        Map.Entry<String, Object> entry = iterator.next();
        if (!fields.contains(entry.getKey())) {
          iterator.remove();
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void updateUserDetailsWithOrgName(
      String requestedFields, List<Map<String, Object>> userMapList, RequestContext context) {
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
        if (fields.contains(JsonKey.ORG_NAME.toLowerCase())) {
          Map<String, Map<String, Object>> filteredOrg = fetchOrgDetails(userMapList, context);
          userMapList
              .stream()
              .forEach(
                  userMap -> {
                    String rootOrgId = (String) userMap.get(JsonKey.ROOT_ORG_ID);
                    if (StringUtils.isNotBlank(rootOrgId)) {
                      Map<String, Object> org = filteredOrg.get(rootOrgId);
                      if (null != org) {
                        userMap.put(JsonKey.ROOT_ORG_NAME, org.get(JsonKey.ORG_NAME));
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
                                  Map<String, Object> org = filteredOrg.get(userOrgId);
                                  if (null != org) {
                                    userOrg.put(JsonKey.ORG_NAME, org.get(JsonKey.ORG_NAME));
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
  private Map<String, Map<String, Object>> fetchOrgDetails(
      List<Map<String, Object>> userMapList, RequestContext context) {
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
    List<Map<String, Object>> organisations = orgService.getOrgByIds(orgIds, context);
    Map<String, Map<String, Object>> orgMap = new HashMap<>();
    organisations
        .stream()
        .forEach(
            org -> {
              orgMap.put((String) org.get(JsonKey.ID), org);
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
