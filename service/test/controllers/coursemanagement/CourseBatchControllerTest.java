package controllers.coursemanagement;

import static org.junit.Assert.assertEquals;

import controllers.BaseControllerTest;
import java.text.SimpleDateFormat;
import java.util.*;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.models.util.JsonKey;
import play.mvc.Result;
import util.RequestInterceptor;

/** Created by arvind on 1/12/17. */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(PowerMockRunner.class)
@PrepareForTest(RequestInterceptor.class)
@PowerMockIgnore("javax.management.*")
public class CourseBatchControllerTest extends BaseControllerTest {
  public static String COURSE_ID = "courseId";
  public static String COURSE_NAME = "courseName";
  public static int DAY_OF_MONTH = 2;
  public static String INVALID_ENROLLMENT_TYPE = "invalid";
  public static String BATCH_ID = "batchID";
  public static List<String> MENTORS = Arrays.asList("mentors");
  public static String INVALID_MENTORS_TYPE = "invalidMentorType";
  public static String INVALID_PARTICIPANTS_TYPE = "invalidParticipantsType";
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
    assertEquals(200, result.status());
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
    assertEquals(200, result.status());
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
    assertEquals(200, result.status());
  }

  @Test
  public void testCreateBatchSuccessWithoutEndDate() {
    Result result =
        performTest(
            "/v1/course/batch/create",
            "POST",
            createAndUpdateCourseBatchRequest(
                COURSE_ID, COURSE_NAME, JsonKey.INVITE_ONLY, new Date(), null, null, null));
    assertEquals(200, result.status());
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
    assertEquals(400, result.status());
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
    assertEquals(400, result.status());
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
    assertEquals(400, result.status());
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
    assertEquals(400, result.status());
  }

  @Test
  public void testUpdateBatchSuccessWithoutEndDate() {
    Result result =
        performTest(
            "/v1/course/batch/update",
            "PATCH",
            createAndUpdateCourseBatchRequest(
                COURSE_ID, COURSE_NAME, JsonKey.INVITE_ONLY, new Date(), null, null, null));
    assertEquals(200, result.status());
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
    assertEquals(400, result.status());
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
    assertEquals(200, result.status());
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
    assertEquals(400, result.status());
  }

  @Test
  public void testGetBatchSuccess() {
    Result result = performTest("/v1/course/batch/read/" + BATCH_ID, "GET", null);
    assertEquals(200, result.status());
  }

  @Test
  public void testSearchBatchSuccess() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.FILTERS, BATCH_ID);
    requestMap.put(JsonKey.REQUEST, innerMap);
    Result result = performTest("/v1/course/batch/search", "POST", requestMap);
    assertEquals(200, result.status());
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
