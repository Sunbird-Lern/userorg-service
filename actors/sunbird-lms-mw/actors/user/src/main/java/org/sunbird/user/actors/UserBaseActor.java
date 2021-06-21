package org.sunbird.user.actors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.TelemetryEnvKey;
import org.sunbird.telemetry.util.TelemetryUtil;

public abstract class UserBaseActor extends BaseActor {

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
