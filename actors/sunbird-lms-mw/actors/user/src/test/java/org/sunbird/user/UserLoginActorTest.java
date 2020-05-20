package org.sunbird.user;

import static akka.testkit.JavaTestKit.duration;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import org.junit.Assert;
import org.junit.Test;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.user.actors.UserLoginActor;

public class UserLoginActorTest {

  private static final String INVALID_OPERATION = "INVALID_OPERATION";
  private static final Props props = Props.create(UserLoginActor.class);
  private static ActorSystem system = ActorSystem.create("system");
  private String userId = "someUserId";
  private TestKit probe = new TestKit(system);
  private ActorRef subject = system.actorOf(props);

  @Test
  public void testUpdateUserLoginTimeSuccess() {
    Request request = new Request();

    request.setOperation(ActorOperations.USER_CURRENT_LOGIN.getValue());
    request.put(JsonKey.USER_ID, userId);

    subject.tell(request, probe.getRef());

    Response response = probe.expectMsgClass(duration("10 second"), Response.class);
    Assert.assertTrue(null != response && response.getResponseCode() == ResponseCode.OK);
  }

  @Test
  public void testUpdateUserLoginTimeFailureWithInvalidMessage() {
    Request request = new Request();

    request.setOperation(INVALID_OPERATION);
    request.put(JsonKey.USER_ID, userId);

    subject.tell(request, probe.getRef());

    ProjectCommonException exception =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    Assert.assertTrue(
        ((ProjectCommonException) exception)
            .getCode()
            .equals(ResponseCode.invalidRequestData.getErrorCode()));
  }
}
