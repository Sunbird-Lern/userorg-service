package controllers.badging.validator;

import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

/**
 * Validates Badge Issuer API requests.
 *
 * @author Arvind, B Vinaya Kumar
 */
public class BadgeIssuerRequestValidator extends BaseRequestValidator {

  private final int ERROR_CODE = ResponseCode.CLIENT_ERROR.getResponseCode();

  /**
   * Validates request of create issuer API.
   *
   * @param request Request containing following parameters: name: The unique name of the badge
   *     issuing entity or organisation. description: A short description of the issuer. url: The
   *     valid homepage URL of the issuer. email: The valid contact e-mail address of the issuer.
   *     image: An image to represent the issuer.
   */
  public void validateCreateBadgeIssuer(Request request) {
    validateParam(
        (String) request.getRequest().get(JsonKey.NAME),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.NAME);
    validateParam(
        (String) request.getRequest().get(JsonKey.DESCRIPTION),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.DESCRIPTION);
    validateParam(
        (String) request.getRequest().get(JsonKey.URL),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.URL);
    validateParam(
        (String) request.getRequest().get(JsonKey.EMAIL),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.EMAIL);

    if (!ProjectUtil.isEmailvalid((String) request.getRequest().get(JsonKey.EMAIL))) {
      throw createExceptionByResponseCode(ResponseCode.emailFormatError, ERROR_CODE);
    }

    if (!ProjectUtil.isUrlvalid((String) request.getRequest().get(JsonKey.URL))) {
      throw createExceptionByResponseCode(ResponseCode.urlFormatError, ERROR_CODE);
    }
  }

  /**
   * Validates request of get issuer API.
   *
   * @param request Request containing following parameters: slug: The ID of the Issuer whose
   *     details need to be returned.
   */
  public void validateGetBadgeIssuerDetail(Request request) {
    validateParam(
        (String) request.getRequest().get(JsonKey.SLUG),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.SLUG);
  }
}
