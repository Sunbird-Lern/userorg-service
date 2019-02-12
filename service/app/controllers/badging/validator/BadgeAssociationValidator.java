package controllers.badging.validator;

import java.text.MessageFormat;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.sunbird.common.exception.ProjectCommonException;
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

  @SuppressWarnings("unchecked")
  public void commonValidation(Request request) {
    validateParam(
        (String) request.getRequest().get(JsonKey.CONTENT_ID),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.CONTENT_ID);
    validateListParam(request.getRequest(), BadgingJsonKey.BADGE_IDs);
    if (CollectionUtils.isEmpty(
        (List<String>) request.getRequest().get(BadgingJsonKey.BADGE_IDs))) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.errorMandatoryParamsEmpty,
          MessageFormat.format(
              ResponseCode.errorMandatoryParamsEmpty.getErrorMessage(), BadgingJsonKey.BADGE_IDs));
    }
  }
}
