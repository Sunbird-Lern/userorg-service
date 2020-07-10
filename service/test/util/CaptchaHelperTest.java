package util;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.models.util.HttpClientUtil;
import org.sunbird.common.models.util.ProjectUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HttpClientUtil.class, CaptchaHelper.class, ProjectUtil.class})
@PowerMockIgnore({"javax.management.*"})
public class CaptchaHelperTest {
  private static HttpClientUtil httpClientUtil;
  private static ProjectUtil projectUtil;

  @Test
  public void testCaptchaHelper() {
    httpClientUtil = mock(HttpClientUtil.class);
    projectUtil = mock(ProjectUtil.class);
    PowerMockito.mockStatic(HttpClientUtil.class);
    PowerMockito.mockStatic(ProjectUtil.class);
    Map map = new HashMap<String, String>();
    map.put("success", true);
    when(HttpClientUtil.postFormData(Mockito.anyString(), Mockito.anyMap(), Mockito.anyMap()))
        .thenReturn(String.valueOf(map));
    when(ProjectUtil.getConfigValue(Mockito.anyString())).thenReturn("anyString");
    boolean isValidate = CaptchaHelper.validate("5ASD");
    assertTrue(isValidate);
  }
}
