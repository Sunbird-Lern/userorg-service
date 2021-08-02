package org.sunbird.util;

import org.junit.Assert;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;

@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*"
})
public class UserFlagEnumTest {

  @Test
  public void testUserFlagEnumPhoneValue() {
    Assert.assertEquals(4, UserFlagEnum.STATE_VALIDATED.getUserFlagValue());
  }

  @Test
  public void testUserFlagEnumPhoneType() {
    Assert.assertEquals("stateValidated", UserFlagEnum.STATE_VALIDATED.getUserFlagType());
  }
}
