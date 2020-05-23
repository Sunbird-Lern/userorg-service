package org.sunbird.user.actors;

import akka.actor.ActorRef;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.IntStream;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.background.BackgroundOperations;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.actorutil.InterServiceCommunication;
import org.sunbird.actorutil.InterServiceCommunicationFactory;
import org.sunbird.bean.ClaimStatus;
import org.sunbird.bean.ShadowUser;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.StringFormatter;
import org.sunbird.common.models.util.TelemetryEnvKey;
import org.sunbird.common.models.util.datasecurity.DataMaskingService;
import org.sunbird.common.models.util.datasecurity.DecryptionService;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.feed.IFeedService;
import org.sunbird.feed.impl.FeedFactory;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.organisation.external.identity.service.OrgExternalService;
import org.sunbird.learner.util.UserFlagEnum;
import org.sunbird.learner.util.Util;
import org.sunbird.models.user.User;
import org.sunbird.services.sso.SSOManager;
import org.sunbird.services.sso.SSOServiceFactory;
import org.sunbird.telemetry.util.TelemetryUtil;
import org.sunbird.user.service.UserService;
import org.sunbird.user.service.impl.UserServiceImpl;
import org.sunbird.user.util.MigrationUtils;
import org.sunbird.user.util.UserActorOperations;
import org.sunbird.user.util.UserUtil;
import scala.concurrent.Future;

/**
 * This class contains method and business logic to migrate user from custodian org to some other
 * root org.
 *
 * @author Amit Kumar
 */
@ActorConfig(
  tasks = {"userTenantMigrate", "migrateUser"},
  asyncTasks = {}
)
public class TenantMigrationActor extends BaseActor {
  public static final String MIGRATE = "migrate";
  private UserService userService = UserServiceImpl.getInstance();
  private OrgExternalService orgExternalService = new OrgExternalService();
  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private Util.DbInfo usrDbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
  private Util.DbInfo usrOrgDbInfo = Util.dbInfoMap.get(JsonKey.USER_ORG_DB);
  private static InterServiceCommunication interServiceCommunication =
      InterServiceCommunicationFactory.getInstance();
  private ObjectMapper mapper = new ObjectMapper();
  private ActorRef systemSettingActorRef = null;
  private ElasticSearchService esUtil = EsClientFactory.getInstance(JsonKey.REST);
  private static final String ACCOUNT_MERGE_EMAIL_TEMPLATE = "accountMerge";
  private static final String MASK_IDENTIFIER = "maskIdentifier";
  private static final int MAX_MIGRATION_ATTEMPT = 2;
  public static final int USER_EXTERNAL_ID_MISMATCH = -1;
  private IFeedService feedService = FeedFactory.getInstance();
  DecryptionService decryptionService =
      org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getDecryptionServiceInstance(
          "");
  DataMaskingService maskingService =
      org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getMaskingServiceInstance("");

  @Override
  public void onReceive(Request request) throws Throwable {
    ProjectLogger.log("TenantMigrationActor:onReceive called.", LoggerEnum.INFO.name());
    Util.initializeContext(request, StringUtils.capitalize(JsonKey.CONSUMER));
    ExecutionContext.setRequestId(request.getRequestId());
    String operation = request.getOperation();
    if (systemSettingActorRef == null) {
      systemSettingActorRef = getActorRef(ActorOperations.GET_SYSTEM_SETTING.getValue());
    }
    switch (operation) {
      case "userTenantMigrate":
        migrateUser(request, true);
        break;
      case "migrateUser":
        processShadowUserMigrate(request);
        break;
      default:
        onReceiveUnsupportedOperation("TenantMigrationActor");
    }
  }

  @SuppressWarnings("unchecked")
  private void migrateUser(Request request, boolean notify) {
    ProjectLogger.log("TenantMigrationActor:migrateUser called.", LoggerEnum.INFO.name());
    Map<String, Object> reqMap = new HashMap<>(request.getRequest());
    Map<String, Object> targetObject = null;
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    Map<String, Object> userDetails =
        userService.esGetPublicUserProfileById((String) request.getRequest().get(JsonKey.USER_ID));
    validateUserCustodianOrgId((String) userDetails.get(JsonKey.ROOT_ORG_ID));
    validateChannelAndGetRootOrgId(request);
    // Add rollup for telemetry event
    ExecutionContext context = ExecutionContext.getCurrent();
    Map<String, String> rollup = new HashMap<>();
    rollup.put("l1", (String) request.getRequest().get(JsonKey.ROOT_ORG_ID));
    context.getRequestContext().put(JsonKey.ROLLUP, rollup);
    String orgId = validateOrgExternalIdOrOrgIdAndGetOrgId(request.getRequest());
    request.getRequest().put(JsonKey.ORG_ID, orgId);
    int userFlagValue = UserFlagEnum.STATE_VALIDATED.getUserFlagValue();
    if (userDetails.containsKey(JsonKey.FLAGS_VALUE)) {
      userFlagValue += Integer.parseInt(String.valueOf(userDetails.get(JsonKey.FLAGS_VALUE)));
    }
    request.getRequest().put(JsonKey.FLAGS_VALUE, userFlagValue);
    Map<String, Object> userUpdateRequest = createUserUpdateRequest(request);
    // Update user channel and rootOrgId
    Response response =
        cassandraOperation.updateRecord(
            usrDbInfo.getKeySpace(), usrDbInfo.getTableName(), userUpdateRequest);
    if (null == response
        || null == (String) response.get(JsonKey.RESPONSE)
        || (null != (String) response.get(JsonKey.RESPONSE)
            && !((String) response.get(JsonKey.RESPONSE)).equalsIgnoreCase(JsonKey.SUCCESS))) {
      // throw exception for migration failed
      ProjectCommonException.throwServerErrorException(ResponseCode.errorUserMigrationFailed);
    }
    if (null != userUpdateRequest.get(JsonKey.IS_DELETED)
        && (Boolean) userUpdateRequest.get(JsonKey.IS_DELETED)) {
      deactivateUserFromKC((String) userUpdateRequest.get(JsonKey.ID));
    }
    ProjectLogger.log(
        "TenantMigrationActor:migrateUser user record got updated.", LoggerEnum.INFO.name());
    // Update user org details
    Response userOrgResponse =
        updateUserOrg(request, (List<Map<String, Object>>) userDetails.get(JsonKey.ORGANISATIONS));
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
    saveUserDetailsToEs((String) request.getRequest().get(JsonKey.USER_ID));
    if (notify) {
      notify(userDetails);
    }
    targetObject =
        TelemetryUtil.generateTargetObject(
            (String) reqMap.get(JsonKey.USER_ID), TelemetryEnvKey.USER, MIGRATE, null);
    TelemetryUtil.telemetryProcessingCall(reqMap, targetObject, correlatedObject);
  }

  private void notify(Map<String, Object> userDetail) {
    ProjectLogger.log(
        "notify starts sending migrate notification to user " + userDetail.get(JsonKey.USER_ID));
    Map<String, Object> userData = createUserData(userDetail);
    Request notificationRequest = createNotificationData(userData);
    tellToAnother(notificationRequest);
  }

  private void deactivateUserFromKC(String userId) {
    try {
      Map<String, Object> userDbMap = new HashMap<>();
      userDbMap.put(JsonKey.USER_ID, userId);
      String status = getSSOManager().deactivateUser(userDbMap);
      ProjectLogger.log(
          "TenantMigrationActor:deactivateUserFromKC:user status in deactivating Keycloak" + status,
          LoggerEnum.INFO.name());
    } catch (Exception e) {
      ProjectLogger.log(
          "TenantMigrationActor:deactivateUserFromKC:Error occurred while deactivating user from  Keycloak"
              + e,
          LoggerEnum.ERROR.name());
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
          JsonKey.EMAIL, decryptionService.decryptData((String) userData.get(JsonKey.EMAIL)));
      userData.put(MASK_IDENTIFIER, maskingService.maskEmail((String) userData.get(JsonKey.EMAIL)));
    } else {
      userData.put(
          JsonKey.PHONE, decryptionService.decryptData((String) userData.get(JsonKey.PHONE)));
      userData.put(MASK_IDENTIFIER, maskingService.maskPhone((String) userData.get(JsonKey.PHONE)));
    }
    return userData;
  }

  private String validateOrgExternalIdOrOrgIdAndGetOrgId(Map<String, Object> migrateReq) {
    ProjectLogger.log(
        "TenantMigrationActor:validateOrgExternalIdOrOrgIdAndGetOrgId called.",
        LoggerEnum.INFO.name());
    String orgId = "";
    if (StringUtils.isNotBlank((String) migrateReq.get(JsonKey.ORG_ID))
        || StringUtils.isNotBlank((String) migrateReq.get(JsonKey.ORG_EXTERNAL_ID))) {
      if (StringUtils.isNotBlank((String) migrateReq.get(JsonKey.ORG_ID))) {
        orgId = (String) migrateReq.get(JsonKey.ORG_ID);
        Future<Map<String, Object>> resultF =
            esUtil.getDataByIdentifier(ProjectUtil.EsType.organisation.getTypeName(), orgId);
        Map<String, Object> result =
            (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
        if (MapUtils.isEmpty(result)) {
          ProjectLogger.log(
              "TenantMigrationActor:validateOrgExternalIdOrOrgIdAndGetOrgId called. OrgId is Invalid",
              LoggerEnum.INFO.name());
          ProjectCommonException.throwClientErrorException(ResponseCode.invalidOrgId);
        } else {
          String reqOrgRootOrgId = (String) result.get(JsonKey.ROOT_ORG_ID);
          if (StringUtils.isNotBlank(reqOrgRootOrgId)
              && !reqOrgRootOrgId.equalsIgnoreCase((String) migrateReq.get(JsonKey.ROOT_ORG_ID))) {
            ProjectCommonException.throwClientErrorException(
                ResponseCode.parameterMismatch,
                MessageFormat.format(
                    ResponseCode.parameterMismatch.getErrorMessage(),
                    StringFormatter.joinByComma(JsonKey.CHANNEL, JsonKey.ORG_ID)));
          }
        }
      } else if (StringUtils.isNotBlank((String) migrateReq.get(JsonKey.ORG_EXTERNAL_ID))) {
        orgId =
            orgExternalService.getOrgIdFromOrgExternalIdAndProvider(
                (String) migrateReq.get(JsonKey.ORG_EXTERNAL_ID),
                (String) migrateReq.get(JsonKey.CHANNEL));
        if (StringUtils.isBlank(orgId)) {
          ProjectLogger.log(
              "TenantMigrationActor:validateOrgExternalIdOrOrgIdAndGetOrgId called. OrgExternalId is Invalid",
              LoggerEnum.INFO.name());
          ProjectCommonException.throwClientErrorException(
              ResponseCode.invalidParameterValue,
              MessageFormat.format(
                  ResponseCode.invalidParameterValue.getErrorMessage(),
                  (String) migrateReq.get(JsonKey.ORG_EXTERNAL_ID),
                  JsonKey.ORG_EXTERNAL_ID));
        }
      }
    }
    return orgId;
  }

  private void validateUserCustodianOrgId(String rootOrgId) {
    String custodianOrgId = userService.getCustodianOrgId(systemSettingActorRef);
    if (!rootOrgId.equalsIgnoreCase(custodianOrgId)) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.parameterMismatch,
          MessageFormat.format(
              ResponseCode.parameterMismatch.getErrorMessage(),
              "user rootOrgId and custodianOrgId"));
    }
  }

  private void saveUserDetailsToEs(String userId) {
    Request userRequest = new Request();
    userRequest.setOperation(ActorOperations.UPDATE_USER_INFO_ELASTIC.getValue());
    userRequest.getRequest().put(JsonKey.ID, userId);
    ProjectLogger.log(
        "TenantMigrationActor:saveUserDetailsToEs: Trigger sync of user details to ES",
        LoggerEnum.INFO.name());
    tellToAnother(userRequest);
  }

  private Response updateUserExternalIds(Request request) {
    ProjectLogger.log("TenantMigrationActor:updateUserExternalIds called.", LoggerEnum.INFO.name());
    Response response = new Response();
    Map<String, Object> userExtIdsReq = new HashMap<>();
    userExtIdsReq.put(JsonKey.ID, request.getRequest().get(JsonKey.USER_ID));
    userExtIdsReq.put(JsonKey.USER_ID, request.getRequest().get(JsonKey.USER_ID));
    userExtIdsReq.put(JsonKey.EXTERNAL_IDS, request.getRequest().get(JsonKey.EXTERNAL_IDS));
    try {
      User user = mapper.convertValue(userExtIdsReq, User.class);
      UserUtil.validateExternalIds(user, JsonKey.CREATE);
      userExtIdsReq.put(JsonKey.EXTERNAL_IDS, user.getExternalIds());
      Request userequest = new Request();
      userequest.setOperation(UserActorOperations.UPSERT_USER_EXTERNAL_IDENTITY_DETAILS.getValue());
      userExtIdsReq.put(JsonKey.OPERATION_TYPE, JsonKey.CREATE);
      userequest.getRequest().putAll(userExtIdsReq);
      response =
          (Response)
              interServiceCommunication.getResponse(
                  getActorRef(UserActorOperations.UPSERT_USER_EXTERNAL_IDENTITY_DETAILS.getValue()),
                  userequest);
      ProjectLogger.log(
          "TenantMigrationActor:updateUserExternalIds user externalIds got updated.",
          LoggerEnum.INFO.name());
    } catch (Exception ex) {
      ProjectLogger.log(
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

  private Response updateUserOrg(Request request, List<Map<String, Object>> userOrgList) {
    ProjectLogger.log("TenantMigrationActor:updateUserOrg called.", LoggerEnum.INFO.name());
    Response response = new Response();
    deleteOldUserOrgMapping(userOrgList);
    Map<String, Object> userDetails = request.getRequest();
    // add mapping root org
    createUserOrgRequestAndUpdate(
        (String) userDetails.get(JsonKey.USER_ID), (String) userDetails.get(JsonKey.ROOT_ORG_ID));
    String orgId = (String) userDetails.get(JsonKey.ORG_ID);
    if (StringUtils.isNotBlank(orgId)
        && !((String) userDetails.get(JsonKey.ROOT_ORG_ID)).equalsIgnoreCase(orgId)) {
      try {
        createUserOrgRequestAndUpdate((String) userDetails.get(JsonKey.USER_ID), orgId);
        ProjectLogger.log(
            "TenantMigrationActor:updateUserOrg user org data got updated.",
            LoggerEnum.INFO.name());
      } catch (Exception ex) {
        ProjectLogger.log(
            "TenantMigrationActor:updateUserOrg:Exception occurred while updating user Org.", ex);
        List<Map<String, Object>> errMsgList = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        map.put(JsonKey.ERROR_MSG, ex.getMessage());
        errMsgList.add(map);
        response.getResult().put(JsonKey.ERRORS, errMsgList);
      }
    }
    return response;
  }

  private void createUserOrgRequestAndUpdate(String userId, String orgId) {
    Map<String, Object> userOrgRequest = new HashMap<>();
    userOrgRequest.put(JsonKey.ID, userId);
    String hashTagId = Util.getHashTagIdFromOrgId(orgId);
    userOrgRequest.put(JsonKey.HASHTAGID, hashTagId);
    userOrgRequest.put(JsonKey.ORGANISATION_ID, orgId);
    List<String> roles = new ArrayList<>();
    roles.add(ProjectUtil.UserRole.PUBLIC.getValue());
    userOrgRequest.put(JsonKey.ROLES, roles);
    Util.registerUserToOrg(userOrgRequest);
  }

  private void deleteOldUserOrgMapping(List<Map<String, Object>> userOrgList) {
    ProjectLogger.log(
        "TenantMigrationActor:deleteOldUserOrgMapping: delete old user org association started.",
        LoggerEnum.INFO.name());
    for (Map<String, Object> userOrg : userOrgList) {
      cassandraOperation.deleteRecord(
          usrOrgDbInfo.getKeySpace(),
          usrOrgDbInfo.getTableName(),
          (String) userOrg.get(JsonKey.ID));
    }
  }

  private void validateChannelAndGetRootOrgId(Request request) {
    String rootOrgId = "";
    String channel = (String) request.getRequest().get(JsonKey.CHANNEL);
    if (StringUtils.isNotBlank(channel)) {
      rootOrgId = userService.getRootOrgIdFromChannel(channel);
      request.getRequest().put(JsonKey.ROOT_ORG_ID, rootOrgId);
    }
  }

  private Map<String, Object> createUserUpdateRequest(Request request) {
    Map<String, Object> userRequest = new HashMap<>();
    userRequest.put(JsonKey.ID, request.getRequest().get(JsonKey.USER_ID));
    userRequest.put(JsonKey.CHANNEL, request.getRequest().get(JsonKey.CHANNEL));
    userRequest.put(JsonKey.ROOT_ORG_ID, request.getRequest().get(JsonKey.ROOT_ORG_ID));
    userRequest.put(JsonKey.FLAGS_VALUE, request.getRequest().get(JsonKey.FLAGS_VALUE));
    userRequest.put(JsonKey.UPDATED_DATE, ProjectUtil.getFormattedDate());
    userRequest.put(JsonKey.USER_TYPE, JsonKey.TEACHER);
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
  private boolean rejectMigration(String userId) {
    ProjectLogger.log(
        "TenantMigrationActor:rejectMigration: started rejecting Migration with userId:" + userId,
        LoggerEnum.INFO.name());
    List<ShadowUser> shadowUserList = MigrationUtils.getEligibleUsersById(userId);
    if (shadowUserList.isEmpty()) {
      ProjectCommonException.throwClientErrorException(ResponseCode.invalidUserId);
    }
    shadowUserList.forEach(
        shadowUser -> {
          MigrationUtils.markUserAsRejected(shadowUser);
        });
    return true;
  }

  private void processShadowUserMigrate(Request request) throws Exception {
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
      rejectMigration(userId);
      deleteUserFeed(feedId);
    } else if (StringUtils.equalsIgnoreCase(action, JsonKey.ACCEPT)) {
      List<ShadowUser> shadowUserList = getShadowUsers(channel, userId);
      checkUserId(shadowUserList);
      int index = getIndexOfShadowUser(shadowUserList, extUserId);
      if (!isIndexValid(index)) {
        if (getRemainingAttempt(shadowUserList) <= 0) {
          deleteUserFeed(feedId);
        }
        response = modifyAttemptCount(response, shadowUserList, extUserId);
      } else {
        selfMigrate(request, userId, extUserId, shadowUserList.get(index));
        increaseAttemptCount(shadowUserList.get(index), false);
        shadowUserList.remove(index);
        rejectRemainingUser(shadowUserList);
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
      Response response, List<ShadowUser> shadowUserList, String extUserId) {
    int remainingAttempt = getRemainingAttempt(shadowUserList);
    if (remainingAttempt <= 0) {
      increaseBulkAttemptCount(shadowUserList, true);
      ProjectCommonException.throwClientErrorException(ResponseCode.userMigrationFiled);
    }
    response = prepareFailureResponse(extUserId, remainingAttempt);
    increaseBulkAttemptCount(shadowUserList, false);
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
  private void rejectRemainingUser(List<ShadowUser> shadowUserList) {
    shadowUserList
        .stream()
        .forEach(
            shadowUser -> {
              MigrationUtils.markUserAsRejected(shadowUser);
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
  private List<ShadowUser> getShadowUsers(String channel, String userId) {
    Map<String, Object> propsMap = new HashMap<>();
    propsMap.put(JsonKey.CHANNEL, channel);
    List<ShadowUser> shadowUserList = MigrationUtils.getEligibleUsersById(userId, propsMap);
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
    ProjectLogger.log(
        "TenantMigrationActor:selfMigrate:request prepared for user migration:"
            + request.getRequest(),
        LoggerEnum.INFO.name());
    migrateUser(request, false);
    Map<String, Object> propertiesMap = new HashMap<>();
    propertiesMap.put(JsonKey.CLAIM_STATUS, ClaimStatus.CLAIMED.getValue());
    propertiesMap.put(JsonKey.UPDATED_ON, new Timestamp(System.currentTimeMillis()));
    propertiesMap.put(JsonKey.CLAIMED_ON, new Timestamp(System.currentTimeMillis()));
    propertiesMap.put(JsonKey.USER_ID, userId);
    MigrationUtils.updateRecord(propertiesMap, shadowUser.getChannel(), shadowUser.getUserExtId());
    deleteUserFeed(feedId);
  }

  private void deleteUserFeed(String feedId) {
    if (StringUtils.isNotBlank(feedId)) {
      ProjectLogger.log(
          "TenantMigrationActor:deleteUserFeed method called for feedId : " + feedId,
          LoggerEnum.INFO.name());
      feedService.delete(feedId);
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

  private void increaseAttemptCount(ShadowUser shadowUser, boolean isFailed) {
    Map<String, Object> propertiesMap = new WeakHashMap<>();
    propertiesMap.put(JsonKey.ATTEMPTED_COUNT, shadowUser.getAttemptedCount() + 1);
    propertiesMap.put(JsonKey.UPDATED_ON, new Timestamp(System.currentTimeMillis()));
    if (isFailed) {
      propertiesMap.put(JsonKey.CLAIM_STATUS, ClaimStatus.FAILED.getValue());
    }
    MigrationUtils.updateRecord(propertiesMap, shadowUser.getChannel(), shadowUser.getUserExtId());
  }

  /**
   * this method will increase the attemptCount of the remaining user found with same channel
   *
   * @param shadowUserList
   * @param isFailed
   */
  private void increaseBulkAttemptCount(List<ShadowUser> shadowUserList, boolean isFailed) {
    shadowUserList
        .stream()
        .forEach(
            shadowUser -> {
              increaseAttemptCount(shadowUser, isFailed);
            });
  }

  private SSOManager getSSOManager() {
    return SSOServiceFactory.getInstance();
  }
}
