package controllers.tac.validator;

import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

public class UserTnCRequestValidator extends BaseRequestValidator {

  public void validateTnCRequest(Request request) {
    validateParam(
        (String) request.get(JsonKey.VERSION),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.VERSION);
  }
}
