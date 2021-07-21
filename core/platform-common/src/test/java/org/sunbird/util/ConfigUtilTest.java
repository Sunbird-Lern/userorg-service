package org.sunbird.util;

import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import com.typesafe.config.Config;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;

@PrepareForTest({ConfigUtil.class, PropertiesCache.class})
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*"
})
public class ConfigUtilTest {

  private String configType = "user";
  private String validJson = "{\"key\" : \"value\"}";
  private static ConfigUtil configUtilMock;

  @BeforeClass
  public static void setup() throws Exception {
    configUtilMock = Mockito.mock(ConfigUtil.class);
    PowerMockito.whenNew(ConfigUtil.class).withAnyArguments().thenReturn(configUtilMock);
  }

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(PropertiesCache.class);
    PropertiesCache propertiesCache = mock(PropertiesCache.class);
    when(PropertiesCache.getInstance()).thenReturn(propertiesCache);
    PowerMockito.when(propertiesCache.getProperty(Mockito.anyString())).thenReturn("anyString");
  }

  @Test(expected = ProjectCommonException.class)
  public void testGetConfigFromJsonStringFailureWithNullString() {
    try {
      ConfigUtil.getConfigFromJsonString(null, configType);
    } catch (ProjectCommonException e) {
      assertTrue(e.getCode().equals(ResponseCode.errorConfigLoadEmptyString.getErrorCode()));
      throw e;
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testGetConfigFromJsonStringFailureWithEmptyString() {
    try {
      ConfigUtil.getConfigFromJsonString("", configType);
    } catch (ProjectCommonException e) {
      assertTrue(e.getCode().equals(ResponseCode.errorConfigLoadEmptyString.getErrorCode()));
      throw e;
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testGetConfigFromJsonStringFailureWithInvalidJsonString() {
    try {
      ConfigUtil.getConfigFromJsonString("{dummy}", configType);
    } catch (ProjectCommonException e) {
      assertTrue(e.getCode().equals(ResponseCode.errorConfigLoadParseString.getErrorCode()));
      throw e;
    }
  }

  @Test
  public void testGetConfigFromJsonStringSuccess() {
    Config config = ConfigUtil.getConfigFromJsonString(validJson, configType);
    assertTrue("value".equals(config.getString("key")));
  }
}
