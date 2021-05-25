package org.sunbird.user.actors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.telemetry.util.TelemetryUtil;

public abstract class UserBaseActor extends BaseActor {

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
      default:
        // Do Nothing
    }
  }
}
