package org.sunbird.location.util;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.models.util.ProjectUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ProjectUtil.class})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class LocationRequestValidatorTest {

  @BeforeClass
  public static void before() {
    PowerMockito.mockStatic(ProjectUtil.class);
    PowerMockito.when(ProjectUtil.getConfigValue(Mockito.anyString()))
        .thenReturn("state,district,block,cluster,school;");
  }

  @Test
  public void isValidLocationTypeTestSuccess() {
    boolean bool = LocationRequestValidator.isValidLocationType("state");
    Assert.assertTrue(bool);
  }

  @Test
  public void isValidLocationTypeTestFailure() {
    try {
      LocationRequestValidator.isValidLocationType("state2");
    } catch (Exception ex) {
      Assert.assertNotNull(ex);
    }
  }
}
