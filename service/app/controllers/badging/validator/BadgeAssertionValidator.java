package controllers.badging.validator;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.BadgingJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

/**
 * This class will do the request validation for Badge assertion.
 *
 * @author Manzarul
 */
public class BadgeAssertionValidator {

  private static final int ERROR_CODE = ResponseCode.CLIENT_ERROR.getResponseCode();

  /*
   * Default private constructor
   */
  private BadgeAssertionValidator() {}

  /**
   * This method will do the basic validation of badge assertion request.
   *
   * @param request Request
   */
  public static void validateBadgeAssertion(Request request) {

    if (StringUtils.isBlank((String) request.getRequest().get(BadgingJsonKey.ISSUER_ID))) {
      throw new ProjectCommonException(
          ResponseCode.issuerIdRequired.getErrorCode(),
          ResponseCode.issuerIdRequired.getErrorMessage(),
          ERROR_CODE);
    }
    if (StringUtils.isBlank((String) request.getRequest().get(BadgingJsonKey.BADGE_ID))) {
      throw new ProjectCommonException(
          ResponseCode.badgeIdRequired.getErrorCode(),
          ResponseCode.badgeIdRequired.getErrorMessage(),
          ERROR_CODE);
    }
    if (!StringUtils.isBlank((String) request.getRequest().get(BadgingJsonKey.EVIDENCE))) {
      boolean response =
          ProjectUtil.isUrlvalid((String) request.getRequest().get(BadgingJsonKey.EVIDENCE));
      if (!response) {
        throw new ProjectCommonException(
            ResponseCode.evidenceRequired.getErrorCode(),
            ResponseCode.evidenceRequired.getErrorMessage(),
            ERROR_CODE);
      }
    }
    if (StringUtils.isBlank((String) request.getRequest().get(BadgingJsonKey.RECIPIENT_ID))) {
      throw new ProjectCommonException(
          ResponseCode.recipientIdRequired.getErrorCode(),
          ResponseCode.recipientIdRequired.getErrorMessage(),
          ERROR_CODE);
    }
    if (StringUtils.isBlank((String) request.getRequest().get(BadgingJsonKey.RECIPIENT_TYPE))) {
      throw new ProjectCommonException(
          ResponseCode.recipientTypeRequired.getErrorCode(),
          ResponseCode.recipientTypeRequired.getErrorMessage(),
          ERROR_CODE);
    }
    if (!matchRecipientType((String) request.getRequest().get(BadgingJsonKey.RECIPIENT_TYPE))) {
      throw new ProjectCommonException(
          ResponseCode.invalidRecipientType.getErrorCode(),
          ResponseCode.invalidRecipientType.getErrorMessage(),
          ERROR_CODE);
    }
  }

  /**
   * This method will verify null and empty check for requested param. if any param is null or empty
   * then it will throw exception
   *
   * @param request Request
   */
  public static void validategetBadgeAssertion(Request request) {
    if (StringUtils.isBlank((String) request.getRequest().get(BadgingJsonKey.ASSERTION_ID))) {
      throw new ProjectCommonException(
          ResponseCode.assertionIdRequired.getErrorCode(),
          ResponseCode.assertionIdRequired.getErrorMessage(),
          ERROR_CODE);
    }
  }

  /**
   * This method will validate get assertion list requested data. expected data "assertions": [{
   * "issuerId": "oracle-university", "badgeId": "java-se-8-programmer", "assertionId":
   * "1ebceaf1-b63b-4edb-97c0-bfc6e3235408" }]
   *
   * @param request
   */
  public static void validateGetAssertionList(Request request) {
    Map<String, Object> filtesMap = (Map) request.getRequest().get(JsonKey.FILTERS);
    if (filtesMap == null) {
      throw new ProjectCommonException(
          ResponseCode.invalidRequestData.getErrorCode(),
          ResponseCode.invalidRequestData.getErrorMessage(),
          ERROR_CODE);
    }
    Object obj = filtesMap.get(BadgingJsonKey.ASSERTIONS);
    if (obj == null || !(obj instanceof List)) {
      throw new ProjectCommonException(
          ResponseCode.invalidRequestData.getErrorCode(),
          ResponseCode.invalidRequestData.getErrorMessage(),
          ERROR_CODE);
    }
    List<String> assertionData = (List<String>) obj;
    int size =
        Integer.parseInt(
            PropertiesCache.getInstance().getProperty(BadgingJsonKey.BADGING_ASSERTION_LIST_SIZE));
    if (assertionData.size() > size) {
      throw new ProjectCommonException(
          ResponseCode.sizeLimitExceed.getErrorCode(),
          MessageFormat.format(ResponseCode.sizeLimitExceed.getErrorMessage(), size),
          ERROR_CODE);
    }

    for (String assertionId : assertionData) {
      Request temp = new Request();
      temp.getRequest().put(BadgingJsonKey.ASSERTION_ID, assertionId);
      validategetBadgeAssertion(temp);
    }
  }

  /**
   * This method will validate revoke assertion request data.
   *
   * @param request Request
   */
  public static void validateRevokeAssertion(Request request) {
    validategetBadgeAssertion(request);
    if (StringUtils.isBlank((String) request.getRequest().get(BadgingJsonKey.REVOCATION_REASON))) {
      throw new ProjectCommonException(
          ResponseCode.revocationReasonRequired.getErrorCode(),
          ResponseCode.revocationReasonRequired.getErrorMessage(),
          ERROR_CODE);
    }
    if (StringUtils.isBlank((String) request.getRequest().get(BadgingJsonKey.RECIPIENT_ID))) {
      throw new ProjectCommonException(
          ResponseCode.recipientIdRequired.getErrorCode(),
          ResponseCode.recipientIdRequired.getErrorMessage(),
          ERROR_CODE);
    }
    if (StringUtils.isBlank((String) request.getRequest().get(BadgingJsonKey.RECIPIENT_TYPE))) {
      throw new ProjectCommonException(
          ResponseCode.recipientTypeRequired.getErrorCode(),
          ResponseCode.recipientTypeRequired.getErrorMessage(),
          ERROR_CODE);
    }
    if (!matchRecipientType((String) request.getRequest().get(BadgingJsonKey.RECIPIENT_TYPE))) {
      throw new ProjectCommonException(
          ResponseCode.invalidRecipientType.getErrorCode(),
          ResponseCode.invalidRecipientType.getErrorMessage(),
          ERROR_CODE);
    }
  }

  /**
   * This method will check valid recipient type.
   *
   * @param recipientType String
   * @return boolean
   */
  private static boolean matchRecipientType(String recipientType) {
    if (BadgingJsonKey.BADGE_TYPE_USER.equalsIgnoreCase(recipientType)
        || BadgingJsonKey.BADGE_TYPE_CONTENT.equalsIgnoreCase(recipientType)) {
      return true;
    }
    return false;
  }
}
