package org.sunbird.util;

import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.keys.JsonKey;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*"
})
public class UserFlagUtilTest {

  @Test
  public void testGetFlagValue() {
    Assert.assertEquals(
        4, UserFlagUtil.getFlagValue(UserFlagEnum.STATE_VALIDATED.getUserFlagType(), true));
  }

  @Test
  public void testAssignUserFlagValues() {
    Map<String, Boolean> userFlagMap = UserFlagUtil.assignUserFlagValues(4);
    Assert.assertEquals(true, userFlagMap.get(JsonKey.STATE_VALIDATED));
  }
}
