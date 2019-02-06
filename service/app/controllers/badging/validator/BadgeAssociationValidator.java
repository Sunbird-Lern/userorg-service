package controllers.badging.validator;

import org.sunbird.common.models.util.BadgingJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

public class BadgeAssociationValidator extends BaseRequestValidator {

  public void validateCreateBadgeAssociationRequest(Request request) {
    commonValidation(request);
  }

  public void validateRemoveBadgeAssociationRequest(Request request) {
    commonValidation(request);
  }

  public void commonValidation(Request request) {
    validateParam(
        (String) request.getRequest().get(JsonKey.CONTENT_ID),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.CONTENT_ID);
    validateListParam(request.getRequest(), BadgingJsonKey.BADGE_IDs);
  }
}
