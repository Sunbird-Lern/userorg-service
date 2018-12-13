package controllers.otp.validator;

import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

public class OTPRequestValidator extends BaseRequestValidator {

  public void validateGenerateOTPRequest(Request otpRequest) {
    validateParam(
        (String) otpRequest.getRequest().get(JsonKey.KEY),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.KEY);
    validateParam(
        (String) otpRequest.getRequest().get(JsonKey.TYPE),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.TYPE);

    validateKeyFormat(otpRequest);
  }

  private void validateKeyFormat(Request otpRequest) {
    Map<String, Object> requestMap = otpRequest.getRequest();

    String type = (String) requestMap.get(JsonKey.TYPE);
    String key = (String) requestMap.get(JsonKey.KEY);

    if (JsonKey.EMAIL.equalsIgnoreCase(type)) {
      validateEmail(key);
    } else if (JsonKey.PHONE.equalsIgnoreCase(type)) {
      validatePhone(key);
    } else {
      ProjectCommonException.throwClientErrorException(ResponseCode.invalidTypeValue);
    }
  }

}
