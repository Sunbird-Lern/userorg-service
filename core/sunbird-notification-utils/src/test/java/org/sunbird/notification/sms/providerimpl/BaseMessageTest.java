package org.sunbird.notification.sms.providerimpl;

import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.ProjectUtil;
import org.sunbird.keys.JsonKey;
import org.sunbird.notification.utils.PropertiesCache;
import org.sunbird.notification.utils.SmsTemplateUtil;

/**
 * Base class for SMS provider tests. Sets up the necessary mocks for HTTP clients, properties, and
 * static utilities used across child test classes.
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
@PrepareForTest({
  HttpClients.class,
  CloseableHttpClient.class,
  PropertiesCache.class,
  ProjectUtil.class,
  SmsTemplateUtil.class
})
public abstract class BaseMessageTest {

  /**
   * Initializes common mock rules for SMS provider tests, including HTTP client execution and
   * configuration property retrieval.
   */
  @Before
  public void initMockRules() {
    PowerMockito.mockStatic(ProjectUtil.class);
    when(ProjectUtil.getConfigValue("sms_gateway_provider")).thenReturn(JsonKey.MSG_91);
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    CloseableHttpResponse httpResp = mock(CloseableHttpResponse.class);
    PropertiesCache propertiesCache = mock(PropertiesCache.class);
    StatusLine statusLine = mock(StatusLine.class);
    PowerMockito.mockStatic(HttpClients.class);
    try {
      doReturn(httpClient).when(HttpClients.class, "createDefault");
      when(httpClient.execute(Mockito.any(HttpPost.class))).thenReturn(httpResp);
      when(httpClient.execute(Mockito.any(HttpGet.class))).thenReturn(httpResp);
      doReturn(statusLine).when(httpResp).getStatusLine();
      doReturn(200).when(statusLine).getStatusCode();
    } catch (Exception e) {
      Assert.fail("Exception while mocking static " + e.getLocalizedMessage());
    }

    try {
      PowerMockito.when(propertiesCache.getProperty(Mockito.anyString())).thenReturn("anyString");
    } catch (Exception e) {
      Assert.fail("Exception while mocking static " + e.getLocalizedMessage());
    }
  }
}
