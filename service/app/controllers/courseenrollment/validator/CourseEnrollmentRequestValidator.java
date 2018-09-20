package controllers.courseenrollment.validator;

import org.sunbird.common.models.util.*;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

public class CourseEnrollmentRequestValidator extends BaseRequestValidator {

  public CourseEnrollmentRequestValidator() {}

  public void validateEnrollCourse(Request courseRequestDto) {
    validateParam(
        (String) courseRequestDto.getRequest().get(JsonKey.COURSE_ID),
        ResponseCode.courseIdRequiredError);
  }

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
