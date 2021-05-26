package org.sunbird.user.service;

import java.util.HashMap;
import org.junit.Test;
import org.sunbird.models.user.User;
import org.sunbird.user.service.impl.UserMergeServiceImpl;

public class UserMergeServiceImplTest {
  @Test
  public void triggerUserMergeTelemetryTest() {
    UserMergeService mergeService = new UserMergeServiceImpl();
    mergeService.triggerUserMergeTelemetry(new HashMap(), new User(), new HashMap<>());
  }
}
