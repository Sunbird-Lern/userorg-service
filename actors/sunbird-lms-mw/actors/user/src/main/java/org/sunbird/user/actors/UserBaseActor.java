package org.sunbird.user.actors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actorutil.systemsettings.SystemSettingClient;
import org.sunbird.actorutil.systemsettings.impl.SystemSettingClientImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.TelemetryEnvKey;
import org.sunbird.common.request.RequestContext;
import org.sunbird.kafka.client.KafkaClient;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.telemetry.util.TelemetryUtil;
import scala.concurrent.Future;

public abstract class UserBaseActor extends BaseActor {

  protected SystemSettingClient systemSettingClient = SystemSettingClientImpl.getInstance();
  protected ElasticSearchService esUtil = EsClientFactory.getInstance(JsonKey.REST);

  protected void cacheFrameworkFieldsConfig(RequestContext context) {
    if (MapUtils.isEmpty(DataCacheHandler.getFrameworkFieldsConfig())) {
      Map<String, List<String>> frameworkFieldsConfig =
          systemSettingClient.getSystemSettingByFieldAndKey(
              getActorRef(ActorOperations.GET_SYSTEM_SETTING.getValue()),
              JsonKey.USER_PROFILE_CONFIG,
              JsonKey.FRAMEWORK,
              new TypeReference<Map<String, List<String>>>() {},
              context);
      DataCacheHandler.setFrameworkFieldsConfig(frameworkFieldsConfig);
    }
  }

  protected Future<String> saveUserToES(
      Map<String, Object> completeUserMap, RequestContext context) {
    return esUtil.save(
        ProjectUtil.EsType.user.getTypeName(),
        (String) completeUserMap.get(JsonKey.USER_ID),
        completeUserMap,
        context);
  }

  protected void saveUserToKafka(Map<String, Object> completeUserMap) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      String event = mapper.writeValueAsString(completeUserMap);
      // user_events
      KafkaClient.send(event, ProjectUtil.getConfigValue("sunbird_user_create_sync_topic"));
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  protected void processTelemetry(
      Map<String, Object> userMap,
      String signUpType,
      String source,
      String userId,
      Map<String, Object> context) {
    Map<String, Object> targetObject = null;
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    Map<String, String> rollUp = new HashMap<>();
    rollUp.put("l1", (String) userMap.get(JsonKey.ROOT_ORG_ID));
    context.put(JsonKey.ROLLUP, rollUp);
    targetObject =
        TelemetryUtil.generateTargetObject(
            (String) userMap.get(JsonKey.ID), TelemetryEnvKey.USER, JsonKey.CREATE, null);
    TelemetryUtil.generateCorrelatedObject(userId, TelemetryEnvKey.USER, null, correlatedObject);
    if (StringUtils.isNotBlank(signUpType)) {
      TelemetryUtil.generateCorrelatedObject(
          signUpType, StringUtils.capitalize(JsonKey.SIGNUP_TYPE), null, correlatedObject);
    }
    if (StringUtils.isNotBlank(source)) {
      TelemetryUtil.generateCorrelatedObject(
          source, StringUtils.capitalize(JsonKey.REQUEST_SOURCE), null, correlatedObject);
    }
    TelemetryUtil.telemetryProcessingCall(userMap, targetObject, correlatedObject, context);
  }

  protected void generateTelemetryEvent(
      Map<String, Object> requestMap,
      String userId,
      String objectType,
      Map<String, Object> context) {
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    Map<String, Object> targetObject =
        TelemetryUtil.generateTargetObject(userId, JsonKey.USER, JsonKey.UPDATE, null);
    Map<String, Object> telemetryAction = new HashMap<>();

    switch (objectType) {
      case "userLevel":
        telemetryAction.put("AssignRole", "role assigned at user level");
        TelemetryUtil.telemetryProcessingCall(
            telemetryAction, targetObject, correlatedObject, context);
        break;
      case "blockUser":
        telemetryAction.put(JsonKey.BLOCK_USER, "user blocked");
        TelemetryUtil.telemetryProcessingCall(
            JsonKey.BLOCK_USER, telemetryAction, targetObject, correlatedObject, context);
        break;
      case "unblockUser":
        telemetryAction.put(JsonKey.UNBLOCK_USER, "user unblocked");
        TelemetryUtil.telemetryProcessingCall(
            JsonKey.UNBLOCK_USER, telemetryAction, targetObject, correlatedObject, context);
        break;
      case "updateUserV2":
      case "updateUser":
        TelemetryUtil.telemetryProcessingCall(requestMap, targetObject, correlatedObject, context);
        break;
      default:
        // Do Nothing
    }
  }
}
