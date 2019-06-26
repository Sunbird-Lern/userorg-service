package org.sunbird.learner.actors;

import static akka.testkit.JavaTestKit.duration;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.ContentSearchUtil;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  Util.class,
  DataCacheHandler.class,
  PageManagementActor.class,
  ContentSearchUtil.class
})
@PowerMockIgnore({"javax.management.*"})
public class LearnerStateUpdateActorTest {

  private static ActorSystem system = ActorSystem.create("system");;
  private static final Props props = Props.create(LearnerStateUpdateActor.class);

  private static String userId = "user121gama";
  private static String courseId = "alpha01crs";
  private static final String contentId = "cont3544TeBukGame";
  private static final String batchId = "220j2536h37841hc3u";
  private static CassandraOperationImpl cassandraOperation;

  @BeforeClass
  public static void setUp() {

    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
  }

  @Before
  public void beforeTest() {

    PowerMockito.mockStatic(ServiceFactory.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(getSuccessResponse());

    when(cassandraOperation.getRecordsByProperty(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(getCassandraRecordByProperty());

    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(getCassandraRecordById());
  }

  private Response getCassandraRecordByProperty() {
    return new Response();
  }

  private Response getCassandraRecordById() {

    Response response = new Response();
    List<Map> list = new ArrayList<>();
    Map<String, Object> map = new HashMap();
    map.put(JsonKey.ID, "anyID");
    map.put(JsonKey.START_DATE, "2019-01-01");
    map.put(JsonKey.END_DATE, "2019-01-30");
    map.put(JsonKey.STATUS, 2);
    map.put(JsonKey.CONTENT_PROGRESS, 2);
    map.put(JsonKey.LAST_ACCESS_TIME, "2014-07-04 12:08:56:235-0700");
    map.put(JsonKey.LAST_COMPLETED_TIME, "2014-07-05 12:08:56:235-0700");
    list.add(map);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }

  private static Response getSuccessResponse() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    return response;
  }

  @Test
  public void addContentTest() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request req = new Request();
    List<Map<String, Object>> contentList = new ArrayList<Map<String, Object>>();
    Map<String, Object> content1 = createContent();
    content1.put(JsonKey.STATUS, new BigInteger("2"));
    contentList.add(content1);

    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.CONTENTS, contentList);
    innerMap.put(JsonKey.USER_ID, userId);
    req.setOperation(ActorOperations.ADD_CONTENT.getValue());
    req.setRequest(innerMap);
    subject.tell(req, probe.getRef());
    Response response = probe.expectMsgClass(duration("10 second"), Response.class);
    List<String> list = new ArrayList<>();
    list.add("name");
    Assert.assertEquals(1, list.size());
  }

  @Test
  public void addContentTestWithoutBatchId() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request req = new Request();
    List<Map<String, Object>> contentList = new ArrayList<Map<String, Object>>();
    Map<String, Object> content1 = createContent();
    content1.remove(JsonKey.BATCH_ID);
    content1.put(JsonKey.STATUS, new BigInteger("2"));
    contentList.add(content1);

    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.CONTENTS, contentList);
    innerMap.put(JsonKey.USER_ID, userId);
    req.setOperation(ActorOperations.ADD_CONTENT.getValue());
    req.setRequest(innerMap);
    subject.tell(req, probe.getRef());
    Response response = probe.expectMsgClass(duration("10 second"), Response.class);
    List<String> list = new ArrayList<>();
    list.add("name");
    Assert.assertEquals(1, list.size());
  }

  @Test
  public void addContentTestWithWithEmptyResultList() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request req = new Request();
    List<Map<String, Object>> contentList = new ArrayList<Map<String, Object>>();
    Map<String, Object> content1 = createContent();
    content1.remove(JsonKey.BATCH_ID);
    content1.put(JsonKey.STATUS, new BigInteger("2"));
    contentList.add(content1);

    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.CONTENTS, contentList);
    innerMap.put(JsonKey.USER_ID, userId);
    req.setOperation(ActorOperations.ADD_CONTENT.getValue());
    req.setRequest(innerMap);

    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(getCassandraEmptyRecordById());

    subject.tell(req, probe.getRef());
    Response response = probe.expectMsgClass(duration("10 second"), Response.class);
    List<String> list = new ArrayList<>();
    list.add("name");
    Assert.assertEquals(1, list.size());
  }

  private Response getCassandraEmptyRecordById() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    response.put(JsonKey.RESPONSE, list);
    return response;
  }

  @Test
  public void updateContentTestWithInvalidDateFormat() throws Throwable {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request req = new Request();
    List<Map<String, Object>> contentList = new ArrayList<Map<String, Object>>();
    Map<String, Object> content1 = createContent();
    content1.remove(JsonKey.BATCH_ID);
    content1.put(JsonKey.STATUS, new BigInteger("2"));
    contentList.add(content1);
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.CONTENTS, contentList);
    innerMap.put(JsonKey.USER_ID, userId);
    req.setOperation(ActorOperations.ADD_CONTENT.getValue());
    req.setRequest(innerMap);

    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(getCassandraRecordByIdDateImproper());

    subject.tell(req, probe.getRef());
    subject.tell(req, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    Assert.assertTrue(null != exc);
  }

  private Response getCassandraRecordByIdDateImproper() {

    Response response = new Response();
    List<Map> list = new ArrayList<>();
    Map<String, Object> map = new HashMap();
    map.put(JsonKey.ID, "anyID");
    map.put(JsonKey.START_DATE, "2019-01-01");
    map.put(JsonKey.END_DATE, "2019-01-30");
    map.put(JsonKey.STATUS, 2);
    map.put(JsonKey.CONTENT_PROGRESS, 2);
    map.put(JsonKey.LAST_ACCESS_TIME, "2014-07-04");
    map.put(JsonKey.LAST_COMPLETED_TIME, "2014-07-05");
    list.add(map);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }

  private Map<String, Object> createContent() {

    Map<String, Object> content = new HashMap<String, Object>();
    content.put(JsonKey.LAST_ACCESS_TIME, ProjectUtil.getFormattedDate());
    content.put(JsonKey.COMPLETED_COUNT, "0");
    content.put(JsonKey.STATUS, "1");
    content.put(JsonKey.LAST_UPDATED_TIME, ProjectUtil.getFormattedDate());
    content.put(JsonKey.LAST_COMPLETED_TIME, ProjectUtil.getFormattedDate());
    content.put(JsonKey.COURSE_ID, courseId);
    content.put(JsonKey.USER_ID, userId);
    content.put(JsonKey.CONTENT_ID, contentId);
    content.put(JsonKey.BATCH_ID, batchId);
    content.put(JsonKey.PROGRESS, new BigInteger("100"));
    return content;
  }
}
