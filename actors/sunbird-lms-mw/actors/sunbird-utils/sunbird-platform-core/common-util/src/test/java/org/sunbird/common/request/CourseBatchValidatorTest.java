/** */
package org.sunbird.common.request;

import static org.junit.Assert.assertEquals;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;

/** @author Manzarul */
public class CourseBatchValidatorTest {

  private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

  @Test
  public void validateCreateBatchSuccess() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.STATUS, 1);
    requestObj.put(JsonKey.COURSE_ID, "do_123233434");
    requestObj.put(JsonKey.NAME, "TestCourse");
    requestObj.put(JsonKey.ENROLLMENT_TYPE, "Open");
    requestObj.put(JsonKey.START_DATE, format.format(new Date()));
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_MONTH, 2);
    requestObj.put(JsonKey.END_DATE, format.format(cal.getTime()));
    List<String> userIds = new ArrayList<String>();
    userIds.add("test123345");
    request.put(JsonKey.COURSE_CREATED_FOR, userIds);
    request.setRequest(requestObj);
    try {
      RequestValidator.validateCreateBatchReq(request);
      response = true;
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals(true, response);
  }

  @Test
  public void validateUpdateCourseBatch() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.COURSE_ID, "do_123233434");
    requestObj.put(JsonKey.NAME, "TestCourse");
    requestObj.put(JsonKey.ENROLLMENT_TYPE, "Open");
    requestObj.put(JsonKey.START_DATE, format.format(new Date()));
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_MONTH, 2);
    requestObj.put(JsonKey.END_DATE, format.format(cal.getTime()));
    List<String> userIds = new ArrayList<String>();
    userIds.add("test123345");
    request.put(JsonKey.COURSE_CREATED_FOR, userIds);
    request.setRequest(requestObj);
    try {
      RequestValidator.validateUpdateCourseBatchReq(request);
      response = true;
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals(true, response);
  }

  @Test
  public void validateCreateBatchWithOutCourseId() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.COURSE_ID, "");
    requestObj.put(JsonKey.NAME, "TestCourse");
    requestObj.put(JsonKey.ENROLLMENT_TYPE, "Open");
    requestObj.put(JsonKey.START_DATE, format.format(new Date()));
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_MONTH, 2);
    requestObj.put(JsonKey.END_DATE, format.format(cal.getTime()));
    List<String> userIds = new ArrayList<String>();
    userIds.add("test123345");
    request.put(JsonKey.COURSE_CREATED_FOR, userIds);
    request.setRequest(requestObj);
    try {
      RequestValidator.validateCreateBatchReq(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.invalidCourseId.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void validateCreateBatchWithOutName() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.COURSE_ID, "do_123233434");
    requestObj.put(JsonKey.ENROLLMENT_TYPE, "Open");
    requestObj.put(JsonKey.NAME, "TestCourse");
    requestObj.put(JsonKey.START_DATE, format.format(new Date()));
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_MONTH, 2);
    requestObj.put(JsonKey.END_DATE, format.format(cal.getTime()));
    List<String> userIds = new ArrayList<String>();
    userIds.add("test123345");
    request.put(JsonKey.COURSE_CREATED_FOR, userIds);
    request.setRequest(requestObj);
    try {
      RequestValidator.validateCreateBatchReq(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.courseNameRequired.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void validateCreateBatchWithOutStartDate() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.COURSE_ID, "do_123233434");
    requestObj.put(JsonKey.ENROLLMENT_TYPE, "Open");
    requestObj.put(JsonKey.NAME, "TestCourse");
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_MONTH, 2);
    requestObj.put(JsonKey.END_DATE, format.format(cal.getTime()));
    List<String> userIds = new ArrayList<String>();
    userIds.add("test123345");
    request.put(JsonKey.COURSE_CREATED_FOR, userIds);
    request.setRequest(requestObj);
    try {
      RequestValidator.validateCreateBatchReq(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.courseBatchStartDateRequired.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void validateCreateBatchWithPastStartDate() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.COURSE_ID, "do_123233434");
    requestObj.put(JsonKey.ENROLLMENT_TYPE, "Open");
    requestObj.put(JsonKey.START_DATE, "2017-01-05");
    requestObj.put(JsonKey.NAME, "TestCourse");
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_MONTH, 2);
    requestObj.put(JsonKey.END_DATE, format.format(cal.getTime()));
    List<String> userIds = new ArrayList<String>();
    userIds.add("test123345");
    request.put(JsonKey.COURSE_CREATED_FOR, userIds);
    request.setRequest(requestObj);
    try {
      RequestValidator.validateCreateBatchReq(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.courseBatchStartDateError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void validateCreateBatchWithInvalidStartDateFormat() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.COURSE_ID, "do_123233434");
    requestObj.put(JsonKey.ENROLLMENT_TYPE, "Open");
    requestObj.put(JsonKey.START_DATE, format.format(new Date()) + " 23:58:59");
    requestObj.put(JsonKey.NAME, "TestCourse");
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_MONTH, 2);
    requestObj.put(JsonKey.END_DATE, format.format(cal.getTime()));
    List<String> userIds = new ArrayList<String>();
    userIds.add("test123345");
    request.put(JsonKey.COURSE_CREATED_FOR, userIds);
    request.setRequest(requestObj);
    try {
      RequestValidator.validateCreateBatchReq(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.dateFormatError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void validateCreateBatchWithEmptyEndDate() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.COURSE_ID, "do_123233434");
    requestObj.put(JsonKey.ENROLLMENT_TYPE, "Open");
    requestObj.put(JsonKey.NAME, "TestCourse");
    requestObj.put(JsonKey.START_DATE, "2017-01-05");
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_MONTH, 2);
    requestObj.put(JsonKey.END_DATE, "");
    List<String> userIds = new ArrayList<String>();
    userIds.add("test123345");
    request.put(JsonKey.COURSE_CREATED_FOR, userIds);
    request.setRequest(requestObj);
    try {
      RequestValidator.validateCreateBatchReq(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.courseBatchStartDateError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void validateCreateBatchWithPastEndDate() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.COURSE_ID, "do_123233434");
    requestObj.put(JsonKey.ENROLLMENT_TYPE, "Open");
    requestObj.put(JsonKey.NAME, "TestCourse");
    requestObj.put(JsonKey.START_DATE, format.format(new Date()));
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_MONTH, -2);
    requestObj.put(JsonKey.END_DATE, format.format(cal.getTime()));
    List<String> userIds = new ArrayList<String>();
    userIds.add("test123345");
    request.put(JsonKey.COURSE_CREATED_FOR, userIds);
    request.setRequest(requestObj);
    try {
      RequestValidator.validateCreateBatchReq(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.endDateError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void validateCreateBatchWithInvalidEndDateFormat() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.COURSE_ID, "do_123233434");
    requestObj.put(JsonKey.ENROLLMENT_TYPE, "Open");
    requestObj.put(JsonKey.NAME, "TestCourse");
    requestObj.put(JsonKey.START_DATE, format.format(new Date()));
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_MONTH, 2);
    requestObj.put(JsonKey.END_DATE, format.format(cal.getTime()) + " 23:59:59+Z:50");
    List<String> userIds = new ArrayList<String>();
    userIds.add("test123345");
    request.put(JsonKey.COURSE_CREATED_FOR, userIds);
    request.setRequest(requestObj);
    try {
      RequestValidator.validateCreateBatchReq(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.dateFormatError.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void validateAddBatchCourse() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.BATCH_ID, "cassandra batch id");
    List<String> list = new ArrayList<>();
    list.add("user id whome need to join");
    requestObj.put(JsonKey.USER_IDs, list);
    request.setRequest(requestObj);
    try {
      RequestValidator.validateAddBatchCourse(request);
      response = true;
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals(true, response);
  }

  @Test
  public void validateAddBatchCourseWithEmptyBatchId() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    List<String> list = new ArrayList<>();
    list.add("user id whome need to join");
    requestObj.put(JsonKey.USER_IDs, list);
    request.setRequest(requestObj);
    try {
      RequestValidator.validateAddBatchCourse(request);
      response = true;
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.courseBatchIdRequired.getErrorCode(), e.getCode());
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
    }
    assertEquals(false, response);
  }

  @Test
  public void validateAddBatchCourseWithEmptyUserId() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.BATCH_ID, "cassandra batch id");
    request.setRequest(requestObj);
    try {
      RequestValidator.validateAddBatchCourse(request);
      response = true;
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.userIdRequired.getErrorCode(), e.getCode());
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
    }
    assertEquals(false, response);
  }

  @Test
  public void validateGetBatchCourse() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.BATCH_ID, "cassandra batch id");
    request.setRequest(requestObj);
    try {
      RequestValidator.validateGetBatchCourse(request);
      response = true;
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals(true, response);
  }

  @Test
  public void validateGetBatchCourseWithOutBatchId() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    request.setRequest(requestObj);
    try {
      RequestValidator.validateGetBatchCourse(request);
      response = true;
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.courseBatchIdRequired.getErrorCode(), e.getCode());
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
    }
    assertEquals(false, response);
  }

  @Test
  public void validateUpdateCourse() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.COURSE_ID, "cassandra batch id");
    request.setRequest(requestObj);
    try {
      RequestValidator.validateUpdateCourse(request);
      response = true;
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals(true, response);
  }

  @Test
  public void validateUpdateCourseWithOurBatchId() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    request.setRequest(requestObj);
    try {
      RequestValidator.validateUpdateCourse(request);
      response = true;
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.courseIdRequired.getErrorCode(), e.getCode());
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
    }
    assertEquals(false, response);
  }

  @Test
  public void validatePublishedCourse() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.COURSE_ID, "cassandra batch id");
    request.setRequest(requestObj);
    try {
      RequestValidator.validatePublishCourse(request);
      response = true;
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals(true, response);
  }

  @Test
  public void validatePublishedCourseWithOutCourseId() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    request.setRequest(requestObj);
    try {
      RequestValidator.validatePublishCourse(request);
      response = true;
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.courseIdRequiredError.getErrorCode(), e.getCode());
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
    }
    assertEquals(false, response);
  }

  @Test
  public void validateDeleteCourse() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.COURSE_ID, "cassandra batch id");
    request.setRequest(requestObj);
    try {
      RequestValidator.validateDeleteCourse(request);
      response = true;
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals(true, response);
  }

  @Test
  public void validateDeleteCourseWithOutCourseId() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    request.setRequest(requestObj);
    try {
      RequestValidator.validateDeleteCourse(request);
      response = true;
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.courseIdRequiredError.getErrorCode(), e.getCode());
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
    }
    assertEquals(false, response);
  }
}
