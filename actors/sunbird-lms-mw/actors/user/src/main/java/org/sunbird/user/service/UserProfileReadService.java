package org.sunbird.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerUtil;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.organisation.dao.OrgDao;
import org.sunbird.learner.organisation.dao.impl.OrgDaoImpl;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.UserFlagUtil;
import org.sunbird.learner.util.UserUtility;
import org.sunbird.learner.util.Util;
import org.sunbird.user.dao.UserDao;
import org.sunbird.user.dao.UserOrgDao;
import org.sunbird.user.dao.impl.UserDaoImpl;
import org.sunbird.user.dao.impl.UserOrgDaoImpl;
import org.sunbird.user.service.impl.UserExternalIdentityServiceImpl;
import org.sunbird.user.service.impl.UserServiceImpl;
import org.sunbird.user.util.UserUtil;
import scala.concurrent.Future;

public class UserProfileReadService {

  private LoggerUtil logger = new LoggerUtil(UserProfileReadService.class);
  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private Util.DbInfo locationDbInfo = Util.dbInfoMap.get(JsonKey.LOCATION);
  private UserService userService = UserServiceImpl.getInstance();
  private UserTncService tncService = new UserTncService();
  private UserOrgDao userOrgDao = UserOrgDaoImpl.getInstance();
  private OrgDao orgDao = OrgDaoImpl.getInstance();
  private Util.DbInfo OrgDb = Util.dbInfoMap.get(JsonKey.ORG_DB);
  private UserDao userDao = UserDaoImpl.getInstance();
  private UserExternalIdentityService userExternalIdentityService =
      new UserExternalIdentityServiceImpl();
  private ElasticSearchService esUtil = EsClientFactory.getInstance(JsonKey.REST);
  private ObjectMapper mapper = new ObjectMapper();

  public Response getUserProfileData(Request actorMessage) {
    String id = (String) actorMessage.getRequest().get(JsonKey.USER_ID);
    String idType = (String) actorMessage.getContext().get(JsonKey.ID_TYPE);
    String provider = (String) actorMessage.getContext().get(JsonKey.PROVIDER);
    String userId;
    // Check whether its normal read by id call or read by externalId call
    validateProviderAndIdType(provider, idType);
    if (StringUtils.isNotBlank(provider)) {
      userId = getUserIdByExternalId(actorMessage, id, idType, provider);
    } else {
      userId = id;
    }
    Map<String, Object> result =
        validateUserIdAndGetUserDetails(userId, actorMessage.getRequestContext());
    result.put(
        JsonKey.ROOT_ORG,
        orgDao.getOrgById(
            (String) result.get(JsonKey.ROOT_ORG_ID), actorMessage.getRequestContext()));
    result.put(
        JsonKey.ORGANISATIONS,
        fetchUserOrgList((String) result.get(JsonKey.USER_ID), actorMessage.getRequestContext()));
    String requestedById =
        (String) actorMessage.getContext().getOrDefault(JsonKey.REQUESTED_BY, "");
    String managedForId = (String) actorMessage.getContext().getOrDefault(JsonKey.MANAGED_FOR, "");
    String managedBy = (String) result.get(JsonKey.MANAGED_BY);
    logger.info(
        actorMessage.getRequestContext(),
        "requested By and requested user id == "
            + requestedById
            + "  "
            + userId
            + " managedForId= "
            + managedForId
            + " managedBy "
            + managedBy);
    if (StringUtils.isNotEmpty(managedBy) && !managedBy.equals(requestedById)) {
      ProjectCommonException.throwUnauthorizedErrorException();
    }

    getManagedToken(actorMessage, userId, result, managedBy);

    String requestFields = (String) actorMessage.getContext().get(JsonKey.FIELDS);
    if (StringUtils.isNotBlank(userId)
        && (userId.equalsIgnoreCase(requestedById) || userId.equalsIgnoreCase(managedForId))
        && StringUtils.isBlank(requestFields)) {
      result.put(
          JsonKey.EXTERNAL_IDS,
          fetchUserExternalIdentity(userId, result, true, actorMessage.getRequestContext()));
    }
    if (StringUtils.isNotBlank((String) actorMessage.getContext().get(JsonKey.FIELDS))) {
      addExtraFieldsInUserProfileResponse(result, requestFields, actorMessage.getRequestContext());
    }
    UserUtility.decryptUserDataFrmES(result);
    updateTnc(result);
    if (null != result.get(JsonKey.ALL_TNC_ACCEPTED)) {
      result.put(
          JsonKey.ALL_TNC_ACCEPTED,
          tncService.convertTncStringToJsonMap(
              (Map<String, String>) result.get(JsonKey.ALL_TNC_ACCEPTED)));
    }
    addFlagValue(result);
    // For Backward compatibility , In ES we were sending identifier field
    result.put(JsonKey.IDENTIFIER, userId);
    Response response = new Response();
    response.put(JsonKey.RESPONSE, result);
    return response;
  }

  private void addFlagValue(Map<String, Object> userDetails) {
    int flagsValue = Integer.parseInt(userDetails.get(JsonKey.FLAGS_VALUE).toString());
    Map<String, Boolean> userFlagMap = UserFlagUtil.assignUserFlagValues(flagsValue);
    userDetails.putAll(userFlagMap);
  }

  private Map<String, Object> getManagedToken(
      Request actorMessage, String userId, Map<String, Object> result, String managedBy) {
    boolean withTokens =
        Boolean.parseBoolean((String) actorMessage.getContext().get(JsonKey.WITH_TOKENS));

    if (withTokens && StringUtils.isNotEmpty(managedBy)) {
      String managedToken = (String) actorMessage.getContext().get(JsonKey.MANAGED_TOKEN);
      if (StringUtils.isEmpty(managedToken)) {
        logger.info(
            actorMessage.getRequestContext(),
            "UserProfileReadActor: getUserProfileData: calling token generation for: " + userId);
        List<Map<String, Object>> userList = new ArrayList<>();
        userList.add(result);
        // Fetch encrypted token from admin utils
        Map<String, Object> encryptedTokenList =
            userService.fetchEncryptedToken(managedBy, userList, actorMessage.getRequestContext());
        // encrypted token for each managedUser in respList
        userService.appendEncryptedToken(
            encryptedTokenList, userList, actorMessage.getRequestContext());
        result = userList.get(0);
      } else {
        result.put(JsonKey.MANAGED_TOKEN, managedToken);
      }
    }
    return result;
  }

  private List<Map<String, Object>> fetchUserOrgList(String userId, RequestContext requestContext) {
    Response response = userOrgDao.getUserOrgListByUserId(userId, requestContext);
    List<Map<String, Object>> userOrgList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    List<Map<String, Object>> usrOrgList = new ArrayList<>();
    for (Map<String, Object> userOrg : userOrgList) {
      Boolean isDeleted = (Boolean) userOrg.get(JsonKey.IS_DELETED);
      if (null == isDeleted || (null != isDeleted && !isDeleted.booleanValue())) {
        usrOrgList.add(userOrg);
      }
    }
    return usrOrgList;
  }

  private Map<String, Object> validateUserIdAndGetUserDetails(
      String userId, RequestContext context) {
    Map<String, Object> user = userDao.getUserDetailsById(userId, context);
    // check user found or not
    if (MapUtils.isEmpty(user)) {
      throw new ProjectCommonException(
          ResponseCode.userNotFound.getErrorCode(),
          ResponseCode.userNotFound.getErrorMessage(),
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }
    // check whether is_deletd true or false
    Boolean isDeleted = (Boolean) user.get(JsonKey.IS_DELETED);
    if (null != isDeleted && isDeleted.booleanValue()) {
      ProjectCommonException.throwClientErrorException(ResponseCode.userAccountlocked);
    }
    removeUserPrivateField(user);
    return user;
  }

  private String getUserIdByExternalId(
      Request actorMessage, String id, String idType, String provider) {
    String userId =
        userExternalIdentityService.getUserV1(
            id, provider, idType, actorMessage.getRequestContext());
    if (userId == null) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.externalIdNotFound,
          ProjectUtil.formatMessage(
              ResponseCode.externalIdNotFound.getErrorMessage(), id, idType, provider));
    }
    return userId;
  }

  private void validateProviderAndIdType(String provider, String idType) {
    if (StringUtils.isNotBlank(provider) && StringUtils.isBlank(idType)) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.mandatoryParamsMissing,
          MessageFormat.format(
              ResponseCode.mandatoryParamsMissing.getErrorMessage(), JsonKey.ID_TYPE));
    }
  }

  private Map<String, Object> removeUserPrivateField(Map<String, Object> responseMap) {
    for (int i = 0; i < ProjectUtil.excludes.length; i++) {
      responseMap.remove(ProjectUtil.excludes[i]);
    }
    responseMap.remove(JsonKey.ENC_EMAIL);
    responseMap.remove(JsonKey.ENC_PHONE);
    responseMap.remove(JsonKey.ADDRESS);
    return responseMap;
  }

  private List<Map<String, Object>> fetchUserDeclarations(String userId, RequestContext context) {
    Map<String, Object> req = new HashMap<>();
    req.put(JsonKey.USER_ID, userId);
    Response response =
        cassandraOperation.getRecordById(
            JsonKey.SUNBIRD, JsonKey.USR_DECLARATION_TABLE, req, context);
    List<Map<String, Object>> resExternalIds;
    List<Map<String, Object>> finalRes = new ArrayList<>();
    if (null != response && null != response.getResult()) {
      resExternalIds = (List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE);
      if (CollectionUtils.isNotEmpty(resExternalIds)) {
        resExternalIds.forEach(
            item -> {
              Map<String, Object> declaration = new HashMap<>();
              Map<String, String> declaredFields =
                  (Map<String, String>) item.get(JsonKey.USER_INFO);
              if (MapUtils.isNotEmpty(declaredFields)) {
                decryptDeclarationFields(declaredFields, context);
              }
              declaration.put(JsonKey.STATUS, item.get(JsonKey.STATUS));
              declaration.put(JsonKey.ERROR_TYPE, item.get(JsonKey.ERROR_TYPE));
              declaration.put(JsonKey.ORG_ID, item.get(JsonKey.ORG_ID));
              declaration.put(JsonKey.PERSONA, item.get(JsonKey.PERSONA));
              declaration.put(JsonKey.INFO, declaredFields);
              finalRes.add(declaration);
            });
      }
    }
    return finalRes;
  }

  private Map<String, String> decryptDeclarationFields(
      Map<String, String> declaredFields, RequestContext context) {
    if (declaredFields.containsKey(JsonKey.DECLARED_EMAIL)) {
      declaredFields.put(
          JsonKey.DECLARED_EMAIL,
          UserUtil.getDecryptedData(declaredFields.get(JsonKey.DECLARED_EMAIL), context));
    }
    if (declaredFields.containsKey(JsonKey.DECLARED_PHONE)) {
      declaredFields.put(
          JsonKey.DECLARED_PHONE,
          UserUtil.getDecryptedData(declaredFields.get(JsonKey.DECLARED_PHONE), context));
    }
    return declaredFields;
  }

  private void decryptUserExternalIds(
      List<Map<String, String>> dbResExternalIds, RequestContext context) {
    if (CollectionUtils.isNotEmpty(dbResExternalIds)) {
      dbResExternalIds
          .stream()
          .forEach(
              s -> {
                s.put(JsonKey.ID, s.get(JsonKey.ORIGINAL_EXTERNAL_ID));
                s.put(JsonKey.ID_TYPE, s.get(JsonKey.ORIGINAL_ID_TYPE));
                s.put(JsonKey.PROVIDER, s.get(JsonKey.ORIGINAL_PROVIDER));
                if (StringUtils.isNotBlank(s.get(JsonKey.ORIGINAL_EXTERNAL_ID))
                    && StringUtils.isNotBlank(s.get(JsonKey.ORIGINAL_ID_TYPE))
                    && StringUtils.isNotBlank(s.get(JsonKey.ORIGINAL_PROVIDER))) {
                  if (JsonKey.DECLARED_EMAIL.equals(s.get(JsonKey.ORIGINAL_ID_TYPE))
                      || JsonKey.DECLARED_PHONE.equals(s.get(JsonKey.ORIGINAL_ID_TYPE))) {

                    String decrytpedOriginalExternalId =
                        UserUtil.getDecryptedData(s.get(JsonKey.ORIGINAL_EXTERNAL_ID), context);
                    s.put(JsonKey.ID, decrytpedOriginalExternalId);

                  } else if (JsonKey.DECLARED_DISTRICT.equals(s.get(JsonKey.ORIGINAL_ID_TYPE))
                      || JsonKey.DECLARED_STATE.equals(s.get(JsonKey.ORIGINAL_ID_TYPE))) {
                    List<String> locationIds = new ArrayList<>(2);
                    locationIds.add(s.get(JsonKey.ORIGINAL_EXTERNAL_ID));
                    List<Map<String, Object>> locationList = getUserLocations(locationIds, context);
                    if (CollectionUtils.isNotEmpty(locationList)) {
                      Map<String, Object> location = locationList.get(0);
                      s.put(
                          JsonKey.ID,
                          (location == null
                              ? s.get(JsonKey.ORIGINAL_EXTERNAL_ID)
                              : (String) location.get(JsonKey.CODE)));
                    }
                  }
                }

                s.remove(JsonKey.EXTERNAL_ID);
                s.remove(JsonKey.ORIGINAL_EXTERNAL_ID);
                s.remove(JsonKey.ORIGINAL_ID_TYPE);
                s.remove(JsonKey.ORIGINAL_PROVIDER);
                s.remove(JsonKey.CREATED_BY);
                s.remove(JsonKey.LAST_UPDATED_BY);
                s.remove(JsonKey.LAST_UPDATED_ON);
                s.remove(JsonKey.CREATED_ON);
                s.remove(JsonKey.USER_ID);
                s.remove(JsonKey.SLUG);
              });
    }
  }

  public void updateTnc(Map<String, Object> userMap) {
    Map<String, Object> tncConfigMap = null;
    try {
      String tncValue = DataCacheHandler.getConfigSettings().get(JsonKey.TNC_CONFIG);
      tncConfigMap = mapper.readValue(tncValue, Map.class);

    } catch (Exception e) {
      logger.error(
          "UserProfileReadActor:updateTncInfo: Exception occurred while getting system setting for"
              + JsonKey.TNC_CONFIG
              + e.getMessage(),
          e);
    }

    if (MapUtils.isNotEmpty(tncConfigMap)) {
      try {
        String tncLatestVersion = (String) tncConfigMap.get(JsonKey.LATEST_VERSION);
        userMap.put(JsonKey.TNC_LATEST_VERSION, tncLatestVersion);
        String tncUserAcceptedVersion = (String) userMap.get(JsonKey.TNC_ACCEPTED_VERSION);
        Object tncUserAcceptedOn = userMap.get(JsonKey.TNC_ACCEPTED_ON);
        userMap.put(JsonKey.PROMPT_TNC, false);
        String url = (String) ((Map) tncConfigMap.get(tncLatestVersion)).get(JsonKey.URL);
        logger.info("UserProfileReadActor:updateTncInfo: url = " + url);
        userMap.put(JsonKey.TNC_LATEST_VERSION_URL, url);
        if ((StringUtils.isEmpty(tncUserAcceptedVersion)
                || !tncUserAcceptedVersion.equalsIgnoreCase(tncLatestVersion)
                || (null == tncUserAcceptedOn))
            && (tncConfigMap.containsKey(tncLatestVersion))) {
          userMap.put(JsonKey.PROMPT_TNC, true);
        }
      } catch (Exception e) {
        logger.error(
            "UserProfileReadActor:updateTncInfo: Exception occurred with error message = "
                + e.getMessage(),
            e);
      }
    }
  }

  public List<Map<String, String>> fetchUserExternalIdentity(
      String userId, Map<String, Object> user, boolean mergeDeclarations, RequestContext context) {
    try {
      List<Map<String, String>> dbResExternalIds =
          UserUtil.getExternalIds(userId, mergeDeclarations, context);

      decryptUserExternalIds(dbResExternalIds, context);
      // update orgId to provider in externalIds
      String rootOrgId = (String) user.get(JsonKey.ROOT_ORG_ID);
      if (CollectionUtils.isNotEmpty(dbResExternalIds)
          && StringUtils.isNotBlank(rootOrgId)
          && StringUtils.isNotBlank(dbResExternalIds.get(0).get(JsonKey.PROVIDER))
          && ((dbResExternalIds.get(0).get(JsonKey.PROVIDER)).equalsIgnoreCase(rootOrgId))) {

        String provider = (String) user.get(JsonKey.CHANNEL);
        dbResExternalIds
            .stream()
            .forEach(
                s -> {
                  if (s.get(JsonKey.PROVIDER) != null
                      && s.get(JsonKey.PROVIDER).equals(s.get(JsonKey.ID_TYPE))) {
                    s.put(JsonKey.ID_TYPE, provider);
                  }
                  s.put(JsonKey.PROVIDER, provider);
                });

      } else {
        UserUtil.updateExternalIdsWithProvider(dbResExternalIds, context);
      }
      return dbResExternalIds;
    } catch (Exception ex) {
      logger.error(
          context, "Exception occurred while fetching user externalId. " + ex.getMessage(), ex);
    }
    return new ArrayList<>();
  }

  public void addExtraFieldsInUserProfileResponse(
      Map<String, Object> result, String fields, RequestContext context) {
    if (!StringUtils.isBlank(fields)) {
      result.put(JsonKey.LAST_LOGIN_TIME, Long.parseLong("0"));
      if (fields.contains(JsonKey.TOPIC)) {
        fetchTopicOfAssociatedOrgs(result, context);
      }
      if (fields.contains(JsonKey.ORGANISATIONS)) {
        updateUserOrgInfo((List) result.get(JsonKey.ORGANISATIONS), context);
      }
      if (fields.contains(JsonKey.ROLES)) {
        result.put(JsonKey.ROLE_LIST, DataCacheHandler.getUserReadRoleList());
      }
      if (fields.contains(JsonKey.LOCATIONS)) {
        List<Map<String, Object>> userLocations =
            getUserLocations((List<String>) result.get(JsonKey.LOCATION_IDS), context);
        if (CollectionUtils.isNotEmpty(userLocations)) {
          result.put(
              JsonKey.USER_LOCATIONS,
              getUserLocations((List<String>) result.get(JsonKey.LOCATION_IDS), context));

          addSchoolLocation(result, context);
          result.remove(JsonKey.LOCATION_IDS);
        }
      }
      if (fields.contains(JsonKey.DECLARATIONS)) {
        List<Map<String, Object>> declarations =
            fetchUserDeclarations((String) result.get(JsonKey.ID), context);
        result.put(JsonKey.DECLARATIONS, declarations);
      }
      if (CollectionUtils.isEmpty((List<Map<String, String>>) result.get(JsonKey.EXTERNAL_IDS))
          && fields.contains(JsonKey.EXTERNAL_IDS)) {
        List<Map<String, String>> resExternalIds =
            fetchUserExternalIdentity((String) result.get(JsonKey.ID), result, false, context);
        result.put(JsonKey.EXTERNAL_IDS, resExternalIds);
      }
    }
  }

  private void addSchoolLocation(Map<String, Object> result, RequestContext context) {
    String rootOrgId = (String) result.get(JsonKey.ROOT_ORG_ID);
    List<Map<String, Object>> organisations =
        (List<Map<String, Object>>) result.get(JsonKey.ORGANISATIONS);
    List<Map<String, Object>> userLocation =
        (List<Map<String, Object>>) result.get(JsonKey.USER_LOCATIONS);
    // inorder to add school, user should have sub-org and attached to locations hierarchy as parent
    // block/cluster
    if (CollectionUtils.isNotEmpty(organisations)
        && organisations.size() > 1
        && userLocation.size() >= 3) {
      for (int i = 0; i < organisations.size(); i++) {
        String organisationId = (String) organisations.get(i).get(JsonKey.ORGANISATION_ID);
        if (StringUtils.isNotBlank(organisationId) && !organisationId.equalsIgnoreCase(rootOrgId)) {
          Map<String, Object> filterMap = new HashMap<>();
          Map<String, Object> searchQueryMap = new HashMap<>();
          filterMap.put(JsonKey.NAME, organisations.get(i).get(JsonKey.ORG_NAME));
          filterMap.put(JsonKey.TYPE, JsonKey.LOCATION_TYPE_SCHOOL);
          filterMap.put(JsonKey.CODE, organisations.get(i).get(JsonKey.EXTERNAL_ID));
          searchQueryMap.put(JsonKey.FILTERS, filterMap);
          Map<String, Object> schoolLocation = searchLocation(searchQueryMap, context);
          if (MapUtils.isNotEmpty(schoolLocation)) {
            userLocation.add(schoolLocation);
          }
        }
      }
    }
  }

  public Map<String, Object> searchLocation(
      Map<String, Object> searchQueryMap, RequestContext context) {
    SearchDTO searchDto = Util.createSearchDto(searchQueryMap);
    String type = ProjectUtil.EsType.location.getTypeName();
    Future<Map<String, Object>> resultF = esUtil.search(searchDto, type, context);
    Map<String, Object> result =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
    if (MapUtils.isNotEmpty(result)
        && CollectionUtils.isNotEmpty((List<Map<String, Object>>) result.get(JsonKey.CONTENT))) {
      return ((List<Map<String, Object>>) result.get(JsonKey.CONTENT)).get(0);
    }
    return Collections.emptyMap();
  }

  private List<Map<String, Object>> getUserLocations(
      List<String> locationIds, RequestContext context) {
    if (CollectionUtils.isNotEmpty(locationIds)) {
      List<String> locationFields =
          Arrays.asList(JsonKey.CODE, JsonKey.NAME, JsonKey.TYPE, JsonKey.PARENT_ID, JsonKey.ID);
      Response locationResponse =
          cassandraOperation.getPropertiesValueById(
              locationDbInfo.getKeySpace(),
              locationDbInfo.getTableName(),
              locationIds,
              locationFields,
              context);
      return (List<Map<String, Object>>) locationResponse.get(JsonKey.RESPONSE);
    }
    return new ArrayList<>();
  }

  private void updateUserOrgInfo(List<Map<String, Object>> userOrgs, RequestContext context) {
    Map<String, Map<String, Object>> orgInfoMap = fetchAllOrgById(userOrgs, context);
    Map<String, Map<String, Object>> locationInfoMap = fetchAllLocationsById(orgInfoMap, context);
    prepUserOrgInfoWithAdditionalData(userOrgs, orgInfoMap, locationInfoMap);
  }

  private Map<String, Map<String, Object>> fetchAllOrgById(
      List<Map<String, Object>> userOrgs, RequestContext context) {
    List<String> orgIds =
        userOrgs
            .stream()
            .map(m -> (String) m.get(JsonKey.ORGANISATION_ID))
            .distinct()
            .collect(Collectors.toList());
    if (CollectionUtils.isNotEmpty(orgIds)) {
      List<String> fields =
          Arrays.asList(
              JsonKey.ORG_NAME,
              JsonKey.CHANNEL,
              JsonKey.HASHTAGID,
              JsonKey.LOCATION_IDS,
              JsonKey.ID,
              JsonKey.EXTERNAL_ID);
      Response userOrgResponse =
          cassandraOperation.getPropertiesValueById(
              OrgDb.getKeySpace(), OrgDb.getTableName(), orgIds, fields, context);
      List<Map<String, Object>> userOrgResponseList =
          (List<Map<String, Object>>) userOrgResponse.get(JsonKey.RESPONSE);
      if (CollectionUtils.isNotEmpty(userOrgResponseList)) {
        return userOrgResponseList
            .stream()
            .collect(Collectors.toMap(obj -> (String) obj.get("id"), val -> val));
      }
    }
    return new HashMap<>();
  }

  private Map<String, Map<String, Object>> fetchAllLocationsById(
      Map<String, Map<String, Object>> orgInfoMap, RequestContext context) {

    Set<String> locationSet = new HashSet<>();
    for (Map<String, Object> org : orgInfoMap.values()) {
      List<String> locationIds = (List<String>) org.get(JsonKey.LOCATION_IDS);
      if (CollectionUtils.isNotEmpty(locationIds)) {
        locationIds.forEach(
            locId -> {
              if (StringUtils.isNotBlank(locId)) {
                locationSet.add(locId);
              }
            });
      }
    }
    if (CollectionUtils.isNotEmpty(locationSet)) {
      List<String> locList = new ArrayList<>(locationSet);
      List<Map<String, Object>> locationResponseList = getUserLocations(locList, context);
      return locationResponseList
          .stream()
          .collect(Collectors.toMap(obj -> (String) obj.get("id"), val -> val));
    } else {
      return new HashMap<>();
    }
  }

  private void prepUserOrgInfoWithAdditionalData(
      List<Map<String, Object>> userOrgs,
      Map<String, Map<String, Object>> orgInfoMap,
      Map<String, Map<String, Object>> locationInfoMap) {
    for (Map<String, Object> usrOrg : userOrgs) {
      Map<String, Object> orgInfo = orgInfoMap.get(usrOrg.get(JsonKey.ORGANISATION_ID));
      if (MapUtils.isNotEmpty(orgInfo)) {
        usrOrg.put(JsonKey.ORG_NAME, orgInfo.get(JsonKey.ORG_NAME));
        usrOrg.put(JsonKey.CHANNEL, orgInfo.get(JsonKey.CHANNEL));
        usrOrg.put(JsonKey.HASHTAGID, orgInfo.get(JsonKey.HASHTAGID));
        usrOrg.put(JsonKey.LOCATION_IDS, orgInfo.get(JsonKey.LOCATION_IDS));
        usrOrg.put(JsonKey.EXTERNAL_ID, orgInfo.get(JsonKey.EXTERNAL_ID));
        if (MapUtils.isNotEmpty(locationInfoMap)) {
          usrOrg.put(
              JsonKey.LOCATIONS,
              prepLocationFields(
                  (List<String>) orgInfo.get(JsonKey.LOCATION_IDS), locationInfoMap));
        }
      }
    }
  }

  private List<Map<String, Object>> prepLocationFields(
      List<String> locationIds, Map<String, Map<String, Object>> locationInfoMap) {
    List<Map<String, Object>> retList = new ArrayList<>();
    if (locationIds != null) {
      for (String locationId : locationIds) {
        retList.add(locationInfoMap.get(locationId));
      }
    }
    return retList;
  }

  private void fetchTopicOfAssociatedOrgs(Map<String, Object> result, RequestContext context) {
    Set<String> topicSet = new HashSet<>();
    List<Map<String, Object>> userOrgList =
        (List<Map<String, Object>>) result.get(JsonKey.ORGANISATIONS);
    if (CollectionUtils.isNotEmpty(userOrgList)) {
      List<String> orgIdList =
          userOrgList
              .stream()
              .map(m -> (String) m.get(JsonKey.ORGANISATION_ID))
              .distinct()
              .collect(Collectors.toList());

      // fetch all org details from cassandra ...
      if (CollectionUtils.isNotEmpty(orgIdList)) {
        List<String> orgfields = new ArrayList<>();
        orgfields.add(JsonKey.ID);
        orgfields.add(JsonKey.LOCATION_ID);
        Response userOrgResponse =
            cassandraOperation.getPropertiesValueById(
                OrgDb.getKeySpace(), OrgDb.getTableName(), orgIdList, orgfields, context);
        List<Map<String, Object>> orgResponseList =
            (List<Map<String, Object>>) userOrgResponse.get(JsonKey.RESPONSE);

        if (CollectionUtils.isNotEmpty(orgResponseList)) {
          List<String> locationIdList = new ArrayList<>();
          for (Map<String, Object> org : orgResponseList) {
            String locId = (String) org.get(JsonKey.LOCATION_ID);
            if (StringUtils.isNotBlank(locId)) {
              locationIdList.add(locId);
            }
          }
          if (CollectionUtils.isNotEmpty(locationIdList)) {
            List<String> geoLocationFields = new ArrayList<>();
            geoLocationFields.add(JsonKey.TOPIC);
            Util.DbInfo geoLocationDbInfo = Util.dbInfoMap.get(JsonKey.GEO_LOCATION_DB);
            Response geoLocationResponse =
                cassandraOperation.getPropertiesValueById(
                    geoLocationDbInfo.getKeySpace(),
                    geoLocationDbInfo.getTableName(),
                    locationIdList,
                    geoLocationFields,
                    context);
            List<Map<String, Object>> geoLocationResponseList =
                (List<Map<String, Object>>) geoLocationResponse.get(JsonKey.RESPONSE);
            List<String> topicList =
                geoLocationResponseList
                    .stream()
                    .map(m -> (String) m.get(JsonKey.TOPIC))
                    .distinct()
                    .collect(Collectors.toList());
            topicSet.addAll(topicList);
          }
        }
      }
    }
    result.put(JsonKey.TOPICS, topicSet);
  }
}
