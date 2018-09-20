package controllers.courseenrollment.validator;

import org.sunbird.common.models.util.*;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

/**
 * This call will do validation for all incoming request data for Course Enrollment.
 *
 * @author Sudhir
 */
public class CourseEnrollmentRequestValidator extends BaseRequestValidator {

  public CourseEnrollmentRequestValidator() {}

  /**
   * This method will do course enrollment request data validation. if all mandatory data is coming
   * then it won't do any thing if any mandatory data is missing then it will throw exception.
   *
   * @param courseRequestDto CourseRequestDto
   */
  public void validateEnrollCourse(Request courseRequestDto) {

    validateParam(
        (String) courseRequestDto.getRequest().get(JsonKey.COURSE_ID),
        ResponseCode.courseIdRequiredError);
  }

  /**
   * This method will do course enrollment request data validation. if all mandatory data is coming
   * then it won't do any thing if any mandatory data is missing then it will throw exception.
   *
   * @param courseRequestDto CourseRequestDto
   */
  public void validateUnenrollCourse(Request courseRequestDto) {

    validateParam(
        (String) courseRequestDto.getRequest().get(JsonKey.COURSE_ID),
        ResponseCode.courseIdRequiredError);

    validateParam(
        (String) courseRequestDto.getRequest().get(JsonKey.BATCH_ID),
        ResponseCode.courseBatchIdRequired);

    validateParam(
        (String) courseRequestDto.getRequest().get(JsonKey.USER_ID), ResponseCode.userIdRequired);
  }
}
