package controllers.courseenrollment;

import static controllers.TestUtil.mapToJson;
import static org.junit.Assert.assertEquals;
import static play.test.Helpers.route;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseControllerTest;
import java.util.HashMap;
import java.util.Map;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.models.util.JsonKey;
import play.libs.Json;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import util.RequestInterceptor;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(PowerMockRunner.class)
@PrepareForTest(RequestInterceptor.class)
@PowerMockIgnore("javax.management.*")
@Ignore
public class CourseEnrollmentControllerTest extends BaseControllerTest {
  public static final String UNENROLL_URI = "/v1/course/unenroll";
  public static final String ENROLL_URI = "/v1/course/enrol";

  public static String COURSE_ID = "courseId";
  public static String BATCH_ID = "batchId";
  public static String USER_ID = "userId";

  @Test
  public void testEnrollCourseBatchSuccess() {
    JsonNode json = createCourseEnrollmentRequest(COURSE_ID, BATCH_ID, USER_ID);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri(ENROLL_URI).method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
  }

  @Test
  public void testUnenrollCourseBatchSuccess() {
    JsonNode json = createCourseEnrollmentRequest(COURSE_ID, BATCH_ID, USER_ID);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri(UNENROLL_URI).method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
  }

  @Test
  public void testEnrollCourseBatchFailureWithoutCourseId() {
    JsonNode json = createCourseEnrollmentRequest(null, BATCH_ID, USER_ID);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri(ENROLL_URI).method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(400, result.status());
  }

  @Test
  public void testUnenrollCourseBatchFailureWithoutCourseId() {
    JsonNode json = createCourseEnrollmentRequest(null, BATCH_ID, USER_ID);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri(UNENROLL_URI).method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(400, result.status());
  }

  @Test
  public void testUnenrollCourseBatchFailureWithoutBatchId() {
    JsonNode json = createCourseEnrollmentRequest(COURSE_ID, null, USER_ID);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri(UNENROLL_URI).method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(400, result.status());
  }

  @Test
  public void testUnenrollCourseBatchFailureWithoutUserId() {
    JsonNode json = createCourseEnrollmentRequest(COURSE_ID, BATCH_ID, null);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri(UNENROLL_URI).method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(400, result.status());
  }

  private JsonNode createCourseEnrollmentRequest(
      String courseId, String batchId, String userId) {
    Map<String, Object> innerMap = new HashMap<>();
    if (courseId != null) innerMap.put(JsonKey.COURSE_ID, courseId);
    if (batchId != null) innerMap.put(JsonKey.BATCH_ID, batchId);
    if (userId != null) innerMap.put(JsonKey.USER_ID, userId);
    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);
    return Json.parse(data);
  }
}
