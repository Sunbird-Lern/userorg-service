package controllers.datasecurity.validator;

import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

public class DataSecurityRequestValidator extends BaseRequestValidator {

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
