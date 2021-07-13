package controllers.usermanagement.validator;

import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.StringFormatter;
import org.sunbird.validator.BaseRequestValidator;
import org.sunbird.request.Request;
import play.mvc.Http;
import util.CaptchaHelper;

public class UserGetRequestValidator extends BaseRequestValidator {
  private static LoggerUtil logger = new LoggerUtil(UserGetRequestValidator.class);

  public void validateGetUserByKeyRequest(Request request) {
    String key = (String) request.getRequest().get(JsonKey.KEY);

    validateParam(key, ResponseCode.mandatoryParamsMissing, JsonKey.KEY);

    validateParam(
        (String) request.getRequest().get(JsonKey.VALUE),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.VALUE);

    if (!(key.equalsIgnoreCase(JsonKey.PHONE)
        || key.equalsIgnoreCase(JsonKey.EMAIL)
        || key.equalsIgnoreCase(JsonKey.LOGIN_ID)
        || key.equalsIgnoreCase(JsonKey.USERNAME))) {

      ProjectCommonException.throwClientErrorException(
          ResponseCode.invalidValue,
          ProjectUtil.formatMessage(
              ResponseCode.invalidValue.getErrorMessage(),
              JsonKey.KEY,
              key,
              String.join(
                  StringFormatter.COMMA,
                  JsonKey.EMAIL,
                  JsonKey.PHONE,
                  JsonKey.LOGIN_ID,
                  JsonKey.USERNAME)));
    }

    if (JsonKey.PHONE.equals(request.get(JsonKey.KEY))) {
      validatePhone((String) request.get(JsonKey.VALUE));
    }
    if (JsonKey.EMAIL.equals(request.get(JsonKey.KEY))) {
      validateEmail((String) request.get(JsonKey.VALUE));
    }
  }

  public void validateGetUserByKeyRequestaWithCaptcha(Request request, Http.Request httpRequest) {
    String captcha = httpRequest.getQueryString(JsonKey.CAPTCHA_RESPONSE);
    logger.info("QueryString: "+httpRequest.uri());
    logger.info("Captach: "+captcha);
    String mobileApp = httpRequest.getQueryString(JsonKey.MOBILE_APP);
    if (Boolean.parseBoolean(ProjectUtil.getConfigValue(JsonKey.ENABLE_CAPTCHA))
        && !new CaptchaHelper().validate(captcha, mobileApp)) {
      throw new ProjectCommonException(
          ResponseCode.invalidCaptcha.getErrorCode(),
          ResponseCode.invalidCaptcha.getErrorMessage(),
          ResponseCode.IM_A_TEAPOT.getResponseCode());
    }
    validateGetUserByKeyRequest(request);
  }
}
