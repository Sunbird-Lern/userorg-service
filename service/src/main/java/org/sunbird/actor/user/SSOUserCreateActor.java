package org.sunbird.actor.user;

import akka.dispatch.Mapper;
import akka.pattern.Patterns;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.actor.user.validator.UserRequestValidator;
import org.sunbird.client.org.OrganisationClient;
import org.sunbird.client.org.impl.OrganisationClientImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.exception.ResponseMessage;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.organisation.Organisation;
import org.sunbird.model.user.User;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.organisation.OrgExternalService;
import org.sunbird.service.organisation.OrgService;
import org.sunbird.service.organisation.impl.OrgExternalServiceImpl;
import org.sunbird.service.organisation.impl.OrgServiceImpl;
import org.sunbird.service.user.AssociationMechanism;
import org.sunbird.service.user.UserRoleService;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.UserRoleServiceImpl;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.telemetry.dto.TelemetryEnvKey;
import org.sunbird.util.DataCacheHandler;
import org.sunbird.util.Matcher;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.StringFormatter;
import org.sunbird.util.UserFlagUtil;
import org.sunbird.util.Util;
import org.sunbird.util.user.UserActorOperations;
import org.sunbird.util.user.UserUtil;
import scala.concurrent.Future;

@ActorConfig(
  tasks = {"createUser", "createSSOUser"},
  asyncTasks = {},
  dispatcher = "most-used-one-dispatcher"
)
public class SSOUserCreateActor extends UserBaseActor {

  private UserRequestValidator userRequestValidator = new UserRequestValidator();
  private UserService userService = UserServiceImpl.getInstance();
  private OrganisationClient organisationClient = OrganisationClientImpl.getInstance();
  private OrgExternalService orgExternalService = new OrgExternalServiceImpl();
  private ObjectMapper mapper = new ObjectMapper();
  private UserRoleService userRoleService = UserRoleServiceImpl.getInstance();
  private ElasticSearchService esUtil = EsClientFactory.getInstance(JsonKey.REST);

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    String operation = request.getOperation();
    switch (operation) {
      case "createUser": // create User [v1,v2,v3]
      case "createSSOUser":
        createSSOUser(request);
        break;
      default:
        onReceiveUnsupportedOperation("SSOUserCreateActor");
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
      populateUserTypeAndSubType(userMap);
      populateLocationCodesFromProfileLocation(userMap);
    }
    validateAndGetLocationCodes(actorMessage);
    validateChannelAndOrganisationId(userMap, actorMessage.getRequestContext());
    validatePrimaryAndRecoveryKeys(userMap);
    populateProfileUserType(userMap, actorMessage.getRequestContext());
    // remove these fields from req
    userMap.remove(JsonKey.ENC_EMAIL);
    userMap.remove(JsonKey.ENC_PHONE);
    actorMessage.getRequest().putAll(userMap);

    boolean isCustodianOrg = false;
    if (StringUtils.isBlank(callerId)) {
      userMap.put(JsonKey.CREATED_BY, actorMessage.getContext().get(JsonKey.REQUESTED_BY));
      if (StringUtils.isBlank((String) userMap.get(JsonKey.CHANNEL))
          && StringUtils.isBlank((String) userMap.get(JsonKey.ROOT_ORG_ID))) {
        userMap.put(
            JsonKey.ROOT_ORG_ID,
            DataCacheHandler.getConfigSettings().get(JsonKey.CUSTODIAN_ORG_ID));
        userMap.put(
            JsonKey.CHANNEL,
            DataCacheHandler.getConfigSettings().get(JsonKey.CUSTODIAN_ORG_CHANNEL));
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
    Map<String, Object> requestMap;
    UserUtil.setUserDefaultValue(userMap, request.getRequestContext());
    // Update external ids provider with OrgId
    UserUtil.updateExternalIdsProviderWithOrgId(userMap, request.getRequestContext());
    User user = mapper.convertValue(userMap, User.class);
    UserUtil.validateExternalIds(user, JsonKey.CREATE, request.getRequestContext());
    userMap.put(JsonKey.EXTERNAL_IDS, user.getExternalIds());
    convertValidatedLocationCodesToIDs(userMap, request.getRequestContext());
    UserUtil.toLower(userMap);
    UserUtil.validateUserPhoneAndEmailUniqueness(user, JsonKey.CREATE, request.getRequestContext());
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
          request.getRequestContext(), "SSOUserCreateActor:processSSOUser: User creation failure");
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
          esUtil
              .save(
                  ProjectUtil.EsType.user.getTypeName(),
                  (String) userDetails.get(JsonKey.USER_ID),
                  userDetails,
                  request.getRequestContext())
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
      sender().tell(response, self());
    }
    requestMap.put(JsonKey.PASSWORD, userMap.get(JsonKey.PASSWORD));
    if (StringUtils.isNotBlank(callerId)) {
      sendEmailAndSms(requestMap, request.getRequestContext());
    }
    generateUserTelemetry(userMap, request, userId, JsonKey.CREATE);
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
    Organisation fetchedOrgById;
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
          "SSOUserCreateActor:validateExternalIdAndGetOrgId: No organisation with orgExternalId = "
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
          "SSOUserCreateActor:validateExternalIdAndGetOrgId Mismatch of organisation from orgExternalId="
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

  private void saveUserDetailsToEs(Map<String, Object> completeUserMap, RequestContext context) {
    Request userRequest = new Request();
    userRequest.setRequestContext(context);
    userRequest.setOperation(ActorOperations.UPDATE_USER_INFO_ELASTIC.getValue());
    userRequest.getRequest().put(JsonKey.ID, completeUserMap.get(JsonKey.ID));
    logger.info(
        context, "SSOUserCreateActor:saveUserDetailsToEs: Trigger sync of user details to ES");
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
