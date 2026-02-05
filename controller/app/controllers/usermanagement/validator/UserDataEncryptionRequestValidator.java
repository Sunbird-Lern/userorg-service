package controllers.usermanagement.validator;

import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.validators.BaseRequestValidator;

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
