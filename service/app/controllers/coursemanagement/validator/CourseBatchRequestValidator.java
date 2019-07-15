package controllers.coursemanagement.validator;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

public class CourseBatchRequestValidator extends BaseRequestValidator {
  private static final int ERROR_CODE = ResponseCode.CLIENT_ERROR.getResponseCode();

  public void validateCreateCourseBatchRequest(Request request) {

    validateParam(
        (String) request.getRequest().get(JsonKey.COURSE_ID),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.COURSE_ID);
    validateParam(
        (String) request.getRequest().get(JsonKey.NAME),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.NAME);
    validateEnrolmentType(request);
    String startDate = (String) request.getRequest().get(JsonKey.START_DATE);
    String endDate = (String) request.getRequest().get(JsonKey.END_DATE);
    String enrollmentEndDate = (String) request.getRequest().get(JsonKey.ENROLLMENT_END_DATE);
    validateStartDate(startDate);
    validateEndDate(startDate, endDate);
    validateEnrollmentEndDate(enrollmentEndDate, startDate, endDate);
    validateCreatedForAndMentors(request);
    validateParticipants(request);
  }

  public void validateUpdateCourseBatchRequest(Request request) {
    validateParam(
        (String) request.getRequest().get(JsonKey.COURSE_ID),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.COURSE_ID);
    if (null != request.getRequest().get(JsonKey.STATUS)) {
      boolean status = validateBatchStatus(request);
      if (!status) {
        throw new ProjectCommonException(
            ResponseCode.progressStatusError.getErrorCode(),
            ResponseCode.progressStatusError.getErrorMessage(),
            ERROR_CODE);
      }
    }
    if (request.getRequest().containsKey(JsonKey.NAME)
        && StringUtils.isBlank((String) request.getRequest().get(JsonKey.NAME))) {
      throw new ProjectCommonException(
          ResponseCode.invalidParameterValue.getErrorCode(),
          ResponseCode.invalidParameterValue.getErrorMessage(),
          ERROR_CODE,
          (String) request.getRequest().get(JsonKey.NAME),
          JsonKey.NAME);
    }
    if (request.getRequest().containsKey(JsonKey.ENROLLMENT_TYPE)) {
      validateEnrolmentType(request);
    }
    validateParam(
        (String) request.getRequest().get(JsonKey.ID),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.ID);
    String startDate = (String) request.getRequest().get(JsonKey.START_DATE);
    String endDate = (String) request.getRequest().get(JsonKey.END_DATE);

    validateUpdateBatchStartDate(startDate);
    validateEndDate(startDate, endDate);

    boolean bool = validateDateWithTodayDate(endDate);
    if (!bool) {
      throw new ProjectCommonException(
          ResponseCode.invalidBatchEndDateError.getErrorCode(),
          ResponseCode.invalidBatchEndDateError.getErrorMessage(),
          ERROR_CODE);
    }

    validateUpdateBatchEndDate(request);
    validateCreatedForAndMentors(request);
    validateParticipants(request);
  }

  public void validateAddUserToCourseBatchRequest(Request courseRequest) {
    validateBatchId(courseRequest);
    validateUserId(courseRequest);
  }

  public void validateDeleteCourseBatchRequest(Request courseRequest) {
    validateBatchId(courseRequest);
    validateUserId(courseRequest);
  }

  private void validateBatchId(Request courseRequest) {
    if (courseRequest.getRequest().get(JsonKey.BATCH_ID) == null) {
      throw new ProjectCommonException(
          ResponseCode.courseBatchIdRequired.getErrorCode(),
          ResponseCode.courseBatchIdRequired.getErrorMessage(),
          ERROR_CODE);
    }
  }

  public void validateUserId(Request courseRequest) {
    if (courseRequest.getRequest().get(JsonKey.USER_IDs) == null) {
      throw new ProjectCommonException(
          ResponseCode.userIdRequired.getErrorCode(),
          ResponseCode.userIdRequired.getErrorMessage(),
          ERROR_CODE);
    }
  }

  private void validateParticipants(Request request) {
    if (request.getRequest().containsKey(JsonKey.PARTICIPANTS)
        && !(request.getRequest().get(JsonKey.PARTICIPANTS) instanceof List)) {
      throw new ProjectCommonException(
          ResponseCode.dataTypeError.getErrorCode(),
          ResponseCode.dataTypeError.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode(),
          JsonKey.PARTICIPANTS,
          "Array");
    }
  }

  private void validateEnrolmentType(Request request) {
    validateParam(
        (String) request.getRequest().get(JsonKey.ENROLLMENT_TYPE),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.ENROLLMENT_TYPE);
    String enrolmentType = (String) request.getRequest().get(JsonKey.ENROLLMENT_TYPE);
    if (!ProjectUtil.EnrolmentType.open.getVal().equalsIgnoreCase(enrolmentType)
        && !ProjectUtil.EnrolmentType.inviteOnly.getVal().equalsIgnoreCase(enrolmentType)) {
      throw new ProjectCommonException(
          ResponseCode.invalidParameterValue.getErrorCode(),
          ResponseCode.invalidParameterValue.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode(),
          enrolmentType,
          JsonKey.ENROLLMENT_TYPE);
    }
  }

  private void validateStartDate(String startDate) {
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    format.setLenient(false);
    validateParam(startDate, ResponseCode.mandatoryParamsMissing, JsonKey.START_DATE);
    try {
      Date batchStartDate = format.parse(startDate);
      Date todayDate = format.parse(format.format(new Date()));
      Calendar cal1 = Calendar.getInstance();
      Calendar cal2 = Calendar.getInstance();
      cal1.setTime(batchStartDate);
      cal2.setTime(todayDate);
      if (batchStartDate.before(todayDate)) {
        throw new ProjectCommonException(
            ResponseCode.courseBatchStartDateError.getErrorCode(),
            ResponseCode.courseBatchStartDateError.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
    } catch (ProjectCommonException e) {
      throw e;
    } catch (Exception e) {
      throw new ProjectCommonException(
          ResponseCode.dateFormatError.getErrorCode(),
          ResponseCode.dateFormatError.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  private static void validateEndDate(String startDate, String endDate) {
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    format.setLenient(false);
    Date batchEndDate = null;
    Date batchStartDate = null;
    try {
      if (StringUtils.isNotEmpty(endDate)) {
        batchEndDate = format.parse(endDate);
        batchStartDate = format.parse(startDate);
      }
    } catch (Exception e) {
      throw new ProjectCommonException(
          ResponseCode.dateFormatError.getErrorCode(),
          ResponseCode.dateFormatError.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    if (StringUtils.isNotEmpty(endDate) && batchStartDate.getTime() >= batchEndDate.getTime()) {
      throw new ProjectCommonException(
          ResponseCode.endDateError.getErrorCode(),
          ResponseCode.endDateError.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  private static void validateEnrollmentEndDate(
      String enrollmentEndDate, String startDate, String endDate) {
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    format.setLenient(false);
    Date batchEndDate = null;
    Date batchStartDate = null;
    Date batchenrollmentEndDate = null;
    try {
      if (StringUtils.isNotEmpty(enrollmentEndDate)) {
        batchenrollmentEndDate = format.parse(enrollmentEndDate);
        batchStartDate = format.parse(startDate);
      }
      if (StringUtils.isNotEmpty(endDate)) {
        batchEndDate = format.parse(endDate);
      }

    } catch (Exception e) {
      throw new ProjectCommonException(
          ResponseCode.dateFormatError.getErrorCode(),
          ResponseCode.dateFormatError.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    if (StringUtils.isNotEmpty(enrollmentEndDate)
        && batchStartDate.getTime() > batchenrollmentEndDate.getTime()) {
      throw new ProjectCommonException(
          ResponseCode.enrollmentEndDateStartError.getErrorCode(),
          ResponseCode.enrollmentEndDateStartError.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    if (StringUtils.isNotEmpty(enrollmentEndDate)
        && StringUtils.isNotEmpty(endDate)
        && batchEndDate.getTime() < batchenrollmentEndDate.getTime()) {
      throw new ProjectCommonException(
          ResponseCode.enrollmentEndDateEndError.getErrorCode(),
          ResponseCode.enrollmentEndDateEndError.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  private void validateCreatedForAndMentors(Request request) {
    if (request.getRequest().containsKey(JsonKey.COURSE_CREATED_FOR)
        && !(request.getRequest().get(JsonKey.COURSE_CREATED_FOR) instanceof List)) {
      throw new ProjectCommonException(
          ResponseCode.dataTypeError.getErrorCode(),
          ResponseCode.dataTypeError.getErrorMessage(),
          ERROR_CODE,
          JsonKey.COURSE_CREATED_FOR,
          "Arrays");
    }

    if (request.getRequest().containsKey(JsonKey.MENTORS)
        && !(request.getRequest().get(JsonKey.MENTORS) instanceof List)) {
      throw new ProjectCommonException(
          ResponseCode.dataTypeError.getErrorCode(),
          ResponseCode.dataTypeError.getErrorMessage(),
          ERROR_CODE,
          JsonKey.MENTORS,
          "Arrays");
    }
  }

  private void validateUpdateBatchStartDate(String startDate) {
    try {
      SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
      format.parse(startDate);
    } catch (Exception e) {
      throw new ProjectCommonException(
          ResponseCode.dateFormatError.getErrorCode(),
          ResponseCode.dateFormatError.getErrorMessage(),
          ERROR_CODE);
    }
  }

  private boolean validateDateWithTodayDate(String date) {
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    format.setLenient(false);
    try {
      if (StringUtils.isNotEmpty(date)) {
        Date reqDate = format.parse(date);
        Date todayDate = format.parse(format.format(new Date()));
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(reqDate);
        cal2.setTime(todayDate);
        if (reqDate.before(todayDate)) {
          return false;
        }
      }
    } catch (Exception e) {
      throw new ProjectCommonException(
          ResponseCode.dateFormatError.getErrorCode(),
          ResponseCode.dateFormatError.getErrorMessage(),
          ERROR_CODE);
    }
    return true;
  }

  private void validateUpdateBatchEndDate(Request request) {

    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    String startDate = (String) request.getRequest().get(JsonKey.START_DATE);
    String endDate = (String) request.getRequest().get(JsonKey.END_DATE);
    format.setLenient(false);
    if (StringUtils.isNotBlank(endDate) && StringUtils.isNotBlank(startDate)) {
      Date batchStartDate = null;
      Date batchEndDate = null;
      try {
        batchStartDate = format.parse(startDate);
        batchEndDate = format.parse(endDate);
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(batchStartDate);
        cal2.setTime(batchEndDate);
      } catch (Exception e) {
        throw new ProjectCommonException(
            ResponseCode.dateFormatError.getErrorCode(),
            ResponseCode.dateFormatError.getErrorMessage(),
            ERROR_CODE);
      }
      if (batchEndDate.before(batchStartDate)) {
        throw new ProjectCommonException(
            ResponseCode.invalidBatchEndDateError.getErrorCode(),
            ResponseCode.invalidBatchEndDateError.getErrorMessage(),
            ERROR_CODE);
      }
    }
  }

  private boolean validateBatchStatus(Request request) {
    boolean status = false;
    try {
      status = checkProgressStatus(Integer.parseInt("" + request.getRequest().get(JsonKey.STATUS)));

    } catch (Exception e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    return status;
  }

  private boolean checkProgressStatus(int status) {
    for (ProjectUtil.ProgressStatus pstatus : ProjectUtil.ProgressStatus.values()) {
      if (pstatus.getValue() == status) {
        return true;
      }
    }
    return false;
  }

  public void validateGetParticipantsRequest(Request request) {
    if(MapUtils.isEmpty((Map) request.getRequest().get(JsonKey.BATCH))){
        throw new ProjectCommonException(
                ResponseCode.invalidRequestData.getErrorCode(),
                MessageFormat.format(ResponseCode.invalidRequestData.getErrorMessage(), JsonKey.BATCH),
                ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    validateParam(
            (String) ((Map<String, Object>)request.getRequest().get(JsonKey.BATCH)).get(JsonKey.BATCH_ID),
            ResponseCode.mandatoryParamsMissing,
            JsonKey.BATCH_ID);
  }
}
