package org.sunbird.exception;

import org.sunbird.message.IResponseMessage;
import org.sunbird.message.Localizer;
import org.sunbird.message.ResponseCode;

import java.text.MessageFormat;
import java.util.Locale;

/**
 * Utility class containing various validation-related exception classes.
 */
public class ValidationException {

  private static Localizer localizer = Localizer.getInstance();

  /**
   * Exception thrown when the request data provided is invalid.
   */
  public static class InvalidRequestData extends BaseException {

    public InvalidRequestData() {
      super(
          IResponseMessage.INVALID_REQUESTED_DATA,
          MessageFormat.format(IResponseMessage.INVALID_REQUESTED_DATA, null),
          400);
    }
  }

  /**
   * Exception thrown when a mandatory parameter is missing in the request.
   */
  public static class MandatoryParamMissing extends BaseException {
    /**
     * Constructs a MandatoryParamMissing exception with a custom response code.
     *
     * @param param The name of the missing parameter.
     * @param parentKey The parent key if applicable.
     * @param responseCode The response code to be used.
     */
    public MandatoryParamMissing(String param, String parentKey, ResponseCode responseCode) {
      super(
          responseCode.getErrorCode(),
          MessageFormat.format(responseCode.getErrorMessage(), param),
          400);
    }

    /**
     * Constructs a MandatoryParamMissing exception with default error details.
     *
     * @param param The name of the missing parameter.
     * @param parentKey The parent key if applicable.
     */
    public MandatoryParamMissing(String param, String parentKey) {
      super(
          IResponseMessage.Key.MANDATORY_PARAMETER_MISSING,
          MessageFormat.format(IResponseMessage.Message.MANDATORY_PARAMETER_MISSING, param),
          400);
    }
  }

  /**
   * Exception thrown when a parameter has an incorrect data type.
   */
  public static class ParamDataTypeError extends BaseException {
    /**
     * Constructs a ParamDataTypeError exception.
     *
     * @param param The name of the parameter.
     * @param type The expected data type.
     */
    public ParamDataTypeError(String param, String type) {
      super(
          IResponseMessage.INVALID_REQUESTED_DATA,
          MessageFormat.format(IResponseMessage.DATA_TYPE_ERROR, param),
          400);
    }
  }

  /**
   * Exception thrown when a parameter has an invalid value.
   */
  public static class InvalidParamValue extends BaseException {
    /**
     * Constructs an InvalidParamValue exception.
     *
     * @param paramValue The invalid value provided.
     * @param paramName The name of the parameter.
     */
    public InvalidParamValue(String paramValue, String paramName) {
      super(
          IResponseMessage.Key.INVALID_PARAMETER_VALUE,
          MessageFormat.format(
              ValidationException.getLocalizedMessage(
                  IResponseMessage.INVALID_PARAMETER_VALUE, null),
              paramValue,
              paramName),
          400);
    }
  }

  /**
   * Retrieves a localized message for a given key and locale.
   *
   * @param key The message key.
   * @param locale The desired locale.
   * @return The localized message string.
   */
  private static String getLocalizedMessage(String key, Locale locale) {
    return localizer.getMessage(key, locale);
  }
}
