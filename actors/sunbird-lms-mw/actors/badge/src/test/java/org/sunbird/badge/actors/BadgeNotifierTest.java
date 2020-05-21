package org.sunbird.badge.actors;

import static akka.testkit.JavaTestKit.duration;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
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
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.BadgingJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseMessage;
import org.sunbird.learner.util.Util;

/** @author Mahesh Kumar Gangula */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Util.class})
@PowerMockIgnore("javax.management.*")
@Ignore
public class BadgeNotifierTest {

  private static ActorSystem system;
  private static TestKit probe;
  private static ActorRef actor;

  @BeforeClass
  public static void setUp() throws Exception {
    system = ActorSystem.create("system");
    probe = new TestKit(system);
    actor = system.actorOf(Props.create(BadgeNotifier.class));
  }

  @Test
  public void checkTelemetryKeyFailure() throws Exception {

    String telemetryEnvKey = "user";
    Request request = new Request();
    request.setOperation(BadgeOperations.assignBadgeMessage.name());

    PowerMockito.mockStatic(Util.class);
    PowerMockito.doNothing()
        .when(
            Util.class,
            "initializeContext",
            Mockito.any(Request.class),
            Mockito.eq(telemetryEnvKey));

    Map<String, Object> data = new HashMap<String, Object>();
    request.setRequest(data);
    data.put(JsonKey.ID, "test_content");
    data.put(BadgingJsonKey.BADGE_ASSERTION, new HashMap<>());
    actor.tell(request, probe.getRef());
    probe.expectMsgClass(Response.class);
    Assert.assertTrue(!(telemetryEnvKey.charAt(0) >= 65 && telemetryEnvKey.charAt(0) <= 90));
  }

  @Test
  public void invalidOperation() {
    Request request = new Request();
    request.setOperation("invalidOperation");
    actor.tell(request, probe.getRef());
    ProjectCommonException ex = probe.expectMsgClass(ProjectCommonException.class);
    Assert.assertTrue(ResponseMessage.Message.INVALID_OPERATION_NAME.equals(ex.getMessage()));
    Assert.assertTrue(ResponseMessage.Key.INVALID_OPERATION_NAME.equals(ex.getCode()));
  }

  @Test
  public void assignBadgeWithoutObjectType() {
    Request request = new Request();
    request.setOperation(BadgeOperations.assignBadgeMessage.name());
    Map<String, Object> data = new HashMap<String, Object>();
    request.setRequest(data);
    data.put(JsonKey.ID, "test_content");
    data.put(BadgingJsonKey.BADGE_ASSERTION, new HashMap<>());
    actor.tell(request, probe.getRef());
    Response res = probe.expectMsgClass(duration("100 second"), Response.class);
    Object obj = res.getResult();
    //    res = probe.expectMsgClass(duration("10 second"), Response.class);
    Assert.assertTrue(null != res);
  }

  @Test
  public void assignBadgeWithoutId() {
    Request request = new Request();
    request.setOperation(BadgeOperations.assignBadgeMessage.name());
    Map<String, Object> data = new HashMap<String, Object>();
    request.setRequest(data);
    data.put(JsonKey.OBJECT_TYPE, "Content");
    data.put(BadgingJsonKey.BADGE_ASSERTION, new HashMap<>());
    actor.tell(request, probe.getRef());
    ProjectCommonException ex = probe.expectMsgClass(ProjectCommonException.class);
    Assert.assertTrue("Please provide content id.".equals(ex.getMessage()));
  }

  @Test
  public void assignBadgeWithout() {
    Request request = new Request();
    request.setOperation(BadgeOperations.assignBadgeMessage.name());
    Map<String, Object> data = new HashMap<String, Object>();
    request.setRequest(data);
    data.put(JsonKey.OBJECT_TYPE, "Content");
    data.put(JsonKey.ID, "test_content");
    actor.tell(request, probe.getRef());
    ProjectCommonException ex = probe.expectMsgClass(ProjectCommonException.class);
    Assert.assertTrue("Please provide badge details.".equals(ex.getMessage()));
  }
}
