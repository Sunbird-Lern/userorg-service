package org.sunbird.actor.sync;

import java.time.Duration;


import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.testkit.javadsl.TestKit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.response.Response;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Patterns.class})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class ESSyncActorTest {

  private ActorSystem system = ActorSystem.create("system");
  private static final Props props = Props.create(EsSyncActor.class);

  @Test
  public void testSyncUser() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.SYNC.getValue());
    Map<String, Object> reqMap = new HashMap<>();
    List<String> ids = new ArrayList<>();
    ids.add("1544646556");
    reqMap.put(JsonKey.OBJECT_IDS, ids);
    reqMap.put(JsonKey.OBJECT_TYPE, JsonKey.USER);
    reqMap.put(JsonKey.OPERATION_TYPE, JsonKey.SYNC);
    reqObj.getRequest().put(JsonKey.DATA, reqMap);
    subject.tell(reqObj, probe.getRef());
    ProjectCommonException res =
        probe.expectMsgClass(Duration.ofSeconds(10), ProjectCommonException.class);
    Assert.assertTrue(null != res);
  }

  @Test
  public void testSyncUserSuccess() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.SYNC.getValue());
    Map<String, Object> reqMap = new HashMap<>();
    List<String> ids = new ArrayList<>();
    ids.add("1544646556");
    reqMap.put(JsonKey.OBJECT_IDS, ids);
    reqMap.put(JsonKey.OBJECT_TYPE, JsonKey.USER);
    reqObj.getRequest().put(JsonKey.DATA, reqMap);
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(Duration.ofSeconds(10), Response.class);
    Assert.assertTrue(null != res);
  }
}
