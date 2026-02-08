package org.sunbird.validators;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.sunbird.common.ProjectUtil;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.Request;

/**
 * Validator class for User Free Up requests.
 * Validates the presence of mandatory ID and ensure the identifier list contains valid types (EMAIL, PHONE).
 */
public class UserFreeUpRequestValidator extends BaseRequestValidator {

  private static final LoggerUtil logger = new LoggerUtil(UserFreeUpRequestValidator.class);
  private final Request request;
  private static final List<String> identifiers = new ArrayList<>();

  static {
    identifiers.add(JsonKey.EMAIL);
    identifiers.add(JsonKey.PHONE);
  }

  private static final int ERROR_CODE = ResponseCode.CLIENT_ERROR.getResponseCode();

  /**
   * Factory method to get an instance of UserFreeUpRequestValidator.
   *
   * @param request The request object to validate.
   * @return A new instance of UserFreeUpRequestValidator.
   */
  public static UserFreeUpRequestValidator getInstance(Request request) {
    return new UserFreeUpRequestValidator(request);
  }

  private UserFreeUpRequestValidator(Request request) {
    this.request = request;
  }

  /**
   * Validates the User Free Up request.
   * Performs checks for ID presence and identifier list validity.
   */
  public void validate() {
    logger.debug("UserFreeUpRequestValidator:validate: Starting validation");
    validateIdPresence();
    validateIdentifier();
    logger.debug("UserFreeUpRequestValidator:validate: Validation successful");
  }

  private void validateIdPresence() {
    validateParam(
        (String) request.getRequest().get(JsonKey.ID),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.ID);
  }

  private void validateIdentifier() {
    validatePresence();
    validateObject();
    validateSubset();
  }

  private void validatePresence() {
    if (!request.getRequest().containsKey(JsonKey.IDENTIFIER)) {
      throw new ProjectCommonException(
          ResponseCode.mandatoryParamsMissing.getErrorCode(),
          MessageFormat.format(
              ResponseCode.mandatoryParamsMissing.getErrorMessage(), JsonKey.IDENTIFIER),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  private void validateObject() {
    Object identifierType = request.getRequest().get(JsonKey.IDENTIFIER);
    if (!(identifierType instanceof List)) {
      throw new ProjectCommonException(
          ResponseCode.dataTypeError.getErrorCode(),
          ProjectUtil.formatMessage(
              ResponseCode.dataTypeError.getErrorMessage(), JsonKey.IDENTIFIER, JsonKey.LIST),
          ERROR_CODE);
    }
  }

  private void validateSubset() {
    @SuppressWarnings("unchecked")
    List<String> identifierVal = (List<String>) request.getRequest().get(JsonKey.IDENTIFIER);
    if (!identifiers.containsAll(identifierVal)) {
      throw new ProjectCommonException(
          ResponseCode.dataTypeError.getErrorCode(),
          ProjectUtil.formatMessage(
              String.format(
                  "%s %s",
                  ResponseCode.invalidIdentifier.getErrorMessage(),
                  Arrays.toString(identifiers.toArray())),
              JsonKey.IDENTIFIER,
              JsonKey.DATA),
          ERROR_CODE);
    }
  }
}
