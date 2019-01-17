package controllers.usermanagement.validator;

import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.StringFormatter;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

public class UserGetRequestValidator extends BaseRequestValidator {

  public void validateGetUserByKeyRequest(Request request) {
    String key = (String) request.getRequest().get(JsonKey.KEY);

    validateParam(key, ResponseCode.mandatoryParamsMissing, JsonKey.KEY);

    validateParam(
        (String) request.getRequest().get(JsonKey.VALUE),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.VALUE);

    if (!(key.equalsIgnoreCase(JsonKey.PHONE)
        || key.equalsIgnoreCase(JsonKey.EMAIL)
        || key.equalsIgnoreCase(JsonKey.LOGIN_ID))) {

      ProjectCommonException.throwClientErrorException(
          ResponseCode.invalidValue,
          ProjectUtil.formatMessage(
              ResponseCode.invalidValue.getErrorMessage(),
              JsonKey.KEY,
              key,
              String.join(StringFormatter.COMMA, JsonKey.EMAIL, JsonKey.PHONE, JsonKey.LOGIN_ID)));
    }

    if (JsonKey.PHONE.equals(request.get(JsonKey.KEY))) {
      validatePhone((String) request.get(JsonKey.VALUE));
    }
    if (JsonKey.EMAIL.equals(request.get(JsonKey.KEY))) {
      validateEmail((String) request.get(JsonKey.VALUE));
    }
  }

  public void validateGetUserByExternalIdRequest(Request request) {
    validateParam(
        (String) request.getRequest().get(JsonKey.ID_TYPE),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.ID_TYPE);
    validateParam(
        (String) request.getRequest().get(JsonKey.EXTERNAL_ID),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.EXTERNAL_ID);
    validateParam(
        (String) request.getRequest().get(JsonKey.PROVIDER),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.PROVIDER);
  }
}
