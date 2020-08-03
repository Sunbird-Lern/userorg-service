package util;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.Ignore;
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
@PrepareForTest({HttpClientUtil.class, ProjectUtil.class, CaptchaHelper.class})
@PowerMockIgnore({"javax.management.*"})
public class CaptchaHelperTest {
  private static HttpClientUtil httpClientUtil;
  private static ProjectUtil projectUtil;

  @Test
  @Ignore
  public void testCaptchaHelper() throws JsonProcessingException {
    httpClientUtil = mock(HttpClientUtil.class);
    projectUtil = mock(ProjectUtil.class);
    PowerMockito.mockStatic(HttpClientUtil.class);
    PowerMockito.mockStatic(ProjectUtil.class);
    Map map = new HashMap<String, String>();
    map.put("success", true);
    ObjectMapper objectMapper = new ObjectMapper();
    String s = objectMapper.writeValueAsString(map);
    when(ProjectUtil.getConfigValue(Mockito.anyString())).thenReturn("anyString");
    when(HttpClientUtil.postFormData(Mockito.anyString(), Mockito.anyMap(), Mockito.anyMap()))
        .thenReturn(s);
    boolean isValidate = CaptchaHelper.validate("5ASD", null);
    assertTrue(isValidate);
  }
}
