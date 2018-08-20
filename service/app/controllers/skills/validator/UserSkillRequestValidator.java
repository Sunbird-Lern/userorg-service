package controllers.skills.validator;

import java.util.List;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

public class UserSkillRequestValidator extends BaseRequestValidator {

  public void validateUpdateSkillRequest(Request request) {
    validateParam(
        (String) request.getRequest().get(JsonKey.USER_ID),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.USER_ID);

    if (request.getRequest().get(JsonKey.SKILLS) == null) {
      throw new ProjectCommonException(
          ResponseCode.mandatoryParamsMissing.getErrorCode(),
          ResponseCode.mandatoryParamsMissing.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode(),
          JsonKey.SKILLS);
    }
    if (!(request.getRequest().get(JsonKey.SKILLS) instanceof List)) {
      throw new ProjectCommonException(
          ResponseCode.dataTypeError.getErrorCode(),
          ResponseCode.dataTypeError.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode(),
          JsonKey.SKILLS,
          "List");
    }

    validateUserId(request, JsonKey.USER_ID);
  }
}
