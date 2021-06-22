package org.sunbird.user.actors;

import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.pattern.Patterns;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.actorutil.location.LocationClient;
import org.sunbird.actorutil.location.impl.LocationClientImpl;
import org.sunbird.actorutil.user.UserClient;
import org.sunbird.actorutil.user.impl.UserClientImpl;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LocationActorOperation;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.TelemetryEnvKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.common.responsecode.ResponseMessage;
import org.sunbird.content.store.util.ContentStoreUtil;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.FormApiUtil;
import org.sunbird.learner.util.UserFlagUtil;
import org.sunbird.learner.util.UserUtility;
import org.sunbird.learner.util.Util;
import org.sunbird.location.service.LocationService;
import org.sunbird.location.service.LocationServiceImpl;
import org.sunbird.models.location.Location;
import org.sunbird.user.service.AssociationMechanism;
import org.sunbird.user.service.UserLookupService;
import org.sunbird.user.service.UserService;
import org.sunbird.user.service.impl.UserLookUpServiceImpl;
import org.sunbird.user.service.impl.UserServiceImpl;
import org.sunbird.user.util.UserUtil;
import org.sunbird.validator.user.UserRequestValidator;
import scala.Tuple2;
import scala.concurrent.Future;

@ActorConfig(
  tasks = {"createUserV4", "createManagedUser", "getManagedUsers"},
  asyncTasks = {},
  dispatcher = "most-used-one-dispatcher"
)
public class ManagedUserActor extends UserBaseActor {
  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private LocationClient locationClient = LocationClientImpl.getInstance();
  private UserClient userClient = UserClientImpl.getInstance();
  private UserService userService = UserServiceImpl.getInstance();
  private ElasticSearchService esUtil = EsClientFactory.getInstance(JsonKey.REST);
  private UserLookupService userLookupService = UserLookUpServiceImpl.getInstance();
  private LocationService locationService = LocationServiceImpl.getInstance();
  private ObjectMapper mapper = new ObjectMapper();
  private Util.DbInfo userOrgDb = Util.dbInfoMap.get(JsonKey.USER_ORG_DB);
  private UserRequestValidator userRequestValidator = new UserRequestValidator();

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    String operation = request.getOperation();
    switch (operation) {
      case "createUserV4":
      case "createManagedUser": // managedUser creation new version
        createManagedUser(request);
        break;
      case "getManagedUsers": // managedUser search
        getManagedUsers(request);
        break;
      default:
        onReceiveUnsupportedOperation("ManagedUserActor");
    }
  }

  /**
   * This method will create managed user in user in cassandra and update to ES as well at same
   * time. Email and phone is not provided, name and managedBy is mandatory. BMGS or Location is
   * optional
   *
   * @param actorMessage
   */
  private void createManagedUser(Request actorMessage) {
    logger.info(
        actorMessage.getRequestContext(), "UserManagementActor:createUserV4 method called.");
    actorMessage.toLower();
    Map<String, Object> userMap = actorMessage.getRequest();
    if (actorMessage
        .getOperation()
        .equalsIgnoreCase(ActorOperations.CREATE_MANAGED_USER.getValue())) {
      setProfileUserTypeAndLocation(userMap);
    }
    validateLocationCodes(actorMessage);

    String managedBy = (String) userMap.get(JsonKey.MANAGED_BY);
    logger.info(
        actorMessage.getRequestContext(),
        "validateUserId :: requestedId: " + actorMessage.getContext().get(JsonKey.REQUESTED_BY));
    String userId = (String) actorMessage.getContext().get(JsonKey.REQUESTED_BY);
    userMap.put(JsonKey.CREATED_BY, userId);
    // If user account isManagedUser (managedBy passed in request) should be same as context
    // user_id
    userService.validateUserId(actorMessage, managedBy, actorMessage.getRequestContext());

    // If managedUser limit is set, validate total number of managed users against it
    UserUtil.validateManagedUserLimit(managedBy, actorMessage.getRequestContext());
    processUserRequestV4(userMap, managedBy, actorMessage);
  }

  private void setProfileUserTypeAndLocation(Map<String, Object> userMap) {
    userMap.remove(JsonKey.LOCATION_CODES);
    if (userMap.containsKey(JsonKey.PROFILE_LOCATION)) {
      List<Map<String, String>> profLocList =
          (List<Map<String, String>>) userMap.get(JsonKey.PROFILE_LOCATION);
      List<String> locationCodes = null;
      if (CollectionUtils.isNotEmpty(profLocList)) {
        locationCodes =
            profLocList.stream().map(m -> m.get(JsonKey.CODE)).collect(Collectors.toList());
        userMap.put(JsonKey.LOCATION_CODES, locationCodes);
      }
      userMap.remove(JsonKey.PROFILE_LOCATION);
    }
  }

  private void validateLocationCodes(Request userRequest) {
    Object locationCodes = userRequest.getRequest().get(JsonKey.LOCATION_CODES);
    if ((locationCodes != null) && !(locationCodes instanceof List)) {
      throw new ProjectCommonException(
          ResponseCode.dataTypeError.getErrorCode(),
          ProjectUtil.formatMessage(
              ResponseCode.dataTypeError.getErrorMessage(), JsonKey.LOCATION_CODES, JsonKey.LIST),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    if (CollectionUtils.isNotEmpty((List) locationCodes)) {
      // As of now locationCode can take array of only locationCodes and map of locationCodes which
      // include type and code of the location
      String stateCode = null;
      List<String> set = new ArrayList<>();
      List<Location> locationList;
      if (((List) locationCodes).get(0) instanceof String) {
        List<String> locations = (List<String>) locationCodes;
        locationList =
            locationClient.getLocationsByCodes(
                getActorRef(LocationActorOperation.SEARCH_LOCATION.getValue()),
                locations,
                userRequest.getRequestContext());
        for (Location location : locationList) {
          if (JsonKey.STATE.equals(location.getType())) {
            stateCode = location.getCode();
          }
        }
      } else {
        locationList = createLocationLists((List<Map<String, String>>) locationCodes);
        for (Location location : locationList) {
          if (JsonKey.STATE.equals(location.getType())) {
            stateCode = location.getCode();
          }
        }
      }
      // Throw an exception if location codes update is not passed with state code
      if (StringUtils.isBlank(stateCode)) {
        throw new ProjectCommonException(
            ResponseCode.mandatoryParamsMissing.getErrorCode(),
            ProjectUtil.formatMessage(
                ResponseCode.mandatoryParamsMissing.getErrorMessage(),
                JsonKey.LOCATION_CODES + " of type State"),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
      Map<String, List<String>> locationTypeConfigMap = DataCacheHandler.getLocationTypeConfig();
      if (MapUtils.isEmpty(locationTypeConfigMap)
          || CollectionUtils.isEmpty(locationTypeConfigMap.get(stateCode))) {
        Map<String, Object> userProfileConfigMap =
            FormApiUtil.getProfileConfig(stateCode, userRequest.getRequestContext());
        // If config is not available check the default profile config
        if (MapUtils.isEmpty(userProfileConfigMap) && !JsonKey.DEFAULT_PERSONA.equals(stateCode)) {
          stateCode = JsonKey.DEFAULT_PERSONA;
          if (CollectionUtils.isEmpty(locationTypeConfigMap.get(stateCode))) {
            userProfileConfigMap =
                FormApiUtil.getProfileConfig(stateCode, userRequest.getRequestContext());
            if (MapUtils.isNotEmpty(userProfileConfigMap)) {
              List<String> locationTypeList =
                  FormApiUtil.getLocationTypeConfigMap(userProfileConfigMap);
              if (CollectionUtils.isNotEmpty(locationTypeList)) {
                locationTypeConfigMap.put(stateCode, locationTypeList);
              }
            }
          }
        } else {
          List<String> locationTypeList =
              FormApiUtil.getLocationTypeConfigMap(userProfileConfigMap);
          if (CollectionUtils.isNotEmpty(locationTypeList)) {
            locationTypeConfigMap.put(stateCode, locationTypeList);
          }
        }
      }
      List<String> typeList = locationTypeConfigMap.get(stateCode);
      for (Location location : locationList) {
        // for create-MUA we allow locations upto district
        if ((location.getType().equals(JsonKey.STATE))
            || (location.getType().equals(JsonKey.DISTRICT))) {
          isValidLocationType(location.getType(), typeList);
          set.add(location.getCode());
        }
      }
      userRequest.getRequest().put(JsonKey.LOCATION_CODES, set);
    }
  }

  public static boolean isValidLocationType(String type, List<String> typeList) {
    if (null != type && !typeList.contains(type.toLowerCase())) {
      throw new ProjectCommonException(
          ResponseCode.invalidValue.getErrorCode(),
          ProjectUtil.formatMessage(
              ResponseCode.invalidValue.getErrorMessage(), JsonKey.LOCATION_TYPE, type, typeList),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    return true;
  }

  private List<Location> createLocationLists(List<Map<String, String>> locationCodes) {
    List<Location> locations = new ArrayList<>();
    for (Map<String, String> locationMap : locationCodes) {
      Location location = new Location();
      location.setCode(locationMap.get(JsonKey.CODE));
      location.setType(locationMap.get(JsonKey.TYPE));
      locations.add(location);
    }
    return locations;
  }

  private void processUserRequestV4(
      Map<String, Object> userMap, String managedBy, Request actorMessage) {
    UserUtil.setUserDefaultValueForV3(userMap, actorMessage.getRequestContext());
    removeUnwanted(userMap);
    UserUtil.toLower(userMap);
    String channel = DataCacheHandler.getConfigSettings().get(JsonKey.CUSTODIAN_ORG_CHANNEL);
    String rootOrgId = DataCacheHandler.getConfigSettings().get(JsonKey.CUSTODIAN_ORG_ID);
    userMap.put(JsonKey.ROOT_ORG_ID, rootOrgId);
    userMap.put(JsonKey.CHANNEL, channel);
    Map<String, Object> managedByInfo =
        UserUtil.validateManagedByUser(managedBy, actorMessage.getRequestContext());
    convertValidatedLocationCodesToIDs(userMap, actorMessage.getRequestContext());
    ignoreOrAcceptFrameworkData(userMap, managedByInfo, actorMessage.getRequestContext());
    String userId = ProjectUtil.generateUniqueId();
    userMap.put(JsonKey.ID, userId);
    userMap.put(JsonKey.USER_ID, userId);
    UserUtil.addMaskEmailAndMaskPhone(userMap);
    try {
      UserUtility.encryptUserData(userMap);
    } catch (Exception ex) {
      logger.error(actorMessage.getRequestContext(), ex.getMessage(), ex);
    }
    userMap.put(JsonKey.IS_DELETED, false);
    Map<String, Boolean> userFlagsMap = new HashMap<>();
    userFlagsMap.put(JsonKey.STATE_VALIDATED, false);

    int userFlagValue = userFlagsToNum(userFlagsMap);
    userMap.put(JsonKey.FLAGS_VALUE, userFlagValue);
    final String password = (String) userMap.get(JsonKey.PASSWORD);
    userMap.remove(JsonKey.PASSWORD);
    userMap.remove(JsonKey.DOB_VALIDATION_DONE);
    Response response = userService.createUser(userMap, actorMessage.getRequestContext());
    insertIntoUserLookUp(userMap, actorMessage.getRequestContext());
    response.put(JsonKey.USER_ID, userMap.get(JsonKey.ID));
    Map<String, Object> esResponse = new HashMap<>();
    if (JsonKey.SUCCESS.equalsIgnoreCase((String) response.get(JsonKey.RESPONSE))) {
      Map<String, Object> orgMap = saveUserOrgInfo(userMap, actorMessage.getRequestContext());
      esResponse = Util.getUserDetails(userMap, orgMap, actorMessage.getRequestContext());
    } else {
      logger.info(
          actorMessage.getRequestContext(),
          "UserManagementActor:processUserRequest: User creation failure");
    }
    if ("kafka".equalsIgnoreCase(ProjectUtil.getConfigValue("sunbird_user_create_sync_type"))) {
      saveUserToKafka(esResponse);
      sender().tell(response, self());
    } else {
      Future<Boolean> kcFuture =
          Futures.future(
              (Callable<Boolean>)
                  () -> {
                    try {
                      Map<String, Object> updatePasswordMap = new HashMap<>();
                      updatePasswordMap.put(JsonKey.ID, userMap.get(JsonKey.ID));
                      updatePasswordMap.put(JsonKey.PASSWORD, password);
                      logger.info(
                          actorMessage.getRequestContext(),
                          "Update password value passed "
                              + password
                              + " --"
                              + userMap.get(JsonKey.ID));
                      return UserUtil.updatePassword(
                          updatePasswordMap, actorMessage.getRequestContext());
                    } catch (Exception e) {
                      logger.error(
                          actorMessage.getRequestContext(),
                          "Error occurred during update password : " + e.getMessage(),
                          e);
                      return false;
                    }
                  },
              getContext().dispatcher());
      Future<Response> future =
          saveUserToES(esResponse, actorMessage.getRequestContext())
              .zip(kcFuture)
              .map(
                  new Mapper<>() {
                    @Override
                    public Response apply(Tuple2<String, Boolean> parameter) {
                      boolean updatePassResponse = parameter._2;
                      logger.info(
                          actorMessage.getRequestContext(),
                          "UserManagementActor:processUserRequest: Response from update password call "
                              + updatePassResponse);
                      if (!updatePassResponse) {
                        response.put(
                            JsonKey.ERROR_MSG, ResponseMessage.Message.ERROR_USER_UPDATE_PASSWORD);
                      }
                      return response;
                    }
                  },
                  getContext().dispatcher());
      Patterns.pipe(future, getContext().dispatcher()).to(sender());
    }

    processTelemetry(userMap, null, null, userId, actorMessage.getContext());
  }

  private void removeUnwanted(Map<String, Object> reqMap) {
    reqMap.remove(JsonKey.ADDRESS);
    reqMap.remove(JsonKey.EDUCATION);
    reqMap.remove(JsonKey.JOB_PROFILE);
    reqMap.remove(JsonKey.ORGANISATION);
    reqMap.remove(JsonKey.REGISTERED_ORG);
    reqMap.remove(JsonKey.ROOT_ORG);
    reqMap.remove(JsonKey.IDENTIFIER);
    reqMap.remove(JsonKey.ORGANISATIONS);
    reqMap.remove(JsonKey.IS_DELETED);
    reqMap.remove(JsonKey.EXTERNAL_ID);
    reqMap.remove(JsonKey.ID_TYPE);
    reqMap.remove(JsonKey.EXTERNAL_ID_TYPE);
    reqMap.remove(JsonKey.PROVIDER);
    reqMap.remove(JsonKey.EXTERNAL_ID_PROVIDER);
    reqMap.remove(JsonKey.EXTERNAL_IDS);
    reqMap.remove(JsonKey.ORGANISATION_ID);
    reqMap.remove(JsonKey.ROLES);
    Util.getUserDefaultValue()
        .keySet()
        .stream()
        .forEach(
            key -> {
              if (!JsonKey.PASSWORD.equalsIgnoreCase(key)) {
                reqMap.remove(key);
              }
            });
  }

  private void convertValidatedLocationCodesToIDs(
      Map<String, Object> userMap, RequestContext context) {
    if (userMap.containsKey(JsonKey.LOCATION_IDS)
        && CollectionUtils.isEmpty((List<String>) userMap.get(JsonKey.LOCATION_IDS))) {
      userMap.remove(JsonKey.LOCATION_IDS);
    }
    if (!userMap.containsKey(JsonKey.LOCATION_IDS)
        && userMap.containsKey(JsonKey.LOCATION_CODES)
        && !CollectionUtils.isEmpty((List<String>) userMap.get(JsonKey.LOCATION_CODES))) {
      List<Map<String, String>> locationIdTypeList =
          locationService.getValidatedRelatedLocationIdAndType(
              (List<String>) userMap.get(JsonKey.LOCATION_CODES), context);
      if (locationIdTypeList != null && !locationIdTypeList.isEmpty()) {
        try {
          userMap.put(JsonKey.PROFILE_LOCATION, mapper.writeValueAsString(locationIdTypeList));
        } catch (Exception ex) {
          logger.error(context, "Exception occurred while mapping", ex);
          ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
        }

        userMap.remove(JsonKey.LOCATION_CODES);
      } else {
        ProjectCommonException.throwClientErrorException(
            ResponseCode.invalidParameterValue,
            MessageFormat.format(
                ResponseCode.invalidParameterValue.getErrorMessage(),
                JsonKey.LOCATION_CODES,
                userMap.get(JsonKey.LOCATION_CODES)));
      }
    }
  }

  private void ignoreOrAcceptFrameworkData(
      Map<String, Object> userRequestMap,
      Map<String, Object> userDbRecord,
      RequestContext context) {
    try {
      validateUserFrameworkData(userRequestMap, userDbRecord, context);
    } catch (ProjectCommonException pce) {
      // Could be that the framework id or value - is invalid, missing.
      userRequestMap.remove(JsonKey.FRAMEWORK);
    }
  }

  @SuppressWarnings("unchecked")
  private void validateUserFrameworkData(
      Map<String, Object> userRequestMap,
      Map<String, Object> userDbRecord,
      RequestContext context) {
    if (userRequestMap.containsKey(JsonKey.FRAMEWORK)) {
      Map<String, Object> framework = (Map<String, Object>) userRequestMap.get(JsonKey.FRAMEWORK);
      List<String> frameworkIdList;
      if (framework.get(JsonKey.ID) instanceof String) {
        String frameworkIdString = (String) framework.remove(JsonKey.ID);
        frameworkIdList = new ArrayList<>();
        frameworkIdList.add(frameworkIdString);
        framework.put(JsonKey.ID, frameworkIdList);
      } else {
        frameworkIdList = (List<String>) framework.get(JsonKey.ID);
      }
      userRequestMap.put(JsonKey.FRAMEWORK, framework);
      List<String> frameworkFields =
          DataCacheHandler.getFrameworkFieldsConfig().get(JsonKey.FIELDS);
      List<String> frameworkMandatoryFields =
          DataCacheHandler.getFrameworkFieldsConfig().get(JsonKey.MANDATORY_FIELDS);
      userRequestValidator.validateMandatoryFrameworkFields(
          userRequestMap, frameworkFields, frameworkMandatoryFields);
      Map<String, Object> rootOrgMap =
          Util.getOrgDetails((String) userDbRecord.get(JsonKey.ROOT_ORG_ID), context);
      String hashtagId = (String) rootOrgMap.get(JsonKey.HASHTAGID);

      verifyFrameworkId(hashtagId, frameworkIdList, context);
      Map<String, List<Map<String, String>>> frameworkCachedValue =
          getFrameworkDetails(frameworkIdList.get(0), context);
      ((Map<String, Object>) userRequestMap.get(JsonKey.FRAMEWORK)).remove(JsonKey.ID);
      userRequestValidator.validateFrameworkCategoryValues(userRequestMap, frameworkCachedValue);
      ((Map<String, Object>) userRequestMap.get(JsonKey.FRAMEWORK))
          .put(JsonKey.ID, frameworkIdList);
    }
  }

  public static void verifyFrameworkId(
      String hashtagId, List<String> frameworkIdList, RequestContext context) {
    List<String> frameworks = DataCacheHandler.getHashtagIdFrameworkIdMap().get(hashtagId);
    String frameworkId = frameworkIdList.get(0);
    if (frameworks != null && frameworks.contains(frameworkId)) {
      return;
    } else {
      Map<String, List<Map<String, String>>> frameworkDetails =
          getFrameworkDetails(frameworkId, context);
      if (frameworkDetails == null)
        throw new ProjectCommonException(
            ResponseCode.errorNoFrameworkFound.getErrorCode(),
            ResponseCode.errorNoFrameworkFound.getErrorMessage(),
            ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }
  }

  public static Map<String, List<Map<String, String>>> getFrameworkDetails(
      String frameworkId, RequestContext context) {
    if (DataCacheHandler.getFrameworkCategoriesMap().get(frameworkId) == null) {
      handleGetFrameworkDetails(frameworkId, context);
    }
    return DataCacheHandler.getFrameworkCategoriesMap().get(frameworkId);
  }

  private static void handleGetFrameworkDetails(String frameworkId, RequestContext context) {
    Map<String, Object> response = ContentStoreUtil.readFramework(frameworkId, context);
    Map<String, List<Map<String, String>>> frameworkCacheMap = new HashMap<>();
    List<String> supportedfFields = DataCacheHandler.getFrameworkFieldsConfig().get(JsonKey.FIELDS);
    Map<String, Object> result = (Map<String, Object>) response.get(JsonKey.RESULT);
    if (MapUtils.isNotEmpty(result)) {
      Map<String, Object> frameworkDetails = (Map<String, Object>) result.get(JsonKey.FRAMEWORK);
      if (MapUtils.isNotEmpty(frameworkDetails)) {
        List<Map<String, Object>> frameworkCategories =
            (List<Map<String, Object>>) frameworkDetails.get(JsonKey.CATEGORIES);
        if (CollectionUtils.isNotEmpty(frameworkCategories)) {
          for (Map<String, Object> frameworkCategoriesValue : frameworkCategories) {
            String frameworkField = (String) frameworkCategoriesValue.get(JsonKey.CODE);
            if (supportedfFields.contains(frameworkField)) {
              List<Map<String, String>> listOfFields = new ArrayList<>();
              List<Map<String, Object>> frameworkTermList =
                  (List<Map<String, Object>>) frameworkCategoriesValue.get(JsonKey.TERMS);
              if (CollectionUtils.isNotEmpty(frameworkTermList)) {
                for (Map<String, Object> frameworkTerm : frameworkTermList) {
                  String id = (String) frameworkTerm.get(JsonKey.IDENTIFIER);
                  String name = (String) frameworkTerm.get(JsonKey.NAME);
                  Map<String, String> writtenValue = new HashMap<>();
                  writtenValue.put(JsonKey.ID, id);
                  writtenValue.put(JsonKey.NAME, name);
                  listOfFields.add(writtenValue);
                }
              }
              if (StringUtils.isNotBlank(frameworkField)
                  && CollectionUtils.isNotEmpty(listOfFields))
                frameworkCacheMap.put(frameworkField, listOfFields);
            }
            if (MapUtils.isNotEmpty(frameworkCacheMap))
              DataCacheHandler.updateFrameworkCategoriesMap(frameworkId, frameworkCacheMap);
          }
        }
      }
    }
  }

  private int userFlagsToNum(Map<String, Boolean> userBooleanMap) {
    int userFlagValue = 0;
    Set<Map.Entry<String, Boolean>> mapEntry = userBooleanMap.entrySet();
    for (Map.Entry<String, Boolean> entry : mapEntry) {
      if (StringUtils.isNotEmpty(entry.getKey())) {
        userFlagValue += UserFlagUtil.getFlagValue(entry.getKey(), entry.getValue());
      }
    }
    return userFlagValue;
  }

  private Response insertIntoUserLookUp(Map<String, Object> userMap, RequestContext context) {
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> lookUp = new HashMap<>();
    if (userMap.get(JsonKey.PHONE) != null) {
      lookUp.put(JsonKey.TYPE, JsonKey.PHONE);
      lookUp.put(JsonKey.USER_ID, userMap.get(JsonKey.ID));
      lookUp.put(JsonKey.VALUE, userMap.get(JsonKey.PHONE));
      list.add(lookUp);
    }
    if (userMap.get(JsonKey.EMAIL) != null) {
      lookUp = new HashMap<>();
      lookUp.put(JsonKey.TYPE, JsonKey.EMAIL);
      lookUp.put(JsonKey.USER_ID, userMap.get(JsonKey.ID));
      lookUp.put(JsonKey.VALUE, userMap.get(JsonKey.EMAIL));
      list.add(lookUp);
    }
    if (CollectionUtils.isNotEmpty((List) userMap.get(JsonKey.EXTERNAL_IDS))) {
      Map<String, Object> externalId =
          ((List<Map<String, Object>>) userMap.get(JsonKey.EXTERNAL_IDS))
              .stream()
              .filter(
                  x -> ((String) x.get(JsonKey.ID_TYPE)).equals((String) x.get(JsonKey.PROVIDER)))
              .findFirst()
              .orElse(null);
      if (MapUtils.isNotEmpty(externalId)) {
        lookUp = new HashMap<>();
        lookUp.put(JsonKey.TYPE, JsonKey.USER_LOOKUP_FILED_EXTERNAL_ID);
        lookUp.put(JsonKey.USER_ID, userMap.get(JsonKey.ID));
        // provider is the orgId, not the channel
        lookUp.put(
            JsonKey.VALUE, externalId.get(JsonKey.ID) + "@" + externalId.get(JsonKey.PROVIDER));
        list.add(lookUp);
      }
    }
    if (userMap.get(JsonKey.USERNAME) != null) {
      lookUp = new HashMap<>();
      lookUp.put(JsonKey.TYPE, JsonKey.USER_LOOKUP_FILED_USER_NAME);
      lookUp.put(JsonKey.USER_ID, userMap.get(JsonKey.ID));
      lookUp.put(JsonKey.VALUE, userMap.get(JsonKey.USERNAME));
      list.add(lookUp);
    }
    Response response = null;
    if (CollectionUtils.isNotEmpty(list)) {
      response = userLookupService.insertRecords(list, context);
    }
    return response;
  }

  private Map<String, Object> saveUserOrgInfo(Map<String, Object> userMap, RequestContext context) {
    Map<String, Object> userOrgMap = createUserOrgRequestData(userMap);
    cassandraOperation.insertRecord(
        userOrgDb.getKeySpace(), userOrgDb.getTableName(), userOrgMap, context);

    return userOrgMap;
  }

  private Map<String, Object> createUserOrgRequestData(Map<String, Object> userMap) {
    Map<String, Object> userOrgMap = new HashMap<String, Object>();
    userOrgMap.put(JsonKey.ID, ProjectUtil.getUniqueIdFromTimestamp(1));
    userOrgMap.put(JsonKey.HASHTAGID, userMap.get(JsonKey.ROOT_ORG_ID));
    userOrgMap.put(JsonKey.USER_ID, userMap.get(JsonKey.USER_ID));
    userOrgMap.put(JsonKey.ORGANISATION_ID, userMap.get(JsonKey.ROOT_ORG_ID));
    userOrgMap.put(JsonKey.ORG_JOIN_DATE, ProjectUtil.getFormattedDate());
    userOrgMap.put(JsonKey.IS_DELETED, false);
    userOrgMap.put(JsonKey.ASSOCIATION_TYPE, AssociationMechanism.SELF_DECLARATION);
    return userOrgMap;
  }

  /**
   * Get managed user list for LUA uuid (JsonKey.ID) and fetch encrypted token for eac user from
   * admin utils if the JsonKey.WITH_TOKENS value sent in query param is true
   *
   * @param request Request
   */
  private void getManagedUsers(Request request) {
    // LUA uuid/ManagedBy Id
    String uuid = (String) request.get(JsonKey.ID);

    boolean withTokens = Boolean.valueOf((String) request.get(JsonKey.WITH_TOKENS));

    Map<String, Object> searchResult =
        userClient.searchManagedUser(
            getActorRef(ActorOperations.USER_SEARCH.getValue()),
            request,
            request.getRequestContext());
    List<Map<String, Object>> userList = (List) searchResult.get(JsonKey.CONTENT);

    List<Map<String, Object>> activeUserList = null;
    if (CollectionUtils.isNotEmpty(userList)) {
      activeUserList =
          userList
              .stream()
              .filter(o -> !BooleanUtils.isTrue((Boolean) o.get(JsonKey.IS_DELETED)))
              .collect(Collectors.toList());
    }
    if (withTokens && CollectionUtils.isNotEmpty(activeUserList)) {
      // Fetch encrypted token from admin utils
      Map<String, Object> encryptedTokenList =
          userService.fetchEncryptedToken(uuid, activeUserList, request.getRequestContext());
      // encrypted token for each managedUser in respList
      userService.appendEncryptedToken(
          encryptedTokenList, activeUserList, request.getRequestContext());
    }
    Map<String, Object> responseMap = new HashMap<>();
    if (CollectionUtils.isNotEmpty(activeUserList)) {
      responseMap.put(JsonKey.CONTENT, activeUserList);
      responseMap.put(JsonKey.COUNT, activeUserList.size());
    } else {
      responseMap.put(JsonKey.CONTENT, new ArrayList<Map<String, Object>>());
      responseMap.put(JsonKey.COUNT, 0);
    }
    Response response = new Response();
    response.put(JsonKey.RESPONSE, responseMap);
    sender().tell(response, self());
  }
}
