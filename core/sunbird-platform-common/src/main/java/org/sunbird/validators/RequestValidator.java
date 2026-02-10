package org.sunbird.validators;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.common.ProjectUtil;
import org.sunbird.common.PropertiesCache;
import org.sunbird.utils.StringFormatter;
import org.sunbird.request.Request;
import org.sunbird.response.ResponseCode;
import org.sunbird.response.ResponseMessage;

/**
 * Validates the request structure and data for various operations.
 *
 * @author Manzarul
 */
public final class RequestValidator {
  
  private static final LoggerUtil logger = new LoggerUtil(RequestValidator.class);
  
  private static final int ERROR_CODE = ResponseCode.CLIENT_ERROR.getResponseCode();

  private RequestValidator() {}

  /**
   * Validates the request structure and data for updating content.
   * Checks for mandatory fields and format validity.
   *
   * @param contentRequestDto The request object containing content update data.
   * @throws ProjectCommonException If validation fails.
   */
  @SuppressWarnings("unchecked")
  public static void validateUpdateContent(Request contentRequestDto) {
    List<Map<String, Object>> list =
        (List<Map<String, Object>>) (contentRequestDto.getRequest().get(JsonKey.CONTENTS));
    if (CollectionUtils.isNotEmpty(list)) {
      for (Map<String, Object> map : list) {
        if (null != map.get(JsonKey.LAST_UPDATED_TIME)) {
          boolean bool =
              ProjectUtil.isDateValidFormat(
                  "yyyy-MM-dd HH:mm:ss:SSSZ", (String) map.get(JsonKey.LAST_UPDATED_TIME));
          if (!bool) {
            throw new ProjectCommonException(
                ResponseCode.dateFormatError.getErrorCode(),
                ResponseCode.dateFormatError.getErrorMessage(),
                ERROR_CODE);
          }
        }
        if (null != map.get(JsonKey.LAST_COMPLETED_TIME)) {
          boolean bool =
              ProjectUtil.isDateValidFormat(
                  "yyyy-MM-dd HH:mm:ss:SSSZ", (String) map.get(JsonKey.LAST_COMPLETED_TIME));
          if (!bool) {
            throw new ProjectCommonException(
                ResponseCode.dateFormatError.getErrorCode(),
                ResponseCode.dateFormatError.getErrorMessage(),
                ERROR_CODE);
          }
        }
        if (map.containsKey(JsonKey.CONTENT_ID)) {
          if (null == map.get(JsonKey.CONTENT_ID)) {
            throw new ProjectCommonException(
                ResponseCode.contentIdRequired.getErrorCode(),
                ResponseCode.contentIdRequiredError.getErrorMessage(),
                ERROR_CODE);
          }
          if (ProjectUtil.isNull(map.get(JsonKey.STATUS))) {
            throw new ProjectCommonException(
                ResponseCode.contentStatusRequired.getErrorCode(),
                ResponseCode.contentStatusRequired.getErrorMessage(),
                ERROR_CODE);
          }
        } else {
          throw new ProjectCommonException(
              ResponseCode.contentIdRequired.getErrorCode(),
              ResponseCode.contentIdRequiredError.getErrorMessage(),
              ERROR_CODE);
        }
      }
    }
    List<Map<String, Object>> assessmentData =
        (List<Map<String, Object>>)
            contentRequestDto.getRequest().get(JsonKey.ASSESSMENT_EVENTS);
    if (!CollectionUtils.isEmpty(assessmentData)) {
      for (Map<String, Object> map : assessmentData) {
        if (!map.containsKey(JsonKey.ASSESSMENT_TS)) {
          throw new ProjectCommonException(
              ResponseCode.assessmentAttemptDateRequired.getErrorCode(),
              ResponseCode.assessmentAttemptDateRequired.getErrorMessage(),
              ERROR_CODE);
        }

        if (!map.containsKey(JsonKey.COURSE_ID)
            || StringUtils.isBlank((String) map.get(JsonKey.COURSE_ID))) {
          throw new ProjectCommonException(
              ResponseCode.courseIdRequired.getErrorCode(),
              ResponseCode.courseIdRequiredError.getErrorMessage(),
              ERROR_CODE);
        }

        if (!map.containsKey(JsonKey.CONTENT_ID)
            || StringUtils.isBlank((String) map.get(JsonKey.CONTENT_ID))) {
          throw new ProjectCommonException(
              ResponseCode.contentIdRequired.getErrorCode(),
              ResponseCode.contentIdRequiredError.getErrorMessage(),
              ERROR_CODE);
        }

        if (!map.containsKey(JsonKey.BATCH_ID)
            || StringUtils.isBlank((String) map.get(JsonKey.BATCH_ID))) {
          throw new ProjectCommonException(
              ResponseCode.courseBatchIdRequired.getErrorCode(),
              ResponseCode.courseBatchIdRequired.getErrorMessage(),
              ERROR_CODE);
        }

        if (!map.containsKey(JsonKey.USER_ID)
            || StringUtils.isBlank((String) map.get(JsonKey.USER_ID))) {
          throw new ProjectCommonException(
              ResponseCode.userIdRequired.getErrorCode(),
              ResponseCode.userIdRequired.getErrorMessage(),
              ERROR_CODE);
        }

        if (!map.containsKey(JsonKey.ATTEMPT_ID)
            || StringUtils.isBlank((String) map.get(JsonKey.ATTEMPT_ID))) {
          throw new ProjectCommonException(
              ResponseCode.attemptIdRequired.getErrorCode(),
              ResponseCode.attemptIdRequired.getErrorMessage(),
              ERROR_CODE);
        }

        if (!map.containsKey(JsonKey.EVENTS)) {
          throw new ProjectCommonException(
              ResponseCode.eventsRequired.getErrorCode(),
              ResponseCode.eventsRequired.getErrorMessage(),
              ERROR_CODE);
        }
      }
    }
  }

  /**
   * Validates the request data for getting page data.
   *
   * @param request The request object.
   * @throws ProjectCommonException If validation fails.
   */
  public static void validateGetPageData(Request request) {
    if (request == null || (StringUtils.isBlank((String) request.get(JsonKey.SOURCE)))) {
      throw new ProjectCommonException(
          ResponseCode.sourceRequired.getErrorCode(),
          ResponseCode.sourceRequired.getErrorMessage(),
          ERROR_CODE);
    }
    if (!validPageSourceType((String) request.get(JsonKey.SOURCE))) {
      throw new ProjectCommonException(
          ResponseCode.invalidPageSource.getErrorCode(),
          ResponseCode.invalidPageSource.getErrorMessage(),
          ERROR_CODE);
    }
    if (StringUtils.isBlank((String) request.get(JsonKey.PAGE_NAME))) {
      throw new ProjectCommonException(
          ResponseCode.pageNameRequired.getErrorCode(),
          ResponseCode.pageNameRequired.getErrorMessage(),
          ERROR_CODE);
    }
  }

  private static boolean validPageSourceType(String source) {
    boolean isValidSource = false;
    for (ProjectUtil.Source src : ProjectUtil.Source.values()) {
      if (src.getValue().equalsIgnoreCase(source)) {
        isValidSource = true;
        break;
      }
    }
    return isValidSource;
  }

  /**
   * Validates the request data for adding a batch to a course.
   *
   * @param courseRequest The request object.
   * @throws ProjectCommonException If validation fails.
   */
  public static void validateAddBatchCourse(Request courseRequest) {
    if (courseRequest.getRequest().get(JsonKey.BATCH_ID) == null) {
      throw new ProjectCommonException(
          ResponseCode.courseBatchIdRequired.getErrorCode(),
          ResponseCode.courseBatchIdRequired.getErrorMessage(),
          ERROR_CODE);
    }
    if (courseRequest.getRequest().get(JsonKey.USER_IDs) == null) {
      throw new ProjectCommonException(
          ResponseCode.userIdRequired.getErrorCode(),
          ResponseCode.userIdRequired.getErrorMessage(),
          ERROR_CODE);
    }
  }

  /**
   * Validates the request data for getting a batch.
   *
   * @param courseRequest The request object.
   * @throws ProjectCommonException If validation fails.
   */
  public static void validateGetBatchCourse(Request courseRequest) {
    if (courseRequest.getRequest().get(JsonKey.BATCH_ID) == null) {
      throw new ProjectCommonException(
          ResponseCode.courseBatchIdRequired.getErrorCode(),
          ResponseCode.courseBatchIdRequired.getErrorMessage(),
          ERROR_CODE);
    }
  }

  /**
   * Validates the request data for updating a course.
   *
   * @param request The request object.
   * @throws ProjectCommonException If validation fails.
   */
  public static void validateUpdateCourse(Request request) {
    if (request.getRequest().get(JsonKey.COURSE_ID) == null) {
      throw new ProjectCommonException(
          ResponseCode.courseIdRequired.getErrorCode(),
          ResponseCode.courseIdRequired.getErrorMessage(),
          ERROR_CODE);
    }
  }

  /**
   * Validates the request data for publishing a course.
   *
   * @param request The request object.
   * @throws ProjectCommonException If validation fails.
   */
  public static void validatePublishCourse(Request request) {
    if (request.getRequest().get(JsonKey.COURSE_ID) == null) {
      throw new ProjectCommonException(
          ResponseCode.courseIdRequiredError.getErrorCode(),
          ResponseCode.courseIdRequiredError.getErrorMessage(),
          ERROR_CODE);
    }
  }

  /**
   * Validates the request data for deleting a course.
   *
   * @param request The request object.
   * @throws ProjectCommonException If validation fails.
   */
  public static void validateDeleteCourse(Request request) {
    if (request.getRequest().get(JsonKey.COURSE_ID) == null) {
      throw new ProjectCommonException(
          ResponseCode.courseIdRequiredError.getErrorCode(),
          ResponseCode.courseIdRequiredError.getErrorMessage(),
          ERROR_CODE);
    }
  }

  /**
   * Validates the request data for creating a section.
   *
   * @param request The request object.
   * @throws ProjectCommonException If validation fails.
   */
  public static void validateCreateSection(Request request) {
    if (StringUtils.isBlank(
        (String)
            (request.getRequest().get(JsonKey.SECTION_NAME) != null
                ? request.getRequest().get(JsonKey.SECTION_NAME)
                : ""))) {
      throw new ProjectCommonException(
          ResponseCode.sectionNameRequired.getErrorCode(),
          ResponseCode.sectionNameRequired.getErrorMessage(),
          ERROR_CODE);
    }
    if (StringUtils.isBlank(
        (String)
            (request.getRequest().get(JsonKey.SECTION_DATA_TYPE) != null
                ? request.getRequest().get(JsonKey.SECTION_DATA_TYPE)
                : ""))) {
      throw new ProjectCommonException(
          ResponseCode.sectionDataTypeRequired.getErrorCode(),
          ResponseCode.sectionDataTypeRequired.getErrorMessage(),
          ERROR_CODE);
    }
  }

  /**
   * Validates the request data for updating a section.
   *
   * @param request The request object.
   * @throws ProjectCommonException If validation fails.
   */
  public static void validateUpdateSection(Request request) {
    if (request.getRequest().containsKey(JsonKey.SECTION_NAME)
        && StringUtils.isBlank(
            (String)
                (request.getRequest().get(JsonKey.SECTION_NAME) != null
                    ? request.getRequest().get(JsonKey.SECTION_NAME)
                    : ""))) {
      throw new ProjectCommonException(
          ResponseCode.sectionNameRequired.getErrorCode(),
          ResponseCode.sectionNameRequired.getErrorMessage(),
          ERROR_CODE);
    }
    if (StringUtils.isBlank(
        (String)
            (request.getRequest().get(JsonKey.ID) != null
                ? request.getRequest().get(JsonKey.ID)
                : ""))) {
      throw new ProjectCommonException(
          ResponseCode.sectionIdRequired.getErrorCode(),
          ResponseCode.sectionIdRequired.getErrorMessage(),
          ERROR_CODE);
    }
    if (request.getRequest().containsKey(JsonKey.SECTION_DATA_TYPE)
        && StringUtils.isBlank(
            (String)
                (request.getRequest().get(JsonKey.SECTION_DATA_TYPE) != null
                    ? request.getRequest().get(JsonKey.SECTION_DATA_TYPE)
                    : ""))) {
      throw new ProjectCommonException(
          ResponseCode.sectionDataTypeRequired.getErrorCode(),
          ResponseCode.sectionDataTypeRequired.getErrorMessage(),
          ERROR_CODE);
    }
  }

  /**
   * Validates the request data for creating a page.
   *
   * @param request The request object.
   * @throws ProjectCommonException If validation fails.
   */
  public static void validateCreatePage(Request request) {
    if (StringUtils.isEmpty(
        (String)
            (request.getRequest().get(JsonKey.PAGE_NAME) != null
                ? request.getRequest().get(JsonKey.PAGE_NAME)
                : ""))) {
      throw new ProjectCommonException(
          ResponseCode.pageNameRequired.getErrorCode(),
          ResponseCode.pageNameRequired.getErrorMessage(),
          ERROR_CODE);
    }
  }

  /**
   * Validates the request data for updating a page.
   *
   * @param request The request object.
   * @throws ProjectCommonException If validation fails.
   */
  public static void validateUpdatepage(Request request) {
    if (request.getRequest().containsKey(JsonKey.PAGE_NAME)
        && StringUtils.isEmpty(
            (String)
                (request.getRequest().get(JsonKey.PAGE_NAME) != null
                    ? request.getRequest().get(JsonKey.PAGE_NAME)
                    : ""))) {
      throw new ProjectCommonException(
          ResponseCode.pageNameRequired.getErrorCode(),
          ResponseCode.pageNameRequired.getErrorMessage(),
          ERROR_CODE);
    }
    if (StringUtils.isBlank(
        (String)
            (request.getRequest().get(JsonKey.ID) != null
                ? request.getRequest().get(JsonKey.ID)
                : ""))) {
      throw new ProjectCommonException(
          ResponseCode.pageIdRequired.getErrorCode(),
          ResponseCode.pageIdRequired.getErrorMessage(),
          ERROR_CODE);
    }
  }

  /**
   * Validates the request data for uploading users.
   *
   * @param reqObj The request object containing user upload data.
   * @throws ProjectCommonException If validation fails.
   */
  public static void validateUploadUser(Map<String, Object> reqObj) {
    if (StringUtils.isBlank((String) reqObj.get(JsonKey.ORGANISATION_ID))
        && (StringUtils.isBlank((String) reqObj.get(JsonKey.ORG_EXTERNAL_ID))
            || StringUtils.isBlank((String) reqObj.get(JsonKey.ORG_PROVIDER)))) {
      throw new ProjectCommonException(
          ResponseCode.mandatoryParamsMissing.getErrorCode(),
          ProjectUtil.formatMessage(
              ResponseCode.mandatoryParamsMissing.getErrorMessage(),
              (ProjectUtil.formatMessage(
                  ResponseMessage.Message.OR_FORMAT,
                  JsonKey.ORGANISATION_ID,
                  ProjectUtil.formatMessage(
                      ResponseMessage.Message.AND_FORMAT,
                      JsonKey.ORG_EXTERNAL_ID,
                      JsonKey.ORG_PROVIDER)))),
          ERROR_CODE);
    }
    if (null == reqObj.get(JsonKey.FILE)) {
      throw new ProjectCommonException(
          ResponseCode.mandatoryParamsMissing.getErrorCode(),
          ProjectUtil.formatMessage(
              ResponseCode.mandatoryParamsMissing.getErrorMessage(), JsonKey.FILE),
          ERROR_CODE);
    }
  }

  /**
   * Validates the request data for creating a batch.
   * <ul>
   * <li>courseId : Should be a valid courseId under EKStep.</li>
   * <li>name : should not be null or empty.</li>
   * <li>enrolmentType: can have only following two values {"open","invite-only"}.</li>
   * <li>startDate : In yyyy-MM-DD format, and must be >= today date.</li>
   * <li>endDate : In yyyy-MM-DD format and must be > startDate.</li>
   * <li>createdFor : List of valid organisation ids. Used in case of "invite-only" enrolmentType.</li>
   * <li>mentors : List of user ids, who will work as a mentor.</li>
   * </ul>
   *
   * @param request The request object.
   * @throws ProjectCommonException If validation fails.
   */
  public static void validateCreateBatchReq(Request request) {
    if (StringUtils.isBlank((String) request.getRequest().get(JsonKey.COURSE_ID))) {
      throw new ProjectCommonException(
          ResponseCode.invalidCourseId.getErrorCode(),
          ResponseCode.invalidCourseId.getErrorMessage(),
          ERROR_CODE);
    }
    if (StringUtils.isBlank((String) request.getRequest().get(JsonKey.NAME))) {
      throw new ProjectCommonException(
          ResponseCode.courseNameRequired.getErrorCode(),
          ResponseCode.courseNameRequired.getErrorMessage(),
          ERROR_CODE);
    }
    String enrolmentType = (String) request.getRequest().get(JsonKey.ENROLLMENT_TYPE);
    validateEnrolmentType(enrolmentType);
    String startDate = (String) request.getRequest().get(JsonKey.START_DATE);
    String endDate = (String) request.getRequest().get(JsonKey.END_DATE);
    validateStartDate(startDate);
    validateEndDate(startDate, endDate);

    if (request.getRequest().containsKey(JsonKey.COURSE_CREATED_FOR)
        && !(request.getRequest().get(JsonKey.COURSE_CREATED_FOR) instanceof List)) {
      throw new ProjectCommonException(
          ResponseCode.dataTypeError.getErrorCode(),
          ResponseCode.dataTypeError.getErrorMessage(),
          ERROR_CODE);
    }
  }

  private static boolean checkProgressStatus(int status) {
    for (ProjectUtil.ProgressStatus pstatus : ProjectUtil.ProgressStatus.values()) {
      if (pstatus.getValue() == status) {
        return true;
      }
    }
    return false;
  }

  /**
   * Validates the request data for updating a batch.
   *
   * @param request The request object.
   * @throws ProjectCommonException If validation fails.
   */
  public static void validateUpdateCourseBatchReq(Request request) {

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
        && StringUtils.isEmpty((String) request.getRequest().get(JsonKey.NAME))) {
      throw new ProjectCommonException(
          ResponseCode.courseNameRequired.getErrorCode(),
          ResponseCode.courseNameRequired.getErrorMessage(),
          ERROR_CODE);
    }
    if (request.getRequest().containsKey(JsonKey.ENROLLMENT_TYPE)) {
      String enrolmentType = (String) request.getRequest().get(JsonKey.ENROLLMENT_TYPE);
      validateEnrolmentType(enrolmentType);
    }
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
    if (request.getRequest().containsKey(JsonKey.COURSE_CREATED_FOR)
        && !(request.getRequest().get(JsonKey.COURSE_CREATED_FOR) instanceof List)) {
      throw new ProjectCommonException(
          ResponseCode.dataTypeError.getErrorCode(),
          ResponseCode.dataTypeError.getErrorMessage(),
          ERROR_CODE);
    }

    if (request.getRequest().containsKey(JsonKey.MENTORS)
        && !(request.getRequest().get(JsonKey.MENTORS) instanceof List)) {
      throw new ProjectCommonException(
          ResponseCode.dataTypeError.getErrorCode(),
          ResponseCode.dataTypeError.getErrorMessage(),
          ERROR_CODE);
    }
  }

  private static void validateUpdateBatchStartDate(String startDate) {
    if (StringUtils.isNotBlank(startDate)) {
      try {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        format.parse(startDate);
      } catch (Exception e) {
        throw new ProjectCommonException(
            ResponseCode.dateFormatError.getErrorCode(),
            ResponseCode.dateFormatError.getErrorMessage(),
            ERROR_CODE);
      }
    } else {
      throw new ProjectCommonException(
          ResponseCode.courseBatchStartDateRequired.getErrorCode(),
          ResponseCode.courseBatchStartDateRequired.getErrorMessage(),
          ERROR_CODE);
    }
  }

  private static boolean validateBatchStatus(Request request) {
    boolean status = false;
    try {
      status = checkProgressStatus(Integer.parseInt("" + request.getRequest().get(JsonKey.STATUS)));

    } catch (Exception e) {
      logger.error("RequestValidator:validateBatchStatus: Error validating batch status", e);
    }
    return status;
  }

  private static void validateUpdateBatchEndDate(Request request) {

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

  private static boolean validateDateWithTodayDate(String date) {
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

  /**
   * Validates the enrollment type.
   *
   * @param enrolmentType The enrollment type string.
   * @throws ProjectCommonException If validation fails.
   */
  public static void validateEnrolmentType(String enrolmentType) {
    if (StringUtils.isBlank(enrolmentType)) {
      throw new ProjectCommonException(
          ResponseCode.enrolmentTypeRequired.getErrorCode(),
          ResponseCode.enrolmentTypeRequired.getErrorMessage(),
          ERROR_CODE);
    }
    if (!(ProjectUtil.EnrolmentType.open.getVal().equalsIgnoreCase(enrolmentType)
        || ProjectUtil.EnrolmentType.inviteOnly.getVal().equalsIgnoreCase(enrolmentType))) {
      throw new ProjectCommonException(
          ResponseCode.enrolmentIncorrectValue.getErrorCode(),
          ResponseCode.enrolmentIncorrectValue.getErrorMessage(),
          ERROR_CODE);
    }
  }

  /**
   * Validates the start date.
   *
   * @param startDate The start date string in yyyy-MM-dd format.
   * @throws ProjectCommonException If validation fails.
   */
  private static void validateStartDate(String startDate) {
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    format.setLenient(false);
    if (StringUtils.isBlank(startDate)) {
      throw new ProjectCommonException(
          ResponseCode.courseBatchStartDateRequired.getErrorCode(),
          ResponseCode.courseBatchStartDateRequired.getErrorMessage(),
          ERROR_CODE);
    }
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
            ERROR_CODE);
      }
    } catch (ProjectCommonException e) {
      throw e;
    } catch (Exception e) {
      throw new ProjectCommonException(
          ResponseCode.dateFormatError.getErrorCode(),
          ResponseCode.dateFormatError.getErrorMessage(),
          ERROR_CODE);
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
          ERROR_CODE);
    }
    if (StringUtils.isNotEmpty(endDate) && batchStartDate.getTime() >= batchEndDate.getTime()) {
      throw new ProjectCommonException(
          ResponseCode.endDateError.getErrorCode(),
          ResponseCode.endDateError.getErrorMessage(),
          ERROR_CODE);
    }
  }

  /**
   * Validates the request data for sync operations.
   *
   * @param request The request object.
   * @throws ProjectCommonException If validation fails.
   */
  public static void validateSyncRequest(Request request) {
    String operation = (String) request.getRequest().get(JsonKey.OPERATION_FOR);
    if ((null != operation) && (!operation.equalsIgnoreCase("keycloak"))) {
      if (request.getRequest().get(JsonKey.OBJECT_TYPE) == null) {
        throw new ProjectCommonException(
            ResponseCode.dataTypeError.getErrorCode(),
            ResponseCode.dataTypeError.getErrorMessage(),
            ERROR_CODE);
      }
      List<String> list =
          new ArrayList<>(
              Arrays.asList(
                  new String[] {
                    JsonKey.USER, JsonKey.ORGANISATION, JsonKey.BATCH, JsonKey.USER_COURSE
                  }));
      if (!list.contains(request.getRequest().get(JsonKey.OBJECT_TYPE))) {
        throw new ProjectCommonException(
            ResponseCode.invalidObjectType.getErrorCode(),
            ResponseCode.invalidObjectType.getErrorMessage(),
            ERROR_CODE);
      }
    }
  }

  /**
   * Validates the request data for updating system settings.
   *
   * @param request The request object.
   * @throws ProjectCommonException If validation fails.
   */
  public static void validateUpdateSystemSettingsRequest(Request request) {
    List<String> list =
        new ArrayList<>(
            Arrays.asList(
                PropertiesCache.getInstance()
                    .getProperty("system_settings_properties")
                    .split(",")));
    for (String str : request.getRequest().keySet()) {
      if (!list.contains(str)) {
        throw new ProjectCommonException(
            ResponseCode.invalidPropertyError.getErrorCode(),
            MessageFormat.format(ResponseCode.invalidPropertyError.getErrorMessage(), str),
            ERROR_CODE);
      }
    }
  }

  /**
   * Validates the request data for sending an email.
   *
   * @param request The request object.
   * @throws ProjectCommonException If validation fails.
   */
  @SuppressWarnings("unchecked")
  public static void validateSendMail(Request request) {
    if (StringUtils.isBlank((String) request.getRequest().get(JsonKey.SUBJECT))) {
      throw new ProjectCommonException(
          ResponseCode.emailSubjectError.getErrorCode(),
          ResponseCode.emailSubjectError.getErrorMessage(),
          ERROR_CODE);
    }
    if (StringUtils.isBlank((String) request.getRequest().get(JsonKey.BODY))) {
      throw new ProjectCommonException(
          ResponseCode.emailBodyError.getErrorCode(),
          ResponseCode.emailBodyError.getErrorMessage(),
          ERROR_CODE);
    }
    if (CollectionUtils.isEmpty((List<String>) (request.getRequest().get(JsonKey.RECIPIENT_EMAILS)))
        && CollectionUtils.isEmpty(
            (List<String>) (request.getRequest().get(JsonKey.RECIPIENT_USERIDS)))
        && MapUtils.isEmpty(
            (Map<String, Object>) (request.getRequest().get(JsonKey.RECIPIENT_SEARCH_QUERY)))
        && CollectionUtils.isEmpty(
            (List<String>) (request.getRequest().get(JsonKey.RECIPIENT_PHONES)))) {
      throw new ProjectCommonException(
          ResponseCode.mandatoryParamsMissing.getErrorCode(),
          MessageFormat.format(
              ResponseCode.mandatoryParamsMissing.getErrorMessage(),
              StringFormatter.joinByOr(
                  StringFormatter.joinByComma(
                      JsonKey.RECIPIENT_EMAILS,
                      JsonKey.RECIPIENT_USERIDS,
                      JsonKey.RECIPIENT_PHONES),
                  JsonKey.RECIPIENT_SEARCH_QUERY)),
          ERROR_CODE);
    }
  }

  /**
   * Validates the request data for file upload.
   *
   * @param reqObj The request object.
   * @throws ProjectCommonException If validation fails.
   */
  public static void validateFileUpload(Request reqObj) {

    if (StringUtils.isBlank((String) reqObj.get(JsonKey.CONTAINER))) {
      throw new ProjectCommonException(
          ResponseCode.storageContainerNameMandatory.getErrorCode(),
          ResponseCode.storageContainerNameMandatory.getErrorMessage(),
          ERROR_CODE);
    }
  }

  /**
   * Validates the request data for creating an organisation type.
   *
   * @param reqObj The request object.
   * @throws ProjectCommonException If validation fails.
   */
  public static void validateCreateOrgType(Request reqObj) {
    if (StringUtils.isBlank((String) reqObj.getRequest().get(JsonKey.NAME))) {
      throw createExceptionInstance(ResponseCode.orgTypeMandatory.getErrorCode());
    }
  }

  /**
   * Validates the request data for updating an organisation type.
   *
   * @param reqObj The request object.
   * @throws ProjectCommonException If validation fails.
   */
  public static void validateUpdateOrgType(Request reqObj) {
    if (StringUtils.isBlank((String) reqObj.getRequest().get(JsonKey.NAME))) {
      throw createExceptionInstance(ResponseCode.orgTypeMandatory.getErrorCode());
    }
    if (StringUtils.isBlank((String) reqObj.getRequest().get(JsonKey.ID))) {
      throw createExceptionInstance(ResponseCode.orgTypeIdRequired.getErrorCode());
    }
  }

  /**
   * Validates the request data for a note.
   *
   * @param request The request object.
   * @throws ProjectCommonException If validation fails.
   */
  @SuppressWarnings("rawtypes")
  public static void validateNote(Request request) {
    if (StringUtils.isBlank((String) request.get(JsonKey.USER_ID))) {
      throw new ProjectCommonException(
          ResponseCode.userIdRequired.getErrorCode(),
          ResponseCode.userIdRequired.getErrorMessage(),
          ERROR_CODE);
    }
    if (StringUtils.isBlank((String) request.get(JsonKey.TITLE))) {
      throw new ProjectCommonException(
          ResponseCode.titleRequired.getErrorCode(),
          ResponseCode.titleRequired.getErrorMessage(),
          ERROR_CODE);
    }
    if (StringUtils.isBlank((String) request.get(JsonKey.NOTE))) {
      throw new ProjectCommonException(
          ResponseCode.noteRequired.getErrorCode(),
          ResponseCode.noteRequired.getErrorMessage(),
          ERROR_CODE);
    }
    if (StringUtils.isBlank((String) request.get(JsonKey.CONTENT_ID))
        && StringUtils.isBlank((String) request.get(JsonKey.COURSE_ID))) {
      throw new ProjectCommonException(
          ResponseCode.contentIdError.getErrorCode(),
          ResponseCode.contentIdError.getErrorMessage(),
          ERROR_CODE);
    }
    if (request.getRequest().containsKey(JsonKey.TAGS)
        && ((request.getRequest().get(JsonKey.TAGS) instanceof List)
            && ((List) request.getRequest().get(JsonKey.TAGS)).isEmpty())) {
      throw new ProjectCommonException(
          ResponseCode.invalidTags.getErrorCode(),
          ResponseCode.invalidTags.getErrorMessage(),
          ERROR_CODE);
    } else if (request.getRequest().get(JsonKey.TAGS) instanceof String) {
      throw new ProjectCommonException(
          ResponseCode.invalidTags.getErrorCode(),
          ResponseCode.invalidTags.getErrorMessage(),
          ERROR_CODE);
    }
  }

  /**
   * Validates the note ID.
   *
   * @param noteId The note ID string.
   * @throws ProjectCommonException If validation fails.
   */
  public static void validateNoteId(String noteId) {
    if (StringUtils.isBlank(noteId)) {
      throw createExceptionInstance(ResponseCode.invalidNoteId.getErrorCode());
    }
  }

  /**
   * Validates the request data for registering a client.
   *
   * @param request The request object.
   * @throws ProjectCommonException If validation fails.
   */
  public static void validateRegisterClient(Request request) {

    if (StringUtils.isBlank((String) request.getRequest().get(JsonKey.CLIENT_NAME))) {
      throw createExceptionInstance(ResponseCode.invalidClientName.getErrorCode());
    }
  }

  /**
   * Validates the request data for updating the client key.
   *
   * @param clientId The client ID.
   * @param masterAccessToken The master access token.
   * @throws ProjectCommonException If validation fails.
   */
  public static void validateUpdateClientKey(String clientId, String masterAccessToken) {
    validateClientId(clientId);
    if (StringUtils.isBlank(masterAccessToken)) {
      throw createExceptionInstance(ResponseCode.invalidRequestData.getErrorCode());
    }
  }

  /**
   * Validates the request data for getting the client key.
   *
   * @param id The client ID.
   * @param type The client type.
   * @throws ProjectCommonException If validation fails.
   */
  public static void validateGetClientKey(String id, String type) {
    validateClientId(id);
    if (StringUtils.isBlank(type)) {
      throw createExceptionInstance(ResponseCode.invalidRequestData.getErrorCode());
    }
  }

  /**
   * Validates the client ID.
   *
   * @param clientId The client ID string.
   * @throws ProjectCommonException If validation fails.
   */
  public static void validateClientId(String clientId) {
    if (StringUtils.isBlank(clientId)) {
      throw createExceptionInstance(ResponseCode.invalidClientId.getErrorCode());
    }
  }

  /**
   * Validates the request data for sending notifications.
   *
   * @param request The request object.
   * @throws ProjectCommonException If validation fails.
   */
  @SuppressWarnings("unchecked")
  public static void validateSendNotification(Request request) {
    if (StringUtils.isBlank((String) request.getRequest().get(JsonKey.TO))) {
      throw createExceptionInstance(ResponseCode.invalidTopic.getErrorCode());
    }
    if (request.getRequest().get(JsonKey.DATA) == null
        || !(request.getRequest().get(JsonKey.DATA) instanceof Map)
        || ((Map<String, Object>) request.getRequest().get(JsonKey.DATA)).size() == 0) {
      throw createExceptionInstance(ResponseCode.invalidTopicData.getErrorCode());
    }

    if (StringUtils.isBlank((String) request.getRequest().get(JsonKey.TYPE))) {
      throw createExceptionInstance(ResponseCode.invalidNotificationType.getErrorCode());
    }
    if (!(JsonKey.FCM.equalsIgnoreCase((String) request.getRequest().get(JsonKey.TYPE)))) {
      throw createExceptionInstance(ResponseCode.notificationTypeSupport.getErrorCode());
    }
  }

  /**
   * Validates the request data for getting user count.
   *
   * @param request The request object.
   * @throws ProjectCommonException If validation fails.
   */
  @SuppressWarnings("rawtypes")
  public static void validateGetUserCount(Request request) {
    if (!validateListType(request, JsonKey.LOCATION_IDS)) {
      throw createDataTypeException(
          ResponseCode.dataTypeError.getErrorCode(), JsonKey.LOCATION_IDS, JsonKey.LIST);
    }
    if (null == request.getRequest().get(JsonKey.LOCATION_IDS)
        && ((List) request.getRequest().get(JsonKey.LOCATION_IDS)).isEmpty()) {
      throw createExceptionInstance(ResponseCode.locationIdRequired.getErrorCode());
    }

    if (!validateBooleanType(request, JsonKey.USER_LIST_REQ)) {
      throw createDataTypeException(
          ResponseCode.dataTypeError.getErrorCode(), JsonKey.USER_LIST_REQ, "Boolean");
    }

    if (null != request.getRequest().get(JsonKey.USER_LIST_REQ)
        && (Boolean) request.getRequest().get(JsonKey.USER_LIST_REQ)) {
      throw createExceptionInstance(ResponseCode.functionalityMissing.getErrorCode());
    }

    if (!validateBooleanType(request, JsonKey.ESTIMATED_COUNT_REQ)) {
      throw createDataTypeException(
          ResponseCode.dataTypeError.getErrorCode(), JsonKey.ESTIMATED_COUNT_REQ, "Boolean");
    }

    if (null != request.getRequest().get(JsonKey.ESTIMATED_COUNT_REQ)
        && (Boolean) request.getRequest().get(JsonKey.ESTIMATED_COUNT_REQ)) {
      throw createExceptionInstance(ResponseCode.functionalityMissing.getErrorCode());
    }
  }

  /**
   * Validates if the request contains the key and the value is a list.
   *
   * @param request The request object.
   * @param key The key to check.
   * @return True if valid, false otherwise.
   */
  private static boolean validateListType(Request request, String key) {
    return !(request.getRequest().containsKey(key)
        && null != request.getRequest().get(key)
        && !(request.getRequest().get(key) instanceof List));
  }

  /**
   * Validates if the request contains the key and the value is a boolean.
   *
   * @param request The request object.
   * @param key The key to check.
   * @return True if valid, false otherwise.
   */
  private static boolean validateBooleanType(Request request, String key) {
    return !(request.getRequest().containsKey(key)
        && null != request.getRequest().get(key)
        && !(request.getRequest().get(key) instanceof Boolean));
  }

  private static ProjectCommonException createDataTypeException(
      String errorCode, String key1, String key2) {
    return new ProjectCommonException(
        ResponseCode.getResponse(errorCode).getErrorCode(),
        ProjectUtil.formatMessage(
            ResponseCode.getResponse(errorCode).getErrorMessage(), key1, key2),
        ERROR_CODE);
  }

  private static ProjectCommonException createExceptionInstance(String errorCode) {
    return new ProjectCommonException(
        ResponseCode.getResponse(errorCode).getErrorCode(),
        ResponseCode.getResponse(errorCode).getErrorMessage(),
        ERROR_CODE);
  }

  /**
   * Validates the request data for group activity aggregates.
   *
   * @param request The request object.
   * @throws ProjectCommonException If validation fails.
   */
  public static void validateGroupActivityAggregatesRequest(Request request) {
    try {
      String message = "";
      if (null == request || MapUtils.isEmpty(request.getRequest())) {
        message += "Error due to missing request body";
        ProjectCommonException.throwClientErrorException(
            ResponseCode.invalidRequestData,
            MessageFormat.format(ResponseCode.invalidRequestData.getErrorMessage(), message));
      }
      if (StringUtils.isBlank((String) request.get(JsonKey.GROUPID))) {
        message += "Error due to missing groupId";
        ProjectCommonException.throwClientErrorException(
            ResponseCode.groupIdMismatch,
            MessageFormat.format(ResponseCode.groupIdMismatch.getErrorMessage(), message));
      }
      if (StringUtils.isBlank((String) request.get(JsonKey.ACTIVITYID))) {
        message += "Error due to missing activityId";
        ProjectCommonException.throwClientErrorException(
            ResponseCode.activityIdMismatch,
            MessageFormat.format(ResponseCode.activityIdMismatch.getErrorMessage(), message));
      }
      if (StringUtils.isBlank((String) request.get(JsonKey.ACTIVITYTYPE))) {
        message += "Error due to missing activity type";
        ProjectCommonException.throwClientErrorException(
            ResponseCode.activityTypeMismatch,
            MessageFormat.format(ResponseCode.activityTypeMismatch.getErrorMessage(), message));
      }
    } catch (Exception ex) {
      throw ex;
    }
  }
}
