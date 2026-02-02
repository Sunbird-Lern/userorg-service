package org.sunbird.validators.orgvalidator;

import java.text.MessageFormat;
import java.util.List;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.validators.BaseRequestValidator;

/**
 * Validates requests for the OrgAssignKeys Controller.
 * This class ensures that required parameters and key lists are present and correctly formatted.
 */
public class KeyManagementValidator extends BaseRequestValidator {

  private Request request;

  private KeyManagementValidator(Request request) {
    this.request = request;
  }

  /**
   * Factory method to get an instance of KeyManagementValidator.
   *
   * @param request The API request to validate.
   * @return A new instance of KeyManagementValidator.
   */
  public static KeyManagementValidator getInstance(Request request) {
    return new KeyManagementValidator(request);
  }

  /**
   * Validates the OrgAssignKeysController request.
   * Checks for ID presence, and validates 'signKeys' and 'encKeys' lists.
   */
  public void validate() {
    id();
    signKeys();
    encKeys();
  }

  /**
   * Validates the presence of the ID parameter.
   */
  private void id() {
    validateParam(
        (String) request.getRequest().get(JsonKey.ID),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.ID);
  }

  /**
   * Validates the 'signKeys' parameter: presence, type (List), and non-empty size.
   */
  private void signKeys() {
    validateKeyPresence(JsonKey.SIGN_KEYS);
    validateListTypeObject(JsonKey.SIGN_KEYS);
    validateSize(JsonKey.SIGN_KEYS);
  }

  /**
   * Validates the 'encKeys' parameter: presence, type (List), and non-empty size.
   */
  private void encKeys() {
    validateKeyPresence(JsonKey.ENC_KEYS);
    validateListTypeObject(JsonKey.ENC_KEYS);
    validateSize(JsonKey.ENC_KEYS);
  }

  /**
   * Validates that the value associated with the given key is of type List.
   *
   * @param key The key to check in the request.
   */
  private void validateListTypeObject(String key) {
    if (!(request.get(key) instanceof List)) {
      throw new ProjectCommonException(
          ResponseCode.dataTypeError,
          MessageFormat.format(ResponseCode.dataTypeError.getErrorMessage(), key, "List"),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  /**
   * Validates that the request contains the specified key.
   *
   * @param key The key to check for presence.
   */
  private void validateKeyPresence(String key) {
    if (!request.getRequest().containsKey(key)) {
      throw new ProjectCommonException(
          ResponseCode.mandatoryParamsMissing,
          ResponseCode.mandatoryParamsMissing.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode(),
          key);
    }
  }

  /**
   * Validates that the list associated with the key is not empty.
   *
   * @param key The key to check the list size for.
   */
  private void validateSize(String key) {
    if (((List) request.get(key)).size() == 0) {
      throw new ProjectCommonException(
          ResponseCode.errorMandatoryParamsEmpty,
          ResponseCode.errorMandatoryParamsEmpty.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode(),
          key);
    }
  }
}
