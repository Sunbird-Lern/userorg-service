package org.sunbird.common.request;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.util.StringFormatter;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.common.responsecode.ResponseMessage;

/**
 * This call will do validation for all incoming request data.
 *
 * @author Manzarul
 */
public final class RequestValidator {
  private static final int ERROR_CODE = ResponseCode.CLIENT_ERROR.getResponseCode();

  private RequestValidator() {}

  /**
   * This method will validate bulk user upload requested data.
   *
   * @param reqObj Request
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

  public static void validateSyncRequest(Request request) {
    if (request.getRequest().get(JsonKey.OBJECT_TYPE) == null) {
      throw new ProjectCommonException(
          ResponseCode.dataTypeError.getErrorCode(),
          ResponseCode.dataTypeError.getErrorMessage(),
          ERROR_CODE);
    }
    List<String> list =
        new ArrayList<>(
            Arrays.asList(new String[] {JsonKey.USER, JsonKey.ORGANISATION, JsonKey.LOCATION}));
    if (!list.contains(request.getRequest().get(JsonKey.OBJECT_TYPE))) {
      throw new ProjectCommonException(
          ResponseCode.invalidObjectType.getErrorCode(),
          ResponseCode.invalidObjectType.getErrorMessage(),
          ERROR_CODE);
    }
  }

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

  public static void validateFileUpload(Request reqObj) {

    if (StringUtils.isBlank((String) reqObj.get(JsonKey.CONTAINER))) {
      throw new ProjectCommonException(
          ResponseCode.storageContainerNameMandatory.getErrorCode(),
          ResponseCode.storageContainerNameMandatory.getErrorMessage(),
          ERROR_CODE);
    }
  }

  /**
   * Method to validate not for userId, title, note, courseId, contentId and tags
   *
   * @param request
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
   * Method to validate noteId
   *
   * @param noteId
   */
  public static void validateNoteId(String noteId) {
    if (StringUtils.isBlank(noteId)) {
      throw createExceptionInstance(ResponseCode.invalidNoteId.getErrorCode());
    }
  }

  private static ProjectCommonException createExceptionInstance(String errorCode) {
    return new ProjectCommonException(
        ResponseCode.getResponse(errorCode).getErrorCode(),
        ResponseCode.getResponse(errorCode).getErrorMessage(),
        ERROR_CODE);
  }
}
