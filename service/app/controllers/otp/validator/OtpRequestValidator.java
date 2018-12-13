package controllers.otp.validator;

import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

public class OtpRequestValidator extends BaseRequestValidator {

  public void validateGenerateOtpRequest(Request otpRequest) {
    commonValidation(otpRequest);
  }

  public void validateVerifyOtpRequest(Request otpRequest) {
    commonValidation(otpRequest);
    validateParam(
        (String) otpRequest.getRequest().get(JsonKey.OTP),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.OTP);
  }

  private void commonValidation(Request otpRequest) {
    validateParam(
        (String) otpRequest.getRequest().get(JsonKey.KEY),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.KEY);
    validateParam(
        (String) otpRequest.getRequest().get(JsonKey.TYPE),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.TYPE);

    validateDataFormat(otpRequest);
  }

  private void validateDataFormat(Request otpRequest) {
    if (JsonKey.EMAIL.equalsIgnoreCase((String) otpRequest.getRequest().get(JsonKey.TYPE))) {
      validateEmail((String) otpRequest.getRequest().get(JsonKey.KEY));
    } else if (JsonKey.PHONE.equalsIgnoreCase((String) otpRequest.getRequest().get(JsonKey.TYPE))) {
      validatePhone((String) otpRequest.getRequest().get(JsonKey.KEY));
    } else {
      ProjectCommonException.throwClientErrorException(ResponseCode.invalidTypeValue);
    }
  }
}
