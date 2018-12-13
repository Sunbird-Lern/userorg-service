package controllers.usermanagement.validator;

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
  }

}
