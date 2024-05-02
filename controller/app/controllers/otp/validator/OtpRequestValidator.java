package controllers.otp.validator;

import org.apache.commons.lang3.StringUtils;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.exception.ResponseMessage;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.StringFormatter;
import org.sunbird.validator.BaseRequestValidator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class OtpRequestValidator extends BaseRequestValidator {

  private final List<String> allowedTemplate =
      Arrays.asList(
          JsonKey.RESET_PASSWORD_TEMPLATE_ID,
          JsonKey.WARD_LOGIN_OTP_TEMPLATE_ID,
          JsonKey.CONTACT_UPDATE_TEMPLATE_ID,
          JsonKey.OTP_DELETE_USER_TEMPLATE_ID);

  public void validateGenerateOtpRequest(Request otpRequest) {
    commonValidation(otpRequest, false);
    validateTemplateId(otpRequest);
  }

  private void validateTemplateId(Request otpRequest) {
    String templateId = (String) otpRequest.getRequest().get(JsonKey.TEMPLATE_ID);
    if (StringUtils.isNotBlank(templateId) && !allowedTemplate.contains(templateId)) {
      throw new ProjectCommonException(
          ResponseCode.invalidIdentifier,
          ProjectUtil.formatMessage(
              ResponseMessage.Message.INVALID_PARAMETER_VALUE, templateId, JsonKey.TEMPLATE_ID),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
  }

  public void validateVerifyOtpRequest(Request otpRequest) {
    commonValidation(otpRequest, true);
  }

  private void commonValidation(Request otpRequest, boolean isOtpMandatory) {
    validateParam(
        (String) otpRequest.getRequest().get(JsonKey.KEY),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.KEY);
    validateParam(
        (String) otpRequest.getRequest().get(JsonKey.TYPE),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.TYPE);
    if (isOtpMandatory) {
      validateParam(
          (String) otpRequest.getRequest().get(JsonKey.OTP),
          ResponseCode.mandatoryParamsMissing,
          JsonKey.OTP);
    }
    validateTypeAndKey(otpRequest);
  }

  private void validateTypeAndKey(Request otpRequest) {
    Map<String, Object> requestMap = otpRequest.getRequest();
    String userId = (String) requestMap.get(JsonKey.USER_ID);
    String type = (String) requestMap.get(JsonKey.TYPE);
    String key = (String) requestMap.get(JsonKey.KEY);
    validateType(type);
    if (StringUtils.isBlank(userId)) {
      if (JsonKey.EMAIL.equalsIgnoreCase(type)) {
        validateEmail(key);
      } else if (JsonKey.PHONE.equalsIgnoreCase(type)) {
        validatePhone(key);
      }
    }
  }

  private void validateType(String type) {
    List<String> allowedTypes =
        new ArrayList<String>(
            Arrays.asList(
                JsonKey.EMAIL,
                JsonKey.PHONE,
                JsonKey.PREV_USED_EMAIL,
                JsonKey.PREV_USED_PHONE,
                JsonKey.RECOVERY_EMAIL,
                JsonKey.RECOVERY_PHONE));
    if (!allowedTypes.contains(type)) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.invalidValue,
          ProjectUtil.formatMessage(
              ResponseCode.invalidValue.getErrorMessage(),
              JsonKey.TYPE,
              type,
              String.join(
                  StringFormatter.COMMA,
                  JsonKey.EMAIL,
                  JsonKey.PHONE,
                  JsonKey.PREV_USED_EMAIL,
                  JsonKey.PREV_USED_PHONE,
                  JsonKey.RECOVERY_EMAIL,
                  JsonKey.RECOVERY_PHONE)));
    }
  }
}
