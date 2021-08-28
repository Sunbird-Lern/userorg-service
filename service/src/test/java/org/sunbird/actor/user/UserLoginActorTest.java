package org.sunbird.actor.user;

import static akka.testkit.JavaTestKit.duration;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import org.junit.Assert;
import org.junit.Test;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.response.Response;

public class UserLoginActorTest {

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
}
