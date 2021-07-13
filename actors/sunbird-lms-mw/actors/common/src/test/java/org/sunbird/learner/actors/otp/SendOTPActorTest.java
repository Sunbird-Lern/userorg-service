package org.sunbird.learner.actors.otp;

import static akka.testkit.JavaTestKit.duration;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
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
import org.sunbird.actor.service.SunbirdMWService;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.models.response.Response;
import org.sunbird.operations.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.datasecurity.impl.DefaultDecryptionServiceImpl;
import org.sunbird.datasecurity.impl.DefaultEncryptionServivceImpl;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.notificationservice.dao.EmailTemplateDao;
import org.sunbird.learner.actors.notificationservice.dao.impl.EmailTemplateDaoImpl;
import org.sunbird.notification.sms.provider.ISmsProvider;
import org.sunbird.notification.sms.providerimpl.Msg91SmsProviderFactory;
import org.sunbird.notification.utils.SMSFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  org.sunbird.datasecurity.impl.ServiceFactory.class,
  CassandraOperationImpl.class,
  CassandraOperation.class,
  DefaultDecryptionServiceImpl.class,
  DefaultEncryptionServivceImpl.class,
  SMSFactory.class,
  Msg91SmsProviderFactory.class,
  EmailTemplateDaoImpl.class,
  SunbirdMWService.class
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
  "org.sunbird.datasecurity.impl.DefaultEncryptionServivceImpl"
})
public class SendOTPActorTest {
  private TestKit probe;
  private ActorRef subject;

  public static DefaultEncryptionServivceImpl encService;
  public static DefaultDecryptionServiceImpl decService;
  public static org.sunbird.datasecurity.impl.ServiceFactory dataServiceFactory;
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
    PowerMockito.mockStatic(DefaultEncryptionServivceImpl.class);
    PowerMockito.mockStatic(org.sunbird.datasecurity.impl.ServiceFactory.class);
    DefaultDecryptionServiceImpl decryptionService =
        Mockito.mock(DefaultDecryptionServiceImpl.class);
    PowerMockito.whenNew(DefaultDecryptionServiceImpl.class)
        .withNoArguments()
        .thenReturn(decryptionService);
    when(org.sunbird.datasecurity.impl.ServiceFactory
            .getDecryptionServiceInstance(null))
        .thenReturn(decryptionService);
    probe = new TestKit(system);
    subject = system.actorOf(props);
    /* when(OTPUtil.getOTPExpirationInMinutes()).thenReturn("123456788");*/

  }

  @Test
  public void sendOTPTestForMobile() throws Exception {
    request = createOtpRequest("phone", "anyMobileNum", "anyUserId");
    when(emailTemplateDao.getTemplate(Mockito.anyString(), Mockito.any()))
        .thenReturn(
            "OTP to reset your password on $installationName is $otp. This is valid for $otpExpiryInMinutes minutes only.");
    subject.tell(request, probe.getRef());
    Response response = probe.expectMsgClass(duration("30 second"), Response.class);
    Assert.assertTrue(response.getResponseCode().equals(ResponseCode.OK));
  }

  @Test
  public void sendOTPTestForEmail() throws Exception {
    PowerMockito.mockStatic(SunbirdMWService.class);
    request = createOtpRequest("email", "anyEmailId", "anyUserId");
    when(emailTemplateDao.getTemplate(Mockito.anyString(), Mockito.any()))
        .thenReturn(
            "OTP to reset your password on $installationName is $otp. This is valid for $otpExpiryInMinutes minutes only.");
    subject.tell(request, probe.getRef());
    Response response = probe.expectMsgClass(duration("30 second"), Response.class);
    Assert.assertTrue(response.getResponseCode().equals(ResponseCode.OK));
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
