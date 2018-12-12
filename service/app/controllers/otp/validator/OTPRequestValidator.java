package controllers.otp.validator;

import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.EmailValidator;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

public class OTPRequestValidator extends BaseRequestValidator {
  private static final int ERROR_CODE = ResponseCode.CLIENT_ERROR.getResponseCode();

  public void validateGenerateOTPRequest(Request otpRequest) {
    commonValidation(otpRequest);
  }

  public void validateVerifyOTPRequest(Request otpRequest) {
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

    validateKeyFormat(otpRequest);
  }

  private void validateKeyFormat(Request otpRequest) {
    if (JsonKey.EMAIL.equalsIgnoreCase((String) otpRequest.getRequest().get(JsonKey.TYPE))) {
      if (!EmailValidator.isEmailValid((String) otpRequest.getRequest().get(JsonKey.KEY))) {
        throw new ProjectCommonException(
            ResponseCode.emailFormatError.getErrorCode(),
            ResponseCode.emailFormatError.getErrorMessage(),
            ERROR_CODE);
      }
    } else if (JsonKey.PHONE.equalsIgnoreCase((String) otpRequest.getRequest().get(JsonKey.TYPE))) {
      if (!ProjectUtil.validatePhoneNumber((String) otpRequest.getRequest().get(JsonKey.KEY))) {
        throw new ProjectCommonException(
            ResponseCode.phoneNoFormatError.getErrorCode(),
            ResponseCode.phoneNoFormatError.getErrorMessage(),
            ERROR_CODE);
      }
    } else {
      throw new ProjectCommonException(
          ResponseCode.invalidTypeValue.getErrorCode(),
          ResponseCode.invalidTypeValue.getErrorMessage(),
          ERROR_CODE);
    }
  }
}
