package org.sunbird.actor.user;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.datasecurity.DataMaskingService;
import org.sunbird.datasecurity.DecryptionService;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.User;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.user.TenantMigrationService;
import org.sunbird.service.user.UserSelfDeclarationService;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.TenantMigrationServiceImpl;
import org.sunbird.service.user.impl.UserLookUpServiceImpl;
import org.sunbird.service.user.impl.UserSelfDeclarationServiceImpl;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.service.userconsent.UserConsentService;
import org.sunbird.service.userconsent.impl.UserConsentServiceImpl;
import org.sunbird.telemetry.dto.TelemetryEnvKey;
import org.sunbird.telemetry.util.TelemetryUtil;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.SMSTemplateProvider;
import org.sunbird.util.UserFlagEnum;
import org.sunbird.util.Util;
import org.sunbird.util.user.UserUtil;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

public class TenantMigrationActor extends BaseActor {

  private final String MASK_IDENTIFIER = "maskIdentifier";
  private final DecryptionService decryptionService =
      org.sunbird.datasecurity.impl.ServiceFactory.getDecryptionServiceInstance();
  private final DataMaskingService maskingService =
      org.sunbird.datasecurity.impl.ServiceFactory.getMaskingServiceInstance();
  private final TenantMigrationService tenantServiceImpl = TenantMigrationServiceImpl.getInstance();
  private final UserSelfDeclarationService userSelfDeclarationService =
      UserSelfDeclarationServiceImpl.getInstance();
  private final UserLookUpServiceImpl userLookUpService = new UserLookUpServiceImpl();
  private final UserService userService = UserServiceImpl.getInstance();
  private final UserConsentService userConsentService = UserConsentServiceImpl.getInstance();

  @Inject
  @Named("user_external_identity_management_actor")
  private ActorRef userExternalIdManagementActor;

  @Inject
  @Named("background_job_manager_actor")
  private ActorRef backgroundJobManager;

  @Inject
  @Named("email_service_actor")
  private ActorRef emailServiceActor;

  @Override
  public void onReceive(Request request) throws Throwable {
    logger.info(request.getRequestContext(), "TenantMigrationActor:onReceive called.");
    request.getContext().put(JsonKey.ACTOR_ID, (String) request.getRequest().get(JsonKey.USER_ID));
    request.getContext().put(JsonKey.ACTOR_TYPE, JsonKey.USER);
    Util.initializeContext(request, StringUtils.capitalize(JsonKey.USER));
    String operation = request.getOperation();
    switch (operation) {
      case "userTenantMigrate":
        migrateUser(request, true);
        break;
      case "userSelfDeclaredTenantMigrate":
        migrateSelfDeclaredUser(request);
        break;
      default:
        onReceiveUnsupportedOperation();
    }
  }

  private void migrateSelfDeclaredUser(Request request) {
    logger.info(
        request.getRequestContext(), "TenantMigrationActor:migrateSelfDeclaredUser called.");
    // update user declaration table status
    String userId = (String) request.getRequest().get(JsonKey.USER_ID);
    List<Map<String, Object>> responseList =
        userSelfDeclarationService.fetchUserDeclarations(userId, request.getRequestContext());
    if (CollectionUtils.isEmpty(responseList)) {
      logger.debug(
          request.getRequestContext(),
          "TenantMigrationActor:migrateSelfDeclaredUser record not found for user: " + userId);
      ProjectCommonException.throwServerErrorException(
          ResponseCode.declaredUserValidatedStatusNotUpdated);
    } else {
      // First migrating user, if migration success, then status gets updated as VALIDATED.
      try {
        migrateUser(request, true);
      } catch (ProjectCommonException pce) {
        logger.error(
            request.getRequestContext(), "TenantMigrationActor:migrateUser user failed.", pce);
        throw pce;
      } catch (Exception e) {
        logger.error(
            request.getRequestContext(), "TenantMigrationActor:migrateUser user failed.", e);
        ProjectCommonException.throwServerErrorException(ResponseCode.errorUserMigrationFailed);
      }
      // Update the status to VALIDATED in user_self_declaration table
      Map<String, Object> responseMap = responseList.get(0);
      responseMap.put(JsonKey.STATUS, JsonKey.VALIDATED);
      Map attrMap = new HashMap<String, Object>();
      attrMap.put(JsonKey.STATUS, JsonKey.VALIDATED);
      Map compositeKeyMap = new HashMap<String, Object>();
      compositeKeyMap.put(JsonKey.USER_ID, userId);
      compositeKeyMap.put(JsonKey.ORG_ID, responseMap.get(JsonKey.ORG_ID));
      compositeKeyMap.put(JsonKey.PERSONA, responseMap.get(JsonKey.PERSONA));
      Response response =
          userSelfDeclarationService.updateSelfDeclaration(
              attrMap, compositeKeyMap, request.getRequestContext());
      sender().tell(response, self());
    }
  }

  @SuppressWarnings("unchecked")
  private void migrateUser(Request request, boolean notify) {
    logger.info(request.getRequestContext(), "TenantMigrationActor:migrateUser called.");
    Map<String, Object> reqMap = new HashMap<>(request.getRequest());
    Map<String, Object> targetObject = null;
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    Map<String, Object> userDetails =
        userService.getUserDetailsForES(
            (String) request.getRequest().get(JsonKey.USER_ID), request.getRequestContext());
    if (null == request.getRequest().get(JsonKey.FORCE_MIGRATION)
            || !(boolean) request.getRequest().get(JsonKey.FORCE_MIGRATION)) {
      tenantServiceImpl.validateUserCustodianOrgId((String) userDetails.get(JsonKey.ROOT_ORG_ID));
    }
    tenantServiceImpl.validateChannelAndGetRootOrgId(request);
    Map<String, String> rollup = new HashMap<>();
    rollup.put("l1", (String) request.getRequest().get(JsonKey.ROOT_ORG_ID));
    request.getContext().put(JsonKey.ROLLUP, rollup);
    String orgId =
        tenantServiceImpl.validateOrgExternalIdOrOrgIdAndGetOrgId(
            request.getRequest(), request.getRequestContext());
    request.getRequest().put(JsonKey.ORG_ID, orgId);
    int userFlagValue = UserFlagEnum.STATE_VALIDATED.getUserFlagValue();
    if (userDetails.containsKey(JsonKey.FLAGS_VALUE)) {
      userFlagValue += Integer.parseInt(String.valueOf(userDetails.get(JsonKey.FLAGS_VALUE)));
    }
    request.getRequest().put(JsonKey.FLAGS_VALUE, userFlagValue);
    Map<String, Object> userUpdateRequest = createUserUpdateRequest(request);
    // Update user channel and rootOrgId
    Response response =
        tenantServiceImpl.migrateUser(userUpdateRequest, request.getRequestContext());
    if (null == response
        || null == response.get(JsonKey.RESPONSE)
        || (null != response.get(JsonKey.RESPONSE)
            && !((String) response.get(JsonKey.RESPONSE)).equalsIgnoreCase(JsonKey.SUCCESS))) {
      // throw exception for migration failed
      ProjectCommonException.throwServerErrorException(ResponseCode.errorUserMigrationFailed);
    }
    if (null != userUpdateRequest.get(JsonKey.IS_DELETED)
        && (Boolean) userUpdateRequest.get(JsonKey.IS_DELETED)) {
      tenantServiceImpl.deactivateUserFromKC(
          (String) userUpdateRequest.get(JsonKey.ID), request.getRequestContext());
    }
    logger.info(
        request.getRequestContext(), "TenantMigrationActor:migrateUser user record got updated.");
    // Update user externalIds
    Response userExternalIdsResponse = updateUserExternalIds(request);
    // Update user org details
    Response userOrgResponse =
        tenantServiceImpl.updateUserOrg(
            request, (List<Map<String, Object>>) userDetails.get(JsonKey.ORGANISATIONS));

    // Revoke org consent
    Map<String, Object> consentReqMap = new HashMap<>();
    consentReqMap.put(JsonKey.USER_ID, (String) request.getRequest().get(JsonKey.USER_ID));
    consentReqMap.put(JsonKey.CONSENT_CONSUMERID, request.getRequest().get(JsonKey.ROOT_ORG_ID));
    consentReqMap.put(JsonKey.CONSENT_OBJECTID, request.getRequest().get(JsonKey.ROOT_ORG_ID));
    consentReqMap.put(JsonKey.CONSENT_OBJECTTYPE, JsonKey.CONSENT_OBJECTTYPE_ORG);
    consentReqMap.put(JsonKey.STATUS, JsonKey.CONSENT_STATUS_DELETED);
    Response consentRes =
        userConsentService.updateConsent(consentReqMap, request.getRequestContext());

    // Collect all the error message
    List<Map<String, Object>> userOrgErrMsgList = new ArrayList<>();
    if (MapUtils.isNotEmpty(userOrgResponse.getResult())
        && CollectionUtils.isNotEmpty(
            (List<Map<String, Object>>) userOrgResponse.getResult().get(JsonKey.ERRORS))) {
      userOrgErrMsgList =
          (List<Map<String, Object>>) userOrgResponse.getResult().get(JsonKey.ERRORS);
    }
    List<Map<String, Object>> userExtIdErrMsgList = new ArrayList<>();
    if (MapUtils.isNotEmpty(userExternalIdsResponse.getResult())
        && CollectionUtils.isNotEmpty(
            (List<Map<String, Object>>) userExternalIdsResponse.getResult().get(JsonKey.ERRORS))) {
      userExtIdErrMsgList =
          (List<Map<String, Object>>) userExternalIdsResponse.getResult().get(JsonKey.ERRORS);
    }
    userOrgErrMsgList.addAll(userExtIdErrMsgList);
    response.getResult().put(JsonKey.ERRORS, userOrgErrMsgList);
    // send the response
    sender().tell(response, self());
    // save user data to ES
    saveUserDetailsToEs(
        (String) request.getRequest().get(JsonKey.USER_ID), request.getRequestContext());
    if (null != request.getRequest().get(JsonKey.NOTIFY_USER_MIGRATION)) {
      notify = (boolean) request.getRequest().get(JsonKey.NOTIFY_USER_MIGRATION);
    }
    if (notify) {
      notify(userDetails, request.getRequestContext());
    }
    targetObject =
        TelemetryUtil.generateTargetObject(
            (String) reqMap.get(JsonKey.USER_ID), TelemetryEnvKey.USER, JsonKey.UPDATE, null);
    reqMap.put(JsonKey.TYPE, JsonKey.MIGRATE_USER);
    TelemetryUtil.telemetryProcessingCall(
        reqMap, targetObject, correlatedObject, request.getContext());
  }

  private void notify(Map<String, Object> userDetail, RequestContext context) {
    logger.debug(
        context,
        "notify starts sending migrate notification to user " + userDetail.get(JsonKey.USER_ID));
    Map<String, Object> userData = createUserData(userDetail);
    Request notificationRequest = createNotificationData(userData, context);
    notificationRequest.setRequestContext(context);
    try {
      emailServiceActor.tell(notificationRequest, self());
    } catch (Exception ex) {
      logger.error(context, "Exception while sending user migrate notification", ex);
    }
  }

  private Request createNotificationData(Map<String, Object> userData, RequestContext context) {
    Request request = new Request();
    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(JsonKey.NAME, userData.get(JsonKey.FIRST_NAME));
    requestMap.put(JsonKey.FIRST_NAME, userData.get(JsonKey.FIRST_NAME));
    if (StringUtils.isNotBlank((String) userData.get(JsonKey.EMAIL))) {
      requestMap.put(JsonKey.RECIPIENT_EMAILS, Arrays.asList(userData.get(JsonKey.EMAIL)));
    } else {
      requestMap.put(JsonKey.RECIPIENT_PHONES, Arrays.asList(userData.get(JsonKey.PHONE)));
      requestMap.put(JsonKey.MODE, JsonKey.SMS);
    }
    String ACCOUNT_MERGE_EMAIL_TEMPLATE = "accountMerge";
    requestMap.put(JsonKey.EMAIL_TEMPLATE_TYPE, ACCOUNT_MERGE_EMAIL_TEMPLATE);
    Map<String, String> templateConfig = new HashMap<>();
    templateConfig.put(
        JsonKey.INSTALLATION_NAME, ProjectUtil.getConfigValue(JsonKey.SUNBIRD_INSTALLATION));
    templateConfig.put(JsonKey.PHONE, (String) userData.get(MASK_IDENTIFIER));
    String body =
        SMSTemplateProvider.getSMSBody(JsonKey.SUNBIRD_MIGRATE_USER_BODY, templateConfig, context);
    requestMap.put(JsonKey.BODY, body);
    requestMap.put(
        JsonKey.SUBJECT, ProjectUtil.getConfigValue(JsonKey.SUNBIRD_ACCOUNT_MERGE_SUBJECT));
    request.getRequest().put(JsonKey.EMAIL_REQUEST, requestMap);
    request.setOperation(ActorOperations.EMAIL_SERVICE.getValue());
    return request;
  }

  private Map<String, Object> createUserData(Map<String, Object> userData) {
    if (StringUtils.isNotBlank((String) userData.get(JsonKey.EMAIL))) {
      userData.put(
          JsonKey.EMAIL, decryptionService.decryptData((String) userData.get(JsonKey.EMAIL), null));
      userData.put(MASK_IDENTIFIER, maskingService.maskEmail((String) userData.get(JsonKey.EMAIL)));
    } else {
      userData.put(
          JsonKey.PHONE, decryptionService.decryptData((String) userData.get(JsonKey.PHONE), null));
      userData.put(MASK_IDENTIFIER, maskingService.maskPhone((String) userData.get(JsonKey.PHONE)));
    }
    return userData;
  }

  private void saveUserDetailsToEs(String userId, RequestContext context) {
    Request userRequest = new Request();
    userRequest.setRequestContext(context);
    userRequest.setOperation(ActorOperations.UPDATE_USER_INFO_ELASTIC.getValue());
    userRequest.getRequest().put(JsonKey.ID, userId);
    logger.debug(
        context, "TenantMigrationActor:saveUserDetailsToEs: Trigger sync of user details to ES");
    try {
      backgroundJobManager.tell(userRequest, self());
    } catch (Exception ex) {
      logger.error(context, "Exception while saving user data to ES", ex);
    }
  }

  private Response updateUserExternalIds(Request request) {
    logger.debug(request.getRequestContext(), "TenantMigrationActor:updateUserExternalIds called.");
    Response response = new Response();
    Map<String, Object> userExtIdsReq = new HashMap<>();
    userExtIdsReq.put(JsonKey.ID, request.getRequest().get(JsonKey.USER_ID));
    userExtIdsReq.put(JsonKey.USER_ID, request.getRequest().get(JsonKey.USER_ID));
    userExtIdsReq.put(JsonKey.EXTERNAL_IDS, request.getRequest().get(JsonKey.EXTERNAL_IDS));
    try {
      ObjectMapper mapper = new ObjectMapper();
      Timeout t = new Timeout(Duration.create(10, TimeUnit.SECONDS));
      // Update channel to orgId  for provider field in usr_external_identiy table
      UserUtil.updateExternalIdsProviderWithOrgId(userExtIdsReq, request.getRequestContext());
      User user = mapper.convertValue(userExtIdsReq, User.class);
      UserUtil.validateExternalIds(user, JsonKey.CREATE, request.getRequestContext());
      userExtIdsReq.put(JsonKey.EXTERNAL_IDS, user.getExternalIds());
      Request userRequest = new Request();
      userRequest.setOperation(ActorOperations.UPSERT_USER_EXTERNAL_IDENTITY_DETAILS.getValue());
      userExtIdsReq.put(JsonKey.OPERATION_TYPE, JsonKey.CREATE);
      userRequest.getRequest().putAll(userExtIdsReq);

      if (null != userExternalIdManagementActor) {
        Future<Object> future = Patterns.ask(userExternalIdManagementActor, userRequest, t);
        response = (Response) Await.result(future, t.duration());
      }
      userLookUpService.insertExternalIdIntoUserLookup(
          (List) userExtIdsReq.get(JsonKey.EXTERNAL_IDS),
          (String) request.getRequest().get(JsonKey.USER_ID),
          request.getRequestContext());
      logger.info(
          request.getRequestContext(),
          "TenantMigrationActor:updateUserExternalIds user externalIds got updated.");
    } catch (Exception ex) {
      logger.error(
          request.getRequestContext(),
          "TenantMigrationActor:updateUserExternalIds:Exception occurred while updating user externalIds.",
          ex);
      List<Map<String, Object>> errMsgList = new ArrayList<>();
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.ERROR_MSG, ex.getMessage());
      errMsgList.add(map);
      response.getResult().put(JsonKey.ERRORS, errMsgList);
    }
    return response;
  }

  private Map<String, Object> createUserUpdateRequest(Request request) {
    Map<String, Object> userRequest = new HashMap<>();
    userRequest.put(JsonKey.ID, request.getRequest().get(JsonKey.USER_ID));
    userRequest.put(JsonKey.CHANNEL, request.getRequest().get(JsonKey.CHANNEL));
    userRequest.put(JsonKey.ROOT_ORG_ID, request.getRequest().get(JsonKey.ROOT_ORG_ID));
    userRequest.put(JsonKey.FLAGS_VALUE, request.getRequest().get(JsonKey.FLAGS_VALUE));
    userRequest.put(JsonKey.UPDATED_DATE, ProjectUtil.getFormattedDate());
    if (request.getRequest().containsKey(JsonKey.PROFILE_LOCATION)
        && StringUtils.isNotEmpty((String) request.getRequest().get(JsonKey.PROFILE_LOCATION))) {
      userRequest.put(JsonKey.PROFILE_LOCATION, request.getRequest().get(JsonKey.PROFILE_LOCATION));
    }
    if (request.getRequest().containsKey(JsonKey.STATUS)) {
      userRequest.put(JsonKey.STATUS, request.getRequest().get(JsonKey.STATUS));
      userRequest.put(
          JsonKey.IS_DELETED,
          (int) request.getRequest().get(JsonKey.STATUS) == ProjectUtil.Status.ACTIVE.getValue()
              ? false
              : true);
    }
    return userRequest;
  }
}
