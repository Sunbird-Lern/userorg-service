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

@RunWith(PowerMockRunner.class)
@PrepareForTest({HttpClientUtil.class, CaptchaHelper.class})
@PowerMockIgnore({"javax.management.*"})
public class CaptchaHelperTest {
  private static HttpClientUtil httpClientUtil;

  @Test
  public void testCaptchaHelper() {
    httpClientUtil = mock(HttpClientUtil.class);
    PowerMockito.mockStatic(HttpClientUtil.class);
    Map map = new HashMap<String, String>();
    map.put("success", true);
    when(HttpClientUtil.postFormData(Mockito.anyString(), Mockito.anyMap(), Mockito.anyMap()))
        .thenReturn(String.valueOf(map));
    boolean isValidate = CaptchaHelper.validate("5ASD");
    assertTrue(isValidate);
  }
}
