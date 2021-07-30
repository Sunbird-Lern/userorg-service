package org.sunbird.user.service;

import java.util.Map;
import org.sunbird.models.user.User;

public interface UserMergeService {
  void triggerUserMergeTelemetry(Map telemetryMap, User merger, Map<String, Object> context);
}
