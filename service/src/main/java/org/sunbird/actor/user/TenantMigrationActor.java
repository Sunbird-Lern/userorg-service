package org.sunbird.actor.user;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.BackgroundOperations;
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
import org.sunbird.telemetry.dto.TelemetryEnvKey;
import org.sunbird.telemetry.util.TelemetryUtil;
import org.sunbird.util.*;
import org.sunbird.util.user.UserActorOperations;
import org.sunbird.util.user.UserUtil;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

public class TenantMigrationActor extends BaseActor {

  private static final String ACCOUNT_MERGE_EMAIL_TEMPLATE = "accountMerge";
  private static final String MASK_IDENTIFIER = "maskIdentifier";
  private DecryptionService decryptionService =
      org.sunbird.datasecurity.impl.ServiceFactory.getDecryptionServiceInstance();
  private DataMaskingService maskingService =
      org.sunbird.datasecurity.impl.ServiceFactory.getMaskingServiceInstance();
  private TenantMigrationService tenantServiceImpl = TenantMigrationServiceImpl.getInstance();
  private UserSelfDeclarationService userSelfDeclarationService =
      UserSelfDeclarationServiceImpl.getInstance();
  private UserLookUpServiceImpl userLookUpService = new UserLookUpServiceImpl();
  private UserService userService = UserServiceImpl.getInstance();

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
    Util.initializeContext(request, StringUtils.capitalize(JsonKey.CONSUMER));
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

  private void migrateUser(Request request, boolean notify) {
    logger.info(request.getRequestContext(), "TenantMigrationActor:migrateUser called.");
    Map<String, Object> reqMap = new HashMap<>(request.getRequest());
    Map<String, Object> targetObject = null;
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    Map<String, Object> userDetails =
        userService.getUserDetailsById(
            (String) request.getRequest().get(JsonKey.USER_ID), request.getRequestContext());
    tenantServiceImpl.validateUserCustodianOrgId((String) userDetails.get(JsonKey.ROOT_ORG_ID));
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
    // Update user org details
    Response userOrgResponse =
        tenantServiceImpl.updateUserOrg(
            request, (List<Map<String, Object>>) userDetails.get(JsonKey.ORGANISATIONS));

    // Update user externalIds
    Response userExternalIdsResponse = updateUserExternalIds(request);
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
    try {
      Map<String, Object> userData = createUserData(userDetail);
      Request notificationRequest = createNotificationData(userData);
      notificationRequest.setRequestContext(context);
      emailServiceActor.tell(notificationRequest, self());
    } catch (Exception ex) {
      logger.error(context, ex.getMessage(), ex);
    }
  }

  private Request createNotificationData(Map<String, Object> userData) {
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
    requestMap.put(JsonKey.EMAIL_TEMPLATE_TYPE, ACCOUNT_MERGE_EMAIL_TEMPLATE);
    String body =
        MessageFormat.format(
            ProjectUtil.getConfigValue(JsonKey.SUNBIRD_MIGRATE_USER_BODY),
            ProjectUtil.getConfigValue(JsonKey.SUNBIRD_INSTALLATION),
            userData.get(MASK_IDENTIFIER));
    requestMap.put(JsonKey.BODY, body);
    requestMap.put(
        JsonKey.SUBJECT, ProjectUtil.getConfigValue(JsonKey.SUNBIRD_ACCOUNT_MERGE_SUBJECT));
    request.getRequest().put(JsonKey.EMAIL_REQUEST, requestMap);
    request.setOperation(BackgroundOperations.emailService.name());
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
    try {
      Request userRequest = new Request();
      userRequest.setRequestContext(context);
      userRequest.setOperation(ActorOperations.UPDATE_USER_INFO_ELASTIC.getValue());
      userRequest.getRequest().put(JsonKey.ID, userId);
      logger.debug(
          context, "TenantMigrationActor:saveUserDetailsToEs: Trigger sync of user details to ES");
      backgroundJobManager.tell(userRequest, self());
    } catch (Exception ex) {
      logger.error(context, ex.getMessage(), ex);
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
      // Update channel to orgId  for provider field in usr_external_identiy table
      UserUtil.updateExternalIdsProviderWithOrgId(userExtIdsReq, request.getRequestContext());
      User user = mapper.convertValue(userExtIdsReq, User.class);
      UserUtil.validateExternalIds(user, JsonKey.CREATE, request.getRequestContext());
      userExtIdsReq.put(JsonKey.EXTERNAL_IDS, user.getExternalIds());
      Request userRequest = new Request();
      userRequest.setOperation(
          UserActorOperations.UPSERT_USER_EXTERNAL_IDENTITY_DETAILS.getValue());
      userExtIdsReq.put(JsonKey.OPERATION_TYPE, JsonKey.CREATE);
      userRequest.getRequest().putAll(userExtIdsReq);
      if (null != userExternalIdManagementActor) {
        Timeout t = new Timeout(Duration.create(10, TimeUnit.SECONDS));
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

  /**
   * this method will be used when user reject the migration
   *
   * @param userId
   */
  private boolean rejectMigration(String userId, RequestContext context) {
    logger.info(
        context,
        "TenantMigrationActor:rejectMigration: started rejecting Migration with userId:" + userId);
    List<ShadowUser> shadowUserList = ShadowUserMigrationService.getEligibleUsersById(userId, context);
    if (shadowUserList.isEmpty()) {
      ProjectCommonException.throwClientErrorException(ResponseCode.invalidUserId);
    }
    shadowUserList.forEach(
        shadowUser -> {
          ShadowUserMigrationService.markUserAsRejected(shadowUser, context);
        });
    return true;
  }

  private void processShadowUserMigrate(Request request) throws Exception {
    logger.info(
        request.getRequestContext(), "TenantMigrationActor:processShadowUserMigrate called.");
    String userId = (String) request.getRequest().get(JsonKey.USER_ID);
    String extUserId = (String) request.getRequest().get(JsonKey.USER_EXT_ID);
    String channel = (String) request.getRequest().get(JsonKey.CHANNEL);
    String action = (String) request.getRequest().get(JsonKey.ACTION);
    String feedId =
        (String) request.getRequest().get(JsonKey.FEED_ID); // will be used with user_feed table
    Response response = new Response();
    response.put(JsonKey.SUCCESS, true);
    response.put(JsonKey.USER_ID, userId);
    if (StringUtils.equalsIgnoreCase(action, JsonKey.REJECT)) {
      rejectMigration(userId, request.getRequestContext());
      deleteUserFeed(
          feedId,
          userId,
          FeedAction.ORG_MIGRATION_ACTION.getfeedAction(),
          request.getRequestContext());
    } else if (StringUtils.equalsIgnoreCase(action, JsonKey.ACCEPT)) {
      logger.info(
          request.getRequestContext(),
          "TenantMigrationActor: processShadowUserMigrate: shadow-user accepted and the extUserId : "
              + extUserId);
      List<ShadowUser> shadowUserList =
          getShadowUsers(channel, userId, request.getRequestContext());
      checkUserId(shadowUserList);
      int index = getIndexOfShadowUser(shadowUserList, extUserId);
      if (!isIndexValid(index)) {
        logger.info(
            request.getRequestContext(),
            "TenantMigrationActor: processShadowUserMigrate: user entered invalid externalId ");
        if (getRemainingAttempt(shadowUserList) <= 0) {
          deleteUserFeed(
              feedId,
              userId,
              FeedAction.ORG_MIGRATION_ACTION.getfeedAction(),
              request.getRequestContext());
        }
        response =
            modifyAttemptCount(response, shadowUserList, extUserId, request.getRequestContext());
      } else {
        logger.info(
            request.getRequestContext(),
            "TenantMigrationActor: processShadowUserMigrate: user entered valid externalId ");
        selfMigrate(request, userId, extUserId, shadowUserList.get(index));
        increaseAttemptCount(shadowUserList.get(index), false, request.getRequestContext());
        shadowUserList.remove(index);
        rejectRemainingUser(shadowUserList, request.getRequestContext());
      }
    } else {
      unSupportedMessage();
    }
    sender().tell(response, self());
  }

  private int getRemainingAttempt(List<ShadowUser> shadowUserList) {
    return MAX_MIGRATION_ATTEMPT - shadowUserList.get(0).getAttemptedCount() - 1;
  }

  private Response modifyAttemptCount(
      Response response,
      List<ShadowUser> shadowUserList,
      String extUserId,
      RequestContext context) {
    int remainingAttempt = getRemainingAttempt(shadowUserList);
    if (remainingAttempt <= 0) {
      increaseBulkAttemptCount(shadowUserList, true, context);
      ProjectCommonException.throwClientErrorException(ResponseCode.userMigrationFiled);
    }
    response = prepareFailureResponse(extUserId, remainingAttempt);
    increaseBulkAttemptCount(shadowUserList, false, context);
    return response;
  }

  /**
   * this method will throw exception if no record found with provided userId and channel.
   *
   * @param shadowUserList
   */
  private void checkUserId(List<ShadowUser> shadowUserList) {
    if (CollectionUtils.isEmpty(shadowUserList)) {
      ProjectCommonException.throwClientErrorException(ResponseCode.invalidUserId);
    }
  }

  private boolean isIndexValid(int index) {
    return (index != USER_EXTERNAL_ID_MISMATCH);
  }

  /**
   * this method will reject the remaining user found with the same channel in shadow_user table
   *
   * @param shadowUserList
   */
  private void rejectRemainingUser(List<ShadowUser> shadowUserList, RequestContext context) {
    shadowUserList
        .stream()
        .forEach(
            shadowUser -> {
              ShadowUserMigrationService.markUserAsRejected(shadowUser, context);
            });
  }
  /**
   * this method will return the index from the shadowUserList whose user ext id is matching with
   * provided one
   *
   * @param shadowUserList
   * @param extUserId
   * @return
   */
  private int getIndexOfShadowUser(List<ShadowUser> shadowUserList, String extUserId) {
    int index =
        IntStream.range(0, shadowUserList.size())
            .filter(i -> Objects.nonNull(shadowUserList.get(i)))
            .filter(
                i -> StringUtils.equalsIgnoreCase(extUserId, shadowUserList.get(i).getUserExtId()))
            .findFirst()
            .orElse(USER_EXTERNAL_ID_MISMATCH);
    return index;
  }

  /**
   * this method will return the shadow user based on channel and userId
   *
   * @param channel
   * @param userId
   * @return
   */
  private List<ShadowUser> getShadowUsers(String channel, String userId, RequestContext context) {
    Map<String, Object> propsMap = new HashMap<>();
    propsMap.put(JsonKey.CHANNEL, channel);
    List<ShadowUser> shadowUserList =
        ShadowUserMigrationService.getEligibleUsersById(userId, propsMap, context);
    return shadowUserList;
  }

  /**
   * this method will migrate the user from custodian channel to non-custodian channel.
   *
   * @param request
   * @param userId
   * @param extUserId
   * @param shadowUser
   */
  private void selfMigrate(
      Request request, String userId, String extUserId, ShadowUser shadowUser) {
    String feedId = (String) request.getRequest().get(JsonKey.FEED_ID);
    request.setRequest(prepareMigrationRequest(shadowUser, userId, extUserId));
    logger.info(
        request.getRequestContext(),
        "TenantMigrationActor:selfMigrate:request prepared for user migration:"
            + request.getRequest());
    migrateUser(request, false);
    Map<String, Object> propertiesMap = new HashMap<>();
    propertiesMap.put(JsonKey.CLAIM_STATUS, ClaimStatus.CLAIMED.getValue());
    propertiesMap.put(JsonKey.UPDATED_ON, new Timestamp(System.currentTimeMillis()));
    propertiesMap.put(JsonKey.CLAIMED_ON, new Timestamp(System.currentTimeMillis()));
    propertiesMap.put(JsonKey.USER_ID, userId);
    ShadowUserMigrationService.updateRecord(
        propertiesMap,
        shadowUser.getChannel(),
        shadowUser.getUserExtId(),
        request.getRequestContext());
    deleteUserFeed(
        feedId,
        userId,
        FeedAction.ORG_MIGRATION_ACTION.getfeedAction(),
        request.getRequestContext());
  }

  private void deleteUserFeed(String feedId, String userId, String action, RequestContext context) {
    if (StringUtils.isNotBlank(feedId)) {
      logger.info(
          context, "TenantMigrationActor:deleteUserFeed method called for feedId : " + feedId);
      feedService.delete(feedId, userId, action, context);

    }
  }

  /**
   * this method will prepare the failure response will return the remainingCount if provided user
   * ext id in incorrect
   *
   * @param extUserId
   * @param remainingAttempt
   * @return
   */
  private Response prepareFailureResponse(String extUserId, int remainingAttempt) {
    Response response = new Response();
    response.setResponseCode(ResponseCode.invalidUserExternalId);
    response.put(JsonKey.ERROR, true);
    response.put(JsonKey.MAX_ATTEMPT, MAX_MIGRATION_ATTEMPT);
    response.put(JsonKey.REMAINING_ATTEMPT, remainingAttempt);
    response.put(
        JsonKey.MESSAGE,
        MessageFormat.format(ResponseCode.invalidUserExternalId.getErrorMessage(), extUserId));
    return response;
  }

  /**
   * this method will prepare the migration request.
   *
   * @param shadowUser
   * @param userId
   * @param extUserId
   */
  private static Map<String, Object> prepareMigrationRequest(
      ShadowUser shadowUser, String userId, String extUserId) {
    Map<String, Object> reqMap = new WeakHashMap<>();
    reqMap.put(JsonKey.USER_ID, userId);
    reqMap.put(JsonKey.CHANNEL, shadowUser.getChannel());
    reqMap.put(JsonKey.ORG_EXTERNAL_ID, shadowUser.getOrgExtId());
    reqMap.put(JsonKey.STATUS, shadowUser.getUserStatus());
    List<Map<String, String>> extUserIds = new ArrayList<>();
    Map<String, String> externalIdMap = new WeakHashMap<>();
    externalIdMap.put(JsonKey.ID, extUserId);
    externalIdMap.put(JsonKey.ID_TYPE, shadowUser.getChannel());
    externalIdMap.put(JsonKey.PROVIDER, shadowUser.getChannel());
    extUserIds.add(externalIdMap);
    reqMap.put(JsonKey.EXTERNAL_IDS, extUserIds);
    return reqMap;
  }

  private void increaseAttemptCount(
      ShadowUser shadowUser, boolean isFailed, RequestContext context) {
    Map<String, Object> propertiesMap = new WeakHashMap<>();
    propertiesMap.put(JsonKey.ATTEMPTED_COUNT, shadowUser.getAttemptedCount() + 1);
    propertiesMap.put(JsonKey.UPDATED_ON, new Timestamp(System.currentTimeMillis()));
    if (isFailed) {
      propertiesMap.put(JsonKey.CLAIM_STATUS, ClaimStatus.FAILED.getValue());
    }
    ShadowUserMigrationService.updateRecord(
        propertiesMap, shadowUser.getChannel(), shadowUser.getUserExtId(), context);
  }

  /**
   * this method will increase the attemptCount of the remaining user found with same channel
   *
   * @param shadowUserList
   * @param isFailed
   */
  private void increaseBulkAttemptCount(
      List<ShadowUser> shadowUserList, boolean isFailed, RequestContext context) {
    shadowUserList
        .stream()
        .forEach(
            shadowUser -> {
              increaseAttemptCount(shadowUser, isFailed, context);
            });
  }

  private SSOManager getSSOManager() {
    return SSOServiceFactory.getInstance();
  }

}
