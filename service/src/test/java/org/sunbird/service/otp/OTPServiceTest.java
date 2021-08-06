package org.sunbird.service.otp;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.dao.notification.EmailTemplateDao;
import org.sunbird.dao.notification.impl.EmailTemplateDaoImpl;
import org.sunbird.dao.otp.OTPDao;
import org.sunbird.dao.otp.impl.OTPDaoImpl;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.RequestContext;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.UserServiceImpl;

import java.util.HashMap;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  UserService.class,
  UserServiceImpl.class,
  EmailTemplateDao.class,
  EmailTemplateDaoImpl.class,
  OTPDao.class,
  OTPDaoImpl.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class OTPServiceTest {

  @Test
  public void getEmailPhoneByUserIdTest() {
    UserService userService = PowerMockito.mock(UserService.class);
    PowerMockito.mockStatic(UserServiceImpl.class);
    PowerMockito.when(UserServiceImpl.getInstance()).thenReturn(userService);
    PowerMockito.when(userService.getDecryptedEmailPhoneByUserId(Mockito.anyString(), Mockito.anyString(), Mockito.any(RequestContext.class))).thenReturn("xyz@xyz.com");
    OTPService otpService = new OTPService();
    String userEmail = otpService.getEmailPhoneByUserId("12312-465-4546",JsonKey.EMAIL, new RequestContext());
    Assert.assertEquals("xyz@xyz.com",userEmail);
  }

  @Test
  public void getEmailPhoneByUserIdTest2() {
    UserService userService = PowerMockito.mock(UserService.class);
    PowerMockito.mockStatic(UserServiceImpl.class);
    PowerMockito.when(UserServiceImpl.getInstance()).thenReturn(userService);
    PowerMockito.when(userService.getDecryptedEmailPhoneByUserId(Mockito.anyString(), Mockito.anyString(), Mockito.any(RequestContext.class))).thenReturn("9999999999");
    OTPService otpService = new OTPService();
    String userEmail = otpService.getEmailPhoneByUserId("12312-465-4546",JsonKey.PHONE, new RequestContext());
    Assert.assertEquals("9999999999",userEmail);
  }

  @Test
  public void getSmsBodyTest() {
    EmailTemplateDao emailTemplateDao = PowerMockito.mock(EmailTemplateDao.class);
    PowerMockito.mockStatic(EmailTemplateDaoImpl.class);
    PowerMockito.when(EmailTemplateDaoImpl.getInstance()).thenReturn(emailTemplateDao);
    String smsTemplate = "One time password to verify your phone number on $installationName is $otp. This is valid for $otpExpiryInMinutes minutes only.\n";
    PowerMockito.when(emailTemplateDao.getTemplate(Mockito.anyString(), Mockito.any(RequestContext.class))).thenReturn(smsTemplate);
    OTPService otpService = new OTPService();
    Map<String, String> smsTemplateMap = new HashMap<>();
    smsTemplateMap.put(JsonKey.OTP,"55555");
    smsTemplateMap.put(JsonKey.OTP_EXPIRATION_IN_MINUTES, "30");
    smsTemplateMap.put(JsonKey.SUNBIRD_INSTALLATION_DISPLAY_NAME,"SunBird");
    String sms = otpService.getSmsBody("otpTemplate" ,smsTemplateMap , new RequestContext());
    Assert.assertNotNull(sms);
  }

  @Test
  public void getOTPDetailsTest() {
    OTPDao otpDao = PowerMockito.mock(OTPDao.class);
    PowerMockito.mockStatic(OTPDaoImpl.class);
    PowerMockito.when(OTPDaoImpl.getInstance()).thenReturn(otpDao);
    Map<String, Object> otp = new HashMap<>();
    otp.put(JsonKey.OTP,"555555");
    PowerMockito.when(otpDao.getOTPDetails(Mockito.anyString(), Mockito.anyString(), Mockito.any(RequestContext.class))).thenReturn(otp);
    OTPService otpService = new OTPService();
    Map<String, Object> otpRes = otpService.getOTPDetails(JsonKey.PHONE,"9999999999", new RequestContext());
    Assert.assertNotNull(otpRes);
  }
}
