package org.sunbird.badge.actors;

import static akka.testkit.JavaTestKit.duration;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import java.util.HashMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.badge.BadgeOperations;
import org.sunbird.badge.service.BadgingService;
import org.sunbird.badge.service.impl.BadgingFactory;
import org.sunbird.badge.service.impl.BadgrServiceImpl;
import org.sunbird.badge.util.BadgeAssertionValidator;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.BadgingJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

/** @author arvind */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
@PrepareForTest({BadgeAssertionValidator.class, BadgingFactory.class})
public class BadgeAssertionActorTest {

  private static BadgrServiceImpl mockBadgingService;
  private static BadgingService badgingService;

  private static String ASSERTION_ID = "badge_assertion_01";
  private static String BADGE_CLASS_ID = "badge_class_01";
  private static String ISSUER_ID = "badge_issuer_01";
  private static String ASSERTION_IMAGE_URL = "badge_image_url_01";
  private static String RECIPIENT_ID = "recipient_01";
  private static String RECIPIENT_TYPE = "user";
  private static String BADGE_ID = "badge_01";

  private static ActorSystem system;
  private static final Props props = Props.create(BadgeAssertionActor.class);

  @BeforeClass
  public static void setUp() {
    system = ActorSystem.create("system");

    mockBadgingService = mock(BadgrServiceImpl.class);
    badgingService = mock(BadgrServiceImpl.class);

    PowerMockito.mockStatic(BadgingFactory.class);
    when(BadgingFactory.getInstance()).thenReturn(badgingService);
  }

  @Before
  public void beforeTest() {
    PowerMockito.mockStatic(BadgeAssertionValidator.class);
  }

  @Test
  @Ignore
  public void testCreateAssertionWithMatchRootOrgOfUserAndBadge() throws Exception {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Response response = new Response();
    when(badgingService.getBadgeClassDetails(Mockito.anyObject())).thenReturn(response);
    when(badgingService.badgeAssertion(Mockito.anyObject())).thenReturn(response);
    PowerMockito.doNothing()
        .when(
            BadgeAssertionValidator.class,
            "validateRootOrg",
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyString());

    subject.tell(getBadgeAssertionRequest(), probe.getRef());
    Response res = probe.expectMsgClass(duration("10 second"), Response.class);
    Assert.assertNotNull(res);
  }

  @Test
  public void testCreateAssertionWithMismatchRootOrgOfUserAndBadge() throws Exception {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Response response = new Response();
    when(badgingService.getBadgeClassDetails(Mockito.anyObject())).thenReturn(response);
    when(badgingService.badgeAssertion(Mockito.anyObject())).thenReturn(response);

    ProjectCommonException projectCommonException =
        new ProjectCommonException(
            ResponseCode.commonAttributeMismatch.getErrorCode(),
            ResponseCode.commonAttributeMismatch.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode(),
            JsonKey.ROOT_ORG,
            BadgingJsonKey.BADGE_TYPE_USER,
            BadgingJsonKey.BADGE);
    PowerMockito.doThrow(projectCommonException)
        .when(
            BadgeAssertionValidator.class,
            "validateRootOrg",
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyString());

    subject.tell(getBadgeAssertionRequest(), probe.getRef());
    ProjectCommonException exception =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    Assert.assertNotNull(exception);
    Assert.assertEquals(exception.getResponseCode(), ResponseCode.CLIENT_ERROR.getResponseCode());
    Assert.assertEquals(exception.getCode(), ResponseCode.commonAttributeMismatch.getErrorCode());
  }

  private Request getBadgeAssertionRequest() {
    Request reqObj = new Request();
    reqObj.setOperation(BadgeOperations.createBadgeAssertion.name());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(BadgingJsonKey.ASSERTION_ID, ASSERTION_ID);
    innerMap.put(BadgingJsonKey.BADGE_CLASS_ID, BADGE_CLASS_ID);
    innerMap.put(BadgingJsonKey.ISSUER_ID, ISSUER_ID);
    innerMap.put(BadgingJsonKey.ASSERTION_IMAGE_URL, ASSERTION_IMAGE_URL);
    innerMap.put(BadgingJsonKey.RECIPIENT_ID, RECIPIENT_ID);
    innerMap.put(BadgingJsonKey.RECIPIENT_TYPE, RECIPIENT_TYPE);
    innerMap.put(BadgingJsonKey.BADGE_ID, BADGE_ID);
    reqObj.getRequest().putAll(innerMap);

    return reqObj;
  }
}
