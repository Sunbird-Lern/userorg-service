package org.sunbird.user;

import static akka.testkit.JavaTestKit.duration;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import java.util.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.KeyCloakConnectionProvider;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.user.actors.UserTypeActor;

@RunWith(PowerMockRunner.class)
@PrepareForTest({KeyCloakConnectionProvider.class})
@PowerMockIgnore("javax.management.*")
public class UserTypeActorTest {

  private static ActorSystem system = ActorSystem.create("system");
  private static final Props props = Props.create(UserTypeActor.class);

  @Test
  public void testGetUserTypesSuccess() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.GET_USER_TYPES.getValue());
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(duration("10 second"), Response.class);
    Assert.assertTrue(res.getResponseCode() == ResponseCode.OK && getResponse(res));
  }

  private boolean getResponse(Response res) {

    List<Map<String, String>> lst =
        (List<Map<String, String>>) res.getResult().get(JsonKey.RESPONSE);
    Set<String> types = new HashSet<>();
    for (Map map : lst) {
      Iterator<Map.Entry<String, String>> itr = map.entrySet().iterator();
      while (itr.hasNext()) {
        Map.Entry<String, String> entry = itr.next();
        types.add(entry.getValue());
      }
    }
    if (types.contains("OTHER") && types.contains("TEACHER") && types.size() == 2) return true;
    return false;
  }
}
