package org.sunbird.learner.actors;

import static akka.testkit.JavaTestKit.duration;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.testkit.javadsl.TestKit;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.kafka.client.InstructionEventGenerator;
import org.sunbird.learner.util.ContentSearchUtil;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  ContentSearchUtil.class,
  ElasticSearchHelper.class,
  ElasticSearchRestHighImpl.class,
  EsClientFactory.class,
  InstructionEventGenerator.class
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
  private static ElasticSearchService esService;

  @BeforeClass
  public static void setUp() {

    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
  }

  @Before
  public void beforeTest() throws Exception {

    PowerMockito.mockStatic(ServiceFactory.class);
    PowerMockito.mockStatic(ElasticSearchHelper.class);
    PowerMockito.mockStatic(EsClientFactory.class);
    PowerMockito.mockStatic(InstructionEventGenerator.class);
    esService = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(cassandraOperation.batchInsert(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyList()))
        .thenReturn(getSuccessResponse());

    when(cassandraOperation.upsertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(getSuccessResponse());

    when(cassandraOperation.getRecords(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.anyList()))
        .thenReturn(getCassandraRecordById());

    PowerMockito.doNothing()
        .when(InstructionEventGenerator.class, "pushInstructionEvent", Mockito.anyMap());

    mockEsUtilforCourseBatch();
  }

  private void mockEsUtilforCourseBatch() {
    Map<String, Object> courseBatches = new HashMap<>();
    List<Map<String, Object>> l1 = new ArrayList<>();
    l1.add(getMapforCourseBatch(batchId));
    l1.add(getMapforCourseBatch("cb2"));
    l1.add(getMapforCourseBatch("cb3"));
    courseBatches.put(JsonKey.CONTENT, l1);
    Promise<Map<String, Object>> promiseCourseBatch = Futures.promise();
    promiseCourseBatch.success(courseBatches);

    when(esService.search(Mockito.any(), Mockito.any())).thenReturn(promiseCourseBatch.future());
    when(ElasticSearchHelper.getResponseFromFuture(Mockito.any())).thenReturn(courseBatches);
  }

  private Map<String, Object> getMapforCourseBatch(String id) {
    Map<String, Object> m1 = new HashMap<>();
    m1.put(JsonKey.BATCH_ID, id);
    m1.put(JsonKey.STATUS, 1);
    return m1;
  }

  private Response getCassandraRecordById() {

    Response response = new Response();
    List<Map> list = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.USER_ID, "anyUserID");
    map.put(JsonKey.CONTENT_ID, contentId);
    map.put(JsonKey.BATCH_ID, batchId);
    map.put(JsonKey.COURSE_ID, "anyCourseId");
    map.put(JsonKey.STATUS, 2);
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
    Assert.assertNotNull(response);
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
    Assert.assertNotNull(response);
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
    Assert.assertNotNull(response);
  }

  private Response getCassandraEmptyRecordById() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    response.put(JsonKey.RESPONSE, list);
    return response;
  }

  @Test
  public void updateContentTestWithInvalidBatch() throws Throwable {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request req = new Request();
    List<Map<String, Object>> contentList = new ArrayList<Map<String, Object>>();
    Map<String, Object> content1 = createContent();
    content1.put(JsonKey.BATCH_ID, "randomWrongBatchId");
    content1.put(JsonKey.STATUS, new BigInteger("2"));
    contentList.add(content1);
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.CONTENTS, contentList);
    innerMap.put(JsonKey.USER_ID, userId);
    req.setOperation(ActorOperations.ADD_CONTENT.getValue());
    req.setRequest(innerMap);

    subject.tell(req, probe.getRef());
    subject.tell(req, probe.getRef());
    Response res = probe.expectMsgClass(duration("10 second"), Response.class);
    Assert.assertNotNull(res.getResult().get("BATCH_NOT_EXISTS"));
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
