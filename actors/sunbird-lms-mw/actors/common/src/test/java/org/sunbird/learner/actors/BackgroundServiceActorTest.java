package org.sunbird.learner.actors;

import static org.junit.Assert.assertTrue;

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
import org.sunbird.actor.background.BackgroundOperations;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Ignore
public class BackgroundServiceActorTest {

  private static ActorSystem system;
  private static final Props props = Props.create(BackGroundServiceActor.class);
  private static CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private static Util.DbInfo geoLocationDbInfo = Util.dbInfoMap.get(JsonKey.GEO_LOCATION_DB);
  private static final String locnId = "hhjcjrdf4scdv56vf79fw4p89";

  @BeforeClass
  public static void setUp() {

    Util.checkCassandraDbConnections(JsonKey.SUNBIRD);
    system = ActorSystem.create("system");

    Map<String, Object> locnMap = new HashMap<String, Object>();
    locnMap.put(JsonKey.ID, locnId);
    cassandraOperation.insertRecord(
        geoLocationDbInfo.getKeySpace(), geoLocationDbInfo.getTableName(), locnMap);
  }

  @Test
  public void updateUserCountTest() {

    List<Object> locnIdList = new ArrayList<>();
    locnIdList.add(locnId);

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = new Request();

    actorMessage.getRequest().put(JsonKey.LOCATION_IDS, locnIdList);
    actorMessage.getRequest().put(JsonKey.OPERATION, "GeoLocationManagementActor");
    actorMessage.setOperation(BackgroundOperations.updateUserCountToLocationID.name());

    subject.tell(actorMessage, probe.getRef());
    try {
      Thread.sleep(20000);
    } catch (InterruptedException e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    Response response =
        cassandraOperation.getRecordById(
            geoLocationDbInfo.getKeySpace(), geoLocationDbInfo.getTableName(), locnId);
    // probe.expectMsgClass(duration("300 second"),Response.class);
    List<Map<String, Object>> reslist = (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    Map<String, Object> map = reslist.get(0);
    int count = (int) map.get(JsonKey.USER_COUNT);
    boolean bool = (count >= 0) ? true : false;
    assertTrue(bool);
  }

  @Test
  public void updateUserCountTest2() {

    Map<String, Object> locnMap = new HashMap<String, Object>();
    locnMap.put(JsonKey.ID, locnId);
    locnMap.put(JsonKey.USER_COUNT, 0);
    cassandraOperation.updateRecord(
        geoLocationDbInfo.getKeySpace(), geoLocationDbInfo.getTableName(), locnMap);

    List<Object> locnIdList = new ArrayList<>();
    locnIdList.add(locnId);

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = new Request();

    actorMessage.getRequest().put(JsonKey.LOCATION_IDS, locnIdList);
    actorMessage.getRequest().put(JsonKey.OPERATION, "UpdateUserCountScheduler");
    actorMessage.setOperation(BackgroundOperations.updateUserCountToLocationID.name());

    subject.tell(actorMessage, probe.getRef());
    try {
      Thread.sleep(20000);
    } catch (InterruptedException e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    Response response =
        cassandraOperation.getRecordById(
            geoLocationDbInfo.getKeySpace(), geoLocationDbInfo.getTableName(), locnId);
    // probe.expectMsgClass(duration("300 second"),Response.class);
    List<Map<String, Object>> reslist = (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    Map<String, Object> map = reslist.get(0);
    int count = (int) map.get(JsonKey.USER_COUNT);
    boolean bool = (count >= 0) ? true : false;
    assertTrue(bool);
  }

  @Test
  public void updateUserCountTest3() {

    Map<String, Object> locnMap = new HashMap<String, Object>();
    locnMap.put(JsonKey.ID, locnId);
    locnMap.put(JsonKey.USER_COUNT, 0);
    locnMap.put(JsonKey.USER_COUNT_TTL, "abc");
    cassandraOperation.updateRecord(
        geoLocationDbInfo.getKeySpace(), geoLocationDbInfo.getTableName(), locnMap);

    List<Object> locnIdList = new ArrayList<>();
    locnIdList.add(locnId);

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = new Request();

    actorMessage.getRequest().put(JsonKey.LOCATION_IDS, locnIdList);
    actorMessage.getRequest().put(JsonKey.OPERATION, "GeoLocationManagementActor");
    actorMessage.setOperation(BackgroundOperations.updateUserCountToLocationID.name());

    subject.tell(actorMessage, probe.getRef());
    try {
      Thread.sleep(20000);
    } catch (InterruptedException e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    Response response =
        cassandraOperation.getRecordById(
            geoLocationDbInfo.getKeySpace(), geoLocationDbInfo.getTableName(), locnId);
    // probe.expectMsgClass(duration("300 second"),Response.class);
    List<Map<String, Object>> reslist = (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    Map<String, Object> map = reslist.get(0);
    int count = (int) map.get(JsonKey.USER_COUNT);
    boolean bool = (count >= 0) ? true : false;
    assertTrue(bool);
  }

  @AfterClass
  public static void destroy() {
    cassandraOperation.deleteRecord(
        geoLocationDbInfo.getKeySpace(), geoLocationDbInfo.getTableName(), locnId);
  }
}
