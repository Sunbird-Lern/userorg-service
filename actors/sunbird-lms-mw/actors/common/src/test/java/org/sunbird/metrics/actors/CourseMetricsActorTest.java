package org.sunbird.metrics.actors;

import static akka.testkit.JavaTestKit.duration;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.testkit.javadsl.TestKit;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpMethod;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil.EsType;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.common.util.CloudStorageUtil;
import org.sunbird.helper.ServiceFactory;
import scala.concurrent.Promise;

/**
 * Junit test cases for course progress metrics.
 *
 * @author arvind.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ElasticSearchRestHighImpl.class,
  HttpClientBuilder.class,
  ServiceFactory.class,
  CloudStorageUtil.class,
  EsClientFactory.class
})
@PowerMockIgnore("javax.management.*")
@Ignore
public class CourseMetricsActorTest {

  private static ActorSystem system;
  private static final Props props = Props.create(CourseMetricsActor.class);
  private static String userId = "dnk298voopir80249";
  private static String batchId = "jkwf6t3r083fp4h";
  private static int limit = 200;
  private static int offset = 0;
  private static int progress = 4;
  private static final String orgId = "vdckcyigc68569";
  private static Map<String, Object> infoMap = new HashMap<>();
  private static Map<String, Object> userOrgMap = new HashMap<>();
  private static final String HTTP_POST = "POST";
  private static ObjectMapper mapper = new ObjectMapper();
  private static CassandraOperationImpl cassandraOperation = mock(CassandraOperationImpl.class);
  private static ElasticSearchService esService;
  private static final String SIGNED_URL = "SIGNED_URL";

  @BeforeClass
  public static void setUp() {
    system = ActorSystem.create("system");
    infoMap.put(JsonKey.FIRST_NAME, "user_first_name");
    infoMap.put(JsonKey.BATCH_ID, "batch_123");
  }

  @Before
  public void before() {
    esService = mock(ElasticSearchRestHighImpl.class);
    PowerMockito.mockStatic(EsClientFactory.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
    mockESComplexSearch();
    mockESGetDataByIdentifier();
    PowerMockito.mockStatic(ServiceFactory.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
  }

  @Test
  public void testCourseProgressMetricsSuccess() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request actorMessage = new Request();
    actorMessage.put(JsonKey.REQUESTED_BY, userId);
    actorMessage.put(JsonKey.BATCH_ID, batchId);
    actorMessage.put(JsonKey.PERIOD, "fromBegining");
    actorMessage.setOperation(ActorOperations.COURSE_PROGRESS_METRICS.getValue());

    subject.tell(actorMessage, probe.getRef());
    Response res = probe.expectMsgClass(duration("100 second"), Response.class);
    Assert.assertTrue(null != res.get(JsonKey.PERIOD));
  }

  @Ignore
  public void testWithUnsupportedMessageType() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    subject.tell("Invalid Object Type", probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    Assert.assertTrue(null != exc);
  }

  @Test
  public void testCourseProgressMetricsWithInvalidPeriod() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request actorMessage = new Request();
    actorMessage.put(JsonKey.REQUESTED_BY, userId);
    actorMessage.put(JsonKey.BATCH_ID, batchId);
    actorMessage.put(JsonKey.PERIOD, "10d");
    actorMessage.setOperation(ActorOperations.COURSE_PROGRESS_METRICS.getValue());

    subject.tell(actorMessage, probe.getRef());
    ProjectCommonException e =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    Assert.assertEquals("INVALID_PERIOD", e.getCode());
  }

  @Test
  public void testCourseProgressMetricsV2WithInvalidBatchId() {
    ProjectCommonException e =
        (ProjectCommonException) getResponseOfCourseMetrics(true, false, false, false, true);
    Assert.assertEquals(ResponseCode.invalidCourseBatchId.getErrorCode(), e.getCode());
  }

  @Test
  public void testCourseProgressMetricsV2WithNoUser() {
    ProjectCommonException e =
        (ProjectCommonException) getResponseOfCourseMetrics(false, false, false, false, true);
    Assert.assertEquals(ResponseCode.invalidUserId.getErrorCode(), e.getCode());
  }

  @Test
  public void testCourseProgressMetricsV2WithUser() {
    Response res = (Response) getResponseOfCourseMetrics(true, true, true, false, false);
    Assert.assertEquals(ResponseCode.OK, res.getResponseCode());
  }

  @Test
  public void testCourseProgressMetricsV2WithCompletedCount() {
    Response res = (Response) getResponseOfCourseMetrics(true, true, true, true, false);
    Assert.assertEquals(1, res.getResult().get(JsonKey.COMPLETED_COUNT));
  }

  @Test
  public void testCourseProgressMetricsWithInvalidBatch() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(userOrgMap);
    when(esService.getDataByIdentifier(EsType.user.getTypeName(), userId))
        .thenReturn(promise.future());
    Promise<Map<String, Object>> promise_null = Futures.promise();
    promise_null.success(null);
    when(esService.getDataByIdentifier(EsType.course.getTypeName(), batchId))
        .thenReturn(promise_null.future());

    Request actorMessage = new Request();
    actorMessage.put(JsonKey.REQUESTED_BY, userId);
    actorMessage.put(JsonKey.BATCH_ID, batchId);
    actorMessage.put(JsonKey.PERIOD, "fromBegining");
    actorMessage.setOperation(ActorOperations.COURSE_PROGRESS_METRICS.getValue());

    subject.tell(actorMessage, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    Assert.assertTrue(null != exc);
  }

  @Test
  public void testCourseProgressMetricsWithInvalidBatchIdNull() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request actorMessage = new Request();
    actorMessage.put(JsonKey.REQUESTED_BY, userId);
    actorMessage.put(JsonKey.BATCH_ID, null);
    actorMessage.put(JsonKey.PERIOD, "fromBegining");
    actorMessage.setOperation(ActorOperations.COURSE_PROGRESS_METRICS.getValue());

    subject.tell(actorMessage, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    Assert.assertTrue(null != exc);
  }

  @Test
  public void testCourseProgressMetricsWithInvalidOperationName() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request actorMessage = new Request();
    actorMessage.put(JsonKey.REQUESTED_BY, userId);
    actorMessage.put(JsonKey.BATCH_ID, null);
    actorMessage.put(JsonKey.PERIOD, "fromBegining");
    actorMessage.setOperation(ActorOperations.COURSE_PROGRESS_METRICS.getValue() + "-Invalid");

    subject.tell(actorMessage, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    Assert.assertTrue(null != exc);
  }

  @Test
  public void testCourseProgressMetricsReportSuccess() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    PowerMockito.mockStatic(CloudStorageUtil.class);
    when(CloudStorageUtil.getAnalyticsSignedUrl(
            Mockito.any(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(SIGNED_URL);
    Request actorMessage = new Request();
    actorMessage.put(JsonKey.REQUESTED_BY, userId);
    actorMessage.put(JsonKey.BATCH_ID, batchId);
    actorMessage.put(JsonKey.PERIOD, "fromBegining");
    actorMessage.put(JsonKey.FORMAT, "csv");
    actorMessage.setOperation(ActorOperations.COURSE_PROGRESS_METRICS_REPORT.getValue());

    subject.tell(actorMessage, probe.getRef());
    Response res = probe.expectMsgClass(duration("10 second"), Response.class);
    Assert.assertEquals(SIGNED_URL, res.get(JsonKey.SIGNED_URL));
  }

  @Test
  public void testCourseProgressMetricsReportWithInvalidBatch() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Promise<Map<String, Object>> promise = Futures.promise();
    Promise<Map<String, Object>> promise_null = Futures.promise();
    promise.success(userOrgMap);

    when(esService.getDataByIdentifier(EsType.user.getTypeName(), userId))
        .thenReturn(promise.future());

    promise_null.success(null);
    when(esService.getDataByIdentifier(EsType.course.getTypeName(), batchId))
        .thenReturn(promise_null.future());

    Request actorMessage = new Request();
    actorMessage.put(JsonKey.REQUESTED_BY, userId);
    actorMessage.put(JsonKey.BATCH_ID, batchId);
    actorMessage.put(JsonKey.PERIOD, "fromBegining");
    actorMessage.put(JsonKey.FORMAT, "csv");
    actorMessage.setOperation(ActorOperations.COURSE_PROGRESS_METRICS_REPORT.getValue());

    subject.tell(actorMessage, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    Assert.assertTrue(null != exc);
  }

  @Test
  public void testCourseProgressMetricsReportWithBatchIdAsNull() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request actorMessage = new Request();
    actorMessage.put(JsonKey.REQUESTED_BY, userId);
    actorMessage.put(JsonKey.BATCH_ID, "");
    actorMessage.put(JsonKey.PERIOD, "fromBegining");
    actorMessage.put(JsonKey.FORMAT, "csv");
    actorMessage.setOperation(ActorOperations.COURSE_PROGRESS_METRICS_REPORT.getValue());

    subject.tell(actorMessage, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    Assert.assertTrue(null != exc);
  }

  @SuppressWarnings({"unchecked", "deprecation"})
  @Test
  public void testCourseConsumptionMetricsSuccess() throws JsonProcessingException {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    mockHttpPostSuccess(
        HTTP_POST,
        new ByteArrayInputStream(
            (mapper.writeValueAsString(courseConsumptionSuccessMap())).getBytes()));

    Request actorMessage = new Request();
    actorMessage.put(JsonKey.COURSE_ID, "mclr309f39");
    actorMessage.put(JsonKey.PERIOD, "7d");
    actorMessage.put(JsonKey.REQUESTED_BY, userId);
    actorMessage.setOperation(ActorOperations.COURSE_CREATION_METRICS.getValue());

    subject.tell(actorMessage, probe.getRef());
    Response res = probe.expectMsgClass(duration("10 second"), Response.class);
    Map<String, Object> data = res.getResult();
    Assert.assertEquals("7d", data.get(JsonKey.PERIOD));
    Assert.assertEquals(
        "mclr309f39", ((Map<String, Object>) data.get("course")).get(JsonKey.COURSE_ID));
    Map<String, Object> series = (Map<String, Object>) data.get(JsonKey.SERIES);
    Assert.assertTrue(series.containsKey("course.consumption.time_spent"));
    Assert.assertTrue(series.containsKey("course.consumption.content.users.count"));
    List<Map<String, Object>> buckets =
        (List<Map<String, Object>>)
            ((Map<String, Object>) series.get("course.consumption.content.users.count"))
                .get("buckets");
    Assert.assertEquals(7, buckets.size());
    Map<String, Object> snapshot = (Map<String, Object>) data.get(JsonKey.SNAPSHOT);
    Assert.assertTrue(snapshot.containsKey("course.consumption.time_spent.count"));
    Assert.assertTrue(snapshot.containsKey("course.consumption.time_per_user"));
    Assert.assertTrue(snapshot.containsKey("course.consumption.users_completed"));
    Assert.assertTrue(snapshot.containsKey("course.consumption.time_spent_completion_count"));
  }

  @SuppressWarnings("deprecation")
  @Test
  public void testCourseConsumptionMetricsWithInvalidUserData() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(null);
    when(esService.getDataByIdentifier(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(promise.future());

    Request actorMessage = new Request();
    actorMessage.put(JsonKey.COURSE_ID, "mclr309f39_INVALID");
    actorMessage.put(JsonKey.PERIOD, "7d");
    actorMessage.put(JsonKey.REQUESTED_BY, userId + "Invalid");
    actorMessage.setOperation(ActorOperations.COURSE_CREATION_METRICS.getValue());

    subject.tell(actorMessage, probe.getRef());
    ProjectCommonException e =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    Assert.assertEquals("UNAUTHORIZED_USER", e.getCode());
  }

  @SuppressWarnings("deprecation")
  @Test
  public void testCourseConsumptionMetricsInvalidPeriod() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request actorMessage = new Request();
    actorMessage.put(JsonKey.COURSE_ID, "mclr309f39");
    actorMessage.put(JsonKey.PERIOD, "10d");
    actorMessage.setOperation(ActorOperations.COURSE_CREATION_METRICS.getValue());

    subject.tell(actorMessage, probe.getRef());
    ProjectCommonException e =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    Assert.assertEquals("INVALID_PERIOD", e.getCode());
  }

  private static void mockHttpPostSuccess(String methodType, InputStream inputStream) {

    if (HttpMethod.POST.name().equalsIgnoreCase(methodType)) {
      HttpClientBuilder httpClientBuilder = PowerMockito.mock(HttpClientBuilder.class);
      CloseableHttpClient client = PowerMockito.mock(CloseableHttpClient.class);
      CloseableHttpResponse httpResponse = Mockito.mock(CloseableHttpResponse.class);
      when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
      when(httpClientBuilder.build()).thenReturn(client);

      try {
        when(client.execute(Mockito.any(HttpPost.class))).thenReturn(httpResponse);
      } catch (IOException e) {
        e.printStackTrace();
      }
      HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
      when(httpResponse.getEntity()).thenReturn(httpEntity);
      StatusLine statusLine = Mockito.mock(StatusLine.class);
      when(httpResponse.getStatusLine()).thenReturn(statusLine);
      when(statusLine.getStatusCode()).thenReturn(200);
      Map<String, Object> responseMap = new HashMap<>();
      Map<String, Object> resultMap = new HashMap<>();
      Map<String, Object> aggregateMap = new HashMap<>();
      Map<String, Object> statusMap = new HashMap<>();
      aggregateMap.put(JsonKey.STATUS, statusMap);
      resultMap.put(JsonKey.AGGREGATIONS, aggregateMap);
      responseMap.put(JsonKey.RESULT, resultMap);
      try {
        when(httpEntity.getContent()).thenReturn(inputStream);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private Map<String, Object> courseConsumptionSuccessMap() {
    Map<String, Object> responseMap = new HashMap<>();
    Map<String, Object> resultMap = new HashMap<>();
    Map<String, Object> aggregateMap = new HashMap<>();
    List<Map<String, Object>> metricsList = new ArrayList<>();
    Map<String, Object> statusMap = new HashMap<>();
    aggregateMap.put(JsonKey.STATUS, statusMap);
    resultMap.put(JsonKey.METRICS, metricsList);
    Map<String, Object> summaryMap = new HashMap<>();
    resultMap.put(JsonKey.SUMMARY, summaryMap);
    responseMap.put(JsonKey.RESULT, resultMap);
    return responseMap;
  }

  private Response createCassandraInsertSuccessResponse() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    return response;
  }

  private void mockESComplexSearch() {
    Map<String, Object> esComplexSearchMap = new HashMap<>();
    esComplexSearchMap.put(JsonKey.USER_ID, "user_id");
    esComplexSearchMap.put(JsonKey.USERNAME, "userName");
    esComplexSearchMap.put(JsonKey.USER_NAME, "user_name");
    esComplexSearchMap.put(JsonKey.ROOT_ORG_ID, "root001");
    esComplexSearchMap.put(JsonKey.ID, "123");
    esComplexSearchMap.put(JsonKey.ORG_NAME, "org123");
    esComplexSearchMap.put(JsonKey.COURSE_ENROLL_DATE, "00-00-0000");
    esComplexSearchMap.put(JsonKey.DATE_TIME, "00-00-0000");

    Map<String, Object> esMap = new HashMap<>();
    List<Map<String, Object>> contentList = new ArrayList<>();
    contentList.add(esComplexSearchMap);
    esMap.put(JsonKey.CONTENT, contentList);

    PowerMockito.mockStatic(HttpClientBuilder.class);
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(esMap);
    when(esService.search(Mockito.any(), Mockito.any())).thenReturn(promise.future());
  }

  private void mockESGetDataByIdentifier() {
    userOrgMap = new HashMap<>();
    userOrgMap.put(JsonKey.ID, orgId);
    userOrgMap.put(JsonKey.IS_ROOT_ORG, true);
    userOrgMap.put(JsonKey.HASHTAGID, orgId);
    userOrgMap.put(JsonKey.ORG_NAME, "rootOrg");
    userOrgMap.put(JsonKey.FIRST_NAME, "user_first_name");
    userOrgMap.put(JsonKey.EMAIL, "user_encrypted email");
    userOrgMap.put(JsonKey.ROOT_ORG_ID, "root123");
    userOrgMap.put(JsonKey.HASHTAGID, "hash123");
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(userOrgMap);
    when(esService.getDataByIdentifier(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(promise.future());
  }

  public Map<String, Object> getBatchData() {
    Map<String, Object> batchData = new HashMap<>();
    batchData.put(JsonKey.START_DATE, "startDate");
    batchData.put(JsonKey.END_DATE, "endDate");
    batchData.put(JsonKey.COMPLETED_COUNT, 1);
    return batchData;
  }

  private Map<String, Object> mockUserData() {
    Map<String, Object> data = new HashMap<>();
    List<Map<String, Object>> users = new ArrayList<>();
    data.put(JsonKey.CONTENT, users);

    return data;
  }

  private Map<String, Object> getCompleteUserCount() {
    Map<String, Object> esMap = new HashMap<>();
    List<Map<String, Object>> dataList = new ArrayList<>();
    Map<String, Object> data = new HashMap<>();
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.BATCH_ID, batchId);
    map.put(JsonKey.PROGRESS, progress);
    list.add(map);
    data.put(JsonKey.BATCHES, list);
    dataList.add(data);
    esMap.put(JsonKey.CONTENT, dataList);
    return esMap;
  }

  public Map<String, Object> getMockUser() {
    Map<String, Object> mockUser = new HashMap<>();
    mockUser.put(JsonKey.FIRST_NAME, "anyName");
    return mockUser;
  }

  private Object getResponseOfCourseMetrics(
      boolean isUserValid,
      boolean isBatchValid,
      boolean isUserMock,
      boolean isGetCompltetedCount,
      boolean errorCode) {
    Request actorMessage = new Request();
    actorMessage.getContext().put(JsonKey.REQUESTED_BY, userId);
    actorMessage.getContext().put(JsonKey.BATCH_ID, batchId);
    actorMessage.getContext().put(JsonKey.LIMIT, limit);
    actorMessage.getContext().put(JsonKey.OFFSET, offset);
    actorMessage.setOperation(ActorOperations.COURSE_PROGRESS_METRICS_V2.getValue());
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    if (!isUserValid) {
      Promise<Map<String, Object>> promise = Futures.promise();
      promise.success(null);
      when(esService.getDataByIdentifier(Mockito.anyString(), Mockito.anyString()))
          .thenReturn(promise.future());
    } else if (isUserValid && !isBatchValid) {
      Promise<Map<String, Object>> promise = Futures.promise();
      Promise<Map<String, Object>> promise_null = Futures.promise();
      promise.success(getMockUser());
      promise_null.success(null);
      when(esService.getDataByIdentifier(Mockito.anyString(), Mockito.anyString()))
          .thenReturn(promise.future())
          .thenReturn(promise_null.future());
    } else {
      Promise<Map<String, Object>> promise = Futures.promise();
      promise.success(getMockUser());
      Promise<Map<String, Object>> promise_ = Futures.promise();
      promise_.success(getBatchData());
      when(esService.getDataByIdentifier(Mockito.anyString(), Mockito.anyString()))
          .thenReturn(promise.future())
          .thenReturn(promise_.future());
    }
    if (isUserMock && !isGetCompltetedCount) {
      Promise<Map<String, Object>> promise = Futures.promise();
      promise.success(mockUserData());
      when(esService.search(Mockito.any(), Mockito.anyString())).thenReturn(promise.future());
    } else {
      Promise<Map<String, Object>> promise = Futures.promise();
      promise.success(mockUserData());
      Promise<Map<String, Object>> promise_ = Futures.promise();
      promise_.success(getCompleteUserCount());
      when(esService.search(Mockito.any(), Mockito.anyString()))
          .thenReturn(promise.future())
          .thenReturn(promise_.future());
    }

    subject.tell(actorMessage, probe.getRef());
    if (!errorCode) {
      Response res = probe.expectMsgClass(duration("10 second"), Response.class);
      return res;
    } else {
      ProjectCommonException projectCommonException =
          probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
      return projectCommonException;
    }
  }
}
