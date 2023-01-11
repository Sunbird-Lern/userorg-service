package org.sunbird.util;

import static org.powermock.api.mockito.PowerMockito.when;
import static org.sunbird.util.FormApiUtil.getFormConfigFromFile;

import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.RequestContext;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HttpClientUtil.class, FormApiUtilHandlerEmptyConfigTest.class})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class FormApiUtilHandlerEmptyConfigTest {

  @Before
  public void setUp() throws Exception {
    PowerMockito.mockStatic(HttpClientUtil.class);
  }

  @Test
  public void testGetFormApiEmptyConfig() {

    when(HttpClientUtil.post(Mockito.anyString(), Mockito.anyString(), Mockito.anyObject(), Mockito.any(RequestContext.class)))
        .thenReturn("");
      Map<String, Object> dataConfigMap =
              FormApiUtil.getProfileConfig("locationCode", new RequestContext());
      Assert.assertNull(dataConfigMap);
  }

  @Test
  public void testGetFormApiEmptyResponseConfig() {

    when(HttpClientUtil.post(Mockito.anyString(), Mockito.anyString(), Mockito.anyObject(), Mockito.any(RequestContext.class)))
        .thenReturn(getFormApiEmptyResponse());
      Map<String, Object> dataConfigMap =
              FormApiUtil.getProfileConfig("locationCode", new RequestContext());
      Assert.assertNull(dataConfigMap);
  }

  public String getFormApiEmptyResponse() {
    String formData =
        "{ \"id\": \"api.form.read\", \"params\": { \"resmsgid\": \"5ebb6cb5-07a0-4407-8013-b45043270d7a\", \"msgid\": \"3af660bf-fc92-4c93-acd1-36ad81cb8f35\", \"status\": \"successful\" }, \"responseCode\": \"OK\", "
            + "\"result\":"
            + ", \"ts\": \"2021-01-13T22:29:26.748Z\", \"ver\": \"1.0\" }";
    return formData;
  }
}
