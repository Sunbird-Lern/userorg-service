package controllers.usermanagement.validator;

import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

public class UserStatusRequestValidator extends BaseRequestValidator {

  public void validateBlockUser(Request request) {
    validateUserId((String) request.getRequest().get(JsonKey.USER_ID));
  }

  public void validateUnBlockUser(Request request) {
    validateUserId((String) request.getRequest().get(JsonKey.USER_ID));
  }

  public void validateUserId(String userId) {
    if (ProjectUtil.isNull(userId)) {
      throw new ProjectCommonException(
          ResponseCode.invalidRequestData.getErrorCode(),
          ResponseCode.invalidRequestData.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }
}
