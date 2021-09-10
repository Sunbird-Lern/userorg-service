package org.sunbird.actor.user;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.telemetry.dto.TelemetryEnvKey;
import org.sunbird.telemetry.util.TelemetryUtil;

public class UserTelemetryActor extends BaseActor {

  @Override
  public void onReceive(Request request) throws Throwable {
    if (request.getOperation().equalsIgnoreCase("generateUserTelemetry")) {
      generateTelemetry(request);
    } else {
      onReceiveUnsupportedOperation();
    }
  }

  private void generateTelemetry(Request request) {
    Map<String, Object> targetObject = null;
    Map<String, Object> userMap = (Map<String, Object>) request.getRequest().get("userMap");
    String userId = (String) request.getRequest().get("userId");
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    Map<String, String> rollUp = new HashMap<>();
    rollUp.put("l1", (String) userMap.get(JsonKey.ROOT_ORG_ID));
    request.getContext().put(JsonKey.ROLLUP, rollUp);
    targetObject =
        TelemetryUtil.generateTargetObject(
            (String) userMap.get(JsonKey.ID),
            TelemetryEnvKey.USER,
            (String) request.getRequest().get(JsonKey.OPERATION_TYPE),
            null);
    TelemetryUtil.generateCorrelatedObject(userId, TelemetryEnvKey.USER, null, correlatedObject);
    String signUpType =
        request.getContext().get(JsonKey.SIGNUP_TYPE) != null
            ? (String) request.getContext().get(JsonKey.SIGNUP_TYPE)
            : "";
    String source =
        request.getContext().get(JsonKey.REQUEST_SOURCE) != null
            ? (String) request.getContext().get(JsonKey.REQUEST_SOURCE)
            : "";
    if (StringUtils.isNotBlank(signUpType)) {
      TelemetryUtil.generateCorrelatedObject(
          signUpType, StringUtils.capitalize(JsonKey.SIGNUP_TYPE), null, correlatedObject);
    }
    if (StringUtils.isNotBlank(source)) {
      TelemetryUtil.generateCorrelatedObject(
          source, StringUtils.capitalize(JsonKey.REQUEST_SOURCE), null, correlatedObject);
    }
    TelemetryUtil.telemetryProcessingCall(
        userMap, targetObject, correlatedObject, request.getContext());
  }
}
