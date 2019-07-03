package org.sunbird.learner.actors.coursemanagement;

import static akka.testkit.JavaTestKit.duration;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
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
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.coursebatch.CourseBatchManagementActor;
import org.sunbird.learner.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ServiceFactory.class, Util.class})
@PowerMockIgnore("javax.management.*")
@Ignore
public class CourseBatchManagementActorTest {

  private TestKit probe;
  private ActorRef subject;

  private static CassandraOperationImpl mockCassandraOperation;
  private static final String BATCH_ID = "123";
  private static final String BATCH_NAME = "Some Batch Name";
  SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

  private String existingStartDate = "";
  private String existingEndDate = "";

  @Before
  public void setUp() {
    mockCassandraOperation = mock(CassandraOperationImpl.class);

    ActorSystem system = ActorSystem.create("system");
    probe = new TestKit(system);

    Props props = Props.create(CourseBatchManagementActor.class, mockCassandraOperation);
    subject = system.actorOf(props);

    PowerMockito.mockStatic(ServiceFactory.class);
    when(ServiceFactory.getInstance()).thenReturn(mockCassandraOperation);
  }

  private String calculateDate(int dayOffset) {

    Calendar calender = Calendar.getInstance();
    calender.add(Calendar.DAY_OF_MONTH, dayOffset);
    return format.format(calender.getTime());
  }

  private ProjectCommonException performUpdateCourseBatchFailureTest(
      String startDate, String endDate, Response mockGetRecordByIdResponse) {
    when(mockCassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(mockGetRecordByIdResponse);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.UPDATE_BATCH.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.ID, BATCH_ID);
    innerMap.put(JsonKey.NAME, BATCH_NAME);
    innerMap.put(JsonKey.START_DATE, startDate);
    innerMap.put(JsonKey.END_DATE, endDate);
    reqObj.getRequest().put(JsonKey.BATCH, innerMap);

    subject.tell(reqObj, probe.getRef());

    ProjectCommonException exception =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    return exception;
  }

  private Response performUpdateCourseBatchSuccessTest(
      String startDate,
      String endDate,
      Response mockGetRecordByIdResponse,
      Response mockUpdateRecordResponse) {

    when(mockCassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(mockGetRecordByIdResponse);

    when(mockCassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(mockUpdateRecordResponse);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.UPDATE_BATCH.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.ID, BATCH_ID);
    innerMap.put(JsonKey.NAME, BATCH_NAME);

    if (startDate != null) {
      innerMap.put(JsonKey.START_DATE, startDate);
    }
    if (endDate != null) {
      innerMap.put(JsonKey.END_DATE, endDate);
    }
    reqObj.getRequest().put(JsonKey.BATCH, innerMap);

    subject.tell(reqObj, probe.getRef());

    Response response = probe.expectMsgClass(duration("10 second"), Response.class);
    return response;
  }

  private Response getMockCassandraResult() {

    Response response = new Response();
    response.put("response", "SUCCESS");
    return response;
  }

  private Response getMockCassandraRecordByIdResponse(int batchProgressStatus) {

    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> courseResponseMap = new HashMap<>();

    courseResponseMap.put(JsonKey.ID, BATCH_ID);
    courseResponseMap.put(JsonKey.VER, "v1");
    courseResponseMap.put(JsonKey.NAME, BATCH_NAME);
    courseResponseMap.put(JsonKey.COUNTER_INCREMENT_STATUS, Boolean.FALSE);
    courseResponseMap.put(JsonKey.ENROLMENTTYPE, JsonKey.INVITE_ONLY);
    courseResponseMap.put(JsonKey.COURSE_ID, "someCourseId");
    courseResponseMap.put(JsonKey.COURSE_CREATED_FOR, new ArrayList<Object>());
    courseResponseMap.put(JsonKey.STATUS, batchProgressStatus);

    if (batchProgressStatus == ProjectUtil.ProgressStatus.STARTED.getValue()) {

      existingStartDate = calculateDate(-4);
      existingEndDate = calculateDate(2);
    } else if (batchProgressStatus == ProjectUtil.ProgressStatus.NOT_STARTED.getValue()) {

      existingStartDate = calculateDate(2);
      existingEndDate = calculateDate(4);
    } else {

      existingStartDate = calculateDate(-4);
      existingEndDate = calculateDate(-2);
    }

    courseResponseMap.put(JsonKey.START_DATE, existingStartDate);
    courseResponseMap.put(JsonKey.END_DATE, existingEndDate);

    list.add(courseResponseMap);
    response.put(JsonKey.RESPONSE, list);

    return response;
  }

  private String getOffsetDate(String date, int offSet) {

    try {
      Calendar calender = Calendar.getInstance();
      calender.setTime(format.parse(date));
      calender.add(Calendar.DATE, offSet);
      return format.format(calender.getTime());
    } catch (ParseException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Test
  public void checkTelemetryKeyFailure() throws Exception {

    String telemetryEnvKey = "batch";
    PowerMockito.mockStatic(Util.class);
    PowerMockito.doNothing()
        .when(
            Util.class,
            "initializeContext",
            Mockito.any(Request.class),
            Mockito.eq(telemetryEnvKey));

    int batchProgressStatus = ProjectUtil.ProgressStatus.STARTED.getValue();
    Response mockGetRecordByIdResponse = getMockCassandraRecordByIdResponse(batchProgressStatus);
    Response mockUpdateRecordResponse = getMockCassandraResult();
    Response response =
        performUpdateCourseBatchSuccessTest(
            existingStartDate,
            existingEndDate,
            mockGetRecordByIdResponse,
            mockUpdateRecordResponse);
    Assert.assertTrue(!(telemetryEnvKey.charAt(0) >= 65 && telemetryEnvKey.charAt(0) <= 90));
  }

  @Test
  public void testUpdateStartedCourseBatchFailureWithStartDate() {

    int batchProgressStatus = ProjectUtil.ProgressStatus.STARTED.getValue();
    Response mockGetRecordByIdResponse = getMockCassandraRecordByIdResponse(batchProgressStatus);
    ProjectCommonException exception =
        performUpdateCourseBatchFailureTest(
            getOffsetDate(existingStartDate, 1), null, mockGetRecordByIdResponse);
    Assert.assertTrue(
        ((ProjectCommonException) exception)
            .getCode()
            .equals(ResponseCode.invalidBatchStartDateError.getErrorCode()));
  }

  @Test
  public void testUpdateStartedCourseBatchFailureWithEndDate() {

    int batchProgressStatus = ProjectUtil.ProgressStatus.STARTED.getValue();
    Response mockGetRecordByIdResponse = getMockCassandraRecordByIdResponse(batchProgressStatus);
    ProjectCommonException exception =
        performUpdateCourseBatchFailureTest(
            null, getOffsetDate(existingEndDate, 4), mockGetRecordByIdResponse);
    Assert.assertTrue(
        ((ProjectCommonException) exception)
            .getCode()
            .equals(ResponseCode.courseBatchStartDateRequired.getErrorCode()));
  }

  @Test
  public void testUpdateStartedCourseBatchFailureWithDifferentStartDateAndEndDate() {

    int batchProgressStatus = ProjectUtil.ProgressStatus.STARTED.getValue();
    Response mockGetRecordByIdResponse = getMockCassandraRecordByIdResponse(batchProgressStatus);
    ProjectCommonException exception =
        performUpdateCourseBatchFailureTest(
            getOffsetDate(existingStartDate, 2),
            getOffsetDate(existingEndDate, 4),
            mockGetRecordByIdResponse);
    Assert.assertTrue(
        ((ProjectCommonException) exception)
            .getCode()
            .equals(ResponseCode.invalidBatchStartDateError.getErrorCode()));
  }

  @Test
  public void testUpdateStartedCourseBatchSuccessWithSameStartDateAndEndDate() {

    int batchProgressStatus = ProjectUtil.ProgressStatus.STARTED.getValue();
    Response mockGetRecordByIdResponse = getMockCassandraRecordByIdResponse(batchProgressStatus);
    Response mockUpdateRecordResponse = getMockCassandraResult();
    Response response =
        performUpdateCourseBatchSuccessTest(
            existingStartDate,
            existingEndDate,
            mockGetRecordByIdResponse,
            mockUpdateRecordResponse);
    Assert.assertTrue(null != response && response.getResponseCode() == ResponseCode.OK);
  }

  @Test
  public void testUpdateNotStartedCourseBatchSuccessWithFutureStartDate() {

    int batchProgressStatus = ProjectUtil.ProgressStatus.NOT_STARTED.getValue();
    Response mockGetRecordByIdResponse = getMockCassandraRecordByIdResponse(batchProgressStatus);
    Response mockUpdateRecordResponse = getMockCassandraResult();
    Response response =
        performUpdateCourseBatchSuccessTest(
            getOffsetDate(existingStartDate, 2),
            null,
            mockGetRecordByIdResponse,
            mockUpdateRecordResponse);
    Assert.assertTrue(null != response && response.getResponseCode() == ResponseCode.OK);
  }

  @Test
  public void testUpdateNotStartedCourseBatchFailureWithFutureEndDate() {

    int batchProgressStatus = ProjectUtil.ProgressStatus.NOT_STARTED.getValue();
    Response mockGetRecordByIdResponse = getMockCassandraRecordByIdResponse(batchProgressStatus);
    ProjectCommonException exception =
        performUpdateCourseBatchFailureTest(null, calculateDate(4), mockGetRecordByIdResponse);
    Assert.assertTrue(
        ((ProjectCommonException) exception)
            .getCode()
            .equals(ResponseCode.courseBatchStartDateRequired.getErrorCode()));
  }

  public void testUpdateNotStartedCourseBatchSuccessWithFutureStartDateAndEndDate() {

    int batchProgressStatus = ProjectUtil.ProgressStatus.NOT_STARTED.getValue();
    Response mockGetRecordByIdResponse = getMockCassandraRecordByIdResponse(batchProgressStatus);
    Response mockUpdateRecordResponse = getMockCassandraResult();
    Response response =
        performUpdateCourseBatchSuccessTest(
            getOffsetDate(existingStartDate, 2),
            getOffsetDate(existingEndDate, 4),
            mockGetRecordByIdResponse,
            mockUpdateRecordResponse);
    Assert.assertTrue(null != response && response.getResponseCode() == ResponseCode.OK);
  }

  @Test
  public void testUpdateNotStartedCourseBatchFailureWithPastStartDate() {

    int batchProgressStatus = ProjectUtil.ProgressStatus.NOT_STARTED.getValue();
    Response mockGetRecordByIdResponse = getMockCassandraRecordByIdResponse(batchProgressStatus);

    ProjectCommonException exception =
        performUpdateCourseBatchFailureTest(calculateDate(-4), null, mockGetRecordByIdResponse);
    Assert.assertTrue(
        ((ProjectCommonException) exception)
            .getCode()
            .equals(ResponseCode.invalidBatchStartDateError.getErrorCode()));
  }

  @Test
  public void testUpdateNotStartedCourseBatchFailureWithPastEndDate() {

    int batchProgressStatus = ProjectUtil.ProgressStatus.NOT_STARTED.getValue();
    Response mockGetRecordByIdResponse = getMockCassandraRecordByIdResponse(batchProgressStatus);
    ProjectCommonException exception =
        performUpdateCourseBatchFailureTest(null, calculateDate(-2), mockGetRecordByIdResponse);
    Assert.assertTrue(
        ((ProjectCommonException) exception)
            .getCode()
            .equals(ResponseCode.courseBatchStartDateRequired.getErrorCode()));
  }

  @Test
  public void testUpdateNotStartedCourseBatchFailureWithEndDateBeforeFutureStartDate() {

    int batchProgressStatus = ProjectUtil.ProgressStatus.NOT_STARTED.getValue();
    Response mockGetRecordByIdResponse = getMockCassandraRecordByIdResponse(batchProgressStatus);
    ProjectCommonException exception =
        performUpdateCourseBatchFailureTest(
            getOffsetDate(existingStartDate, 6),
            getOffsetDate(existingEndDate, 2),
            mockGetRecordByIdResponse);
    Assert.assertTrue(
        ((ProjectCommonException) exception)
            .getCode()
            .equals(ResponseCode.invalidBatchStartDateError.getErrorCode()));
  }

  @Test
  public void testUpdateCompletedCourseBatchFailureWithStartDate() {

    int batchProgressStatus = ProjectUtil.ProgressStatus.COMPLETED.getValue();
    Response mockGetRecordByIdResponse = getMockCassandraRecordByIdResponse(batchProgressStatus);
    ProjectCommonException exception =
        performUpdateCourseBatchFailureTest(
            getOffsetDate(existingStartDate, 2), null, mockGetRecordByIdResponse);
    Assert.assertTrue(
        ((ProjectCommonException) exception)
            .getCode()
            .equals(ResponseCode.invalidBatchStartDateError.getErrorCode()));
  }

  @Test
  public void testUpdateCompletedCourseBatchFailureWithEndDate() {

    int batchProgressStatus = ProjectUtil.ProgressStatus.COMPLETED.getValue();
    Response mockGetRecordByIdResponse = getMockCassandraRecordByIdResponse(batchProgressStatus);
    ProjectCommonException exception =
        performUpdateCourseBatchFailureTest(
            null, getOffsetDate(existingEndDate, 4), mockGetRecordByIdResponse);
    Assert.assertTrue(
        ((ProjectCommonException) exception)
            .getCode()
            .equals(ResponseCode.courseBatchStartDateRequired.getErrorCode()));
  }

  @Test
  public void testUpdateCompletedCourseBatchFailureWithStartDateAndEndDate() {

    int batchProgressStatus = ProjectUtil.ProgressStatus.COMPLETED.getValue();
    Response mockGetRecordByIdResponse = getMockCassandraRecordByIdResponse(batchProgressStatus);
    ProjectCommonException exception =
        performUpdateCourseBatchFailureTest(
            getOffsetDate(existingStartDate, 2),
            getOffsetDate(existingEndDate, 4),
            mockGetRecordByIdResponse);
    Assert.assertTrue(
        ((ProjectCommonException) exception)
            .getCode()
            .equals(ResponseCode.invalidBatchStartDateError.getErrorCode()));
  }
}
