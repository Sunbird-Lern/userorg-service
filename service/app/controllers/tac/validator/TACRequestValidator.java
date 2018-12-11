package controllers.tac.validator;

import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

/** Created by rajatgupta on 11/12/18. */
public class TACRequestValidator extends BaseRequestValidator {

  public void validateTACRequest(Request request) {
    validateParam(
        (String) request.get(JsonKey.VERSION),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.VERSION);
  }
}
