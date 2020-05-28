package org.sunbird.user.actors;

import akka.actor.ActorRef;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.telemetry.util.TelemetryUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class UserBaseActor extends BaseActor {

  private ActorRef systemSettingActorRef;

  protected void generateTelemetryEvent(
      Map<String, Object> requestMap, String userId, String objectType , Map<String,Object> context) {
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    Map<String, Object> targetObject =
        TelemetryUtil.generateTargetObject(userId, JsonKey.USER, JsonKey.UPDATE, null);
    Map<String, Object> telemetryAction = new HashMap<>();

    switch (objectType) {
      case "userLevel":
        telemetryAction.put("AssignRole", "role assigned at user level");
        break;
      case "blockUser":
        telemetryAction.put("BlockUser", "user blocked");
        break;
      case "unblockUser":
        telemetryAction.put("UnblockUser", "user unblocked");
        break;
      case "profileVisibility":
        telemetryAction.put("ProfileVisibility", "profile visibility setting changed");
        break;
      default:
        // Do Nothing
    }

    TelemetryUtil.telemetryProcessingCall(telemetryAction, targetObject, correlatedObject, context);
  }

  protected ActorRef getSystemSettingActorRef() {
    if (systemSettingActorRef == null) {
      systemSettingActorRef = getActorRef(ActorOperations.GET_SYSTEM_SETTING.getValue());
    }

    return systemSettingActorRef;
  }

}
