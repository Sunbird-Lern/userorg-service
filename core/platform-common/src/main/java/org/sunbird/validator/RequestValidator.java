package org.sunbird.validator;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.exception.ResponseMessage;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.StringFormatter;

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
          ResponseCode.mandatoryParamsMissing,
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
          ResponseCode.mandatoryParamsMissing,
          ProjectUtil.formatMessage(
              ResponseCode.mandatoryParamsMissing.getErrorMessage(), JsonKey.FILE),
          ERROR_CODE);
    }
  }

  public static void validateSyncRequest(Request request) {
    if (request.getRequest().get(JsonKey.OBJECT_TYPE) == null) {
      throw new ProjectCommonException(
          ResponseCode.dataTypeError, ResponseCode.dataTypeError.getErrorMessage(), ERROR_CODE);
    }
    List<String> list =
        new ArrayList<>(
            Arrays.asList(new String[] {JsonKey.USER, JsonKey.ORGANISATION, JsonKey.LOCATION}));
    if (!list.contains(request.getRequest().get(JsonKey.OBJECT_TYPE))) {
      throw new ProjectCommonException(
          ResponseCode.invalidObjectType,
          ResponseCode.invalidObjectType.getErrorMessage(),
          ERROR_CODE);
    }
  }

  public static void validateSendMail(Request request) {
    if (StringUtils.isBlank((String) request.getRequest().get(JsonKey.SUBJECT))) {
      throw new ProjectCommonException(
          ResponseCode.mandatoryParamsMissing,
          String.format(ResponseCode.mandatoryParamsMissing.getErrorMessage(), JsonKey.SUBJECT),
          ERROR_CODE);
    }
    if (StringUtils.isBlank((String) request.getRequest().get(JsonKey.BODY))) {
      throw new ProjectCommonException(
          ResponseCode.mandatoryParamsMissing,
          String.format(ResponseCode.mandatoryParamsMissing.getErrorMessage(), JsonKey.BODY),
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
          ResponseCode.mandatoryParamsMissing,
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
          ResponseCode.mandatoryParamsMissing,
          String.format(ResponseCode.mandatoryParamsMissing.getErrorMessage(), JsonKey.CONTAINER),
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
          ResponseCode.mandatoryParamsMissing,
          String.format(ResponseCode.mandatoryParamsMissing.getErrorMessage(), JsonKey.USER_ID),
          ERROR_CODE);
    }
    if (StringUtils.isBlank((String) request.get(JsonKey.TITLE))) {
      throw new ProjectCommonException(
          ResponseCode.mandatoryParamsMissing,
          String.format(ResponseCode.mandatoryParamsMissing.getErrorMessage(), JsonKey.TITLE),
          ERROR_CODE);
    }
    if (StringUtils.isBlank((String) request.get(JsonKey.NOTE))) {
      throw new ProjectCommonException(
          ResponseCode.mandatoryParamsMissing,
          String.format(ResponseCode.mandatoryParamsMissing.getErrorMessage(), JsonKey.NOTE),
          ERROR_CODE);
    }
    if (StringUtils.isBlank((String) request.get(JsonKey.CONTENT_ID))
        && StringUtils.isBlank((String) request.get(JsonKey.COURSE_ID))) {
      throw new ProjectCommonException(
          ResponseCode.mandatoryParamsMissing,
          String.format(
              ResponseCode.mandatoryParamsMissing.getErrorMessage(),
              JsonKey.CONTENT_ID + "," + JsonKey.COURSE_ID),
          ERROR_CODE);
    }
    if (request.getRequest().containsKey(JsonKey.TAGS)
        && ((request.getRequest().get(JsonKey.TAGS) instanceof List)
            && ((List) request.getRequest().get(JsonKey.TAGS)).isEmpty())) {
      throw new ProjectCommonException(
          ResponseCode.errorMandatoryParamsEmpty,
          String.format(ResponseCode.errorMandatoryParamsEmpty.getErrorMessage(), JsonKey.TAGS),
          ERROR_CODE);
    } else if (request.getRequest().get(JsonKey.TAGS) instanceof String) {
      throw new ProjectCommonException(
          ResponseCode.invalidParameterValue,
          String.format(ResponseCode.invalidParameterValue.getErrorMessage(), JsonKey.TAGS),
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
      throw new ProjectCommonException(
          ResponseCode.invalidParameterValue,
          String.format(ResponseCode.invalidParameterValue.getErrorMessage(), JsonKey.NOTE_ID),
          ERROR_CODE);
    }
  }
}
