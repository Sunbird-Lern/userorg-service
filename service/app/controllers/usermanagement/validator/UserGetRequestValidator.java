package controllers.usermanagement.validator;

import java.text.MessageFormat;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

public class UserGetRequestValidator extends BaseRequestValidator {

  public void validateGetUserByKeyRequest(Request request) {
    validateParam(
        (String) request.getRequest().get(JsonKey.KEY),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.KEY);
    validateParam(
        (String) request.getRequest().get(JsonKey.VALUE),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.VALUE);
    String fieldValue = (String) request.getRequest().get(JsonKey.FIELD);
    if (!(fieldValue.equalsIgnoreCase(JsonKey.PHONE)
        || fieldValue.equalsIgnoreCase(JsonKey.EMAIL)
        || fieldValue.equalsIgnoreCase(JsonKey.USER_NAME))) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.invalidValue,
          MessageFormat.format(
              ResponseCode.invalidValue.getErrorMessage(), new Object[] {JsonKey.FIELD}));
    }
  }

}
