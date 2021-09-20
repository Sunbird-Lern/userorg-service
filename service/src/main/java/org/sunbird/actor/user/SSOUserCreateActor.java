package org.sunbird.actor.user;

import akka.dispatch.Mapper;
import akka.pattern.Patterns;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.actor.user.validator.UserRequestValidator;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.User;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.user.AssociationMechanism;
import org.sunbird.service.user.SSOUserService;
import org.sunbird.service.user.UserRoleService;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.SSOUserServiceImpl;
import org.sunbird.service.user.impl.UserRoleServiceImpl;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.telemetry.dto.TelemetryEnvKey;
import org.sunbird.util.DataCacheHandler;
import org.sunbird.util.ProjectUtil;
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
  private ObjectMapper mapper = new ObjectMapper();
  private UserRoleService userRoleService = UserRoleServiceImpl.getInstance();
  private ElasticSearchService esUtil = EsClientFactory.getInstance(JsonKey.REST);
  private SSOUserService ssoUserService = SSOUserServiceImpl.getInstance();

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
    logger.debug(actorMessage.getRequestContext(), "SSOUserCreateActor:createSSOUser: starts : ");
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
    ssoUserService.validateOrgIdAndPrimaryRecoveryKeys(userMap, actorMessage);
    processSSOUser(userMap, callerId, actorMessage);
    logger.debug(actorMessage.getRequestContext(), "SSOUserCreateActor:createSSOUser: ends : ");
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
    // removing roles from requestMap, so it won't get save in user table
    List<String> roles = (List<String>) requestMap.get(JsonKey.ROLES);
    removeUnwanted(requestMap);
    requestMap.put(JsonKey.IS_DELETED, false);
    Map<String, Boolean> userFlagsMap = new HashMap<>();
    // checks if the user is belongs to state and sets a validation flag
    setStateValidation(requestMap, userFlagsMap);
    int userFlagValue = userFlagsToNum(userFlagsMap);
    requestMap.put(JsonKey.FLAGS_VALUE, userFlagValue);
    Response response = ssoUserService.createUserAndPassword(requestMap, userMap, request);
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
