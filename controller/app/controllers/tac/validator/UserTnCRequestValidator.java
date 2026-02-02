package controllers.tac.validator;

import java.text.MessageFormat;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.common.ProjectUtil;
import org.sunbird.validators.BaseRequestValidator;

public class UserTnCRequestValidator extends BaseRequestValidator {

  public void validateTnCRequest(Request request) {
    validateParam(
        (String) request.get(JsonKey.VERSION),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.VERSION);

    // if managedUserId's terms and conditions are accepted, validate userId from request
    String managedUserId = (String) request.getRequest().get(JsonKey.USER_ID);
    if (StringUtils.isNotBlank(managedUserId) && !ProjectUtil.validateUUID(managedUserId)) {
      throw new ProjectCommonException(
          ResponseCode.invalidPropertyError,
          MessageFormat.format(
              ResponseCode.invalidPropertyError.getErrorMessage(), JsonKey.USER_ID),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }
}
