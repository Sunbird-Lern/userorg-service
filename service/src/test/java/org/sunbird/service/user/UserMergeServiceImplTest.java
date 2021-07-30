package org.sunbird.service.user;

import java.util.HashMap;
import org.junit.Test;
import org.sunbird.model.user.User;
import org.sunbird.service.user.impl.UserMergeServiceImpl;

public class UserMergeServiceImplTest {
  @Test
  public void triggerUserMergeTelemetryTest() {
    UserMergeService mergeService = new UserMergeServiceImpl();
    mergeService.triggerUserMergeTelemetry(new HashMap(), new User(), new HashMap<>());
  }
}
