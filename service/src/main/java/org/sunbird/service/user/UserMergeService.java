package org.sunbird.service.user;

import java.util.Map;
import org.sunbird.model.user.User;

public interface UserMergeService {
  void triggerUserMergeTelemetry(Map telemetryMap, User merger, Map<String, Object> context);
}
