package org.sunbird.learner.actors.coursemanagement;

import static akka.testkit.JavaTestKit.duration;
import static org.powermock.api.mockito.PowerMockito.*;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.actor.router.RequestRouter;
import org.sunbird.actorutil.systemsettings.impl.SystemSettingClientImpl;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.coursebatch.CourseBatchManagementActor;
import org.sunbird.learner.util.CourseBatchUtil;
import org.sunbird.learner.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  EsClientFactory.class,
  RequestRouter.class,
  SystemSettingClientImpl.class,
  CourseBatchUtil.class,
  Util.class
})
@PowerMockIgnore({"javax.management.*"})
public class CourseBatchManagementActorTest {

  public ActorSystem system = ActorSystem.create("system");
  public static final Props props = Props.create(CourseBatchManagementActor.class);
  private static CassandraOperationImpl mockCassandraOperation;
  private static final String BATCH_ID = "123";
  private static final String BATCH_NAME = "Some Batch Name";
  SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

  private String existingStartDate = "";
  private String existingEndDate = "";
  private String existingEnrollmentEndDate = "";

  @Before
  public void setUp() {
    mockCassandraOperation = mock(CassandraOperationImpl.class);
    ActorRef actorRef = mock(ActorRef.class);
    PowerMockito.mockStatic(RequestRouter.class);
    when(RequestRouter.getActor(Mockito.anyString())).thenReturn(actorRef);
    PowerMockito.mockStatic(ServiceFactory.class);
    when(ServiceFactory.getInstance()).thenReturn(mockCassandraOperation);
    PowerMockito.mockStatic(CourseBatchUtil.class);
  }

  private String calculateDate(int dayOffset) {

    Calendar calender = Calendar.getInstance();
    calender.add(Calendar.DAY_OF_MONTH, dayOffset);
    return format.format(calender.getTime());
  }

  private ProjectCommonException performUpdateCourseBatchFailureTest(
      String startDate,
      String enrollmentEndDate,
      String endDate,
      Response mockGetRecordByIdResponse) {
    when(mockCassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(mockGetRecordByIdResponse);
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.UPDATE_BATCH.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.ID, BATCH_ID);
    innerMap.put(JsonKey.NAME, BATCH_NAME);
    innerMap.put(JsonKey.START_DATE, startDate);
    innerMap.put(JsonKey.END_DATE, endDate);
    innerMap.put(JsonKey.ENROLLMENT_END_DATE, enrollmentEndDate);
    reqObj.getRequest().putAll(innerMap);
    subject.tell(reqObj, probe.getRef());

    ProjectCommonException exception =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    return exception;
  }

  private Response performUpdateCourseBatchSuccessTest(
      String startDate,
      String enrollmentEndDate,
      String endDate,
      Response mockGetRecordByIdResponse,
      Response mockUpdateRecordResponse) {

    when(mockCassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(mockGetRecordByIdResponse);

    when(mockCassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.anyMap()))
        .thenReturn(mockUpdateRecordResponse);

    PowerMockito.doNothing().when(CourseBatchUtil.class);
    CourseBatchUtil.syncCourseBatchForeground(BATCH_ID, new HashMap<>());

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.UPDATE_BATCH.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.ID, BATCH_ID);
    innerMap.put(JsonKey.COURSE_ID, "someCourseId");
    innerMap.put(JsonKey.NAME, BATCH_NAME);

    if (startDate != null) {
      innerMap.put(JsonKey.START_DATE, startDate);
    }
    if (endDate != null) {
      innerMap.put(JsonKey.END_DATE, endDate);
    }
    if (enrollmentEndDate != null) {
      innerMap.put(JsonKey.ENROLLMENT_END_DATE, enrollmentEndDate);
    }
    reqObj.getRequest().putAll(innerMap);
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

    courseResponseMap.put(JsonKey.BATCH_ID, BATCH_ID);
    courseResponseMap.put(JsonKey.VER, "v1");
    courseResponseMap.put(JsonKey.NAME, BATCH_NAME);
    courseResponseMap.put(JsonKey.ENROLMENTTYPE, JsonKey.INVITE_ONLY);
    courseResponseMap.put(JsonKey.COURSE_ID, "someCourseId");
    courseResponseMap.put(JsonKey.COURSE_CREATED_FOR, new ArrayList<Object>());
    courseResponseMap.put(JsonKey.STATUS, batchProgressStatus);

    if (batchProgressStatus == ProjectUtil.ProgressStatus.STARTED.getValue()) {

      existingStartDate = calculateDate(-4);
      existingEnrollmentEndDate = calculateDate(1);
      existingEndDate = calculateDate(3);
    } else if (batchProgressStatus == ProjectUtil.ProgressStatus.NOT_STARTED.getValue()) {

      existingStartDate = calculateDate(2);
      existingEnrollmentEndDate = calculateDate(3);
      existingEndDate = calculateDate(4);
    } else {

      existingStartDate = calculateDate(-4);
      existingEnrollmentEndDate = calculateDate(-3);
      existingEndDate = calculateDate(-2);
    }

    courseResponseMap.put(JsonKey.START_DATE, existingStartDate);
    courseResponseMap.put(JsonKey.ENROLLMENT_END_DATE, existingEnrollmentEndDate);
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
    doNothing()
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
            null,
            existingEndDate,
            mockGetRecordByIdResponse,
            mockUpdateRecordResponse);
    Assert.assertTrue(!(telemetryEnvKey.charAt(0) >= 65 && telemetryEnvKey.charAt(0) <= 90));
  }

  @Test
  public void testUpdateEnrollmentEndDateFailureBeforeStartDate() {
    int batchProgressStatus = ProjectUtil.ProgressStatus.NOT_STARTED.getValue();
    Response mockGetRecordByIdResponse = getMockCassandraRecordByIdResponse(batchProgressStatus);
    ProjectCommonException exception =
        performUpdateCourseBatchFailureTest(
            getOffsetDate(existingStartDate, 0),
            getOffsetDate(existingEnrollmentEndDate, -2),
            getOffsetDate(existingEndDate, 0),
            mockGetRecordByIdResponse);

    Assert.assertTrue(
        ((ProjectCommonException) exception)
            .getCode()
            .equals(ResponseCode.enrollmentEndDateStartError.getErrorCode()));
  }

  @Test
  public void testUpdateEnrollmentEndDateFailureAfterEndDate() {
    int batchProgressStatus = ProjectUtil.ProgressStatus.NOT_STARTED.getValue();
    Response mockGetRecordByIdResponse = getMockCassandraRecordByIdResponse(batchProgressStatus);
    ProjectCommonException exception =
        performUpdateCourseBatchFailureTest(
            getOffsetDate(existingStartDate, 0),
            getOffsetDate(existingEnrollmentEndDate, 2),
            getOffsetDate(existingEndDate, 0),
            mockGetRecordByIdResponse);

    Assert.assertTrue(
        ((ProjectCommonException) exception)
            .getCode()
            .equals(ResponseCode.enrollmentEndDateEndError.getErrorCode()));
  }

  @Test
  public void testUpdateStartedCourseBatchFailureWithStartDate() {

    int batchProgressStatus = ProjectUtil.ProgressStatus.STARTED.getValue();
    Response mockGetRecordByIdResponse = getMockCassandraRecordByIdResponse(batchProgressStatus);
    ProjectCommonException exception =
        performUpdateCourseBatchFailureTest(
            getOffsetDate(existingStartDate, 1), null, null, mockGetRecordByIdResponse);
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
            null, getOffsetDate(existingEndDate, 4), null, mockGetRecordByIdResponse);
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
            null,
            getOffsetDate(existingEndDate, 4),
            mockGetRecordByIdResponse);
    Assert.assertTrue(
        ((ProjectCommonException) exception)
            .getCode()
            .equals(ResponseCode.invalidBatchStartDateError.getErrorCode()));
  }

  @Test
  public void testUpdateStartedCourseBatchSuccessWithFutureEndDate() {

    int batchProgressStatus = ProjectUtil.ProgressStatus.STARTED.getValue();
    Response mockGetRecordByIdResponse = getMockCassandraRecordByIdResponse(batchProgressStatus);
    Response mockUpdateRecordResponse = getMockCassandraResult();
    Response response =
        performUpdateCourseBatchSuccessTest(
            existingStartDate,
            null,
            getOffsetDate(existingEndDate, 2),
            mockGetRecordByIdResponse,
            mockUpdateRecordResponse);
    Assert.assertTrue(null != response && response.getResponseCode() == ResponseCode.OK);
  }

  @Test
  public void testUpdateStartedCourseBatchSuccessWithEnrollmentEndEndDate() {

    int batchProgressStatus = ProjectUtil.ProgressStatus.NOT_STARTED.getValue();
    Response mockGetRecordByIdResponse = getMockCassandraRecordByIdResponse(batchProgressStatus);
    Response mockUpdateRecordResponse = getMockCassandraResult();
    Response response =
        performUpdateCourseBatchSuccessTest(
            getOffsetDate(existingStartDate, 0),
            getOffsetDate(existingEnrollmentEndDate, 1),
            getOffsetDate(existingEndDate, 0),
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
        performUpdateCourseBatchFailureTest(
            null, null, calculateDate(4), mockGetRecordByIdResponse);
    Assert.assertTrue(
        ((ProjectCommonException) exception)
            .getCode()
            .equals(ResponseCode.courseBatchStartDateRequired.getErrorCode()));
  }

  @Test
  public void testUpdateNotStartedCourseBatchSuccessWithFutureStartDateAndEndDate() {

    int batchProgressStatus = ProjectUtil.ProgressStatus.NOT_STARTED.getValue();
    Response mockGetRecordByIdResponse = getMockCassandraRecordByIdResponse(batchProgressStatus);
    Response mockUpdateRecordResponse = getMockCassandraResult();
    Response response =
        performUpdateCourseBatchSuccessTest(
            getOffsetDate(existingStartDate, 2),
            null,
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
        performUpdateCourseBatchFailureTest(
            calculateDate(-4), null, null, mockGetRecordByIdResponse);
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
        performUpdateCourseBatchFailureTest(
            null, null, calculateDate(-2), mockGetRecordByIdResponse);
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
            null,
            getOffsetDate(existingEndDate, 2),
            mockGetRecordByIdResponse);
    Assert.assertTrue(
        ((ProjectCommonException) exception)
            .getCode()
            .equals(ResponseCode.invalidBatchEndDateError.getErrorCode()));
  }

  @Test
  public void testUpdateCompletedCourseBatchFailureWithStartDate() {

    int batchProgressStatus = ProjectUtil.ProgressStatus.COMPLETED.getValue();
    Response mockGetRecordByIdResponse = getMockCassandraRecordByIdResponse(batchProgressStatus);
    ProjectCommonException exception =
        performUpdateCourseBatchFailureTest(
            getOffsetDate(existingStartDate, 2), null, null, mockGetRecordByIdResponse);
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
            null, null, getOffsetDate(existingEndDate, 4), mockGetRecordByIdResponse);
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
            null,
            getOffsetDate(existingEndDate, 4),
            mockGetRecordByIdResponse);
    Assert.assertTrue(
        ((ProjectCommonException) exception)
            .getCode()
            .equals(ResponseCode.invalidBatchStartDateError.getErrorCode()));
  }
}
