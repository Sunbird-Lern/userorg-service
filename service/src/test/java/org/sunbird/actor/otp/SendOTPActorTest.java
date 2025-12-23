package org.sunbird.actor.otp;

import java.time.Duration;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.testkit.javadsl.TestKit;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.dao.notification.EmailTemplateDao;
import org.sunbird.dao.notification.impl.EmailTemplateDaoImpl;
import org.sunbird.datasecurity.impl.DefaultDecryptionServiceImpl;
import org.sunbird.datasecurity.impl.DefaultEncryptionServiceImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.notification.sms.provider.ISmsProvider;
import org.sunbird.notification.sms.providerimpl.Msg91SmsProviderFactory;
import org.sunbird.notification.utils.SMSFactory;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.response.Response;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  org.sunbird.datasecurity.impl.ServiceFactory.class,
  CassandraOperationImpl.class,
  CassandraOperation.class,
  DefaultDecryptionServiceImpl.class,
  DefaultEncryptionServiceImpl.class,
  SMSFactory.class,
  Msg91SmsProviderFactory.class,
  EmailTemplateDaoImpl.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*"
})
@SuppressStaticInitializationFor({
  "org.sunbird.datasecurity.impl.ServiceFactory.class",
  "org.sunbird.datasecurity.impl.DefaultDecryptionServiceImpl",
  "org.sunbird.datasecurity.impl.DefaultEncryptionServiceImpl"
})
public class SendOTPActorTest {
  private TestKit probe;
  private ActorRef subject;
  private static final ActorSystem system = ActorSystem.create("system");
  public static final CassandraOperationImpl mockCassandraOperation =
      mock(CassandraOperationImpl.class);
  public static final ISmsProvider iSmsProvider = mock(ISmsProvider.class);
  public static final EmailTemplateDao emailTemplateDao = mock(EmailTemplateDao.class);
  private static final Props props = Props.create(SendOTPActor.class);
  private Request request;

  @Before
  public void beforeEachTestCase() throws Exception {
    PowerMockito.mockStatic(ServiceFactory.class);
    PowerMockito.mockStatic(SMSFactory.class);
    PowerMockito.mockStatic(EmailTemplateDaoImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(mockCassandraOperation);
    when(SMSFactory.getInstance()).thenReturn(iSmsProvider);
    when(EmailTemplateDaoImpl.getInstance()).thenReturn(emailTemplateDao);
    PowerMockito.mockStatic(DefaultDecryptionServiceImpl.class);
    PowerMockito.mockStatic(DefaultEncryptionServiceImpl.class);
    PowerMockito.mockStatic(org.sunbird.datasecurity.impl.ServiceFactory.class);
    DefaultDecryptionServiceImpl decryptionService =
        Mockito.mock(DefaultDecryptionServiceImpl.class);
    PowerMockito.whenNew(DefaultDecryptionServiceImpl.class)
        .withNoArguments()
        .thenReturn(decryptionService);
    when(org.sunbird.datasecurity.impl.ServiceFactory.getDecryptionServiceInstance())
        .thenReturn(decryptionService);
    probe = new TestKit(system);
    subject = system.actorOf(props);
  }

  @Test
  public void testWithInvalidRequest() {
    Request request = new Request();
    request.setOperation("invalidOperation");
    subject.tell(request, probe.getRef());
    ProjectCommonException exception =
        probe.expectMsgClass(Duration.ofSeconds(10), ProjectCommonException.class);
    Assert.assertNotNull(exception);
  }

  @Test
  public void sendOTPTestForMobile() {
    request = createOtpRequest("phone", "anyMobileNum", "anyUserId");
    when(emailTemplateDao.getTemplate(Mockito.anyString(), Mockito.any()))
        .thenReturn(
            "OTP to reset your password on $installationName is $otp. This is valid for $otpExpiryInMinutes minutes only.");
    subject.tell(request, probe.getRef());
    Response response = probe.expectMsgClass(Duration.ofSeconds(30), Response.class);
    Assert.assertEquals(ResponseCode.OK, response.getResponseCode());
  }

  @Test
  public void sendOTPTestWithoutEmailAndPhone() {
    request = createOtpRequest("phone", "anyMobileNum", "anyUserId");
    request.getRequest().remove(JsonKey.TYPE);
    when(emailTemplateDao.getTemplate(Mockito.anyString(), Mockito.any()))
        .thenReturn(
            "OTP to reset your password on $installationName is $otp. This is valid for $otpExpiryInMinutes minutes only.");
    subject.tell(request, probe.getRef());
    Response response = probe.expectMsgClass(Duration.ofSeconds(30), Response.class);
    Assert.assertEquals(ResponseCode.OK, response.getResponseCode());
  }

  @Test
  public void sendOTPTestWithPreUsedPhone() {
    request = createOtpRequest("phone", "anyMobileNum", "anyUserId");
    request.getRequest().put(JsonKey.TYPE, JsonKey.PREV_USED_PHONE);
    when(emailTemplateDao.getTemplate(Mockito.anyString(), Mockito.any()))
        .thenReturn(
            "OTP to reset your password on $installationName is $otp. This is valid for $otpExpiryInMinutes minutes only.");
    subject.tell(request, probe.getRef());
    Response response = probe.expectMsgClass(Duration.ofSeconds(30), Response.class);
    Assert.assertEquals(ResponseCode.OK, response.getResponseCode());
  }

  @Test
  public void sendOTPTestWithRecoveryPhone() {
    request = createOtpRequest("phone", "anyMobileNum", "anyUserId");
    request.getRequest().put(JsonKey.TYPE, JsonKey.RECOVERY_PHONE);
    when(emailTemplateDao.getTemplate(Mockito.anyString(), Mockito.any()))
        .thenReturn(
            "OTP to reset your password on $installationName is $otp. This is valid for $otpExpiryInMinutes minutes only.");
    subject.tell(request, probe.getRef());
    Response response = probe.expectMsgClass(Duration.ofSeconds(30), Response.class);
    Assert.assertEquals(ResponseCode.OK, response.getResponseCode());
  }

  @Test
  public void sendOTPTestWithPreUsedEmail() {
    request = createOtpRequest("email", "anyEmailId", "anyUserId");
    request.getRequest().put(JsonKey.TYPE, JsonKey.PREV_USED_EMAIL);
    when(emailTemplateDao.getTemplate(Mockito.anyString(), Mockito.any()))
        .thenReturn(
            "OTP to reset your password on $installationName is $otp. This is valid for $otpExpiryInMinutes minutes only.");
    subject.tell(request, probe.getRef());
    Response response = probe.expectMsgClass(Duration.ofSeconds(30), Response.class);
    Assert.assertEquals(ResponseCode.OK, response.getResponseCode());
  }

  @Test
  public void sendOTPTestWithRecoveryEmail() {
    request = createOtpRequest("email", "anyEmailId", "anyUserId");
    request.getRequest().put(JsonKey.TYPE, JsonKey.RECOVERY_EMAIL);
    when(emailTemplateDao.getTemplate(Mockito.anyString(), Mockito.any()))
        .thenReturn(
            "OTP to reset your password on $installationName is $otp. This is valid for $otpExpiryInMinutes minutes only.");
    subject.tell(request, probe.getRef());
    Response response = probe.expectMsgClass(Duration.ofSeconds(30), Response.class);
    Assert.assertEquals(ResponseCode.OK, response.getResponseCode());
  }

  @Test
  public void sendOTPTestForEmail() {
    request = createOtpRequest("email", "anyEmailId", "anyUserId");
    when(emailTemplateDao.getTemplate(Mockito.anyString(), Mockito.any()))
        .thenReturn(
            "OTP to reset your password on $installationName is $otp. This is valid for $otpExpiryInMinutes minutes only.");
    subject.tell(request, probe.getRef());
    Response response = probe.expectMsgClass(Duration.ofSeconds(30), Response.class);
    Assert.assertEquals(ResponseCode.OK, response.getResponseCode());
  }

  @Test
  public void sendOTPTestForEmail2() {
    request = createOtpRequest("email", "anyEmailId", "");
    when(emailTemplateDao.getTemplate(Mockito.anyString(), Mockito.any()))
        .thenReturn(
            "OTP to reset your password on $installationName is $otp. This is valid for $otpExpiryInMinutes minutes only.");
    subject.tell(request, probe.getRef());
    Response response = probe.expectMsgClass(Duration.ofSeconds(30), Response.class);
    Assert.assertEquals(ResponseCode.OK, response.getResponseCode());
  }

  private Request createOtpRequest(String type, String key, String userId) {
    Request request = new Request();
    request.setOperation(ActorOperations.SEND_OTP.getValue());
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.TYPE, type);
    innerMap.put(JsonKey.KEY, key);
    innerMap.put(JsonKey.OTP, "000000");
    innerMap.put(JsonKey.USER_ID, userId);
    innerMap.put(JsonKey.TEMPLATE_ID, "anyTemplatedId");
    request.setRequest(innerMap);
    return request;
  }
}
