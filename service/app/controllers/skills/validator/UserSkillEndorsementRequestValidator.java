package controllers.skills.validator;

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
        (String) request.getRequest().get("skillId"),
        ResponseCode.mandatoryParamsMissing,
        "skillId");

    validateUserId(request, JsonKey.USER_ID);
  }
}
