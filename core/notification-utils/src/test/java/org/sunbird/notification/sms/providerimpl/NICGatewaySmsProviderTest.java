package org.sunbird.notification.sms.providerimpl;

import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.keys.JsonKey;
import org.sunbird.notification.sms.provider.ISmsProvider;
import org.sunbird.notification.utils.PropertiesCache;
import org.sunbird.notification.utils.SMSFactory;
import org.sunbird.notification.utils.SmsTemplateUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.util.ProjectUtil;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
@PrepareForTest({
  HttpClients.class,
  CloseableHttpClient.class,
  PropertiesCache.class,
  SmsTemplateUtil.class,
  ProjectUtil.class,
  URLEncoder.class
})
public class NICGatewaySmsProviderTest {

  @Before
  public void initMockRules() throws Exception {
    PowerMockito.mockStatic(ProjectUtil.class);
    when(ProjectUtil.getConfigValue("sms_gateway_provider")).thenReturn("NIC");
    PowerMockito.mockStatic(URLEncoder.class);
    when(URLEncoder.encode(Mockito.anyString(), Mockito.anyString())).thenReturn("dfgdgfg");

    initMockRulesFor200();
    PowerMockito.mockStatic(SmsTemplateUtil.class);
    Map<String, Map<String, String>> template = new HashMap<>();
    Map<String, String> template1 = new HashMap<>();
    template1.put(
        "OTP to verify your phone number on $installationName is $otp. This is valid for $otpExpiryInMinutes minutes only.",
        "1");
    template1.put(
        "OTP to reset your password on $installationName is $otp. This is valid for $otpExpiryInMinutes minutes only.",
        "2");
    template1.put(
        "Your ward has requested for registration on $installationName using this phone number. Use OTP $otp to agree and create the account. This is valid for $otpExpiryInMinutes minutes only.",
        "3");
    template.put(JsonKey.NIC, template1);
    when(SmsTemplateUtil.getSmsTemplateConfigMap()).thenReturn(template);
  }

  private void initMockRulesFor200() {
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    CloseableHttpResponse httpResp = mock(CloseableHttpResponse.class);
    PropertiesCache propertiesCache = mock(PropertiesCache.class);
    StatusLine statusLine = mock(StatusLine.class);
    PowerMockito.mockStatic(HttpClients.class);
    try {
      doReturn(httpClient).when(HttpClients.class, "createDefault");
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

  @Test
  public void testSendSms() {
    ISmsProvider iSmsProvider = SMSFactory.getInstance();
    String sms =
        "OTP to reset your password on instance is 456123. This is valid for 30 minutes only.";
    boolean response = iSmsProvider.send("4321111111", sms, new RequestContext());
    Assert.assertFalse(response);
  }

  @Test
  public void testSendSms2() {
    ISmsProvider iSmsProvider = SMSFactory.getInstance();
    String sms =
        "OTP to reset your password on instance is 456123. This is valid for 30 minutes only.";
    boolean response = iSmsProvider.send("4321111111", "91", sms, new RequestContext());
    Assert.assertFalse(response);
  }

  @Test
  public void testSendSms3() {
    List<String> phones = new ArrayList<>();
    phones.add("4321111111");
    ISmsProvider iSmsProvider = SMSFactory.getInstance();
    String sms =
        "OTP to reset your password on instance is 456123. This is valid for 30 minutes only.";
    boolean response = iSmsProvider.send(phones, sms, new RequestContext());
    Assert.assertTrue(response);
  }
}
