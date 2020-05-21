package org.sunbird.user.actors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.actorutil.systemsettings.SystemSettingClient;
import org.sunbird.actorutil.systemsettings.impl.SystemSettingClientImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.*;
import org.sunbird.common.models.util.datasecurity.OneWayHashing;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.common.responsecode.ResponseMessage;
import org.sunbird.common.util.ConfigUtil;
import org.sunbird.kafka.client.KafkaClient;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.Util;
import org.sunbird.models.systemsetting.SystemSetting;
import org.sunbird.models.user.User;
import org.sunbird.services.sso.SSOManager;
import org.sunbird.services.sso.SSOServiceFactory;
import org.sunbird.telemetry.dto.Actor;
import org.sunbird.telemetry.dto.Context;
import org.sunbird.telemetry.dto.Target;
import org.sunbird.telemetry.dto.Telemetry;
import org.sunbird.telemetry.util.TelemetryUtil;
import org.sunbird.user.service.UserService;
import org.sunbird.user.service.impl.UserServiceImpl;
import org.sunbird.user.util.KafkaConfigConstants;

@ActorConfig(
  tasks = {"mergeUser"},
  asyncTasks = {}
)
public class UserMergeActor extends UserBaseActor {
  String topic = null;
  Producer<String, String> producer = null;
  private ObjectMapper objectMapper = new ObjectMapper();
  private UserService userService = UserServiceImpl.getInstance();
  private SSOManager keyCloakService = SSOServiceFactory.getInstance();
  private SystemSettingClient systemSettingClient = SystemSettingClientImpl.getInstance();

  @Override
  public void onReceive(Request userRequest) throws Throwable {
    Util.initializeContext(userRequest, TelemetryEnvKey.USER);
    if (producer == null) {
      initKafkaClient();
    }
    updateUserMergeDetails(userRequest);
  }

  /**
   * Main method for calling user-course service, merge user details and then call user-cert service
   *
   * @param userRequest
   * @throws IOException
   */
  private void updateUserMergeDetails(Request userRequest) throws IOException {
    ProjectLogger.log("UserMergeActor:updateUserMergeDetails: starts : ", LoggerEnum.DEBUG.name());
    Response response = new Response();
    Map mergeeDBMap = new HashMap<String, Object>();
    HashMap requestMap = (HashMap) userRequest.getRequest();
    Map userCertMap = (Map) requestMap.clone();
    Map headers = (Map) userRequest.getContext().get(JsonKey.HEADER);
    String mergeeId = (String) requestMap.get(JsonKey.FROM_ACCOUNT_ID);
    String mergerId = (String) requestMap.get(JsonKey.TO_ACCOUNT_ID);
    // validating tokens
    checkTokenDetails(headers, mergeeId, mergerId);
    Map telemetryMap = (HashMap) requestMap.clone();
    User mergee = userService.getUserById(mergeeId);
    User merger = userService.getUserById(mergerId);
    String custodianId = getCustodianValue();
    if ((!custodianId.equals(mergee.getRootOrgId())) || custodianId.equals(merger.getRootOrgId())) {
      ProjectLogger.log(
          "UserMergeActor:updateUserMergeDetails: Either custodian id is not matching with mergeeid root-org"
              + mergeeId
              + "or matching with mergerid root-org"
              + mergerId,
          LoggerEnum.ERROR.name());
      throw new ProjectCommonException(
          ResponseCode.accountNotFound.getErrorCode(),
          ResponseCode.accountNotFound.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    if (!mergee.getIsDeleted()) {
      prepareMergeeAccountData(mergee, mergeeDBMap);
      userRequest.put(JsonKey.USER_MERGEE_ACCOUNT, mergeeDBMap);
      Response mergeeResponse = getUserDao().updateUser(mergeeDBMap);
      String mergeeResponseStr = (String) mergeeResponse.get(JsonKey.RESPONSE);
      ProjectLogger.log(
          "UserMergeActor: updateUserMergeDetails: mergeeResponseStr = " + mergeeResponseStr,
          LoggerEnum.INFO.name());
      Map result = new HashMap<String, Object>();
      result.put(JsonKey.STATUS, JsonKey.SUCCESS);
      response.put(JsonKey.RESULT, result);
      sender().tell(response, self());

      // update user-course-cert details
      mergeCertCourseDetails(mergee, merger);

      // update mergee details in ES
      mergeUserDetailsToEs(userRequest);

      // deleting User From KeyCloak
      CompletableFuture.supplyAsync(
              () -> {
                return deactivateMergeeFromKC((String) mergeeDBMap.get(JsonKey.ID));
              })
          .thenApply(
              status -> {
                ProjectLogger.log(
                    "UserMergeActor: updateUserMergeDetails: user deleted from KeyCloak: " + status,
                    LoggerEnum.INFO.name());
                return null;
              });

      // create telemetry event for merge
      triggerUserMergeTelemetry(telemetryMap, merger);

    } else {
      ProjectLogger.log(
          "UserMergeActor:updateUserMergeDetails: User mergee is not exist : " + mergeeId,
          LoggerEnum.ERROR.name());
      throw new ProjectCommonException(
          ResponseCode.invalidIdentifier.getErrorCode(),
          ProjectUtil.formatMessage(
              ResponseMessage.Message.INVALID_PARAMETER_VALUE, mergeeId, JsonKey.FROM_ACCOUNT_ID),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
  }

  /**
   * This method returns system custodian value
   *
   * @return rootCustodianValue
   */
  private String getCustodianValue() {
    String custodianId = null;
    try {
      Map<String, String> configSettingMap = DataCacheHandler.getConfigSettings();
      custodianId = configSettingMap.get(JsonKey.CUSTODIAN_ORG_ID);
      if (custodianId == null || custodianId.isEmpty()) {
        SystemSetting custodianIdSetting =
            systemSettingClient.getSystemSettingByField(
                getActorRef(ActorOperations.GET_SYSTEM_SETTING.getValue()),
                JsonKey.CUSTODIAN_ORG_ID);
        if (custodianIdSetting != null) {
          configSettingMap.put(custodianIdSetting.getId(), custodianIdSetting.getValue());
          custodianId = custodianIdSetting.getValue();
        }
      }
    } catch (Exception e) {
      ProjectLogger.log(
          "UserMergeActor:updateTncInfo: Exception occurred while getting system setting for"
              + JsonKey.CUSTODIAN_ORG_ID
              + e.getMessage(),
          LoggerEnum.ERROR.name());
    }
    return custodianId;
  }

  /**
   * This method creates Kafka topic for user-cert
   *
   * @param mergee
   * @param merger
   * @throws IOException
   */
  private void mergeCertCourseDetails(User mergee, User merger) throws IOException {
    String content = null;
    Telemetry userCertMergeRequest = createAccountMergeTopicData(mergee, merger);
    content = objectMapper.writeValueAsString(userCertMergeRequest);
    ProjectLogger.log(
        "UserMergeActor:mergeCertCourseDetails: Kafka producer topic::" + content,
        LoggerEnum.INFO.name());
    ProducerRecord<String, String> record = new ProducerRecord<>(topic, content);
    if (producer != null) {
      producer.send(record);
    } else {
      ProjectLogger.log(
          "UserMergeActor:mergeCertCourseDetails: Kafka producer is not initialised.",
          LoggerEnum.INFO.name());
    }
  }

  private Telemetry createAccountMergeTopicData(User mergee, User merger) {
    Map<String, Object> edata = new HashMap<>();
    Telemetry mergeUserEvent = new Telemetry();
    Actor actor = new Actor();
    actor.setId(JsonKey.TELEMETRY_ACTOR_USER_MERGE_ID);
    actor.setType(JsonKey.SYSTEM);
    mergeUserEvent.setActor(actor);
    mergeUserEvent.setEid(JsonKey.BE_JOB_REQUEST);
    edata.put(JsonKey.ACTION, JsonKey.TELEMETRY_EDATA_USER_MERGE_ACTION);
    edata.put(JsonKey.FROM_ACCOUNT_ID, mergee.getId());
    edata.put(JsonKey.TO_ACCOUNT_ID, merger.getId());
    edata.put(JsonKey.ROOT_ORG_ID, merger.getRootOrgId());
    edata.put(JsonKey.ITERATION, 1);
    mergeUserEvent.setEdata(edata);
    Context context = new Context();
    org.sunbird.telemetry.dto.Producer dataProducer = new org.sunbird.telemetry.dto.Producer();
    dataProducer.setVer("1.0");
    dataProducer.setId(JsonKey.TELEMETRY_PRODUCER_USER_MERGE_ID);
    context.setPdata(dataProducer);
    mergeUserEvent.setContext(context);
    Target target = new Target();
    target.setId(OneWayHashing.encryptVal(mergee.getId() + "_" + merger.getId()));
    target.setType(JsonKey.TELEMETRY_TARGET_USER_MERGE_TYPE);
    mergeUserEvent.setObject(target);
    return mergeUserEvent;
  }

  private void triggerUserMergeTelemetry(Map telemetryMap, User merger) {
    ProjectLogger.log(
        "UserMergeActor:triggerUserMergeTelemetry: generating telemetry event for merge");
    Map<String, Object> targetObject = null;
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    Map<String, String> rollUp = new HashMap<>();
    rollUp.put("l1", merger.getRootOrgId());
    ExecutionContext.getCurrent().getRequestContext().put(JsonKey.ROLLUP, rollUp);
    targetObject =
        TelemetryUtil.generateTargetObject(
            (String) telemetryMap.get(JsonKey.FROM_ACCOUNT_ID),
            TelemetryEnvKey.USER,
            JsonKey.MERGE_USER,
            null);
    TelemetryUtil.generateCorrelatedObject(
        (String) telemetryMap.get(JsonKey.FROM_ACCOUNT_ID),
        JsonKey.FROM_ACCOUNT_ID,
        null,
        correlatedObject);
    TelemetryUtil.generateCorrelatedObject(
        (String) telemetryMap.get(JsonKey.TO_ACCOUNT_ID),
        JsonKey.TO_ACCOUNT_ID,
        null,
        correlatedObject);
    telemetryMap.remove(JsonKey.ID);
    telemetryMap.remove(JsonKey.USER_ID);
    TelemetryUtil.telemetryProcessingCall(telemetryMap, targetObject, correlatedObject);
  }

  private void mergeUserDetailsToEs(Request userRequest) {
    userRequest.setOperation(ActorOperations.MERGE_USER_TO_ELASTIC.getValue());
    ProjectLogger.log(
        "UserMergeActor: mergeUserDetailsToEs: Trigger sync of user details to ES for user id"
            + userRequest.getRequest().get(JsonKey.FROM_ACCOUNT_ID),
        LoggerEnum.INFO.name());
    tellToAnother(userRequest);
  }

  private void prepareMergeeAccountData(User mergee, Map mergeeDBMap) {
    mergeeDBMap.put(JsonKey.STATUS, 0);
    mergeeDBMap.put(JsonKey.IS_DELETED, true);
    mergeeDBMap.put(JsonKey.EMAIL, null);
    mergeeDBMap.put(JsonKey.PHONE, null);
    mergeeDBMap.put(JsonKey.USERNAME, null);
    mergeeDBMap.put(JsonKey.PREV_USED_EMAIL, mergee.getEmail());
    mergeeDBMap.put(JsonKey.PREV_USED_PHONE, mergee.getPhone());
    mergeeDBMap.put(JsonKey.UPDATED_DATE, ProjectUtil.getFormattedDate());
    mergeeDBMap.put(JsonKey.ID, mergee.getId());
  }

  private void checkTokenDetails(Map headers, String mergeeId, String mergerId) {
    String userAuthToken = (String) headers.get(JsonKey.X_AUTHENTICATED_USER_TOKEN);
    String sourceUserAuthToken = (String) headers.get(JsonKey.X_SOURCE_USER_TOKEN);
    String subDomainUrl = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SUBDOMAIN_KEYCLOAK_BASE_URL);
    ProjectLogger.log(
        "UserMergeActor:checkTokenDetails subdomain url value " + subDomainUrl,
        LoggerEnum.INFO.name());
    String userId = keyCloakService.verifyToken(userAuthToken);
    // Since source token is generated from subdomain , so verification also need with
    // same subdomain.
    String sourceUserId = keyCloakService.verifyToken(sourceUserAuthToken, subDomainUrl);
    if (!(mergeeId.equals(sourceUserId) && mergerId.equals(userId))) {
      throw new ProjectCommonException(
          ResponseCode.unAuthorized.getErrorCode(),
          ProjectUtil.formatMessage(ResponseMessage.Message.UNAUTHORIZED_USER, mergeeId),
          ResponseCode.UNAUTHORIZED.getResponseCode());
    }
  }

  /** Initialises Kafka producer required for dispatching messages on Kafka. */
  private void initKafkaClient() {
    ProjectLogger.log("UserMergeActor:initKafkaClient: starts = ", LoggerEnum.INFO.name());
    Config config = ConfigUtil.getConfig();
    topic = config.getString(KafkaConfigConstants.SUNBIRD_USER_CERT_KAFKA_TOPIC);
    ProjectLogger.log("UserMergeActor:initKafkaClient: topic = " + topic, LoggerEnum.INFO.name());
    try {
      producer = KafkaClient.getProducer();
    } catch (Exception e) {
      ProjectLogger.log(
          "UserMergeActor:initKafkaClient: An exception occurred." + e, LoggerEnum.ERROR.name());
    }
  }

  private String deactivateMergeeFromKC(String userId) {
    Map<String, Object> userMap = new HashMap<>();
    userMap.put(JsonKey.USER_ID, userId);
    ProjectLogger.log(
        "UserMergeActor:deactivateMergeeFromKC: request Got to deactivate mergee account from KC:"
            + userMap,
        LoggerEnum.INFO.name());
    return keyCloakService.removeUser(userMap);
  }
}
