package controllers.notesmanagement.validator;

import java.util.List;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

/** Created by rajatgupta on 04/09/18. */
public class NoteValidator extends BaseRequestValidator {

  public void validateNote(Request request) {
    validateUserId(request, JsonKey.USER_ID);
    validateParam(
        (String) request.get("userId"), ResponseCode.mandatoryParamsMissing, JsonKey.USER_ID);
    validateParam(
        (String) request.get("title"), ResponseCode.mandatoryParamsMissing, JsonKey.TITLE);
    validateParam((String) request.get("note"), ResponseCode.mandatoryParamsMissing, JsonKey.NOTE);
    validateParam(
        (String) request.get("contentId"), ResponseCode.mandatoryParamsMissing, JsonKey.CONTENT_ID);
    if (request.getRequest().containsKey("tags")
        && request.getRequest().get("tags") instanceof List
        && ((List) request.getRequest().get("tags")).isEmpty()) {
      throw new ProjectCommonException(
          ResponseCode.invalidTags.getErrorCode(),
          ResponseCode.invalidTags.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    if (request.getRequest().get("tags") instanceof String) {
      throw new ProjectCommonException(
          ResponseCode.invalidTags.getErrorCode(),
          ResponseCode.invalidTags.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  public void validateNoteId(String noteId) {
    validateParam(noteId, ResponseCode.invalidNoteId);
  }
}
