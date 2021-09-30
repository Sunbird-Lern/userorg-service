package org.sunbird.service.user;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.User;
import org.sunbird.service.user.impl.UserMergeServiceImpl;
import org.sunbird.telemetry.util.TelemetryUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TelemetryUtil.class})
@PowerMockIgnore({
        "javax.management.*",
        "javax.net.ssl.*",
        "javax.security.*",
        "jdk.internal.reflect.*",
        "javax.crypto.*"
})
public class UserMergeServiceImplTest {

  private static UserMergeService userMergeService;
  private static TelemetryUtil telemetryUtil;

  @Before
  public void setUp() {
    PowerMockito.mockStatic(TelemetryUtil.class);
    // telemetryUtil = spy(TelemetryUtil.class);

    userMergeService = UserMergeServiceImpl.getInstance();
  }

  @Test
  public void triggerUserMergeTelemetryTest() {
    User user = new User();
    user.setRootOrgId("0123456789");
    Map<String, Object> telemetryMap = new HashMap();
    telemetryMap.put(JsonKey.FROM_ACCOUNT_ID, "fromAccountID");
    telemetryMap.put(JsonKey.TO_ACCOUNT_ID, "toAccountID");
    userMergeService.triggerUserMergeTelemetry(new HashMap(), user, new HashMap<>());
    // PowerMockito.verify(times(1));
  }
}