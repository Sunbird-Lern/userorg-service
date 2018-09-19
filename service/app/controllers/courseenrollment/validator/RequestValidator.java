package controllers.courseenrollment.validator;


import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.*;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

/**
 * This call will do validation for all incoming request data.
 *
 * @author Manzarul
 */
public final class RequestValidator extends BaseRequestValidator {
  private static final int ERROR_CODE = ResponseCode.CLIENT_ERROR.getResponseCode();

  private RequestValidator() {}

  /**
   * This method will do course enrollment request data validation. if all mandatory data is coming
   * then it won't do any thing if any mandatory data is missing then it will throw exception.
   *
   * @param courseRequestDto CourseRequestDto
   */
  public static void validateEnrollCourse(Request courseRequestDto) {
    if (courseRequestDto.getRequest().get(JsonKey.COURSE_ID) == null) {
      throw new ProjectCommonException(
          ResponseCode.courseIdRequiredError.getErrorCode(),
          ResponseCode.courseIdRequiredError.getErrorMessage(),
          ERROR_CODE);
    }
  }
  
  /**
   * This method will do course enrollment request data validation. if all mandatory data is coming
   * then it won't do any thing if any mandatory data is missing then it will throw exception.
   *
   * @param courseRequestDto CourseRequestDto
   */
  public static void validateUnenrollCourse(Request courseRequestDto) {
    if (courseRequestDto.getRequest().get(JsonKey.COURSE_ID) == null) {
      throw new ProjectCommonException(
          ResponseCode.courseIdRequiredError.getErrorCode(),
          ResponseCode.courseIdRequiredError.getErrorMessage(),
          ERROR_CODE);
    }
    
    if (courseRequestDto.getRequest().get(JsonKey.BATCH_ID) == null) {
    	throw new ProjectCommonException(
    	   ResponseCode.courseBatchIdRequired.getErrorCode(),
    	   ResponseCode.courseBatchIdRequired.getErrorMessage(),
    			ERROR_CODE);
    	
    }
    
    if(courseRequestDto.getRequest().get(JsonKey.USER_ID) == null ) {
    	throw new ProjectCommonException(
    	   ResponseCode.userIdRequired.getErrorCode(),
    	   ResponseCode.userIdRequired.getErrorMessage(),
    	   ERROR_CODE);
    }
    
    if (!courseRequestDto.getRequest().get(JsonKey.USER_ID).equals(courseRequestDto.getContext().get(JsonKey.REQUESTED_BY))) {
    	  throw new ProjectCommonException(
    			ResponseCode.invalidParameterValue.getErrorCode(),
              ResponseCode.invalidParameterValue.getErrorMessage(),
    	        ERROR_CODE);
    	         	  }
  }
}

