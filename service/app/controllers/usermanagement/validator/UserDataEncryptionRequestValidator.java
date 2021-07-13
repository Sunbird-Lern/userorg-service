package controllers.usermanagement.validator;

import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;

public class UserDataEncryptionRequestValidator extends BaseRequestValidator {

  public void validateEncryptRequest(Request request) {
    commonValidation(request);
  }

  public void validateDecryptRequest(Request request) {
    commonValidation(request);
  }

  private void commonValidation(Request request) {
    if (request.getRequest().get(JsonKey.USER_IDs) == null) {
      validateParam(null, ResponseCode.mandatoryParamsMissing, JsonKey.USER_IDs);
    }
    validateListParam(request.getRequest(), JsonKey.USER_IDs);
  }
}
