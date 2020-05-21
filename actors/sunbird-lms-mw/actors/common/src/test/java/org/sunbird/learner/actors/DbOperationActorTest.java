package org.sunbird.learner.actors;

import static akka.testkit.JavaTestKit.duration;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.sunbird.actor.service.SunbirdMWService;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.learner.datapersistence.DbOperationActor;
import org.sunbird.learner.util.Util;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Ignore
public class DbOperationActorTest {
  private static ActorSystem system;
  private static final Props props = Props.create(DbOperationActor.class);

  @BeforeClass
  public static void setUp() {
    SunbirdMWService.init();
    system = ActorSystem.create("system");
    Util.checkCassandraDbConnections(JsonKey.SUNBIRD_PLUGIN);
  }

  @Test
  public void testInvalidOperation() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation("INVALID_OPERATION");

    subject.tell(reqObj, probe.getRef());
    ProjectCommonException exc = probe.expectMsgClass(ProjectCommonException.class);
    Assert.assertTrue(null != exc);
  }

  @Test
  public void testInvalidMessageType() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    subject.tell("Invalid Type", probe.getRef());
    ProjectCommonException exc = probe.expectMsgClass(ProjectCommonException.class);
    Assert.assertTrue(null != exc);
  }

  @Test
  public void testA1Create() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.CREATE_DATA.getValue());
    Map<String, Object> map = new HashMap<>();
    map.put("entityName", "announcement");
    map.put("indexed", true);
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put("id", "454ee9-17-a2-47-id");
    innerMap.put("sourceid", "45_sourceId");
    innerMap.put("userid", "230cb747-userId");
    innerMap.put("status", "active");
    map.put("payload", innerMap);
    reqObj.setRequest(map);
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(duration("20 second"), Response.class);
    Assert.assertTrue(null != res.get(JsonKey.RESPONSE));
  }

  @Test
  public void testA1CreateFrExc() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.CREATE_DATA.getValue());
    Map<String, Object> map = new HashMap<>();
    map.put("entityName", "announcement");
    map.put("indexed", true);
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put("id", "454ee9-17-a2-47-id");
    innerMap.put("sourceid", "45_sourceId");
    innerMap.put("userid", "230cb747-userId");
    innerMap.put("status1", "active");
    map.put("payload", innerMap);
    reqObj.setRequest(map);
    subject.tell(reqObj, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("20 second"), ProjectCommonException.class);
    Assert.assertTrue(null != exc);
  }

  @Test
  public void testA2Update() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.UPDATE_DATA.getValue());
    Map<String, Object> map = new HashMap<>();
    map.put("entityName", "announcement");
    map.put("indexed", true);
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put("id", "454ee9-17-a2-47-id");
    innerMap.put("sourceid", "45_sourceId");
    innerMap.put("userid", "230cb747-userId");
    innerMap.put("status", "inactive");
    map.put("payload", innerMap);
    reqObj.setRequest(map);
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(duration("20 second"), Response.class);
    Assert.assertTrue(null != res.get(JsonKey.RESPONSE));
  }

  @Test
  public void testA2UpdateFrExc() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.UPDATE_DATA.getValue());
    Map<String, Object> map = new HashMap<>();
    map.put("entityName", "announcement");
    map.put("indexed", true);
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put("id", "454ee9-17-a2-47-id");
    innerMap.put("sourceid", "45_sourceId");
    innerMap.put("userid", "230cb747-userId");
    innerMap.put("status1", "inactive");
    map.put("payload", innerMap);
    reqObj.setRequest(map);
    subject.tell(reqObj, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("20 second"), ProjectCommonException.class);
    Assert.assertTrue(null != exc);
  }

  @Test
  public void testA3Read() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.READ_DATA.getValue());
    Map<String, Object> map = new HashMap<>();
    map.put("entityName", "announcement");
    map.put("id", "454ee9-17-a2-47-id");
    reqObj.setRequest(map);
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(duration("20 second"), Response.class);
    Assert.assertTrue(null != res.get(JsonKey.RESPONSE));
  }

  @Test
  public void testA3ReadFrExec() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.READ_DATA.getValue());
    Map<String, Object> map = new HashMap<>();
    map.put("entityName", "announcement1");
    map.put("id", "454ee9-17-a2-47-id");
    reqObj.setRequest(map);
    subject.tell(reqObj, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("20 second"), ProjectCommonException.class);
    Assert.assertTrue(null != exc);
  }

  @Test
  public void testA4ReadAll() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.READ_ALL_DATA.getValue());
    Map<String, Object> map = new HashMap<>();
    map.put("entityName", "announcement");
    reqObj.setRequest(map);
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(duration("20 second"), Response.class);
    Assert.assertTrue(null != res.get(JsonKey.RESPONSE));
  }

  @Test
  public void testA4ReadAllFrExc() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.READ_ALL_DATA.getValue());
    Map<String, Object> map = new HashMap<>();
    map.put("entityName", "announcement1");
    reqObj.setRequest(map);
    subject.tell(reqObj, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("20 second"), ProjectCommonException.class);
    Assert.assertTrue(null != exc);
  }

  @Test
  public void testA5Search() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.SEARCH_DATA.getValue());
    Map<String, Object> map = new HashMap<>();
    map.put("entityName", "announcement");
    List<String> list = new ArrayList<>();
    list.add("id");
    map.put("requiredFields", list);
    Map<String, Object> filter = new HashMap<>();
    map.put(JsonKey.FILTERS, filter);
    reqObj.setRequest(map);
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(duration("20 second"), Response.class);
    Assert.assertTrue(null != res.get(JsonKey.RESPONSE));
  }

  @Test
  public void testA6delete() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.DELETE_DATA.getValue());
    Map<String, Object> map = new HashMap<>();
    map.put("entityName", "announcement");
    map.put("indexed", true);
    map.put("id", "454ee9-17-a2-47-id");
    reqObj.setRequest(map);
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(duration("20 second"), Response.class);
    Assert.assertTrue(null != res.get(JsonKey.RESPONSE));
  }

  @Test
  public void testA7getMetrics() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.GET_METRICS.getValue());
    Map<String, Object> map = new HashMap<>();
    map.put("entityName", "announcement");
    Map<String, Object> query = new HashMap<>();
    map.put("rawQuery", query);
    reqObj.setRequest(map);
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(duration("20 second"), Response.class);
    Assert.assertTrue(null != res.get(JsonKey.RESPONSE));
  }
}
