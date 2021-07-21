package org.sunbird.user.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sunbird.keys.JsonKey;
import org.sunbird.models.user.User;
import org.sunbird.telemetry.dto.TelemetryEnvKey;
import org.sunbird.telemetry.util.TelemetryUtil;
import org.sunbird.user.service.UserMergeService;

public class UserMergeServiceImpl implements UserMergeService {
  private static UserMergeService mergeService = null;

  public static UserMergeService getInstance() {
    if (mergeService == null) {
      mergeService = new UserMergeServiceImpl();
    }
    return mergeService;
  }

  public void triggerUserMergeTelemetry(
      Map telemetryMap, User merger, Map<String, Object> context) {
    Map<String, Object> targetObject = null;
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    Map<String, String> rollUp = new HashMap<>();
    rollUp.put("l1", merger.getRootOrgId());
    context.put(JsonKey.ROLLUP, rollUp);
    targetObject =
        TelemetryUtil.generateTargetObject(
            (String) telemetryMap.get(JsonKey.TO_ACCOUNT_ID),
            TelemetryEnvKey.USER,
            JsonKey.UPDATE,
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
    telemetryMap.put(JsonKey.TYPE, JsonKey.MERGE_USER);
    telemetryMap.remove(JsonKey.ID);
    telemetryMap.remove(JsonKey.USER_ID);
    // Generating Audit event for merger/to_user user
    TelemetryUtil.telemetryProcessingCall(telemetryMap, targetObject, correlatedObject, context);

    correlatedObject = new ArrayList<>();
    targetObject =
        TelemetryUtil.generateTargetObject(
            (String) telemetryMap.get(JsonKey.FROM_ACCOUNT_ID),
            TelemetryEnvKey.USER,
            JsonKey.UPDATE,
            null);
    telemetryMap.put(JsonKey.TYPE, JsonKey.BLOCK_USER);
    // Generating Audit event for deleted/mergee/from_user user
    TelemetryUtil.telemetryProcessingCall(telemetryMap, targetObject, correlatedObject, context);
  }
}
