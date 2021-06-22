package org.sunbird.user.actors;

import akka.actor.ActorRef;
import akka.dispatch.Mapper;
import akka.pattern.Patterns;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.actorutil.location.LocationClient;
import org.sunbird.actorutil.location.impl.LocationClientImpl;
import org.sunbird.actorutil.org.OrganisationClient;
import org.sunbird.actorutil.org.impl.OrganisationClientImpl;
import org.sunbird.actorutil.systemsettings.SystemSettingClient;
import org.sunbird.actorutil.systemsettings.impl.SystemSettingClientImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LocationActorOperation;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.StringFormatter;
import org.sunbird.common.models.util.TelemetryEnvKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.common.responsecode.ResponseMessage;
import org.sunbird.common.util.Matcher;
import org.sunbird.learner.organisation.external.identity.service.OrgExternalService;
import org.sunbird.learner.organisation.service.OrgService;
import org.sunbird.learner.organisation.service.impl.OrgServiceImpl;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.FormApiUtil;
import org.sunbird.learner.util.UserFlagUtil;
import org.sunbird.learner.util.Util;
import org.sunbird.location.service.LocationService;
import org.sunbird.location.service.LocationServiceImpl;
import org.sunbird.models.location.Location;
import org.sunbird.models.organisation.Organisation;
import org.sunbird.models.user.User;
import org.sunbird.user.service.AssociationMechanism;
import org.sunbird.user.service.UserLookupService;
import org.sunbird.user.service.UserRoleService;
import org.sunbird.user.service.UserService;
import org.sunbird.user.service.impl.UserLookUpServiceImpl;
import org.sunbird.user.service.impl.UserRoleServiceImpl;
import org.sunbird.user.service.impl.UserServiceImpl;
import org.sunbird.user.util.UserActorOperations;
import org.sunbird.user.util.UserUtil;
import org.sunbird.validator.user.UserRequestValidator;
import scala.concurrent.Future;

@ActorConfig(
  tasks = {"createUser", "createSSOUser"},
  asyncTasks = {},
  dispatcher = "most-used-one-dispatcher"
)
public class SSOUserCreateActor extends UserBaseActor {

  private UserRequestValidator userRequestValidator = new UserRequestValidator();
  private static LocationClient locationClient = LocationClientImpl.getInstance();
  private static LocationService locationService = LocationServiceImpl.getInstance();
  private UserService userService = UserServiceImpl.getInstance();
  private SystemSettingClient systemSettingClient = SystemSettingClientImpl.getInstance();
  private OrganisationClient organisationClient = OrganisationClientImpl.getInstance();
  private OrgExternalService orgExternalService = new OrgExternalService();
  private Util.DbInfo userOrgDb = Util.dbInfoMap.get(JsonKey.USER_ORG_DB);
  private ObjectMapper mapper = new ObjectMapper();
  private ActorRef systemSettingActorRef = null;
  private UserLookupService userLookupService = UserLookUpServiceImpl.getInstance();
  private UserRoleService userRoleService = UserRoleServiceImpl.getInstance();

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    cacheFrameworkFieldsConfig(request.getRequestContext());
    if (systemSettingActorRef == null) {
      systemSettingActorRef = getActorRef(ActorOperations.GET_SYSTEM_SETTING.getValue());
    }
    String operation = request.getOperation();
    switch (operation) {
      case "createUser": // create User [v1,v2,v3]
      case "createSSOUser":
        createSSOUser(request);
        break;
      default:
        onReceiveUnsupportedOperation("UserManagementActor");
    }
  }

  /**
   * Method to create the new user , Username should be unique .
   *
   * @param actorMessage Request
   */
  private void createSSOUser(Request actorMessage) {
    actorMessage.toLower();
    Map<String, Object> userMap = actorMessage.getRequest();
    String callerId = (String) actorMessage.getContext().get(JsonKey.CALLER_ID);
    userRequestValidator.validateCreateUserRequest(actorMessage);
    if (StringUtils.isNotBlank(callerId)) {
      userMap.put(JsonKey.ROOT_ORG_ID, actorMessage.getContext().get(JsonKey.ROOT_ORG_ID));
    }
    if (actorMessage.getOperation().equalsIgnoreCase(ActorOperations.CREATE_SSO_USER.getValue())) {
      setProfileUserTypeAndLocation(userMap, actorMessage);
    }
    validateLocationCodes(actorMessage);
    validateChannelAndOrganisationId(userMap, actorMessage.getRequestContext());
    validatePrimaryAndRecoveryKeys(userMap);
    profileUserType(userMap, actorMessage.getRequestContext());
    // remove these fields from req
    userMap.remove(JsonKey.ENC_EMAIL);
    userMap.remove(JsonKey.ENC_PHONE);
    actorMessage.getRequest().putAll(userMap);

    boolean isCustodianOrg = false;
    if (StringUtils.isBlank(callerId)) {
      userMap.put(JsonKey.CREATED_BY, actorMessage.getContext().get(JsonKey.REQUESTED_BY));
      if (StringUtils.isBlank((String) userMap.get(JsonKey.CHANNEL))
          && StringUtils.isBlank((String) userMap.get(JsonKey.ROOT_ORG_ID))) {
        String channel = DataCacheHandler.getConfigSettings().get(JsonKey.CUSTODIAN_ORG_CHANNEL);
        String custodianRootOrgId =
            DataCacheHandler.getConfigSettings().get(JsonKey.CUSTODIAN_ORG_ID);
        userMap.put(JsonKey.ROOT_ORG_ID, custodianRootOrgId);
        userMap.put(JsonKey.CHANNEL, channel);
        isCustodianOrg = true;
      }
    }
    if (userMap.containsKey(JsonKey.ORG_EXTERNAL_ID)) {
      String orgId = validateExternalIdAndGetOrgId(userMap, actorMessage.getRequestContext());
      userMap.put(JsonKey.ORGANISATION_ID, orgId);

      // Fetch locationids of the suborg and update the location of sso user
      if (!isCustodianOrg) {
        OrgService orgService = OrgServiceImpl.getInstance();
        Map<String, Object> orgMap = orgService.getOrgById(orgId, actorMessage.getRequestContext());
        if (MapUtils.isNotEmpty(orgMap)) {
          userMap.put(JsonKey.PROFILE_LOCATION, orgMap.get(JsonKey.ORG_LOCATION));
        }
      }
    }
    processSSOUser(userMap, callerId, actorMessage);
  }

  private void processSSOUser(Map<String, Object> userMap, String callerId, Request request) {
    Map<String, Object> requestMap = null;
    UserUtil.setUserDefaultValue(userMap, callerId, request.getRequestContext());
    ObjectMapper mapper = new ObjectMapper();
    // Update external ids provider with OrgId
    UserUtil.updateExternalIdsProviderWithOrgId(userMap, request.getRequestContext());
    User user = mapper.convertValue(userMap, User.class);
    UserUtil.validateExternalIds(user, JsonKey.CREATE, request.getRequestContext());
    userMap.put(JsonKey.EXTERNAL_IDS, user.getExternalIds());
    UserUtil.validateUserPhoneAndEmailUniqueness(user, JsonKey.CREATE, request.getRequestContext());
    convertValidatedLocationCodesToIDs(userMap, request.getRequestContext());
    UserUtil.toLower(userMap);
    UserUtil.addMaskEmailAndMaskPhone(userMap);
    String userId = ProjectUtil.generateUniqueId();
    userMap.put(JsonKey.ID, userId);
    userMap.put(JsonKey.USER_ID, userId);
    requestMap = UserUtil.encryptUserData(userMap);
    Map<String, Object> userLookUpData = new HashMap<>(requestMap);
    // removing roles from requestMap, so it won't get save in user table
    List<String> roles = (List<String>) requestMap.get(JsonKey.ROLES);
    removeUnwanted(requestMap);
    requestMap.put(JsonKey.IS_DELETED, false);
    Map<String, Boolean> userFlagsMap = new HashMap<>();
    // checks if the user is belongs to state and sets a validation flag
    setStateValidation(requestMap, userFlagsMap);
    int userFlagValue = userFlagsToNum(userFlagsMap);
    requestMap.put(JsonKey.FLAGS_VALUE, userFlagValue);
    Response response = null;
    boolean isPasswordUpdated = false;
    try {
      response = userService.createUser(requestMap, request.getRequestContext());
      insertIntoUserLookUp(userLookUpData, request.getRequestContext());
      isPasswordUpdated = UserUtil.updatePassword(userMap, request.getRequestContext());

    } finally {
      if (response == null) {
        response = new Response();
      }
      response.put(JsonKey.USER_ID, userMap.get(JsonKey.ID));
      if (!isPasswordUpdated) {
        response.put(JsonKey.ERROR_MSG, ResponseMessage.Message.ERROR_USER_UPDATE_PASSWORD);
      }
    }
    // update roles to user_roles
    if (CollectionUtils.isNotEmpty(roles)) {
      requestMap.put(JsonKey.ROLES, roles);
      requestMap.put(JsonKey.ROLE_OPERATION, JsonKey.CREATE);
      List<Map<String, Object>> formattedRoles =
          userRoleService.updateUserRole(requestMap, request.getRequestContext());
      requestMap.put(JsonKey.ROLES, formattedRoles);
    }
    Response resp = null;
    if (((String) response.get(JsonKey.RESPONSE)).equalsIgnoreCase(JsonKey.SUCCESS)) {
      Map<String, Object> userRequest = new HashMap<>();
      userRequest.putAll(userMap);
      userRequest.put(JsonKey.OPERATION_TYPE, JsonKey.CREATE);
      userRequest.put(JsonKey.CALLER_ID, callerId);
      userRequest.put(JsonKey.ASSOCIATION_TYPE, AssociationMechanism.SSO);
      if (StringUtils.isNotBlank(callerId) && callerId.equalsIgnoreCase(JsonKey.BULK_USER_UPLOAD)) {
        userRequest.put(JsonKey.ASSOCIATION_TYPE, AssociationMechanism.SYSTEM_UPLOAD);
      }
      resp =
          userService.saveUserAttributes(
              userRequest,
              getActorRef(UserActorOperations.SAVE_USER_ATTRIBUTES.getValue()),
              request.getRequestContext());
    } else {
      logger.info(
          request.getRequestContext(),
          "UserManagementActor:processUserRequest: User creation failure");
    }
    Map<String, Object> esResponse = new HashMap<>();
    if (null != resp) {
      esResponse.putAll((Map<String, Object>) resp.getResult().get(JsonKey.RESPONSE));
      esResponse.putAll(requestMap);
      response.put(
          JsonKey.ERRORS,
          ((Map<String, Object>) resp.getResult().get(JsonKey.RESPONSE)).get(JsonKey.ERRORS));
    }
    Response syncResponse = new Response();
    syncResponse.putAll(response.getResult());

    if (null != resp && userMap.containsKey("sync") && (boolean) userMap.get("sync")) {
      Map<String, Object> userDetails = Util.getUserDetails(userId, request.getRequestContext());
      Future<Response> future =
          saveUserToES(userDetails, request.getRequestContext())
              .map(
                  new Mapper<String, Response>() {
                    @Override
                    public Response apply(String parameter) {
                      return syncResponse;
                    }
                  },
                  context().dispatcher());
      Patterns.pipe(future, getContext().dispatcher()).to(sender());
    } else {
      if (null != resp) {
        saveUserDetailsToEs(esResponse, request.getRequestContext());
      }
      /*The pattern of this call was incorrect that it tells the ES actor after sending a response. In a high load system,
      this could be fatal, due to this it was  throw an error that the user is not found . so shifted this line after saving to ES */
      sender().tell(response, self());
    }
    requestMap.put(JsonKey.PASSWORD, userMap.get(JsonKey.PASSWORD));
    if (StringUtils.isNotBlank(callerId)) {
      sendEmailAndSms(requestMap, request.getRequestContext());
    }
    generateUserTelemetry(userMap, request, userId);
  }

  private void generateUserTelemetry(Map<String, Object> userMap, Request request, String userId) {
    Request telemetryReq = new Request();
    telemetryReq.getRequest().put("userMap", userMap);
    telemetryReq.getRequest().put("userId", userId);
    telemetryReq.setContext(request.getContext());
    telemetryReq.setOperation("generateUserTelemetry");
    tellToAnother(telemetryReq);
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

  private void setProfileUserTypeAndLocation(Map<String, Object> userMap, Request actorMessage) {
    userMap.remove(JsonKey.USER_TYPE);
    userMap.remove(JsonKey.USER_SUB_TYPE);
    if (userMap.containsKey(JsonKey.PROFILE_USERTYPE)) {
      Map<String, Object> userTypeAndSubType =
          (Map<String, Object>) userMap.get(JsonKey.PROFILE_USERTYPE);
      userMap.put(JsonKey.USER_TYPE, userTypeAndSubType.get(JsonKey.TYPE));
      userMap.put(JsonKey.USER_SUB_TYPE, userTypeAndSubType.get(JsonKey.SUB_TYPE));
    }
    if (!actorMessage.getOperation().equalsIgnoreCase(ActorOperations.CREATE_SSU_USER.getValue())) {
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
      String stateId = null;
      for (Location location : locationList) {
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

  public static void isValidLocationType(String type, List<String> typeList) {
    if (null != type && !typeList.contains(type.toLowerCase())) {
      throw new ProjectCommonException(
          ResponseCode.invalidValue.getErrorCode(),
          ProjectUtil.formatMessage(
              ResponseCode.invalidValue.getErrorMessage(), JsonKey.LOCATION_TYPE, type, typeList),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  private void validateChannelAndOrganisationId(
      Map<String, Object> userMap, RequestContext context) {
    String requestedOrgId = (String) userMap.get(JsonKey.ORGANISATION_ID);
    String requestedChannel = (String) userMap.get(JsonKey.CHANNEL);
    String fetchedRootOrgIdByChannel = "";
    if (StringUtils.isNotBlank(requestedChannel)) {
      fetchedRootOrgIdByChannel = userService.getRootOrgIdFromChannel(requestedChannel, context);
      if (StringUtils.isBlank(fetchedRootOrgIdByChannel)) {
        throw new ProjectCommonException(
            ResponseCode.invalidParameterValue.getErrorCode(),
            ProjectUtil.formatMessage(
                ResponseCode.invalidParameterValue.getErrorMessage(),
                requestedChannel,
                JsonKey.CHANNEL),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
      userMap.put(JsonKey.ROOT_ORG_ID, fetchedRootOrgIdByChannel);
    }
    Organisation fetchedOrgById = null;
    if (StringUtils.isNotBlank(requestedOrgId)) {
      fetchedOrgById = organisationClient.esGetOrgById(requestedOrgId, context);
      if (null == fetchedOrgById) {
        ProjectCommonException.throwClientErrorException(ResponseCode.invalidOrgData);
      }
      // if requested orgId is not blank then its channel should match with requested channel
      if (StringUtils.isNotBlank(requestedChannel)
          && !requestedChannel.equalsIgnoreCase(fetchedOrgById.getChannel())) {
        throwParameterMismatchException(JsonKey.CHANNEL, JsonKey.ORGANISATION_ID);
      }
      if (fetchedOrgById.isTenant()) {
        if (StringUtils.isNotBlank(requestedChannel)
            && !fetchedRootOrgIdByChannel.equalsIgnoreCase(fetchedOrgById.getId())) {
          throwParameterMismatchException(JsonKey.CHANNEL, JsonKey.ORGANISATION_ID);
        }
        userMap.put(JsonKey.ROOT_ORG_ID, fetchedOrgById.getId());
        userMap.put(JsonKey.CHANNEL, fetchedOrgById.getChannel());
      } else {
        if (StringUtils.isNotBlank(requestedChannel)) {
          userMap.put(JsonKey.ROOT_ORG_ID, fetchedRootOrgIdByChannel);
        } else {
          // fetch rootorgid by requested orgid channel
          String rootOrgId =
              userService.getRootOrgIdFromChannel(fetchedOrgById.getChannel(), context);
          userMap.put(JsonKey.ROOT_ORG_ID, rootOrgId);
          userMap.put(JsonKey.CHANNEL, fetchedOrgById.getChannel());
        }
      }
    }
  }

  private void throwParameterMismatchException(String... param) {
    ProjectCommonException.throwClientErrorException(
        ResponseCode.parameterMismatch,
        MessageFormat.format(
            ResponseCode.parameterMismatch.getErrorMessage(), StringFormatter.joinByComma(param)));
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

  private String validateExternalIdAndGetOrgId(
      Map<String, Object> userMap, RequestContext context) {
    String orgExternalId = (String) userMap.get(JsonKey.ORG_EXTERNAL_ID);
    String channel = (String) userMap.get(JsonKey.CHANNEL);
    String orgId =
        orgExternalService.getOrgIdFromOrgExternalIdAndProvider(orgExternalId, channel, context);
    if (StringUtils.isBlank(orgId)) {
      logger.info(
          context,
          "UserManagementActor:createUser: No organisation with orgExternalId = "
              + orgExternalId
              + " and channel = "
              + channel);
      ProjectCommonException.throwClientErrorException(
          ResponseCode.invalidParameterValue,
          MessageFormat.format(
              ResponseCode.invalidParameterValue.getErrorMessage(),
              orgExternalId,
              JsonKey.ORG_EXTERNAL_ID));
    }
    if (userMap.containsKey(JsonKey.ORGANISATION_ID)
        && !orgId.equals(userMap.get(JsonKey.ORGANISATION_ID))) {
      logger.info(
          context,
          "UserManagementActor:createUser Mismatch of organisation from orgExternalId="
              + orgExternalId
              + " and channel="
              + channel
              + " as organisationId="
              + orgId
              + " and request organisationId="
              + userMap.get(JsonKey.ORGANISATION_ID));
      throwParameterMismatchException(JsonKey.ORG_EXTERNAL_ID, JsonKey.ORGANISATION_ID);
    }
    userMap.remove(JsonKey.ORG_EXTERNAL_ID);
    return orgId;
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

  private void saveUserDetailsToEs(Map<String, Object> completeUserMap, RequestContext context) {
    Request userRequest = new Request();
    userRequest.setRequestContext(context);
    userRequest.setOperation(ActorOperations.UPDATE_USER_INFO_ELASTIC.getValue());
    userRequest.getRequest().put(JsonKey.ID, completeUserMap.get(JsonKey.ID));
    logger.info(
        context, "UserManagementActor:saveUserDetailsToEs: Trigger sync of user details to ES");
    tellToAnother(userRequest);
  }

  private void sendEmailAndSms(Map<String, Object> userMap, RequestContext context) {
    // sendEmailAndSms
    Request EmailAndSmsRequest = new Request();
    EmailAndSmsRequest.getRequest().putAll(userMap);
    EmailAndSmsRequest.setRequestContext(context);
    EmailAndSmsRequest.setOperation(UserActorOperations.PROCESS_ONBOARDING_MAIL_AND_SMS.getValue());
    tellToAnother(EmailAndSmsRequest);
  }
}
