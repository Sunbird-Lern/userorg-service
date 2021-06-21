package org.sunbird.user.actors;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.actorutil.location.LocationClient;
import org.sunbird.actorutil.location.impl.LocationClientImpl;
import org.sunbird.actorutil.org.OrganisationClient;
import org.sunbird.actorutil.org.impl.OrganisationClientImpl;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LocationActorOperation;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.TelemetryEnvKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.common.util.Matcher;
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
import org.sunbird.models.organisation.Organisation;
import org.sunbird.models.user.User;
import org.sunbird.models.user.UserDeclareEntity;
import org.sunbird.models.user.org.UserOrg;
import org.sunbird.telemetry.util.TelemetryUtil;
import org.sunbird.user.dao.UserOrgDao;
import org.sunbird.user.dao.UserSelfDeclarationDao;
import org.sunbird.user.dao.impl.UserOrgDaoImpl;
import org.sunbird.user.dao.impl.UserSelfDeclarationDaoImpl;
import org.sunbird.user.service.AssociationMechanism;
import org.sunbird.user.service.UserLookupService;
import org.sunbird.user.service.UserService;
import org.sunbird.user.service.impl.UserLookUpServiceImpl;
import org.sunbird.user.service.impl.UserServiceImpl;
import org.sunbird.user.util.UserActorOperations;
import org.sunbird.user.util.UserUtil;
import org.sunbird.validator.user.UserRequestValidator;

@ActorConfig(
  tasks = {"updateUser", "updateUserV2"},
  asyncTasks = {},
  dispatcher = "most-used-one-dispatcher"
)
public class UserUpdateActor extends BaseActor {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private UserRequestValidator userRequestValidator = new UserRequestValidator();
  private LocationClient locationClient = LocationClientImpl.getInstance();
  private ObjectMapper mapper = new ObjectMapper();
  private UserService userService = UserServiceImpl.getInstance();
  private LocationService locationService = LocationServiceImpl.getInstance();
  private Util.DbInfo usrDbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
  private UserLookupService userLookupService = UserLookUpServiceImpl.getInstance();
  private UserSelfDeclarationDao userSelfDeclarationDao = UserSelfDeclarationDaoImpl.getInstance();

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    String operation = request.getOperation();
    switch (operation) {
      case "updateUser":
      case "updateUserV2":
        updateUser(request);
        break;
      default:
        onReceiveUnsupportedOperation("UserUpdateActor");
    }
  }

  private void updateUser(Request actorMessage) {
    actorMessage.toLower();
    String callerId = (String) actorMessage.getContext().get(JsonKey.CALLER_ID);
    Map<String, Object> userMap = actorMessage.getRequest();
    logger.info(actorMessage.getRequestContext(), "Incoming update request body: " + userMap);
    userRequestValidator.validateUpdateUserRequest(actorMessage);
    // update externalIds provider from channel to orgId
    UserUtil.updateExternalIdsProviderWithOrgId(userMap, actorMessage.getRequestContext());
    Map<String, Object> userDbRecord =
        UserUtil.validateExternalIdsAndReturnActiveUser(userMap, actorMessage.getRequestContext());
    String managedById = (String) userDbRecord.get(JsonKey.MANAGED_BY);
    if (actorMessage.getOperation().equalsIgnoreCase(ActorOperations.UPDATE_USER_V2.getValue())) {
      setProfileUserTypeAndLocation(userMap, actorMessage);
    } else {
      if (userMap.containsKey(JsonKey.PROFILE_LOCATION)) {
        userMap.remove(JsonKey.PROFILE_LOCATION);
      }
      if (userMap.containsKey(JsonKey.PROFILE_USERTYPE)) {
        userMap.remove(JsonKey.PROFILE_USERTYPE);
      }
    }
    validateLocationCodes(actorMessage);
    validateUserTypeAndSubType(
        actorMessage.getRequest(), userDbRecord, actorMessage.getRequestContext());
    if (StringUtils.isNotBlank(callerId)) {
      userService.validateUploader(actorMessage, actorMessage.getRequestContext());
    } else {
      userService.validateUserId(actorMessage, managedById, actorMessage.getRequestContext());
    }

    validateUserFrameworkData(userMap, userDbRecord, actorMessage.getRequestContext());
    // Check if the user is Custodian Org user
    boolean isCustodianOrgUser = isCustodianOrgUser((String) userDbRecord.get(JsonKey.ROOT_ORG_ID));
    encryptExternalDetails(userMap, userDbRecord);
    User user = mapper.convertValue(userMap, User.class);
    UserUtil.validateExternalIdsForUpdateUser(
        user, isCustodianOrgUser, actorMessage.getRequestContext());
    userMap.put(JsonKey.EXTERNAL_IDS, user.getExternalIds());
    updateLocationCodeToIds(
        (List<Map<String, String>>) userMap.get(JsonKey.EXTERNAL_IDS),
        actorMessage.getRequestContext());
    UserUtil.validateUserPhoneAndEmailUniqueness(
        user, JsonKey.UPDATE, actorMessage.getRequestContext());
    // not allowing user to update the status,provider,userName
    removeFieldsFrmReq(userMap);
    convertValidatedLocationCodesToIDs(userMap, actorMessage.getRequestContext());
    userMap.put(JsonKey.UPDATED_DATE, ProjectUtil.getFormattedDate());
    if (StringUtils.isBlank(callerId)) {
      userMap.put(JsonKey.UPDATED_BY, actorMessage.getContext().get(JsonKey.REQUESTED_BY));
    }
    UserUtil.addMaskEmailAndMaskPhone(userMap);
    Map<String, Object> requestMap = UserUtil.encryptUserData(userMap);
    validateRecoveryEmailPhone(userDbRecord, userMap);
    Map<String, Object> userLookUpData = new HashMap<>(requestMap);
    removeUnwanted(requestMap);
    if (requestMap.containsKey(JsonKey.TNC_ACCEPTED_ON)) {
      requestMap.put(
          JsonKey.TNC_ACCEPTED_ON, new Timestamp((Long) requestMap.get(JsonKey.TNC_ACCEPTED_ON)));
    }
    // update userSubType to null if userType is changed and subType are not provided
    if (requestMap.containsKey(JsonKey.USER_TYPE)
        && !requestMap.containsKey(JsonKey.USER_SUB_TYPE)) {
      requestMap.put(JsonKey.USER_SUB_TYPE, null);
    }

    Map<String, Boolean> userBooleanMap =
        updatedUserFlagsMap(userMap, userDbRecord, actorMessage.getRequestContext());
    int userFlagValue = userFlagsToNum(userBooleanMap);
    requestMap.put(JsonKey.FLAGS_VALUE, userFlagValue);
    // As of now disallowing updating manageble user's phone/email, will le allowed in next release
    boolean resetPasswordLink = false;
    if (StringUtils.isNotEmpty(managedById)
        && ((StringUtils.isNotEmpty((String) requestMap.get(JsonKey.EMAIL))
            || (StringUtils.isNotEmpty((String) requestMap.get(JsonKey.PHONE)))))) {
      requestMap.put(JsonKey.MANAGED_BY, null);
      resetPasswordLink = true;
    }

    Response response =
        cassandraOperation.updateRecord(
            usrDbInfo.getKeySpace(),
            usrDbInfo.getTableName(),
            requestMap,
            actorMessage.getRequestContext());
    insertIntoUserLookUp(userLookUpData, actorMessage.getRequestContext());
    removeUserLookupEntry(userLookUpData, userDbRecord, actorMessage.getRequestContext());
    if (StringUtils.isNotBlank(callerId)) {
      userMap.put(JsonKey.ROOT_ORG_ID, actorMessage.getContext().get(JsonKey.ROOT_ORG_ID));
    }
    Response resp = null;
    if (((String) response.get(JsonKey.RESPONSE)).equalsIgnoreCase(JsonKey.SUCCESS)) {
      if (StringUtils.isNotEmpty((String) userMap.get(JsonKey.ORG_EXTERNAL_ID))) {
        OrganisationClient organisationClient = OrganisationClientImpl.getInstance();
        Map<String, Object> filters = new HashMap<>();
        filters.put(JsonKey.EXTERNAL_ID, userMap.get(JsonKey.ORG_EXTERNAL_ID));
        if (StringUtils.isNotEmpty((String) userMap.get(JsonKey.STATE_ID))) {
          filters.put(
              String.join(".", JsonKey.ORG_LOCATION, JsonKey.ID), userMap.get(JsonKey.STATE_ID));
        } else {
          logger.info(
              actorMessage.getRequestContext(), "profileLocation is empty in user update request.");
          List<Map<String, String>> profileLocation =
              (List<Map<String, String>>) userDbRecord.get(JsonKey.PROFILE_LOCATION);
          profileLocation
              .stream()
              .forEach(
                  loc -> {
                    String locType = loc.get(JsonKey.TYPE);
                    if (JsonKey.STATE.equalsIgnoreCase(locType)) {
                      filters.put(
                          String.join(".", JsonKey.ORG_LOCATION, JsonKey.ID), loc.get(JsonKey.ID));
                    }
                  });
        }
        logger.info(
            actorMessage.getRequestContext(),
            "fetching org by orgExternalId and orgLocationId : " + filters);
        List<Organisation> organisations =
            organisationClient.esSearchOrgByFilter(filters, actorMessage.getRequestContext());
        if (organisations.size() == 0 || organisations.size() > 1) {
          logger.info(
              actorMessage.getRequestContext(),
              "Got empty search result by orgExternalId and orgLocationId : " + filters);
        } else {
          Map<String, Object> org =
              (Map<String, Object>) mapper.convertValue(organisations.get(0), Map.class);
          List<Map<String, Object>> orgList = new ArrayList();
          if (MapUtils.isNotEmpty(org)) {
            orgList.add(org);
          }
          actorMessage.getRequest().put(JsonKey.ORGANISATIONS, orgList);
          actorMessage.getRequest().put(JsonKey.ROOT_ORG_ID, userDbRecord.get(JsonKey.ROOT_ORG_ID));
          updateUserOrganisations(actorMessage);
        }
      }
      Map<String, Object> userRequest = new HashMap<>(userMap);
      userRequest.put(JsonKey.OPERATION_TYPE, JsonKey.UPDATE);
      userRequest.put(JsonKey.CALLER_ID, callerId);

      resp =
          userService.saveUserAttributes(
              userRequest,
              getActorRef(UserActorOperations.SAVE_USER_ATTRIBUTES.getValue()),
              actorMessage.getRequestContext());
    } else {
      logger.info(
          actorMessage.getRequestContext(), "UserManagementActor:updateUser: User update failure");
    }
    if (null != resp) {
      response.put(
          JsonKey.ERRORS,
          ((Map<String, Object>) resp.getResult().get(JsonKey.RESPONSE)).get(JsonKey.ERRORS));
    }
    sender().tell(response, self());
    // Managed-users should get ResetPassword Link
    if (resetPasswordLink) {
      sendResetPasswordLink(requestMap, actorMessage.getRequestContext());
    }
    if (null != resp) {
      Map<String, Object> completeUserDetails = new HashMap<>(userDbRecord);
      completeUserDetails.putAll(requestMap);
      saveUserDetailsToEs(completeUserDetails, actorMessage.getRequestContext());
    }
    Map<String, Object> targetObject = null;
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    targetObject =
        TelemetryUtil.generateTargetObject(
            (String) userMap.get(JsonKey.USER_ID), TelemetryEnvKey.USER, JsonKey.UPDATE, null);
    TelemetryUtil.telemetryProcessingCall(
        userMap, targetObject, correlatedObject, actorMessage.getContext());
  }

  private void setProfileUserTypeAndLocation(Map<String, Object> userMap, Request actorMessage) {
    userMap.remove(JsonKey.USER_TYPE);
    userMap.remove(JsonKey.USER_SUB_TYPE);
    if (userMap.containsKey(JsonKey.PROFILE_USERTYPE)) {
      Map<String, Object> userTypeAndSubType =
          (Map<String, Object>) userMap.get(JsonKey.PROFILE_USERTYPE);
      userMap.put(JsonKey.USER_TYPE, userTypeAndSubType.get(JsonKey.TYPE));
      userMap.put(JsonKey.USER_SUB_TYPE, userTypeAndSubType.get(JsonKey.SUB_TYPE));
    }
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
      // As of now locationCode can take array of only locationcodes and map of locationCodes which
      // include type and code of the location
      String stateCode = null;
      List<String> set = new ArrayList<>();
      List<Location> locationList = new ArrayList<>();
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
      String stateId = null;
      for (Location location : locationList) {
        // for create-MUA we allow locations upto district for remaining we will validate all.
        if (((userRequest.getOperation().equals(ActorOperations.CREATE_USER_V4.getValue())
                    || userRequest
                        .getOperation()
                        .equals(ActorOperations.CREATE_MANAGED_USER.getValue()))
                && ((location.getType().equals(JsonKey.STATE))
                    || (location.getType().equals(JsonKey.DISTRICT))))
            || (!userRequest.getOperation().equals(ActorOperations.CREATE_USER_V4.getValue())
                && !userRequest
                    .getOperation()
                    .equals(ActorOperations.CREATE_MANAGED_USER.getValue()))) {
          isValidLocationType(location.getType(), typeList);
          if (location.getType().equalsIgnoreCase(JsonKey.STATE)) {
            stateId = location.getId();
          }
          if (!location.getType().equals(JsonKey.LOCATION_TYPE_SCHOOL)) {
            set.add(location.getCode());
          } else {
            userRequest.getRequest().put(JsonKey.ORG_EXTERNAL_ID, location.getCode());
          }
        }
      }
      if (StringUtils.isNotBlank((String) userRequest.getRequest().get(JsonKey.ORG_EXTERNAL_ID))) {
        userRequest.getRequest().put(JsonKey.STATE_ID, stateId);
      }
      userRequest.getRequest().put(JsonKey.LOCATION_CODES, set);
    }
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

  private void validateUserTypeAndSubType(
      Map<String, Object> userMap, Map<String, Object> userDbRecord, RequestContext context) {
    if (null != userMap.get(JsonKey.USER_TYPE)) {
      List<String> locationCodes = (List<String>) userMap.get(JsonKey.LOCATION_CODES);
      List<Location> locations = new ArrayList<>();
      if (CollectionUtils.isEmpty(locationCodes)) {
        // userDbRecord is record from ES , so it contains complete user data and profileLocation as
        // List<Map<String, String>>
        List<Map<String, String>> profLocList =
            (List<Map<String, String>>) userDbRecord.get(JsonKey.PROFILE_LOCATION);
        List<String> locationIds = null;
        if (CollectionUtils.isNotEmpty(profLocList)) {
          locationIds =
              profLocList.stream().map(m -> m.get(JsonKey.ID)).collect(Collectors.toList());
        }
        // Get location code from user records locations Ids
        logger.info(
            context,
            String.format(
                "Locations for userId:%s is:%s", userMap.get(JsonKey.USER_ID), locationIds));
        if (CollectionUtils.isNotEmpty(locationIds)) {
          locations =
              locationClient.getLocationByIds(
                  getActorRef(LocationActorOperation.SEARCH_LOCATION.getValue()),
                  locationIds,
                  context);
        }
      } else {
        locations =
            locationClient.getLocationsByCodes(
                getActorRef(LocationActorOperation.SEARCH_LOCATION.getValue()),
                locationCodes,
                context);
      }
      if (CollectionUtils.isNotEmpty(locations)) {
        String stateCode = null;
        for (Location location : locations) {
          if (JsonKey.STATE.equals(location.getType())) {
            stateCode = location.getCode();
          }
        }
        logger.info(context, String.format("Validating UserType for state code:%s", stateCode));
        if (StringUtils.isNotBlank(stateCode)) {
          // Validate UserType and UserSubType configure based on user state config else user
          // default config
          validateUserTypeAndSubType(userMap, context, stateCode);
        }
      } else {
        // If location is null or empty .Validate with default config
        logger.info(
            context,
            String.format("Validating UserType for state code:%s", JsonKey.DEFAULT_PERSONA));
        validateUserTypeAndSubType(userMap, context, JsonKey.DEFAULT_PERSONA);
      }
    }
  }

  private void validateUserTypeAndSubType(
      Map<String, Object> userMap, RequestContext context, String stateCode) {
    String stateCodeConfig = userRequestValidator.validateUserType(userMap, stateCode, context);
    userRequestValidator.validateUserSubType(userMap, stateCodeConfig);
    // after all validations set userType and userSubtype to profileUsertype
    profileUserType(userMap, context);
  }

  private void profileUserType(Map<String, Object> userMap, RequestContext requestContext) {
    Map<String, String> userTypeAndSubType = new HashMap<>();
    userMap.remove(JsonKey.PROFILE_USERTYPE);
    if (userMap.containsKey(JsonKey.USER_TYPE)) {
      userTypeAndSubType.put(JsonKey.TYPE, (String) userMap.get(JsonKey.USER_TYPE));
      if (userMap.containsKey(JsonKey.USER_SUB_TYPE)) {
        userTypeAndSubType.put(JsonKey.SUB_TYPE, (String) userMap.get(JsonKey.USER_SUB_TYPE));
      } else {
        userTypeAndSubType.put(JsonKey.SUB_TYPE, null);
      }
      try {
        userMap.put(JsonKey.PROFILE_USERTYPE, mapper.writeValueAsString(userTypeAndSubType));
      } catch (Exception ex) {
        logger.error(requestContext, "Exception occurred while mapping", ex);
        ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
      }

      userMap.remove(JsonKey.USER_TYPE);
      userMap.remove(JsonKey.USER_SUB_TYPE);
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

  private boolean isCustodianOrgUser(String userRootOrgId) {
    String custodianRootOrgId = DataCacheHandler.getConfigSettings().get(JsonKey.CUSTODIAN_ORG_ID);
    if (StringUtils.isNotBlank(custodianRootOrgId) && StringUtils.isNotBlank(userRootOrgId)) {
      return userRootOrgId.equalsIgnoreCase(custodianRootOrgId);
    }
    return false;
  }

  private void encryptExternalDetails(
      Map<String, Object> userMap, Map<String, Object> userDbRecords) {
    List<Map<String, Object>> extList =
        (List<Map<String, Object>>) userMap.get(JsonKey.EXTERNAL_IDS);
    if (!(extList == null || extList.isEmpty())) {
      extList.forEach(
          map -> {
            try {
              String idType = (String) map.get(JsonKey.ID_TYPE);
              switch (idType) {
                case JsonKey.DECLARED_EMAIL:
                case JsonKey.DECLARED_PHONE:
                  /* Check whether email and phone contains mask value, if mask then copy the
                      encrypted value from user table
                  * */
                  if (UserUtility.isMasked((String) map.get(JsonKey.ID))) {
                    if (idType.equals(JsonKey.DECLARED_EMAIL)) {
                      map.put(JsonKey.ID, userDbRecords.get(JsonKey.EMAIL));
                    } else {
                      map.put(JsonKey.ID, userDbRecords.get(JsonKey.PHONE));
                    }
                  } else {
                    // If not masked encrypt the plain text
                    map.put(JsonKey.ID, UserUtility.encryptData((String) map.get(JsonKey.ID)));
                  }
                  break;
                default: // do nothing
              }

            } catch (Exception e) {
              logger.error("Error in encrypting in the external id details", e);
              throw new ProjectCommonException(
                  ResponseCode.dataEncryptionError.getErrorCode(),
                  ResponseCode.dataEncryptionError.getErrorMessage(),
                  ResponseCode.dataEncryptionError.getResponseCode());
            }
          });
    }
  }

  private void updateLocationCodeToIds(
      List<Map<String, String>> externalIds, RequestContext context) {
    List<String> locCodeLst = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(externalIds)) {
      externalIds.forEach(
          externalIdMap -> {
            if (externalIdMap.containsValue(JsonKey.DECLARED_STATE)
                || externalIdMap.containsValue(JsonKey.DECLARED_DISTRICT)) {
              locCodeLst.add(externalIdMap.get(JsonKey.ID));
            }
          });
      List<Location> locationIdList =
          locationClient.getLocationByCodes(
              getActorRef(LocationActorOperation.GET_RELATED_LOCATION_IDS.getValue()),
              locCodeLst,
              context);
      if (CollectionUtils.isNotEmpty(locationIdList)) {
        locationIdList.forEach(
            location -> {
              externalIds.forEach(
                  externalIdMap -> {
                    if (externalIdMap.containsValue(JsonKey.DECLARED_STATE)
                        || externalIdMap.containsValue(JsonKey.DECLARED_DISTRICT)) {
                      if (location.getCode().equals(externalIdMap.get(JsonKey.ID))) {
                        externalIdMap.put(JsonKey.ID, location.getId());
                        externalIdMap.put(JsonKey.ORIGINAL_EXTERNAL_ID, location.getId());
                      }
                    }
                  });
            });
      }
    }
  }

  private void removeFieldsFrmReq(Map<String, Object> userMap) {
    userMap.remove(JsonKey.ENC_EMAIL);
    userMap.remove(JsonKey.ENC_PHONE);
    userMap.remove(JsonKey.STATUS);
    userMap.remove(JsonKey.PROVIDER);
    userMap.remove(JsonKey.USERNAME);
    userMap.remove(JsonKey.ROOT_ORG_ID);
    userMap.remove(JsonKey.LOGIN_ID);
    userMap.remove(JsonKey.ROLES);
    userMap.remove(JsonKey.CHANNEL);
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

  private void validateRecoveryEmailPhone(
      Map<String, Object> userDbRecord, Map<String, Object> userReqMap) {
    String userPrimaryPhone = (String) userDbRecord.get(JsonKey.PHONE);
    String userPrimaryEmail = (String) userDbRecord.get(JsonKey.EMAIL);
    String recoveryEmail = (String) userReqMap.get(JsonKey.RECOVERY_EMAIL);
    String recoveryPhone = (String) userReqMap.get(JsonKey.RECOVERY_PHONE);
    if (StringUtils.isNotBlank(recoveryEmail)
        && Matcher.matchIdentifiers(userPrimaryEmail, recoveryEmail)) {
      throwRecoveryParamsMatchException(JsonKey.EMAIL, JsonKey.RECOVERY_EMAIL);
    }
    if (StringUtils.isNotBlank(recoveryPhone)
        && Matcher.matchIdentifiers(userPrimaryPhone, recoveryPhone)) {
      throwRecoveryParamsMatchException(JsonKey.PHONE, JsonKey.RECOVERY_PHONE);
    }
    validatePrimaryEmailOrPhone(userDbRecord, userReqMap);
    validatePrimaryAndRecoveryKeys(userReqMap);
  }

  private void validatePrimaryEmailOrPhone(
      Map<String, Object> userDbRecord, Map<String, Object> userReqMap) {
    String userPrimaryPhone = (String) userReqMap.get(JsonKey.PHONE);
    String userPrimaryEmail = (String) userReqMap.get(JsonKey.EMAIL);
    String recoveryEmail = (String) userDbRecord.get(JsonKey.RECOVERY_EMAIL);
    String recoveryPhone = (String) userDbRecord.get(JsonKey.RECOVERY_PHONE);
    if (StringUtils.isNotBlank(userPrimaryEmail)
        && Matcher.matchIdentifiers(userPrimaryEmail, recoveryEmail)) {
      throwRecoveryParamsMatchException(JsonKey.EMAIL, JsonKey.RECOVERY_EMAIL);
    }
    if (StringUtils.isNotBlank(userPrimaryPhone)
        && Matcher.matchIdentifiers(userPrimaryPhone, recoveryPhone)) {
      throwRecoveryParamsMatchException(JsonKey.PHONE, JsonKey.RECOVERY_PHONE);
    }
  }

  private void validatePrimaryAndRecoveryKeys(Map<String, Object> userReqMap) {
    String userPhone = (String) userReqMap.get(JsonKey.PHONE);
    String userEmail = (String) userReqMap.get(JsonKey.EMAIL);
    String userRecoveryEmail = (String) userReqMap.get(JsonKey.RECOVERY_EMAIL);
    String userRecoveryPhone = (String) userReqMap.get(JsonKey.RECOVERY_PHONE);
    if (StringUtils.isNotBlank(userEmail)
        && Matcher.matchIdentifiers(userEmail, userRecoveryEmail)) {
      throwRecoveryParamsMatchException(JsonKey.EMAIL, JsonKey.RECOVERY_EMAIL);
    }
    if (StringUtils.isNotBlank(userPhone)
        && Matcher.matchIdentifiers(userPhone, userRecoveryPhone)) {
      throwRecoveryParamsMatchException(JsonKey.PHONE, JsonKey.RECOVERY_PHONE);
    }
  }

  private void throwRecoveryParamsMatchException(String type, String recoveryType) {
    logger.info(
        "UserManagementActor:throwParamMatchException:".concat(recoveryType + "")
            + "should not same as primary ".concat(type + ""));
    ProjectCommonException.throwClientErrorException(
        ResponseCode.recoveryParamsMatchException,
        MessageFormat.format(
            ResponseCode.recoveryParamsMatchException.getErrorMessage(), recoveryType, type));
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

  private Map<String, Boolean> updatedUserFlagsMap(
      Map<String, Object> userMap, Map<String, Object> userDbRecord, RequestContext context) {
    Map<String, Boolean> userBooleanMap = new HashMap<>();

    // for existing users, it won't contain state-validation
    // adding in release-2.4.0
    // userDbRecord- record from es.
    if (!userDbRecord.containsKey(JsonKey.STATE_VALIDATED)) {
      setStateValidation(userDbRecord, userBooleanMap);
    } else {
      userBooleanMap.put(
          JsonKey.STATE_VALIDATED, (boolean) userDbRecord.get(JsonKey.STATE_VALIDATED));
    }
    return userBooleanMap;
  }

  private void setStateValidation(
      Map<String, Object> requestMap, Map<String, Boolean> userBooleanMap) {
    String rootOrgId = (String) requestMap.get(JsonKey.ROOT_ORG_ID);
    String custodianRootOrgId = DataCacheHandler.getConfigSettings().get(JsonKey.CUSTODIAN_ORG_ID);
    // if the user is creating for non-custodian(i.e state) the value is set as true else false
    userBooleanMap.put(JsonKey.STATE_VALIDATED, !custodianRootOrgId.equals(rootOrgId));
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

  private void removeUserLookupEntry(
      Map<String, Object> userLookUpData,
      Map<String, Object> userDbRecord,
      RequestContext requestContext) {
    List<Map<String, String>> reqList = new ArrayList<>();
    if (UserUtil.isEmailOrPhoneDiff(userLookUpData, userDbRecord, JsonKey.EMAIL)) {
      String email = (String) userDbRecord.get(JsonKey.EMAIL);
      Map<String, String> lookupMap = new LinkedHashMap<>();
      lookupMap.put(JsonKey.TYPE, JsonKey.EMAIL);
      lookupMap.put(JsonKey.VALUE, email);
      reqList.add(lookupMap);
    }
    if (UserUtil.isEmailOrPhoneDiff(userLookUpData, userDbRecord, JsonKey.PHONE)) {
      String phone = (String) userDbRecord.get(JsonKey.PHONE);
      Map<String, String> lookupMap = new LinkedHashMap<>();
      lookupMap.put(JsonKey.TYPE, JsonKey.PHONE);
      lookupMap.put(JsonKey.VALUE, phone);
      reqList.add(lookupMap);
    }
    if (CollectionUtils.isNotEmpty(reqList)) {
      userLookupService.deleteRecords(reqList, requestContext);
    }
  }

  private void updateUserOrganisations(Request actorMessage) {
    logger.info(
        actorMessage.getRequestContext(), "UserManagementActor: updateUserOrganisation called");
    List<Map<String, Object>> orgList = null;
    if (null != actorMessage.getRequest().get(JsonKey.ORGANISATIONS)) {
      orgList = (List<Map<String, Object>>) actorMessage.getRequest().get(JsonKey.ORGANISATIONS);
    }
    if (CollectionUtils.isNotEmpty(orgList)) {
      String userId = (String) actorMessage.getRequest().get(JsonKey.USER_ID);
      String rootOrgId = (String) actorMessage.getRequest().remove(JsonKey.ROOT_ORG_ID);
      List<Map<String, Object>> userOrgListDb =
          UserUtil.getUserOrgDetails(false, userId, actorMessage.getRequestContext());
      Map<String, Object> userOrgDbMap = new HashMap<>();
      if (CollectionUtils.isNotEmpty(userOrgListDb)) {
        userOrgListDb.forEach(
            userOrg -> userOrgDbMap.put((String) userOrg.get(JsonKey.ORGANISATION_ID), userOrg));
      }

      for (Map<String, Object> org : orgList) {
        createOrUpdateOrganisations(org, userOrgDbMap, actorMessage);
        updateUserSelfDeclaredData(actorMessage, org, userId);
      }

      String requestedBy = (String) actorMessage.getContext().get(JsonKey.REQUESTED_BY);
      removeOrganisations(userOrgDbMap, rootOrgId, requestedBy, actorMessage.getRequestContext());
      logger.info(
          actorMessage.getRequestContext(),
          "UserManagementActor:updateUserOrganisations : " + "updateUserOrganisation Completed");
    }
  }

  private void createOrUpdateOrganisations(
      Map<String, Object> org, Map<String, Object> userOrgDbMap, Request actorMessage) {
    UserOrgDao userOrgDao = UserOrgDaoImpl.getInstance();
    String userId = (String) actorMessage.getRequest().get(JsonKey.USER_ID);
    if (MapUtils.isNotEmpty(org)) {
      UserOrg userOrg =
          mapper.convertValue(org, UserOrg.class); // setting userOrg fields from org details
      String orgId =
          null != org.get(JsonKey.ORGANISATION_ID)
              ? (String) org.get(JsonKey.ORGANISATION_ID)
              : (String) org.get(JsonKey.ID);

      userOrg.setUserId(userId);
      userOrg.setDeleted(false);
      if (null != orgId && userOrgDbMap.containsKey(orgId)) {
        userOrg.setUpdatedDate(ProjectUtil.getFormattedDate());
        userOrg.setUpdatedBy((String) (actorMessage.getContext().get(JsonKey.REQUESTED_BY)));
        userOrg.setOrganisationId(
            (String) ((Map<String, Object>) userOrgDbMap.get(orgId)).get(JsonKey.ORGANISATION_ID));
        AssociationMechanism associationMechanism = new AssociationMechanism();
        if (null != userOrgDbMap.get(JsonKey.ASSOCIATION_TYPE)) {
          associationMechanism.setAssociationType(
              (int) ((Map<String, Object>) userOrgDbMap.get(orgId)).get(JsonKey.ASSOCIATION_TYPE));
        }
        associationMechanism.appendAssociationType(AssociationMechanism.SELF_DECLARATION);
        userOrg.setAssociationType(associationMechanism.getAssociationType());
        userOrgDao.updateUserOrg(userOrg, actorMessage.getRequestContext());
        userOrgDbMap.remove(orgId);
      } else {
        userOrg.setHashTagId((String) (org.get(JsonKey.HASHTAGID)));
        userOrg.setOrgJoinDate(ProjectUtil.getFormattedDate());
        userOrg.setAddedBy((String) actorMessage.getContext().get(JsonKey.REQUESTED_BY));
        userOrg.setId(ProjectUtil.getUniqueIdFromTimestamp(actorMessage.getEnv()));
        userOrg.setOrganisationId((String) (org.get(JsonKey.ID)));
        userOrg.setAssociationType(AssociationMechanism.SELF_DECLARATION);
        userOrgDao.createUserOrg(userOrg, actorMessage.getRequestContext());
      }
    }
  }

  private void removeOrganisations(
      Map<String, Object> userOrgDbMap,
      String rootOrgId,
      String requestedBy,
      RequestContext context) {
    Set<String> ids = userOrgDbMap.keySet();
    UserOrgDao userOrgDao = UserOrgDaoImpl.getInstance();
    ids.remove(rootOrgId);
    ObjectMapper mapper = new ObjectMapper();
    for (String id : ids) {
      UserOrg userOrg = mapper.convertValue(userOrgDbMap.get(id), UserOrg.class);
      userOrg.setDeleted(true);
      userOrg.setId((String) ((Map<String, Object>) userOrgDbMap.get(id)).get(JsonKey.ID));
      userOrg.setUpdatedDate(ProjectUtil.getFormattedDate());
      userOrg.setUpdatedBy(requestedBy);
      userOrg.setOrgLeftDate(ProjectUtil.getFormattedDate());
      userOrgDao.updateUserOrg(userOrg, context);
    }
  }

  private void updateUserSelfDeclaredData(Request actorMessage, Map org, String userId) {
    List<Map<String, Object>> declredDetails =
        userSelfDeclarationDao.getUserSelfDeclaredFields(userId, actorMessage.getRequestContext());
    if (!CollectionUtils.isEmpty(declredDetails)) {
      UserDeclareEntity userDeclareEntity =
          mapper.convertValue(declredDetails.get(0), UserDeclareEntity.class);
      Map declaredInfo = userDeclareEntity.getUserInfo();
      if (StringUtils.isEmpty((String) declaredInfo.get(JsonKey.DECLARED_SCHOOL_UDISE_CODE))
          || !org.get(JsonKey.EXTERNAL_ID)
              .equals(declaredInfo.get(JsonKey.DECLARED_SCHOOL_UDISE_CODE))) {
        declaredInfo.put(JsonKey.DECLARED_SCHOOL_UDISE_CODE, org.get(JsonKey.EXTERNAL_ID));
        declaredInfo.put(JsonKey.DECLARED_SCHOOL_NAME, org.get(JsonKey.ORG_NAME));
        userSelfDeclarationDao.upsertUserSelfDeclaredFields(
            userDeclareEntity, actorMessage.getRequestContext());
      }
    }
  }

  private void sendResetPasswordLink(Map<String, Object> userMap, RequestContext context) {
    Request EmailAndSmsRequest = new Request();
    EmailAndSmsRequest.getRequest().putAll(userMap);
    EmailAndSmsRequest.setRequestContext(context);
    EmailAndSmsRequest.setOperation(
        UserActorOperations.PROCESS_PASSWORD_RESET_MAIL_AND_SMS.getValue());
    tellToAnother(EmailAndSmsRequest);
  }

  private void saveUserDetailsToEs(Map<String, Object> completeUserMap, RequestContext context) {
    Request userRequest = new Request();
    userRequest.setRequestContext(context);
    userRequest.setOperation(ActorOperations.UPDATE_USER_INFO_ELASTIC.getValue());
    userRequest.getRequest().put(JsonKey.ID, completeUserMap.get(JsonKey.ID));
    logger.info(
        context, "UserManagementActor:saveUserDetailsToEs: Trigger sync of user details to ES");
    tellToAnother(userRequest);
  }
}
