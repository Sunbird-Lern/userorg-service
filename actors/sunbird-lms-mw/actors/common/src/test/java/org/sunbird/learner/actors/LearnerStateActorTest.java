package org.sunbird.learner.actors;

import static akka.testkit.JavaTestKit.duration;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.testkit.javadsl.TestKit;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.ContentSearchUtil;
import org.sunbird.learner.util.EkStepRequestUtil;
import scala.concurrent.Promise;

/** @author arvind */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  ElasticSearchHelper.class,
  EsClientFactory.class,
  ContentSearchUtil.class,
  ElasticSearchRestHighImpl.class,
  EkStepRequestUtil.class
})
@PowerMockIgnore({"javax.management.*", "javax.crypto.*", "javax.net.ssl.*", "javax.security.*"})
public class LearnerStateActorTest {

  private static ActorSystem system;
  private static final Props props = Props.create(LearnerStateActor.class);
  private static CassandraOperation cassandraOperation;
  private static String userId = "user121gama";
  private static String courseId = "alpha01crs12";
  private static String courseId2 = "alpha01crs15";
  private static String batchId = "115";
  private static final String contentId = "cont3544TeBuk";
  private static ElasticSearchService esService;

  @BeforeClass
  public static void setUp() {
    system = ActorSystem.create("system");
  }

  @Before
  public void beforeTest() throws IOException {
    cassandraOperation = mock(CassandraOperation.class);
    PowerMockito.mockStatic(ServiceFactory.class);
    PowerMockito.mockStatic(EsClientFactory.class);
    PowerMockito.mockStatic(ContentSearchUtil.class);
    PowerMockito.mockStatic(ElasticSearchHelper.class);
    PowerMockito.mockStatic(EkStepRequestUtil.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    esService = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
    Promise<Map<String, Object>> promise = Futures.promise();

    Map<String, Object> esResult = new HashMap<>();
    esResult.put(JsonKey.CONTENT, new ArrayList<Map<String, Object>>());
    promise.success(esResult);
    when(esService.search(Mockito.anyObject(), Mockito.anyObject())).thenReturn(promise.future());

    Response dbResponse = new Response();
    List<Map<String, Object>> dbResponseList = new ArrayList<>();
    dbResponse.put(JsonKey.RESPONSE, dbResponseList);
    when(cassandraOperation.getRecordsByProperty(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyObject()))
        .thenReturn(dbResponse);
    when(cassandraOperation.getRecordsByProperties(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(dbResponse);
    when(cassandraOperation.getRecords(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.anyList()))
        .thenReturn(dbResponse);
  }

  @Test
  public void testGetCourseByUserId() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    mockEsUtilforUserNcourseBatch();
    mockContentUtil();
    Request request = new Request();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.USER_ID, userId);
    request.setRequest(map);
    request.setOperation(ActorOperations.GET_COURSE.getValue());
    subject.tell(request, probe.getRef());
    Response res = probe.expectMsgClass(duration("10 second"), Response.class);
    Assert.assertNotNull(res);
  }

  @Test
  public void testGetCourseWithInvalidOperation() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request request = new Request();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.USER_ID, userId);
    request.setRequest(map);
    request.setOperation("INVALID_OPERATION");
    subject.tell(request, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    Assert.assertNotNull(exc);
  }

  @Test
  public void testContentStateByAllFields() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    HashMap<String, Object> innerMap = new HashMap<>();
    Request request = new Request();
    innerMap.put(JsonKey.USER_ID, userId);
    innerMap.put(JsonKey.BATCH_ID, batchId);
    List<String> contentList = Arrays.asList(contentId);
    List<String> courseIds = Arrays.asList(courseId);
    innerMap.put(JsonKey.CONTENT_IDS, contentList);
    innerMap.put(JsonKey.COURSE_IDS, courseIds);
    request.setRequest(innerMap);
    request.setOperation(ActorOperations.GET_CONTENT.getValue());
    subject.tell(request, probe.getRef());
    Response res = probe.expectMsgClass(duration("10 second"), Response.class);
    Assert.assertNotNull(res);
  }

  @Test
  public void testContentStateByBatchId() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    HashMap<String, Object> innerMap = new HashMap<>();
    mockEkstepContentUtil();
    Request request = new Request();
    innerMap.put(JsonKey.USER_ID, userId);
    innerMap.put(JsonKey.BATCH_ID, batchId);
    innerMap.put(JsonKey.COURSE_ID, courseId);
    request.setRequest(innerMap);
    request.setOperation(ActorOperations.GET_CONTENT.getValue());
    subject.tell(request, probe.getRef());
    Response res = probe.expectMsgClass(duration("10 second"), Response.class);
    Assert.assertNotNull(res);
  }

  @Test
  public void testContentStateByOneBatchAndMultipleCourseIds() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    HashMap<String, Object> innerMap = new HashMap<>();
    Request request = new Request();
    innerMap.put(JsonKey.USER_ID, userId);
    innerMap.put(JsonKey.BATCH_ID, batchId);
    List<String> courseIds = Arrays.asList(courseId, courseId2);
    innerMap.put(JsonKey.COURSE_IDS, courseIds);
    request.setRequest(innerMap);
    request.setOperation(ActorOperations.GET_CONTENT.getValue());
    subject.tell(request, probe.getRef());
    ProjectCommonException exception =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    Assert.assertNotNull(exception);
    Assert.assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), exception.getResponseCode());
  }

  @Test
  public void testForGetContentByCourseIds() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    mockEkstepContentUtil();
    HashMap<String, Object> innerMap = new HashMap<>();
    Request request = new Request();
    innerMap.put(JsonKey.USER_ID, userId);
    innerMap.put(JsonKey.COURSE_ID, courseId);
    request.setRequest(innerMap);
    request.setOperation(ActorOperations.GET_CONTENT.getValue());
    subject.tell(request, probe.getRef());
    Response res = probe.expectMsgClass(duration("10 second"), Response.class);
    Assert.assertNotNull(res);
  }

  @Test
  public void testForGetContentByContentIds() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    HashMap<String, Object> innerMap = new HashMap<>();
    Request request = new Request();
    mockEkstepContentUtil();
    innerMap.put(JsonKey.USER_ID, userId);
    List<String> contentList = Arrays.asList(courseId, courseId2);
    innerMap.put(JsonKey.CONTENT_IDS, contentList);
    request.setRequest(innerMap);
    request.setOperation(ActorOperations.GET_CONTENT.getValue());
    subject.tell(request, probe.getRef());
    Response res = probe.expectMsgClass(duration("10 second"), Response.class);
    Assert.assertNotNull(res);
  }

  @Test
  public void testGetCourseByUserIdAndCourseBatchesSuccess() {
    String s[] = {JsonKey.STATUS};
    Map<String, Object> batch = testGetEnrolledCoursesWithBatchInfo(s);
    Assert.assertEquals(1, batch.get("status"));
  }

  @Test
  public void testGetCourseByUserIdFailureWithInvalidFieldNames() {
    String s[] = {"invalid", "status", "erre"};
    Map<String, Object> batch = testGetEnrolledCoursesWithBatchInfo(s);
    Assert.assertEquals(null, batch.get("invalid"));
  }

  @Test
  public void testGetCourseByUserIdSuccessWithoutFieldNames() {
    String s[] = {};
    Map<String, Object> batch = testGetEnrolledCoursesWithBatchInfo(s);
    Assert.assertEquals(null, batch);
  }

  private Map<String, Object> testGetEnrolledCoursesWithBatchInfo(String[] s) {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request request = new Request();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.USER_ID, "user1");
    request.setRequest(map);
    Map<String, String[]> queryParams = new HashMap<>();

    request.getContext().put(JsonKey.BATCH_DETAILS, s);
    request.setOperation(ActorOperations.GET_COURSE.getValue());
    mockEsUtilforUserNcourseBatch();
    mockContentUtil();
    subject.tell(request, probe.getRef());
    Response res = probe.expectMsgClass(duration("10 second"), Response.class);
    ProjectLogger.log(
        "Result After Completeing test is " + res.getResult() + " ", LoggerEnum.INFO.name());
    List<Map<String, Object>> courses = (List<Map<String, Object>>) res.get(JsonKey.COURSES);
    return (Map<String, Object>) ((Map<String, Object>) courses.get(0)).get(JsonKey.BATCH);
  }

  private void mockContentUtil() {
    Map<String, Object> courses = new HashMap<>();
    List<Map<String, Object>> l1 = new ArrayList<>();
    l1.add(getMapforCourse("q1", "q1", "first"));
    l1.add(getMapforCourse("q2", "q2", "second"));
    l1.add(getMapforCourse("q3", "q3", "third"));
    courses.put(JsonKey.CONTENTS, l1);

    when(ContentSearchUtil.searchContentSync(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(courses);
  }

  private void mockEkstepContentUtil() {
    Map<String, Object> courses = new HashMap<>();
    List<Map<String, Object>> l1 = new ArrayList<>();
    l1.add(getMapforCourse("q1", "q1", "first"));
    l1.add(getMapforCourse("q2", "q2", "second"));
    l1.add(getMapforCourse("q3", "q3", "third"));
    courses.put(JsonKey.CONTENTS, l1);

    when(EkStepRequestUtil.searchContent(Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(courses);
  }

  private Map<String, Object> getMapforCourse(String id, String cId, String cName) {
    Map<String, Object> m1 = new HashMap<>();
    m1.put(JsonKey.IDENTIFIER, id);
    m1.put(JsonKey.COURSE_NAME, cName);
    m1.put(JsonKey.COURSE_ID, cId);
    return m1;
  }

  private Map<String, Object> getMapforCourseBatch(String id, String cbId, String cbName) {
    Map<String, Object> m1 = new HashMap<>();
    m1.put(JsonKey.IDENTIFIER, id);
    m1.put(JsonKey.STATUS, 1);
    return m1;
  }

  private void mockEsUtilforUserNcourseBatch() {
    Map<String, Object> resultUser = new HashMap<>();
    List<Map<String, Object>> userenrolledDetails = new ArrayList<>();
    userenrolledDetails.add(getMap("u1", "cb1", "q1"));
    userenrolledDetails.add(getMap("u2", "cb2", "q2"));
    userenrolledDetails.add(getMap("u3", "cb3", "q3"));
    resultUser.put(JsonKey.CONTENT, userenrolledDetails);

    Map<String, Object> courseBatches = new HashMap<>();
    List<Map<String, Object>> l1 = new ArrayList<>();
    l1.add(getMapforCourseBatch("cb1", "q1", "first"));
    l1.add(getMapforCourseBatch("cb2", "q2", "second"));
    l1.add(getMapforCourseBatch("cb3", "q3", "third"));
    courseBatches.put(JsonKey.CONTENT, l1);

    Map<String, Object> filter = new HashMap<>();
    filter.put(JsonKey.USER_ID, "user1");
    filter.put(JsonKey.ACTIVE, ProjectUtil.ActiveStatus.ACTIVE.getValue());
    SearchDTO searchDto = new SearchDTO();
    searchDto.getAdditionalProperties().put(JsonKey.FILTERS, filter);
    Promise<Map<String, Object>> promiseResultUser = Futures.promise();
    promiseResultUser.success(resultUser);
    Promise<Map<String, Object>> promiseCourseBatch = Futures.promise();
    promiseCourseBatch.success(courseBatches);

    when(esService.search(Mockito.any(), Mockito.any()))
        .thenReturn(promiseResultUser.future())
        .thenReturn(promiseCourseBatch.future());
    when(ElasticSearchHelper.getResponseFromFuture(Mockito.any()))
        .thenReturn(resultUser)
        .thenReturn(courseBatches);
  }

  private Map<String, Object> getMap(String id, String bId, String cId) {
    Map<String, Object> m1 = new HashMap<>();
    m1.put(JsonKey.IDENTIFIER, id);
    m1.put(JsonKey.BATCH_ID, bId);
    m1.put(JsonKey.COURSE_ID, cId);
    m1.put(JsonKey.USER_ID, "test");
    return m1;
  }
}
