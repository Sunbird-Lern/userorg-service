package controllers.skills.validator;

import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

public class UserSkillEndorsementRequestValidator extends BaseRequestValidator {

  public void validateSkillEndorsementRequest(Request request) {
    validateParam(
        (String) request.getRequest().get(JsonKey.USER_ID),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.USER_ID);
    validateParam(
        (String) request.getRequest().get(JsonKey.ENDORSED_USER_ID),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.ENDORSED_USER_ID);
    validateParam(
        (String) request.getRequest().get(JsonKey.SKILL_NAME),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.SKILL_NAME);

    validateUserId(request, JsonKey.USER_ID);
    validateSelfEndorsement(request);
  }

  private void validateSelfEndorsement(Request request) {
    String endorsedUserId = (String) request.getRequest().get(JsonKey.ENDORSED_USER_ID);
    String userId = (String) request.getRequest().get(JsonKey.USER_ID);
    if (userId.equalsIgnoreCase(endorsedUserId)) {
      throw new ProjectCommonException(
          ResponseCode.invalidDuplicateValue.getErrorCode(),
          ResponseCode.invalidDuplicateValue.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode(),
          JsonKey.USER_ID,
          JsonKey.ENDORSED_USER_ID);
    }
  }
}
