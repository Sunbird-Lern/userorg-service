package org.sunbird.actor.user;

import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.testkit.javadsl.TestKit;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.service.organisation.OrgService;
import org.sunbird.service.organisation.impl.OrgServiceImpl;
import org.sunbird.sso.KeycloakRequiredActionLinkUtil;
import org.sunbird.sso.SSOManager;
import org.sunbird.sso.SSOServiceFactory;
import org.sunbird.util.UserUtility;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  OrgServiceImpl.class,
  OrgService.class,
  KeycloakRequiredActionLinkUtil.class,
  UserUtility.class,
  SSOServiceFactory.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class UserOnBoardingNotificationActorTest {
  private ActorSystem system = ActorSystem.create("system");
  private TestKit probe = new TestKit(system);
  private Props props = Props.create(UserOnboardingNotificationActor.class);

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(SSOServiceFactory.class);
    SSOManager ssoManager = PowerMockito.mock(SSOManager.class);
    PowerMockito.when(SSOServiceFactory.getInstance()).thenReturn(ssoManager);
    PowerMockito.mockStatic(UserUtility.class);
    PowerMockito.mockStatic(OrgServiceImpl.class);
    OrgService orgService = mock(OrgServiceImpl.class);
    when(OrgServiceImpl.getInstance()).thenReturn(orgService);
    Map<String, Object> orgMap = new HashMap<>();
    orgMap.put(JsonKey.SLUG, "slug");
    when(orgService.getOrgById(Mockito.anyString(), Mockito.any())).thenReturn(orgMap);
  }

  @Test
  public void testsendEmail() {
    PowerMockito.mockStatic(KeycloakRequiredActionLinkUtil.class);
    PowerMockito.when(
            KeycloakRequiredActionLinkUtil.getLink(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn("someLink");

    Request request = new Request();
    RequestContext context = new RequestContext();
    context.setReqId("54654654646546");
    context.setOp("operation");
    request.setRequestContext(context);
    request.setOperation(ActorOperations.PROCESS_ONBOARDING_MAIL_AND_SMS.getValue());
    request.getRequest().put(JsonKey.USERNAME, "userName");
    request.getRequest().put(JsonKey.ROOT_ORG_ID, "rootOrgId");
    request.getRequest().put(JsonKey.EMAIL, "xyz@xyz.com");
    request.getRequest().put(JsonKey.PASSWORD, "password");
    ActorRef subject = system.actorOf(props);
    subject.tell(request, probe.getRef());
    probe.expectNoMessage();
    assertTrue(true);
  }

  @Test
  public void testsendSms() {
    PowerMockito.mockStatic(KeycloakRequiredActionLinkUtil.class);
    PowerMockito.when(
            KeycloakRequiredActionLinkUtil.getLink(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn("someLink");

    Request request = new Request();
    RequestContext context = new RequestContext();
    context.setReqId("54654654646546");
    context.setOp("operation");
    request.setRequestContext(context);
    request.setOperation(ActorOperations.PROCESS_ONBOARDING_MAIL_AND_SMS.getValue());
    request.getRequest().put(JsonKey.USERNAME, "userName");
    request.getRequest().put(JsonKey.ROOT_ORG_ID, "rootOrgId");
    request.getRequest().put(JsonKey.SET_PASSWORD_LINK, "link");
    request.getRequest().put(JsonKey.PHONE, "9999999999");
    request.getRequest().put(JsonKey.PASSWORD, "password");
    ActorRef subject = system.actorOf(props);
    subject.tell(request, probe.getRef());
    probe.expectNoMessage();
    assertTrue(true);
  }

  @Test
  public void testResetPass() {
    PowerMockito.mockStatic(KeycloakRequiredActionLinkUtil.class);
    PowerMockito.when(
            KeycloakRequiredActionLinkUtil.getLink(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn("someLink");

    Request request = new Request();
    RequestContext context = new RequestContext();
    context.setReqId("54654654646546");
    context.setOp("operation");
    request.setRequestContext(context);
    request.setOperation(ActorOperations.PROCESS_PASSWORD_RESET_MAIL_AND_SMS.getValue());
    request.getRequest().put(JsonKey.USERNAME, "userName");
    request.getRequest().put(JsonKey.ROOT_ORG_ID, "rootOrgId");
    request.getRequest().put(JsonKey.EMAIL, "xyz@xyz.com");
    // request.getRequest().put(JsonKey.PASSWORD,"password");

    ActorRef subject = system.actorOf(props);
    subject.tell(request, probe.getRef());
    probe.expectNoMessage();
    assertTrue(true);
  }

  @Test
  public void testResetPassWithPhone() {
    PowerMockito.mockStatic(KeycloakRequiredActionLinkUtil.class);
    PowerMockito.when(
            KeycloakRequiredActionLinkUtil.getLink(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn("someLink");

    Request request = new Request();
    RequestContext context = new RequestContext();
    context.setReqId("54654654646546");
    context.setOp("operation");
    request.setRequestContext(context);
    request.setOperation(ActorOperations.PROCESS_PASSWORD_RESET_MAIL_AND_SMS.getValue());
    request.getRequest().put(JsonKey.USERNAME, "userName");
    request.getRequest().put(JsonKey.ROOT_ORG_ID, "rootOrgId");
    request.getRequest().put(JsonKey.PHONE, "9999999999");
    // request.getRequest().put(JsonKey.PASSWORD,"password");

    ActorRef subject = system.actorOf(props);
    subject.tell(request, probe.getRef());
    probe.expectNoMessage();
    assertTrue(true);
  }
}
