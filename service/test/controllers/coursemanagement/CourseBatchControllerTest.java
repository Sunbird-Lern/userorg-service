package controllers.coursemanagement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import controllers.BaseControllerTest;
import java.text.SimpleDateFormat;
import java.util.*;
import org.junit.Test;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;
import play.mvc.Result;

public class CourseBatchControllerTest extends BaseControllerTest {

  public static String COURSE_ID = "courseId";
  public static String COURSE_NAME = "courseName";
  public static int DAY_OF_MONTH = 2;
  public static String INVALID_ENROLLMENT_TYPE = "invalid";
  public static String BATCH_ID = "batchID";
  public static List<String> MENTORS = Arrays.asList("mentors");
  public static String INVALID_MENTORS_TYPE = "invalidMentorType";
  public static List<String> PARTICIPANTS = Arrays.asList("participants");

  @Test
  public void testCreateBatchSuccess() {
    Result result =
        performTest(
            "/v1/course/batch/create",
            "POST",
            createAndUpdateCourseBatchRequest(
                COURSE_ID,
                COURSE_NAME,
                JsonKey.INVITE_ONLY,
                new Date(),
                getEndDate(true),
                null,
                null));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testCreateBatchSuccessWithValidMentors() {
    Result result =
        performTest(
            "/v1/course/batch/create",
            "POST",
            createAndUpdateCourseBatchRequest(
                COURSE_ID,
                COURSE_NAME,
                JsonKey.INVITE_ONLY,
                new Date(),
                getEndDate(true),
                MENTORS,
                null));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testCreateBatchSuccessWithValidMentorsAndParticipants() {
    Result result =
        performTest(
            "/v1/course/batch/create",
            "POST",
            createAndUpdateCourseBatchRequest(
                COURSE_ID,
                COURSE_NAME,
                JsonKey.INVITE_ONLY,
                new Date(),
                getEndDate(true),
                MENTORS,
                PARTICIPANTS));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testCreateBatchSuccessWithoutEndDate() {
    Result result =
        performTest(
            "/v1/course/batch/create",
            "POST",
            createAndUpdateCourseBatchRequest(
                COURSE_ID, COURSE_NAME, JsonKey.INVITE_ONLY, new Date(), null, null, null));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testCreateBatchFailureWithInvalidEnrollmentType() {
    Result result =
        performTest(
            "/v1/course/batch/create",
            "POST",
            createAndUpdateCourseBatchRequest(
                COURSE_ID,
                COURSE_NAME,
                INVALID_ENROLLMENT_TYPE,
                new Date(),
                getEndDate(true),
                null,
                null));
    assertEquals(getResponseCode(result), ResponseCode.invalidParameterValue.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testCreateBatchFailureWithInvalidMentorType() {

    Result result =
        performTest(
            "/v1/course/batch/create",
            "POST",
            createAndUpdateCourseBatchRequest(
                COURSE_ID,
                COURSE_NAME,
                INVALID_ENROLLMENT_TYPE,
                new Date(),
                getEndDate(true),
                INVALID_MENTORS_TYPE,
                null));
    assertEquals(getResponseCode(result), ResponseCode.invalidParameterValue.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testCreateBatchFailureWithEndDateBeforeStartDate() {

    Result result =
        performTest(
            "/v1/course/batch/create",
            "POST",
            createAndUpdateCourseBatchRequest(
                COURSE_ID,
                COURSE_NAME,
                INVALID_ENROLLMENT_TYPE,
                new Date(),
                getEndDate(false),
                null,
                null));
    assertEquals(getResponseCode(result), ResponseCode.invalidParameterValue.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testCreateBatchFailureWithSameStartAndEndDate() {
    Date currentdate = new Date();
    Result result =
        performTest(
            "/v1/course/batch/create",
            "POST",
            createAndUpdateCourseBatchRequest(
                COURSE_ID,
                COURSE_NAME,
                INVALID_ENROLLMENT_TYPE,
                currentdate,
                currentdate,
                null,
                null));
    assertEquals(getResponseCode(result), ResponseCode.invalidParameterValue.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testUpdateBatchSuccessWithoutEndDate() {
    Result result =
        performTest(
            "/v1/course/batch/update",
            "PATCH",
            createAndUpdateCourseBatchRequest(
                COURSE_ID, COURSE_NAME, JsonKey.INVITE_ONLY, new Date(), null, null, null));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testUpdateBatchFailureWithEndDateBeforeStartDate() {
    Result result =
        performTest(
            "/v1/course/batch/update",
            "PATCH",
            createAndUpdateCourseBatchRequest(
                COURSE_ID,
                COURSE_NAME,
                INVALID_ENROLLMENT_TYPE,
                new Date(),
                getEndDate(false),
                null,
                null));
    assertEquals(getResponseCode(result), ResponseCode.invalidParameterValue.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testUpdateBatchSuccess() {
    Result result =
        performTest(
            "/v1/course/batch/update",
            "PATCH",
            createAndUpdateCourseBatchRequest(
                COURSE_ID,
                COURSE_NAME,
                JsonKey.INVITE_ONLY,
                new Date(),
                getEndDate(true),
                null,
                null));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testUpdateBatchFailureWithSameStartAndEndDate() {
    Date currentDate = new Date();
    Result result =
        performTest(
            "/v1/course/batch/update",
            "PATCH",
            createAndUpdateCourseBatchRequest(
                COURSE_ID,
                COURSE_NAME,
                INVALID_ENROLLMENT_TYPE,
                currentDate,
                currentDate,
                null,
                null));
    assertEquals(getResponseCode(result), ResponseCode.invalidParameterValue.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testGetBatchSuccess() {
    Result result = performTest("/v1/course/batch/read/" + BATCH_ID, "GET", null);
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testSearchBatchSuccess() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.BATCH_ID, BATCH_ID);
    innerMap.put(JsonKey.FILTERS, filters);
    requestMap.put(JsonKey.REQUEST, innerMap);
    Result result = performTest("/v1/course/batch/search", "POST", requestMap);
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testSearchBatchSuccessWithoutFilters() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    requestMap.put(JsonKey.REQUEST, innerMap);
    Result result = performTest("/v1/course/batch/search", "POST", requestMap);
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testSearchBatchSuccessWithEmptyFilters() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.FILTERS, null);
    requestMap.put(JsonKey.REQUEST, innerMap);
    Result result = performTest("/v1/course/batch/search", "POST", requestMap);
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testAddUserToBatchSuccess() {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    List<String> users = new ArrayList();
    users.add("123");
    innerMap.put(JsonKey.USER_IDs, users);
    requestMap.put(JsonKey.REQUEST, innerMap);

    Result result = performTest("/v1/course/batch/users/add/" + BATCH_ID, "POST", requestMap);
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testAddUserToBatchFailureWithoutUserIds() {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    requestMap.put(JsonKey.REQUEST, innerMap);
    Result result = performTest("/v1/course/batch/users/add/" + BATCH_ID, "POST", requestMap);
    assertEquals(getResponseCode(result), ResponseCode.userIdRequired.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  private Map<String, Object> createAndUpdateCourseBatchRequest(
      String courseId,
      String name,
      String enrollmentType,
      Date startDate,
      Date endDate,
      Object mentors,
      Object participants) {
    Map<String, Object> innerMap = new HashMap<>();
    if (courseId != null) innerMap.put(JsonKey.COURSE_ID, courseId);
    if (name != null) innerMap.put(JsonKey.NAME, name);
    if (enrollmentType != null) innerMap.put(JsonKey.ENROLLMENT_TYPE, enrollmentType);
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    if (startDate != null) innerMap.put(JsonKey.START_DATE, format.format(startDate));
    if (endDate != null) {
      innerMap.put(JsonKey.END_DATE, format.format(endDate));
    }
    if (participants != null) innerMap.put(JsonKey.PARTICIPANTS, participants);
    if (mentors != null) innerMap.put(JsonKey.MENTORS, mentors);
    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(JsonKey.REQUEST, innerMap);
    return requestMap;
  }

  private Date getEndDate(boolean isFuture) {
    Calendar calendar = Calendar.getInstance();
    if (isFuture) {
      calendar.add(Calendar.DAY_OF_MONTH, DAY_OF_MONTH);
    } else {
      calendar.add(Calendar.DAY_OF_MONTH, -DAY_OF_MONTH);
    }
    return calendar.getTime();
  }
}
