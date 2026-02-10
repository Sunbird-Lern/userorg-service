package org.sunbird.validator.systemsettings;

import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.validators.BaseRequestValidator;

public class SystemSettingsRequestValidator extends BaseRequestValidator {
  public void validateSetSystemSetting(Request request) {
    validateParam(
        (String) request.getRequest().get(JsonKey.ID),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.ID);
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
