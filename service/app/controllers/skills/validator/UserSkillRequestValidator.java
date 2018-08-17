package controllers.skills.validator;

import java.util.List;
import org.apache.commons.collections.CollectionUtils;
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

    if (CollectionUtils.isEmpty((List<String>) request.getRequest().get(JsonKey.SKILLS))) {
      throw new ProjectCommonException(
          ResponseCode.invalidParameterValue.getErrorCode(),
          ResponseCode.invalidParameterValue.getErrorMessage(),
          ResponseCode.invalidParameterValue.getResponseCode(),
          "[]",
          JsonKey.SKILLS);
    }

    validateUserId(request, JsonKey.USER_ID);
  }
}
