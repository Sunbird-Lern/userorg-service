package controllers.tac.validator;

import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

import java.text.MessageFormat;

public class UserTnCRequestValidator extends BaseRequestValidator {

  public void validateTnCRequest(Request request) {
    validateParam(
        (String) request.get(JsonKey.VERSION),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.VERSION);

    //When managedProfile terms and conditions accepted, pick userId from request
    String requestUserId = (String) request.getRequest().get(JsonKey.USER_ID);
    if (StringUtils.isNotBlank(requestUserId) && !ProjectUtil.validateUUID(requestUserId)){
      throw new ProjectCommonException(
              ResponseCode.invalidPropertyError.getErrorCode(),
              MessageFormat.format(ResponseCode.invalidPropertyError.getErrorMessage(), JsonKey.USER_ID),
              ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }
}
