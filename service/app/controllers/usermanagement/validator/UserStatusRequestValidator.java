package controllers.usermanagement.validator;

import org.sunbird.validator.BaseRequestValidator;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;

public class UserStatusRequestValidator extends BaseRequestValidator {

  public void validateBlockUserRequest(Request request) {
    validateUserId((String) request.getRequest().get(JsonKey.USER_ID));
  }

  public void validateUnblockUserRequest(Request request) {
    validateUserId((String) request.getRequest().get(JsonKey.USER_ID));
  }

  public void validateUserId(String userId) {
    validateParam(userId, ResponseCode.mandatoryParamsMissing, JsonKey.USER_ID);
  }
  
}
