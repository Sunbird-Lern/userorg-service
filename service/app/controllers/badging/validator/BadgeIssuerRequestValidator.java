package controllers.badging.validator;

import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

/** Created by arvind on 2/3/18. */
public class BadgeIssuerRequestValidator {

  private BadgeIssuerRequestValidator() {}

  private static final int ERROR_CODE = ResponseCode.CLIENT_ERROR.getResponseCode();

  /**
   * Method to validate noteId
   *
   * @param request
   */
  public static void validateCreateBadgeIssuer(Request request) {
    if (StringUtils.isBlank((String) request.getRequest().get(JsonKey.NAME))) {
      throw createExceptionInstance(
          ResponseCode.invalidDataForCreateBadgeIssuer.getErrorCode(), "name is required.");
    }
    if (ProjectUtil.isStringNullOREmpty((String) request.getRequest().get(JsonKey.DESCRIPTION))) {
      throw createExceptionInstance(
          ResponseCode.invalidDataForCreateBadgeIssuer.getErrorCode(), "Description is required.");
    }
    if (StringUtils.isBlank((String) request.getRequest().get(JsonKey.URL))) {
      throw createExceptionInstance(
          ResponseCode.invalidDataForCreateBadgeIssuer.getErrorCode(), "url is required.");
    }
    if (StringUtils.isBlank((String) request.getRequest().get(JsonKey.EMAIL))) {
      throw createExceptionInstance(
          ResponseCode.invalidDataForCreateBadgeIssuer.getErrorCode(), "email is required.");
    }

    if (!ProjectUtil.isEmailvalid((String) request.getRequest().get(JsonKey.EMAIL))) {
      throw createExceptionInstance(
          ResponseCode.invalidDataForCreateBadgeIssuer.getErrorCode(), "email is invalid.");
    }
  }

  public static void validateGetBadgeIssuerDetail(Request request) {
    if (StringUtils.isBlank((String) request.getRequest().get(JsonKey.SLUG))) {
      throw new ProjectCommonException(
          ResponseCode.slugRequired.getErrorCode(),
          ResponseCode.slugRequired.getErrorMessage(),
          ERROR_CODE);
    }
  }

  private static ProjectCommonException createExceptionInstance(String errorCode, String errMsg) {
    return new ProjectCommonException(
        ResponseCode.getResponse(errorCode).getErrorCode(),
        ResponseCode.getResponse(errorCode).getErrorMessage(),
        ERROR_CODE,
        errMsg);
  }
}
