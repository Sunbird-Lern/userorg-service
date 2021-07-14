package org.sunbird.validator.systemsettings;

import org.sunbird.validator.BaseRequestValidator;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;

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
