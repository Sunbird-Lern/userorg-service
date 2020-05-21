package org.sunbird.user.actors;

import static org.sunbird.learner.util.Util.isNotNull;

import akka.actor.ActorRef;
import akka.dispatch.Mapper;
import akka.pattern.Patterns;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.text.MessageFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.actorutil.systemsettings.SystemSettingClient;
import org.sunbird.actorutil.systemsettings.impl.SystemSettingClientImpl;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.*;
import org.sunbird.common.models.util.ProjectUtil.EsType;
import org.sunbird.common.models.util.datasecurity.EncryptionService;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.UserUtility;
import org.sunbird.learner.util.Util;
import org.sunbird.models.user.User;
import org.sunbird.services.sso.SSOManager;
import org.sunbird.services.sso.SSOServiceFactory;
import org.sunbird.user.dao.UserDao;
import org.sunbird.user.dao.impl.UserDaoImpl;
import org.sunbird.user.dao.impl.UserExternalIdentityDaoImpl;
import org.sunbird.user.util.UserUtil;
import scala.Tuple2;
import scala.concurrent.Await;
import scala.concurrent.Future;

@ActorConfig(
  tasks = {
    "getUserDetailsByLoginId",
    "getUserProfile",
    "getUserProfileV2",
    "getUserByKey",
    "checkUserExistence"
  },
  asyncTasks = {}
)
public class UserProfileReadActor extends BaseActor {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private EncryptionService encryptionService =
      org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getEncryptionServiceInstance(
          null);
  private Util.DbInfo userOrgDbInfo = Util.dbInfoMap.get(JsonKey.USER_ORG_DB);
  private Util.DbInfo geoLocationDbInfo = Util.dbInfoMap.get(JsonKey.GEO_LOCATION_DB);
  private ActorRef systemSettingActorRef = null;
  private UserExternalIdentityDaoImpl userExternalIdentityDao = new UserExternalIdentityDaoImpl();
  private ElasticSearchService esUtil = getESInstance();
  private static ObjectMapper mapper = new ObjectMapper();

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    ExecutionContext.setRequestId(request.getRequestId());
    if (systemSettingActorRef == null) {
      systemSettingActorRef = getActorRef(ActorOperations.GET_SYSTEM_SETTING.getValue());
    }
    String operation = request.getOperation();
    switch (operation) {
      case "getUserProfile":
        getUserProfile(request);
        break;
      case "getUserProfileV2":
        getUserProfileV2(request);
        break;
      case "getUserDetailsByLoginId":
        getUserDetailsByLoginId(request);
        break;
      case "getUserByKey":
        getKey(request);
        break;
      case "checkUserExistence":
        checkUserExistence(request);
        break;
      default:
        onReceiveUnsupportedOperation("UserProfileReadActor");
    }
  }

  /**
   * Method to get user profile (version 1).
   *
   * @param actorMessage Request containing user ID
   */
  private void getUserProfile(Request actorMessage) {
    Response response = getUserProfileData(actorMessage);
    sender().tell(response, self());
  }

  private Response getUserProfileData(Request actorMessage) {
    Map<String, Object> userMap = actorMessage.getRequest();
    String id = (String) userMap.get(JsonKey.USER_ID);
    String userId;
    String provider = (String) actorMessage.getContext().get(JsonKey.PROVIDER);
    String idType = (String) actorMessage.getContext().get(JsonKey.ID_TYPE);
    boolean showMaskedData = false;
    if (!StringUtils.isEmpty(provider)) {
      if (StringUtils.isEmpty(idType)) {
        userId = null;
        ProjectCommonException.throwClientErrorException(
            ResponseCode.mandatoryParamsMissing,
            MessageFormat.format(
                ResponseCode.mandatoryParamsMissing.getErrorMessage(), JsonKey.ID_TYPE));
      } else {
        userId = userExternalIdentityDao.getUserIdByExternalId(id, provider, idType);
        if (userId == null) {
          ProjectCommonException.throwClientErrorException(
              ResponseCode.externalIdNotFound,
              ProjectUtil.formatMessage(
                  ResponseCode.externalIdNotFound.getErrorMessage(), id, idType, provider));
        }
        showMaskedData = true;
      }

    } else {
      userId = id;
      showMaskedData = false;
    }
    boolean isPrivate = (boolean) actorMessage.getContext().get(JsonKey.PRIVATE);
    Map<String, Object> result = null;
    if (!isPrivate) {
      Future<Map<String, Object>> resultF =
          esUtil.getDataByIdentifier(ProjectUtil.EsType.user.getTypeName(), userId);
      try {
        Object object = Await.result(resultF, ElasticSearchHelper.timeout.duration());
        if (object != null) {
          result = (Map<String, Object>) object;
        }
      } catch (Exception e) {
        ProjectLogger.log(
            String.format(
                "%s:%s:User not found with provided id == %s and error %s",
                this.getClass().getSimpleName(), "getUserProfileData", e.getMessage()),
            LoggerEnum.ERROR.name());
      }
    } else {
      UserDao userDao = new UserDaoImpl();
      User foundUser = userDao.getUserById(userId);
      if (foundUser == null) {
        throw new ProjectCommonException(
            ResponseCode.userNotFound.getErrorCode(),
            ResponseCode.userNotFound.getErrorMessage(),
            ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
      }
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
      result = objectMapper.convertValue(foundUser, Map.class);
      result.put(JsonKey.ORGANISATIONS, Util.getUserOrgDetails(userId));
    }
    // check user found or not
    if (result == null || result.size() == 0) {
      throw new ProjectCommonException(
          ResponseCode.userNotFound.getErrorCode(),
          ResponseCode.userNotFound.getErrorMessage(),
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }
    // check whether is_deletd true or false
    if (ProjectUtil.isNotNull(result)
        && result.containsKey(JsonKey.IS_DELETED)
        && ProjectUtil.isNotNull(result.get(JsonKey.IS_DELETED))
        && (Boolean) result.get(JsonKey.IS_DELETED)) {
      ProjectCommonException.throwClientErrorException(ResponseCode.userAccountlocked);
    }
    Future<Map<String, Object>> esResultF = fetchRootAndRegisterOrganisation(result);
    Map<String, Object> esResult =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(esResultF);
    result.put(JsonKey.ROOT_ORG, esResult);
    // having check for removing private filed from user , if call user and response
    // user data id is not same.
    String requestedById =
        (String) actorMessage.getContext().getOrDefault(JsonKey.REQUESTED_BY, "");
    ProjectLogger.log(
        "requested By and requested user id == " + requestedById + "  " + (String) userId);
    try {
      if (!(userId).equalsIgnoreCase(requestedById) && !showMaskedData) {
        result = removeUserPrivateField(result);
      } else {
        // These values are set to ensure backward compatibility post introduction of global
        // settings in user profile visibility
        setCompleteProfileVisibilityMap(result);
        setDefaultUserProfileVisibility(result);

        // If the user requests his data then we are fetching the private data from
        // userprofilevisibility index
        // and merge it with user index data
        Future<Map<String, Object>> privateResultF =
            esUtil.getDataByIdentifier(
                ProjectUtil.EsType.userprofilevisibility.getTypeName(), userId);
        Map<String, Object> privateResult =
            (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(privateResultF);
        // fetch user external identity
        List<Map<String, String>> dbResExternalIds = fetchUserExternalIdentity(userId);
        result.put(JsonKey.EXTERNAL_IDS, dbResExternalIds);
        result.putAll(privateResult);
      }
    } catch (Exception e) {
      ProjectCommonException.throwServerErrorException(ResponseCode.userDataEncryptionError);
    }
    if (null != actorMessage.getContext().get(JsonKey.FIELDS)) {
      String requestFields = (String) actorMessage.getContext().get(JsonKey.FIELDS);
      addExtraFieldsInUserProfileResponse(result, requestFields, userId);
    } else {
      result.remove(JsonKey.MISSING_FIELDS);
      result.remove(JsonKey.COMPLETENESS);
    }

    Response response = new Response();

    if (null != result) {
      UserUtility.decryptUserDataFrmES(result);
      updateSkillWithEndoresmentCount(result);
      updateTnc(result);
      // loginId is used internally for checking the duplicate user
      result.remove(JsonKey.LOGIN_ID);
      result.remove(JsonKey.ENC_EMAIL);
      result.remove(JsonKey.ENC_PHONE);
      // String username = ssoManager.getUsernameById(userId);
      //  result.put(JsonKey.USERNAME, username);
      response.put(JsonKey.RESPONSE, result);
    } else {
      result = new HashMap<>();
      response.put(JsonKey.RESPONSE, result);
    }
    return response;
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, String>> fetchUserExternalIdentity(String userId) {
    Response response =
        cassandraOperation.getRecordsByIndexedProperty(
            JsonKey.SUNBIRD, JsonKey.USR_EXT_IDNT_TABLE, JsonKey.USER_ID, userId);
    List<Map<String, String>> dbResExternalIds = new ArrayList<>();
    if (null != response && null != response.getResult()) {
      dbResExternalIds = (List<Map<String, String>>) response.getResult().get(JsonKey.RESPONSE);
      if (null != dbResExternalIds) {
        dbResExternalIds
            .stream()
            .forEach(
                s -> {
                  if (StringUtils.isNotBlank(s.get(JsonKey.ORIGINAL_EXTERNAL_ID))
                      && StringUtils.isNotBlank(s.get(JsonKey.ORIGINAL_ID_TYPE))
                      && StringUtils.isNotBlank(s.get(JsonKey.ORIGINAL_PROVIDER))) {
                    s.put(JsonKey.ID, s.get(JsonKey.ORIGINAL_EXTERNAL_ID));
                    s.put(JsonKey.ID_TYPE, s.get(JsonKey.ORIGINAL_ID_TYPE));
                    s.put(JsonKey.PROVIDER, s.get(JsonKey.ORIGINAL_PROVIDER));

                  } else {
                    s.put(JsonKey.ID, s.get(JsonKey.EXTERNAL_ID));
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
    return dbResExternalIds;
  }

  @SuppressWarnings("unchecked")
  private void setCompleteProfileVisibilityMap(Map<String, Object> userMap) {
    Map<String, String> profileVisibilityMap =
        (Map<String, String>) userMap.get(JsonKey.PROFILE_VISIBILITY);
    Map<String, String> completeProfileVisibilityMap =
        Util.getCompleteProfileVisibilityMap(profileVisibilityMap, systemSettingActorRef);
    userMap.put(JsonKey.PROFILE_VISIBILITY, completeProfileVisibilityMap);
  }

  private void setDefaultUserProfileVisibility(Map<String, Object> userMap) {
    userMap.put(
        JsonKey.DEFAULT_PROFILE_FIELD_VISIBILITY,
        ProjectUtil.getConfigValue(JsonKey.SUNBIRD_USER_PROFILE_FIELD_DEFAULT_VISIBILITY));
  }

  /**
   * This method will remove user private field from response map
   *
   * @param responseMap Map<String,Object>
   */
  private Map<String, Object> removeUserPrivateField(Map<String, Object> responseMap) {
    ProjectLogger.log("Start removing User private field==");
    for (int i = 0; i < ProjectUtil.excludes.length; i++) {
      responseMap.remove(ProjectUtil.excludes[i]);
    }
    ProjectLogger.log("All private filed removed=");
    return responseMap;
  }

  private Future<Map<String, Object>> fetchRootAndRegisterOrganisation(Map<String, Object> result) {
    try {
      if (isNotNull(result.get(JsonKey.ROOT_ORG_ID))) {
        String rootOrgId = (String) result.get(JsonKey.ROOT_ORG_ID);
        return esUtil.getDataByIdentifier(ProjectUtil.EsType.organisation.getTypeName(), rootOrgId);
      }
    } catch (Exception ex) {
      ProjectLogger.log(ex.getMessage(), ex);
    }
    return null;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void addExtraFieldsInUserProfileResponse(
      Map<String, Object> result, String fields, String userId) {
    if (!StringUtils.isBlank(fields)) {
      if (!fields.contains(JsonKey.COMPLETENESS)) {
        result.remove(JsonKey.COMPLETENESS);
      }
      if (!fields.contains(JsonKey.MISSING_FIELDS)) {
        result.remove(JsonKey.MISSING_FIELDS);
      }
      if (fields.contains(JsonKey.LAST_LOGIN_TIME)) {
        result.put(
            JsonKey.LAST_LOGIN_TIME,
            Long.parseLong(getLastLoginTime(userId, (String) result.get(JsonKey.LAST_LOGIN_TIME))));
      } else {
        result.remove(JsonKey.LAST_LOGIN_TIME);
      }
      if (fields.contains(JsonKey.TOPIC)) {
        // fetch the topic details of all user associated orgs and append in the result
        fetchTopicOfAssociatedOrgs(result);
      }
      if (fields.contains(JsonKey.ORGANISATIONS)) {
        updateUserOrgInfo((List) result.get(JsonKey.ORGANISATIONS));
      }
      if (fields.contains(JsonKey.ROLES)) {
        updateRoleMasterInfo(result);
      }
      if (fields.contains(JsonKey.LOCATIONS)) {
        result.put(
            JsonKey.USER_LOCATIONS,
            getUserLocations((List<String>) result.get(JsonKey.LOCATION_IDS)));
        result.remove(JsonKey.LOCATION_IDS);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void updateSkillWithEndoresmentCount(Map<String, Object> result) {
    if (MapUtils.isNotEmpty(result) && result.containsKey(JsonKey.SKILLS)) {
      List<Map<String, Object>> skillList = (List<Map<String, Object>>) result.get(JsonKey.SKILLS);
      if (CollectionUtils.isEmpty(skillList)) {
        return;
      }
      for (Map<String, Object> skill : skillList) {
        skill.put(
            JsonKey.ENDORSEMENT_COUNT.toLowerCase(),
            (int) skill.getOrDefault(JsonKey.ENDORSEMENT_COUNT, 0));
      }
    }
  }

  private String getLastLoginTime(String userId, String time) {
    String lastLoginTime = "";
    if (Boolean.parseBoolean(PropertiesCache.getInstance().getProperty(JsonKey.IS_SSO_ENABLED))) {
      SSOManager manager = SSOServiceFactory.getInstance();
      lastLoginTime = manager.getLastLoginTime(userId);
    } else {
      lastLoginTime = time;
    }
    if (StringUtils.isBlank(lastLoginTime)) {
      return "0";
    }
    return lastLoginTime;
  }

  @SuppressWarnings("unchecked")
  private void fetchTopicOfAssociatedOrgs(Map<String, Object> result) {

    String userId = (String) result.get(JsonKey.ID);
    Map<String, Object> locationCache = new HashMap<>();
    Set<String> topicSet = new HashSet<>();

    // fetch all associated user orgs
    Response response1 =
        cassandraOperation.getRecordsByProperty(
            userOrgDbInfo.getKeySpace(), userOrgDbInfo.getTableName(), JsonKey.USER_ID, userId);

    List<Map<String, Object>> list = (List<Map<String, Object>>) response1.get(JsonKey.RESPONSE);

    List<String> orgIdsList = new ArrayList<>();
    if (!list.isEmpty()) {

      for (Map<String, Object> m : list) {
        String orgId = (String) m.get(JsonKey.ORGANISATION_ID);
        orgIdsList.add(orgId);
      }

      // fetch all org details from elasticsearch ...
      if (!orgIdsList.isEmpty()) {

        Map<String, Object> filters = new HashMap<>();
        filters.put(JsonKey.ID, orgIdsList);

        List<String> orgfields = new ArrayList<>();
        orgfields.add(JsonKey.ID);
        orgfields.add(JsonKey.LOCATION_ID);

        SearchDTO searchDTO = new SearchDTO();
        searchDTO.getAdditionalProperties().put(JsonKey.FILTERS, filters);
        searchDTO.setFields(orgfields);

        Future<Map<String, Object>> esresultF =
            esUtil.search(searchDTO, EsType.organisation.getTypeName());
        Map<String, Object> esresult =
            (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(esresultF);
        List<Map<String, Object>> esContent =
            (List<Map<String, Object>>) esresult.get(JsonKey.CONTENT);

        if (!esContent.isEmpty()) {
          for (Map<String, Object> m : esContent) {
            if (!StringUtils.isBlank((String) m.get(JsonKey.LOCATION_ID))) {
              String locationId = (String) m.get(JsonKey.LOCATION_ID);
              if (locationCache.containsKey(locationId)) {
                topicSet.add((String) locationCache.get(locationId));
              } else {
                // get the location id info from db and set to the cacche and
                // topicSet
                Response response3 =
                    cassandraOperation.getRecordById(
                        geoLocationDbInfo.getKeySpace(),
                        geoLocationDbInfo.getTableName(),
                        locationId);
                List<Map<String, Object>> list3 =
                    (List<Map<String, Object>>) response3.get(JsonKey.RESPONSE);
                if (!list3.isEmpty()) {
                  Map<String, Object> locationInfoMap = list3.get(0);
                  String topic = (String) locationInfoMap.get(JsonKey.TOPIC);
                  topicSet.add(topic);
                  locationCache.put(locationId, topic);
                }
              }
            }
          }
        }
      }
    }
    result.put(JsonKey.TOPICS, topicSet);
  }

  private void updateUserOrgInfo(List<Map<String, Object>> userOrgs) {
    Map<String, Map<String, Object>> orgInfoMap = fetchAllOrgsById(userOrgs);
    Map<String, Map<String, Object>> locationInfoMap = fetchAllLocationsById(orgInfoMap);
    prepUserOrgInfoWithAdditionalData(userOrgs, orgInfoMap, locationInfoMap);
  }

  private Map<String, Map<String, Object>> fetchAllOrgsById(List<Map<String, Object>> userOrgs) {
    List<String> orgIds =
        userOrgs
            .stream()
            .map(m -> (String) m.get(JsonKey.ORGANISATION_ID))
            .distinct()
            .collect(Collectors.toList());
    List<String> fields =
        Arrays.asList(
            JsonKey.ORG_NAME, JsonKey.CHANNEL, JsonKey.HASHTAGID, JsonKey.LOCATION_IDS, JsonKey.ID);

    Map<String, Map<String, Object>> orgInfoMap =
        getEsResultByListOfIds(orgIds, fields, EsType.organisation);
    return orgInfoMap;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Map<String, Object>> fetchAllLocationsById(
      Map<String, Map<String, Object>> orgInfoMap) {
    List<String> searchLocations = new ArrayList<>();
    for (Map<String, Object> org : orgInfoMap.values()) {
      List<String> locations = (List<String>) org.get(JsonKey.LOCATION_IDS);
      if (locations != null) {
        for (String location : locations) {
          if (!searchLocations.contains(location)) {
            searchLocations.add(location);
          }
        }
      }
    }
    List<String> locationFields =
        Arrays.asList(JsonKey.CODE, JsonKey.NAME, JsonKey.TYPE, JsonKey.PARENT_ID, JsonKey.ID);
    Map<String, Map<String, Object>> locationInfoMap =
        getEsResultByListOfIds(searchLocations, locationFields, EsType.location);
    return locationInfoMap;
  }

  @SuppressWarnings("unchecked")
  private void prepUserOrgInfoWithAdditionalData(
      List<Map<String, Object>> userOrgs,
      Map<String, Map<String, Object>> orgInfoMap,
      Map<String, Map<String, Object>> locationInfoMap) {
    for (Map<String, Object> usrOrg : userOrgs) {
      Map<String, Object> orgInfo = orgInfoMap.get(usrOrg.get(JsonKey.ORGANISATION_ID));
      usrOrg.put(JsonKey.ORG_NAME, orgInfo.get(JsonKey.ORG_NAME));
      usrOrg.put(JsonKey.CHANNEL, orgInfo.get(JsonKey.CHANNEL));
      usrOrg.put(JsonKey.HASHTAGID, orgInfo.get(JsonKey.HASHTAGID));
      usrOrg.put(JsonKey.LOCATION_IDS, orgInfo.get(JsonKey.LOCATION_IDS));
      usrOrg.put(
          JsonKey.LOCATIONS,
          prepLocationFields((List<String>) orgInfo.get(JsonKey.LOCATION_IDS), locationInfoMap));
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

  @SuppressWarnings("unchecked")
  private Map<String, Map<String, Object>> getEsResultByListOfIds(
      List<String> orgIds, List<String> fields, EsType typeToSearch) {

    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.ID, orgIds);

    SearchDTO searchDTO = new SearchDTO();
    searchDTO.getAdditionalProperties().put(JsonKey.FILTERS, filters);
    searchDTO.setFields(fields);

    Future<Map<String, Object>> resultF = esUtil.search(searchDTO, typeToSearch.getTypeName());
    Map<String, Object> result =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);

    List<Map<String, Object>> esContent = (List<Map<String, Object>>) result.get(JsonKey.CONTENT);
    return esContent
        .stream()
        .collect(
            Collectors.toMap(
                obj -> {
                  return (String) obj.get("id");
                },
                val -> val));
  }

  private void updateRoleMasterInfo(Map<String, Object> result) {
    Set<Entry<String, Object>> roleSet = DataCacheHandler.getRoleMap().entrySet();
    List<Map<String, String>> roleList = new ArrayList<>();
    roleSet
        .parallelStream()
        .forEach(
            (roleSetItem) -> {
              Map<String, String> roleMap = new HashMap<>();
              roleMap.put(JsonKey.ID, roleSetItem.getKey());
              roleMap.put(JsonKey.NAME, (String) roleSetItem.getValue());
              roleList.add(roleMap);
            });
    result.put(JsonKey.ROLE_LIST, roleList);
  }

  /**
   * Method to get user profile (version 2).
   *
   * @param actorMessage Request containing user ID
   */
  @SuppressWarnings("unchecked")
  private void getUserProfileV2(Request actorMessage) {
    Response response = getUserProfileData(actorMessage);
    SystemSettingClient systemSetting = new SystemSettingClientImpl();
    Object excludedFieldList =
        systemSetting.getSystemSettingByFieldAndKey(
            systemSettingActorRef,
            JsonKey.USER_PROFILE_CONFIG,
            JsonKey.SUNBIRD_USER_PROFILE_READ_EXCLUDED_FIELDS,
            new TypeReference<List<String>>() {});
    if (excludedFieldList != null) {
      removeExcludedFieldsFromUserProfileResponse(
          (Map<String, Object>) response.get(JsonKey.RESPONSE), (List<String>) excludedFieldList);
    } else {
      ProjectLogger.log(
          "UserProfileReadActor:getUserProfileV2: System setting userProfileConfig.read.excludedFields not configured.",
          LoggerEnum.INFO.name());
    }
    sender().tell(response, self());
  }

  private void removeExcludedFieldsFromUserProfileResponse(
      Map<String, Object> response, List<String> excludeFields) {
    if (CollectionUtils.isNotEmpty(excludeFields)) {
      for (String key : excludeFields) {
        response.remove(key);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void getUserDetailsByLoginId(Request actorMessage) {
    actorMessage.toLower();
    Map<String, Object> userMap = actorMessage.getRequest();
    if (null != userMap.get(JsonKey.LOGIN_ID)) {
      String loginId = (String) userMap.get(JsonKey.LOGIN_ID);
      try {
        loginId = encryptionService.encryptData((String) userMap.get(JsonKey.LOGIN_ID));
      } catch (Exception e) {
        ProjectCommonException exception =
            new ProjectCommonException(
                ResponseCode.userDataEncryptionError.getErrorCode(),
                ResponseCode.userDataEncryptionError.getErrorMessage(),
                ResponseCode.SERVER_ERROR.getResponseCode());
        sender().tell(exception, self());
        return;
      }

      SearchDTO searchDto = new SearchDTO();
      Map<String, Object> filter = new HashMap<>();
      filter.put(JsonKey.LOGIN_ID, loginId);
      searchDto.getAdditionalProperties().put(JsonKey.FILTERS, filter);
      Future<Map<String, Object>> esResponseF =
          esUtil.search(searchDto, ProjectUtil.EsType.user.getTypeName());
      Map<String, Object> esResponse =
          (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(esResponseF);
      List<Map<String, Object>> userList =
          (List<Map<String, Object>>) esResponse.get(JsonKey.CONTENT);
      Map<String, Object> result = null;
      if (null != userList && !userList.isEmpty()) {
        result = userList.get(0);
      } else {
        throw new ProjectCommonException(
            ResponseCode.userNotFound.getErrorCode(),
            ResponseCode.userNotFound.getErrorMessage(),
            ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
      }
      // String username = ssoManager.getUsernameById((String) result.get(JsonKey.USER_ID));
      // result.put(JsonKey.USERNAME, username);
      sendResponse(actorMessage, result);

    } else {
      ProjectCommonException exception =
          new ProjectCommonException(
              ResponseCode.userNotFound.getErrorCode(),
              ResponseCode.userNotFound.getErrorMessage(),
              ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
      sender().tell(exception, self());
      return;
    }
  }

  private void sendResponse(Request actorMessage, Map<String, Object> result) {
    if (result == null || result.size() == 0) {
      throw new ProjectCommonException(
          ResponseCode.userNotFound.getErrorCode(),
          ResponseCode.userNotFound.getErrorMessage(),
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }

    // check whether is_deletd true or false
    if (ProjectUtil.isNotNull(result)
        && result.containsKey(JsonKey.IS_DELETED)
        && ProjectUtil.isNotNull(result.get(JsonKey.IS_DELETED))
        && (Boolean) result.get(JsonKey.IS_DELETED)) {
      throw new ProjectCommonException(
          ResponseCode.userAccountlocked.getErrorCode(),
          ResponseCode.userAccountlocked.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    Future<Map<String, Object>> future = fetchRootAndRegisterOrganisation(result);
    Future<Response> response =
        future.map(
            new Mapper<Map<String, Object>, Response>() {
              @Override
              public Response apply(Map<String, Object> responseMap) {
                ProjectLogger.log(
                    "UserProfileReadActor:handle user profile read async call ",
                    LoggerEnum.INFO.name());
                result.put(JsonKey.ROOT_ORG, responseMap);
                Response response = new Response();
                handleUserCallAsync(result, response, actorMessage);
                return response;
              }
            },
            getContext().dispatcher());
    Patterns.pipe(response, getContext().dispatcher()).to(sender());
  }

  private void handleUserCallAsync(
      Map<String, Object> result, Response response, Request actorMessage) {
    // having check for removing private filed from user , if call user and response
    // user data id is not same.
    String requestedById =
        (String) actorMessage.getContext().getOrDefault(JsonKey.REQUESTED_BY, "");
    ProjectLogger.log(
        "requested By and requested user id == "
            + requestedById
            + "  "
            + (String) result.get(JsonKey.USER_ID));

    try {
      if (!(((String) result.get(JsonKey.USER_ID)).equalsIgnoreCase(requestedById))) {
        result = removeUserPrivateField(result);
      } else {
        // These values are set to ensure backward compatibility post introduction of
        // global
        // settings in user profile visibility
        setCompleteProfileVisibilityMap(result);
        setDefaultUserProfileVisibility(result);
        // If the user requests his data then we are fetching the private data from
        // userprofilevisibility index
        // and merge it with user index data
        Future<Map<String, Object>> privateResultF =
            esUtil.getDataByIdentifier(
                ProjectUtil.EsType.userprofilevisibility.getTypeName(),
                (String) result.get(JsonKey.USER_ID));
        Map<String, Object> privateResult =
            (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(privateResultF);
        // fetch user external identity
        List<Map<String, String>> dbResExternalIds = fetchUserExternalIdentity(requestedById);
        result.put(JsonKey.EXTERNAL_IDS, dbResExternalIds);
        result.putAll(privateResult);
      }
    } catch (Exception e) {
      ProjectCommonException exception =
          new ProjectCommonException(
              ResponseCode.userDataEncryptionError.getErrorCode(),
              ResponseCode.userDataEncryptionError.getErrorMessage(),
              ResponseCode.SERVER_ERROR.getResponseCode());
      sender().tell(exception, self());
      return;
    }

    if (null != result) {
      // remove email and phone no from response
      result.remove(JsonKey.ENC_EMAIL);
      result.remove(JsonKey.ENC_PHONE);
      updateTnc(result);
      if (null != actorMessage.getRequest().get(JsonKey.FIELDS)) {
        List<String> requestFields = (List<String>) actorMessage.getRequest().get(JsonKey.FIELDS);
        if (requestFields != null) {
          addExtraFieldsInUserProfileResponse(
              result, String.join(",", requestFields), (String) result.get(JsonKey.USER_ID));
        } else {
          result.remove(JsonKey.MISSING_FIELDS);
          result.remove(JsonKey.COMPLETENESS);
        }
      } else {
        result.remove(JsonKey.MISSING_FIELDS);
        result.remove(JsonKey.COMPLETENESS);
      }
      response.put(JsonKey.RESPONSE, result);
      UserUtility.decryptUserDataFrmES(result);
    } else {
      result = new HashMap<>();
      response.put(JsonKey.RESPONSE, result);
    }
  }

  private void updateTnc(Map<String, Object> userSearchMap) {
    Map<String, Object> tncConfigMap = null;
    try {
      String tncValue = DataCacheHandler.getConfigSettings().get(JsonKey.TNC_CONFIG);
      tncConfigMap = mapper.readValue(tncValue, Map.class);

    } catch (Exception e) {
      ProjectLogger.log(
          "UserProfileReadActor:updateTncInfo: Exception occurred while getting system setting for"
              + JsonKey.TNC_CONFIG
              + e.getMessage(),
          LoggerEnum.ERROR.name());
    }

    if (MapUtils.isNotEmpty(tncConfigMap)) {
      try {
        String tncLatestVersion = (String) tncConfigMap.get(JsonKey.LATEST_VERSION);
        userSearchMap.put(JsonKey.TNC_LATEST_VERSION, tncLatestVersion);
        String tncUserAcceptedVersion = (String) userSearchMap.get(JsonKey.TNC_ACCEPTED_VERSION);
        String tncUserAcceptedOn = (String) userSearchMap.get(JsonKey.TNC_ACCEPTED_ON);
        if (StringUtils.isEmpty(tncUserAcceptedVersion)
            || !tncUserAcceptedVersion.equalsIgnoreCase(tncLatestVersion)
            || StringUtils.isEmpty(tncUserAcceptedOn)) {
          userSearchMap.put(JsonKey.PROMPT_TNC, true);
        } else {
          userSearchMap.put(JsonKey.PROMPT_TNC, false);
        }

        if (tncConfigMap.containsKey(tncLatestVersion)) {
          String url = (String) ((Map) tncConfigMap.get(tncLatestVersion)).get(JsonKey.URL);
          ProjectLogger.log(
              "UserProfileReadActor:updateTncInfo: url = " + url, LoggerEnum.INFO.name());
          userSearchMap.put(JsonKey.TNC_LATEST_VERSION_URL, url);
        } else {
          userSearchMap.put(JsonKey.PROMPT_TNC, false);
          ProjectLogger.log(
              "UserProfileReadActor:updateTncInfo: TnC version URL is missing from configuration");
        }
      } catch (Exception e) {
        ProjectLogger.log(
            "UserProfileReadActor:updateTncInfo: Exception occurred with error message = "
                + e.getMessage(),
            LoggerEnum.ERROR.name());
        ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
      }
    }
  }

  private List<Map<String, Object>> getUserLocations(List<String> locationIds) {
    if (CollectionUtils.isNotEmpty(locationIds)) {
      List<String> locationFields =
          Arrays.asList(JsonKey.CODE, JsonKey.NAME, JsonKey.TYPE, JsonKey.PARENT_ID, JsonKey.ID);
      Map<String, Map<String, Object>> locationInfoMap =
          getEsResultByListOfIds(locationIds, locationFields, EsType.location);

      return locationInfoMap.values().stream().collect(Collectors.toList());
    }
    return new ArrayList<>();
  }

  private void checkUserExistences(Request request) {

    String value = (String) request.get(JsonKey.VALUE);
    String type = (String) request.get(JsonKey.KEY);
    try {
      boolean exists = UserUtil.identifierExists(type, value);
      Response response = new Response();
      response.put(JsonKey.EXISTS, exists);
      sender().tell(response, self());
    } catch (Exception var11) {
      ProjectCommonException exception =
          new ProjectCommonException(
              ResponseCode.userDataEncryptionError.getErrorCode(),
              ResponseCode.userDataEncryptionError.getErrorMessage(),
              ResponseCode.SERVER_ERROR.getResponseCode());
      sender().tell(exception, self());
    }
  }

  private void checkUserExistence(Request request) {
    Map<String, Object> searchMap = new WeakHashMap<>();
    String value = (String) request.get(JsonKey.VALUE);
    String encryptedValue = null;
    try {
      encryptedValue = encryptionService.encryptData(StringUtils.lowerCase(value));
    } catch (Exception var11) {
      throw new ProjectCommonException(
          ResponseCode.userDataEncryptionError.getErrorCode(),
          ResponseCode.userDataEncryptionError.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    searchMap.put((String) request.get(JsonKey.KEY), encryptedValue);
    ProjectLogger.log(
        "UserProfileReadActor:checkUserExistence: search map prepared " + searchMap,
        LoggerEnum.INFO.name());
    SearchDTO searchDTO = new SearchDTO();
    searchDTO.getAdditionalProperties().put(JsonKey.FILTERS, searchMap);
    Future<Map<String, Object>> esFuture = esUtil.search(searchDTO, EsType.user.getTypeName());
    Future<Response> userResponse =
        esFuture.map(
            new Mapper<Map<String, Object>, Response>() {
              @Override
              public Response apply(Map<String, Object> responseMap) {
                List<Map<String, Object>> respList = (List) responseMap.get(JsonKey.CONTENT);
                long size = respList.size();
                Response resp = new Response();
                resp.put(JsonKey.EXISTS, true);
                if (size <= 0) {
                  resp.put(JsonKey.EXISTS, false);
                }
                return resp;
              }
            },
            getContext().dispatcher());

    Patterns.pipe(userResponse, getContext().dispatcher()).to(sender());
  }

  private ElasticSearchService getESInstance() {
    return EsClientFactory.getInstance(JsonKey.REST);
  }

  private void getKey(Request actorMessage) {
    String key = (String) actorMessage.getRequest().get(JsonKey.KEY);
    String value = (String) actorMessage.getRequest().get(JsonKey.VALUE);
    if (JsonKey.LOGIN_ID.equalsIgnoreCase(key) || JsonKey.EMAIL.equalsIgnoreCase(key)) {
      value = value.toLowerCase();
    }
    String encryptedValue = null;
    try {
      encryptedValue = encryptionService.encryptData(value);
    } catch (Exception e) {
      ProjectCommonException exception =
          new ProjectCommonException(
              ResponseCode.userDataEncryptionError.getErrorCode(),
              ResponseCode.userDataEncryptionError.getErrorMessage(),
              ResponseCode.SERVER_ERROR.getResponseCode());
      sender().tell(exception, self());
      return;
    }

    Map<String, Object> searchMap = new WeakHashMap<>();
    searchMap.put(key, encryptedValue);
    SearchDTO searchDTO = new SearchDTO();
    searchDTO.getAdditionalProperties().put(JsonKey.FILTERS, searchMap);
    handleUserSearchAsyncRequest(searchDTO, actorMessage);
  }

  private void handleUserSearchAsyncRequest(SearchDTO searchDto, Request actorMessage) {
    Future<Map<String, Object>> futureResponse =
        esUtil.search(searchDto, EsType.user.getTypeName());

    Future<Map<String, Object>> userResponse =
        futureResponse.map(
            new Mapper<Map<String, Object>, Map<String, Object>>() {
              @Override
              public Map<String, Object> apply(Map<String, Object> responseMap) {
                ProjectLogger.log(
                    "SearchHandlerActor:handleUserSearchAsyncRequest user search call ",
                    LoggerEnum.INFO);
                List<Map<String, Object>> respList = (List) responseMap.get(JsonKey.CONTENT);
                isUserExists(respList);
                Map<String, Object> userMap = respList.get(0);
                userMap.put(JsonKey.EMAIL, userMap.get(JsonKey.MASKED_EMAIL));
                userMap.put(JsonKey.PHONE, userMap.get(JsonKey.MASKED_PHONE));
                isUserAccountDeleted(userMap);
                return userMap;
              }
            },
            getContext().dispatcher());

    Future<Object> orgResponse =
        userResponse.map(
            new Mapper<Map<String, Object>, Object>() {
              @Override
              public Object apply(Map<String, Object> parameter) {
                Map<String, Object> esOrgMap = new HashMap<>();
                esOrgMap =
                    (Map<String, Object>)
                        ElasticSearchHelper.getResponseFromFuture(
                            fetchRootAndRegisterOrganisation(parameter));
                return esOrgMap;
              }
            },
            getContext().dispatcher());

    Future<Map<String, Object>> userOrgResponse =
        userResponse
            .zip(orgResponse)
            .map(
                new Mapper<Tuple2<Map<String, Object>, Object>, Map<String, Object>>() {
                  @Override
                  public Map<String, Object> apply(Tuple2<Map<String, Object>, Object> parameter) {
                    Map<String, Object> userMap = parameter._1;
                    userMap.put(JsonKey.ROOT_ORG, (Map<String, Object>) parameter._2);
                    userMap.remove(JsonKey.ENC_EMAIL);
                    userMap.remove(JsonKey.ENC_PHONE);
                    String requestedById =
                        (String) actorMessage.getContext().getOrDefault(JsonKey.REQUESTED_BY, "");
                    if (!(((String) userMap.get(JsonKey.USER_ID))
                        .equalsIgnoreCase(requestedById))) {
                      userMap = removeUserPrivateField(userMap);
                    }
                    return userMap;
                  }
                },
                getContext().dispatcher());

    Future<Map<String, Object>> externalIdFuture =
        userResponse.map(
            new Mapper<Map<String, Object>, Map<String, Object>>() {
              @Override
              public Map<String, Object> apply(Map<String, Object> result) {
                Map<String, Object> extIdMap = new HashMap<>();
                String requestedById =
                    (String) actorMessage.getContext().getOrDefault(JsonKey.REQUESTED_BY, "");
                if (((String) result.get(JsonKey.USER_ID)).equalsIgnoreCase(requestedById)) {
                  List<Map<String, String>> dbResExternalIds =
                      fetchUserExternalIdentity((String) result.get(JsonKey.USER_ID));
                  extIdMap.put(JsonKey.EXTERNAL_IDS, dbResExternalIds);
                  return extIdMap;
                }
                return extIdMap;
              }
            },
            getContext().dispatcher());

    Future<Map<String, Object>> tncFuture =
        userOrgResponse.map(
            new Mapper<Map<String, Object>, Map<String, Object>>() {
              @Override
              public Map<String, Object> apply(Map<String, Object> result) {
                updateTnc(result);
                if (null != actorMessage.getRequest().get(JsonKey.FIELDS)) {
                  List<String> requestFields =
                      (List<String>) actorMessage.getRequest().get(JsonKey.FIELDS);
                  if (requestFields != null) {
                    addExtraFieldsInUserProfileResponse(
                        result,
                        String.join(",", requestFields),
                        (String) result.get(JsonKey.USER_ID));
                  } else {
                    result.remove(JsonKey.MISSING_FIELDS);
                    result.remove(JsonKey.COMPLETENESS);
                  }
                } else {
                  result.remove(JsonKey.MISSING_FIELDS);
                  result.remove(JsonKey.COMPLETENESS);
                }
                UserUtility.decryptUserDataFrmES(result);
                return result;
              }
            },
            getContext().dispatcher());

    Future<Response> sumFuture =
        externalIdFuture
            .zip(tncFuture)
            .map(
                new Mapper<Tuple2<Map<String, Object>, Map<String, Object>>, Response>() {
                  @Override
                  public Response apply(
                      Tuple2<Map<String, Object>, Map<String, Object>> parameter) {
                    Map<String, Object> externalIdMap = parameter._1;
                    System.out.println("the externalId maap is " + externalIdMap);
                    Map<String, Object> tncMap = parameter._2;
                    tncMap.putAll(externalIdMap);
                    Response response = new Response();
                    response.put(JsonKey.RESPONSE, tncMap);
                    return response;
                  }
                },
                getContext().dispatcher());

    Patterns.pipe(sumFuture, getContext().dispatcher()).to(sender());
  }

  private void isUserAccountDeleted(Map<String, Object> responseMap) {
    if (BooleanUtils.isTrue((Boolean) responseMap.get(JsonKey.IS_DELETED))) {
      throw new ProjectCommonException(
          ResponseCode.userAccountlocked.getErrorCode(),
          ResponseCode.userAccountlocked.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  private void isUserExists(List<Map<String, Object>> respList) {
    if (null == respList || respList.size() == 0) {
      throw new ProjectCommonException(
          ResponseCode.userNotFound.getErrorCode(),
          ResponseCode.userNotFound.getErrorMessage(),
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }
  }
}
