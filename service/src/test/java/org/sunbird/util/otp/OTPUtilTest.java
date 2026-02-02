package org.sunbird.util.otp;

import static org.powermock.api.mockito.PowerMockito.when;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.keys.JsonKey;
import org.sunbird.notification.sms.provider.ISmsProvider;
import org.sunbird.notification.utils.SMSFactory;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.service.otp.OTPService;
import org.sunbird.common.ProjectUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  OTPService.class,
  ProjectUtil.class,
  SMSFactory.class,
  ISmsProvider.class,
  CassandraOperationImpl.class,
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class OTPUtilTest {

  @Test
  public void generateOtpTest() {
    for (int i = 0; i < 10000; i++) {
      String code = OTPUtil.generateOTP(null);
      Assert.assertTrue(code.length() >= 4);
    }
  }

  @Test
  public void sendOTPViaSMSTest() throws Exception {
    PowerMockito.mockStatic(ProjectUtil.class);
    when(ProjectUtil.getConfigValue(Mockito.anyString())).thenReturn("anyString");
    OTPService otpService = PowerMockito.mock(OTPService.class);
    PowerMockito.whenNew(OTPService.class).withNoArguments().thenReturn(otpService);
    when(otpService.getSmsBody(
            Mockito.anyString(), Mockito.anyMap(), Mockito.any(RequestContext.class)))
        .thenReturn("some sms text");
    ISmsProvider smsProvider = PowerMockito.mock(ISmsProvider.class);
    PowerMockito.mockStatic(SMSFactory.class);
    when(SMSFactory.getInstance()).thenReturn(smsProvider);
    when(smsProvider.send(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.any(RequestContext.class)))
        .thenReturn(true);
    Map<String, Object> otpMap = new HashMap<>();
    otpMap.put(JsonKey.PHONE, "9742511111");
    otpMap.put(JsonKey.TEMPLATE_ID, "someTemplateId");
    otpMap.put(JsonKey.OTP_EXPIRATION_IN_MINUTES, "30");
    otpMap.put(JsonKey.SUNBIRD_INSTALLATION_DISPLAY_NAME, "displayName");
    otpMap.put(JsonKey.COUNTRY_CODE, "91");
    boolean bool = OTPUtil.sendOTPViaSMS(otpMap, new RequestContext());
    Assert.assertTrue(bool);
  }

  @Test
  public void sendOTPViaSMSTest2() throws Exception {
    PowerMockito.mockStatic(ProjectUtil.class);
    when(ProjectUtil.getConfigValue(Mockito.anyString())).thenReturn("anyString");
    OTPService otpService = PowerMockito.mock(OTPService.class);
    PowerMockito.whenNew(OTPService.class).withNoArguments().thenReturn(otpService);
    when(otpService.getSmsBody(
            Mockito.anyString(), Mockito.anyMap(), Mockito.any(RequestContext.class)))
        .thenReturn("some sms text");
    ISmsProvider smsProvider = PowerMockito.mock(ISmsProvider.class);
    PowerMockito.mockStatic(SMSFactory.class);
    when(SMSFactory.getInstance()).thenReturn(smsProvider);
    when(smsProvider.send(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.any(RequestContext.class)))
        .thenReturn(true);
    Map<String, Object> otpMap = new HashMap<>();
    otpMap.put(JsonKey.PHONE, "9742511111");
    otpMap.put(JsonKey.OTP_EXPIRATION_IN_MINUTES, "30");
    otpMap.put(JsonKey.SUNBIRD_INSTALLATION_DISPLAY_NAME, "displayName");
    otpMap.put(JsonKey.COUNTRY_CODE, "91");
    boolean bool = OTPUtil.sendOTPViaSMS(otpMap, new RequestContext());
    Assert.assertTrue(bool);
  }

  @Test
  public void sendOTPViaSMSTest3() throws Exception {
    PowerMockito.mockStatic(ProjectUtil.class);
    when(ProjectUtil.getConfigValue(Mockito.anyString())).thenReturn("anyString");
    OTPService otpService = PowerMockito.mock(OTPService.class);
    PowerMockito.whenNew(OTPService.class).withNoArguments().thenReturn(otpService);
    when(otpService.getSmsBody(
            Mockito.anyString(), Mockito.anyMap(), Mockito.any(RequestContext.class)))
        .thenReturn("some sms text");
    ISmsProvider smsProvider = PowerMockito.mock(ISmsProvider.class);
    PowerMockito.mockStatic(SMSFactory.class);
    when(SMSFactory.getInstance()).thenReturn(smsProvider);
    when(smsProvider.send(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.any(RequestContext.class)))
        .thenReturn(true);
    Map<String, Object> otpMap = new HashMap<>();
    otpMap.put(JsonKey.PHONE, "9742511111");
    otpMap.put(JsonKey.TEMPLATE_ID, JsonKey.WARD_LOGIN_OTP_TEMPLATE_ID);
    otpMap.put(JsonKey.OTP_EXPIRATION_IN_MINUTES, "30");
    otpMap.put(JsonKey.SUNBIRD_INSTALLATION_DISPLAY_NAME, "displayName");
    otpMap.put(JsonKey.COUNTRY_CODE, "91");
    boolean bool = OTPUtil.sendOTPViaSMS(otpMap, new RequestContext());
    Assert.assertTrue(bool);
  }

  @Test
  public void sendOTPViaSMSTest4() throws Exception {
    PowerMockito.mockStatic(ProjectUtil.class);
    when(ProjectUtil.getConfigValue(Mockito.anyString())).thenReturn("anyString");
    OTPService otpService = PowerMockito.mock(OTPService.class);
    PowerMockito.whenNew(OTPService.class).withNoArguments().thenReturn(otpService);
    when(otpService.getSmsBody(
            Mockito.anyString(), Mockito.anyMap(), Mockito.any(RequestContext.class)))
        .thenReturn("some sms text");
    ISmsProvider smsProvider = PowerMockito.mock(ISmsProvider.class);
    PowerMockito.mockStatic(SMSFactory.class);
    when(SMSFactory.getInstance()).thenReturn(smsProvider);
    when(smsProvider.send(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.any(RequestContext.class)))
        .thenReturn(true);
    Map<String, Object> otpMap = new HashMap<>();
    otpMap.put(JsonKey.PHONE, "9742511111");
    otpMap.put(JsonKey.TEMPLATE_ID, JsonKey.RESET_PASSWORD_TEMPLATE_ID);
    otpMap.put(JsonKey.OTP_EXPIRATION_IN_MINUTES, "30");
    otpMap.put(JsonKey.SUNBIRD_INSTALLATION_DISPLAY_NAME, "displayName");
    otpMap.put(JsonKey.COUNTRY_CODE, "91");
    boolean bool = OTPUtil.sendOTPViaSMS(otpMap, new RequestContext());
    Assert.assertTrue(bool);
  }

  @Test
  public void sendOTPViaSMSTest5() throws Exception {
    PowerMockito.mockStatic(ProjectUtil.class);
    when(ProjectUtil.getConfigValue(Mockito.anyString())).thenReturn("anyString");
    OTPService otpService = PowerMockito.mock(OTPService.class);
    PowerMockito.whenNew(OTPService.class).withNoArguments().thenReturn(otpService);
    when(otpService.getSmsBody(
            Mockito.anyString(), Mockito.anyMap(), Mockito.any(RequestContext.class)))
        .thenReturn("some sms text");
    ISmsProvider smsProvider = PowerMockito.mock(ISmsProvider.class);
    PowerMockito.mockStatic(SMSFactory.class);
    when(SMSFactory.getInstance()).thenReturn(smsProvider);
    when(smsProvider.send(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.any(RequestContext.class)))
        .thenReturn(true);
    Map<String, Object> otpMap = new HashMap<>();
    otpMap.put(JsonKey.PHONE, "9742511111");
    otpMap.put(JsonKey.TEMPLATE_ID, JsonKey.CONTACT_UPDATE_TEMPLATE_ID);
    otpMap.put(JsonKey.OTP_EXPIRATION_IN_MINUTES, "30");
    otpMap.put(JsonKey.SUNBIRD_INSTALLATION_DISPLAY_NAME, "displayName");
    otpMap.put(JsonKey.COUNTRY_CODE, "91");
    boolean bool = OTPUtil.sendOTPViaSMS(otpMap, new RequestContext());
    Assert.assertTrue(bool);
  }

  @Test
  public void sendOTPViaSMSWithoutPhoneTest() {
    Map<String, Object> otpMap = new HashMap<>();
    otpMap.put(JsonKey.TEMPLATE_ID, "someTemplateId");
    otpMap.put(JsonKey.OTP_EXPIRATION_IN_MINUTES, "30");
    otpMap.put(JsonKey.SUNBIRD_INSTALLATION_DISPLAY_NAME, "displayName");
    otpMap.put(JsonKey.COUNTRY_CODE, "91");
    boolean bool = OTPUtil.sendOTPViaSMS(otpMap, new RequestContext());
    Assert.assertFalse(bool);
  }

  // @Test
  public void getEmailPhoneByUserIdTest() throws Exception {
    Map<String, Object> user = new HashMap<>();
    user.put(JsonKey.USER_ID, "1235-4578-156645");
    user.put(JsonKey.EMAIL, "xyz@xyz.com");
    user.put(JsonKey.PHONE, "9999999999");
    OTPService otpService = PowerMockito.mock(OTPService.class);
    PowerMockito.whenNew(OTPService.class).withNoArguments().thenReturn(otpService);
    String value =
        otpService.getEmailPhoneByUserId("1235-4578-156645", JsonKey.EMAIL, new RequestContext());
    Assert.assertNotNull(value);
  }

  @Test(expected = ProjectCommonException.class)
  public void getEmailPhoneByUserId2Test() {
    OTPService otpService = new OTPService();
    otpService.getEmailPhoneByUserId("1235-4578-156645", JsonKey.EMAIL, new RequestContext());
  }

  @Test(expected = ProjectCommonException.class)
  public void getEmailPhoneByUserId3Test() {
    Map<String, Object> user = new HashMap<>();
    user.put(JsonKey.USER_ID, "1235-4578-156645");
    OTPService otpService = new OTPService();
    otpService.getEmailPhoneByUserId("1235-4578-156645", JsonKey.EMAIL, new RequestContext());
  }

  @Test
  public void getRequestToSendOTPViaEmailTest() {
    PowerMockito.mockStatic(ProjectUtil.class);
    when(ProjectUtil.getConfigValue(Mockito.anyString())).thenReturn("anyString");

    Map<String, Object> emailTemplateMap = new HashMap<>();
    emailTemplateMap.put(JsonKey.EMAIL, "xyz@xyz.com");

    Request request = OTPUtil.getRequestToSendOTPViaEmail(emailTemplateMap, new RequestContext());
    Assert.assertNotNull(request);
  }

  @Test
  public void getRequestToSendOTPViaEmailTest2() {
    PowerMockito.mockStatic(ProjectUtil.class);
    when(ProjectUtil.getConfigValue(Mockito.anyString())).thenReturn("anyString");

    Map<String, Object> emailTemplateMap = new HashMap<>();

    Request request = OTPUtil.getRequestToSendOTPViaEmail(emailTemplateMap, new RequestContext());
    Assert.assertNull(request);
  }

  @Test
  public void getRequestToSendOTPViaEmailTest3() {
    PowerMockito.mockStatic(ProjectUtil.class);
    when(ProjectUtil.getConfigValue(Mockito.anyString())).thenReturn("anyString");

    Map<String, Object> emailTemplateMap = new HashMap<>();
    emailTemplateMap.put(JsonKey.EMAIL, "xyz@xyz.com");
    emailTemplateMap.put(JsonKey.TEMPLATE_ID, JsonKey.WARD_LOGIN_OTP_TEMPLATE_ID);
    Request request = OTPUtil.getRequestToSendOTPViaEmail(emailTemplateMap, new RequestContext());
    Assert.assertNotNull(request);
  }

  @Test
  public void getRequestToSendOTPViaEmailTest4() {
    PowerMockito.mockStatic(ProjectUtil.class);
    when(ProjectUtil.getConfigValue(Mockito.anyString())).thenReturn("anyString");

    Map<String, Object> emailTemplateMap = new HashMap<>();
    emailTemplateMap.put(JsonKey.EMAIL, "xyz@xyz.com");
    emailTemplateMap.put(JsonKey.TEMPLATE_ID, JsonKey.RESET_PASSWORD_TEMPLATE_ID);
    Request request = OTPUtil.getRequestToSendOTPViaEmail(emailTemplateMap, new RequestContext());
    Assert.assertNotNull(request);
  }

  @Test
  public void getRequestToSendOTPViaEmailTest5() {
    PowerMockito.mockStatic(ProjectUtil.class);
    when(ProjectUtil.getConfigValue(Mockito.anyString())).thenReturn("anyString");

    Map<String, Object> emailTemplateMap = new HashMap<>();
    emailTemplateMap.put(JsonKey.EMAIL, "xyz@xyz.com");
    emailTemplateMap.put(JsonKey.TEMPLATE_ID, JsonKey.CONTACT_UPDATE_TEMPLATE_ID);
    Request request = OTPUtil.getRequestToSendOTPViaEmail(emailTemplateMap, new RequestContext());
    Assert.assertNotNull(request);
  }
}
