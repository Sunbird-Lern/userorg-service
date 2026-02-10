package controllers.notesmanagement.validator;

import java.text.MessageFormat;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.validators.BaseRequestValidator;

public class NoteRequestValidator extends BaseRequestValidator {

  public void validateNote(Request request) {

    validateParam(
        (String) request.get(JsonKey.USER_ID),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.USER_ID);
    validateParam(
        (String) request.get(JsonKey.TITLE), ResponseCode.mandatoryParamsMissing, JsonKey.TITLE);
    validateParam(
        (String) request.get(JsonKey.NOTE), ResponseCode.mandatoryParamsMissing, JsonKey.NOTE);
    if (StringUtils.isBlank((String) request.get(JsonKey.CONTENT_ID))
        && StringUtils.isBlank((String) request.get(JsonKey.COURSE_ID))) {
      throw new ProjectCommonException(
          ResponseCode.invalidParameter,
          MessageFormat.format(
              ResponseCode.invalidParameter.getErrorMessage(),
              JsonKey.CONTENT_ID + " or " + JsonKey.COURSE_ID),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    if (request.getRequest().containsKey(JsonKey.TAGS)
        && request.getRequest().get(JsonKey.TAGS) instanceof List
        && ((List) request.getRequest().get(JsonKey.TAGS)).isEmpty()) {
      throw new ProjectCommonException(
          ResponseCode.invalidParameter,
          MessageFormat.format(ResponseCode.invalidParameter.getErrorMessage(), JsonKey.TAGS),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    if (request.getRequest().get(JsonKey.TAGS) instanceof String) {
      throw new ProjectCommonException(
          ResponseCode.invalidParameter,
          MessageFormat.format(ResponseCode.invalidParameter.getErrorMessage(), JsonKey.TAGS),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    validateUserId(request, JsonKey.USER_ID);
  }

  public void validateNoteId(String noteId) {
    validateParam(
        noteId,
        ResponseCode.invalidParameter,
        MessageFormat.format(ResponseCode.invalidParameter.getErrorMessage(), JsonKey.NOTE_ID));
  }
}
