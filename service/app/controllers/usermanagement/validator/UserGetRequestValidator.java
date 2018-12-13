package controllers.usermanagement.validator;

import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

/** Created by rajatgupta on 13/12/18. */
public class UserGetRequestValidator extends BaseRequestValidator {

  public void validateGetRequest(Request request) {
    validateParam(
        (String) request.getRequest().get(JsonKey.FIELD),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.FIELD);
    validateParam(
        (String) request.getRequest().get(JsonKey.VALUE),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.VALUE);
  }
}
