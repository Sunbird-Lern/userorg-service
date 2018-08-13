package controllers.skills.validator;

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
    validateUserId(request);
  }

  private static void validateUserId(Request request) {
    if (request
        .getRequest()
        .get(JsonKey.USER_ID)
        .equals(request.getContext().get(JsonKey.USER_ID))) {

      throw new ProjectCommonException(
          ResponseCode.invalidParameterValue.getErrorCode(),
          ResponseCode.invalidParameterValue.getErrorMessage(),
          ResponseCode.invalidParameterValue.getResponseCode(),
          (String) request.getRequest().get(JsonKey.USER_ID),
          JsonKey.USER_ID);
    }
  }
}
