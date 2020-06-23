package org.sunbird.common.request.orgvalidator;

import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

public class OrgTypeRequestValidator extends BaseOrgRequestValidator {

  public void validateUpdateOrgTypeRequest(Request request) {
    validateCreateOrgTypeRequest(request);
    validateParam(
        (String) request.getRequest().get(JsonKey.ID),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.ID);
  }

  public void validateCreateOrgTypeRequest(Request request) {
    validateParam(
        (String) request.getRequest().get(JsonKey.NAME),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.NAME);
  }
}
