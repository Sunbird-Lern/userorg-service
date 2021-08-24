package org.sunbird.actor.user;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.BackgroundOperations;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.datasecurity.DataMaskingService;
import org.sunbird.datasecurity.DecryptionService;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.User;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.user.TenantMigrationService;
import org.sunbird.service.user.UserLookupService;
import org.sunbird.service.user.impl.TenantMigrationServiceImpl;
import org.sunbird.service.user.impl.UserLookUpServiceImpl;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.telemetry.dto.TelemetryEnvKey;
import org.sunbird.telemetry.util.TelemetryUtil;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.Util;
import org.sunbird.util.user.UserActorOperations;
import org.sunbird.util.user.UserUtil;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

/**
 * This class contains method and business logic to migrate user from custodian org to some other
 * root org.
 *
 * @author Amit Kumar
 */
@ActorConfig(
  tasks = {"userTenantMigrate"},
  asyncTasks = {"userSelfDeclaredTenantMigrate"}
)
public class TenantMigrationActor extends BaseActor {

  private Util.DbInfo usrDecDbInfo = Util.dbInfoMap.get(JsonKey.USR_DECLARATION_TABLE);
  private ActorRef systemSettingActorRef = null;
  private static final String ACCOUNT_MERGE_EMAIL_TEMPLATE = "accountMerge";
  private static final String MASK_IDENTIFIER = "maskIdentifier";
  private TenantMigrationService tenantMigrationService = TenantMigrationServiceImpl.getInstance();
  private DecryptionService decryptionService =
      org.sunbird.datasecurity.impl.ServiceFactory.getDecryptionServiceInstance("");
  private DataMaskingService maskingService =
      org.sunbird.datasecurity.impl.ServiceFactory.getMaskingServiceInstance("");

  @Override
  public void onReceive(Request request) throws Throwable {
    logger.debug(request.getRequestContext(), "TenantMigrationActor:onReceive called.");
    Util.initializeContext(request, StringUtils.capitalize(JsonKey.CONSUMER));
    String operation = request.getOperation();
    if (systemSettingActorRef == null) {
      systemSettingActorRef = getActorRef(ActorOperations.GET_SYSTEM_SETTING.getValue());
    }
    switch (operation) {
      case "userTenantMigrate":
        migrateUser(request, true);
        break;
      case "userSelfDeclaredTenantMigrate":
        migrateSelfDeclaredUser(request);
        break;
      default:
        onReceiveUnsupportedOperation("TenantMigrationActor");
    }
  }

  private void migrateSelfDeclaredUser(Request request) {
    Response response = null;
    logger.debug(
        request.getRequestContext(), "TenantMigrationActor:migrateSelfDeclaredUser called.");
    CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    // update user declaration table status
    String userId = (String) request.getRequest().get(JsonKey.USER_ID);
    Map compositeKeyMap = new HashMap<String, Object>();
    compositeKeyMap.put(JsonKey.USER_ID, userId);
    Response existingRecord =
        cassandraOperation.getRecordById(
            usrDecDbInfo.getKeySpace(),
            usrDecDbInfo.getTableName(),
            compositeKeyMap,
            request.getRequestContext());
    List<Map<String, Object>> responseList =
        (List<Map<String, Object>>) existingRecord.get(JsonKey.RESPONSE);
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
      Map attrMap = new HashMap<String, Object>();
      responseMap.put(JsonKey.STATUS, JsonKey.VALIDATED);
      attrMap.put(JsonKey.STATUS, JsonKey.VALIDATED);
      compositeKeyMap.put(JsonKey.ORG_ID, responseMap.get(JsonKey.ORG_ID));
      compositeKeyMap.put(JsonKey.PERSONA, responseMap.get(JsonKey.PERSONA));
      response =
          cassandraOperation.updateRecord(
              usrDecDbInfo.getKeySpace(),
              usrDecDbInfo.getTableName(),
              attrMap,
              compositeKeyMap,
              null);
      sender().tell(response, self());
    }
  }

  @SuppressWarnings("unchecked")
  private void migrateUser(Request request, boolean notify) {
    logger.debug(request.getRequestContext(), "TenantMigrationActor:migrateUser called.");
    Map<String, Object> reqMap = new HashMap<>(request.getRequest());
    Map<String, Object> targetObject = null;
    Response response = null;
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    Map<String, Object> userDetails =
        UserServiceImpl.getInstance()
            .esGetPublicUserProfileById(
                (String) request.getRequest().get(JsonKey.USER_ID), request.getRequestContext());
    validateUserCustodianOrgId(
        (String) userDetails.get(JsonKey.ROOT_ORG_ID), request.getRequestContext());
    Response userOrgResponse = tenantMigrationService.migrateUser(request, userDetails);
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

  private void validateUserCustodianOrgId(String rootOrgId, RequestContext context) {
    String custodianOrgId =
        UserServiceImpl.getInstance().getCustodianOrgId(systemSettingActorRef, context);
    if (!rootOrgId.equalsIgnoreCase(custodianOrgId)) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.parameterMismatch,
          MessageFormat.format(
              ResponseCode.parameterMismatch.getErrorMessage(),
              "user rootOrgId and custodianOrgId"));
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
      userRequest.setOperation(
          UserActorOperations.UPSERT_USER_EXTERNAL_IDENTITY_DETAILS.getValue());
      userExtIdsReq.put(JsonKey.OPERATION_TYPE, JsonKey.CREATE);
      userRequest.getRequest().putAll(userExtIdsReq);

      ActorRef actorRef =
          getActorRef(UserActorOperations.UPSERT_USER_EXTERNAL_IDENTITY_DETAILS.getValue());
      Future<Object> future = Patterns.ask(actorRef, userRequest, t);
      response = (Response) Await.result(future, t.duration());
      UserLookupService userLookUpService = UserLookUpServiceImpl.getInstance();
      userLookUpService.insertExternalIdIntoUserLookup(
          (List) userExtIdsReq.get(JsonKey.EXTERNAL_IDS),
          (String) request.getRequest().get(JsonKey.USER_ID),
          request.getRequestContext());
      logger.debug(
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

  private void notify(Map<String, Object> userDetail, RequestContext context) {
    logger.info(
        context,
        "notify starts sending migrate notification to user " + userDetail.get(JsonKey.USER_ID));
    Map<String, Object> userData = createUserData(userDetail);
    Request notificationRequest = createNotificationData(userData);
    notificationRequest.setRequestContext(context);
    tellToAnother(notificationRequest);
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
    Request userRequest = new Request();
    userRequest.setRequestContext(context);
    userRequest.setOperation(ActorOperations.UPDATE_USER_INFO_ELASTIC.getValue());
    userRequest.getRequest().put(JsonKey.ID, userId);
    logger.info(
        context, "TenantMigrationActor:saveUserDetailsToEs: Trigger sync of user details to ES");
    tellToAnother(userRequest);
  }
}
