package org.sunbird.exception;

import java.text.MessageFormat;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;

/**
 * A comprehensive exception class used across the backend to handle error scenarios.
 * This class encapsulates error codes, messages, and HTTP status codes, supporting both
 * unified error handling and backward compatibility for various service modules.
 */
public class ProjectCommonException extends RuntimeException {

  private static final long serialVersionUID = 1L;
  
  /** The application-specific error code (e.g., "ERR_USER_NOT_FOUND"). */
  private String errorCode;
  
  /** The human-readable error message. */
  private String errorMessage;
  
  /** The HTTP status code associated with this error (e.g., 400, 404, 500). */
  private int errorResponseCode;

  /** The rich enum representation of the error, if available. */
  private ResponseCode responseCode;

  /**
   * Constructs a new ProjectCommonException using a ResponseCode enum.
   *
   * @param code The ResponseCode enum representing the error type.
   * @param message A custom error message description.
   * @param responseCode The HTTP status code to return to the client.
   */
  public ProjectCommonException(ResponseCode code, String message, int responseCode) {
    super(message);
    this.responseCode = code;
    this.errorCode = code.getErrorCode();
    this.errorMessage = message;
    this.errorResponseCode = responseCode;
  }

  /**
   * Constructs a new ProjectCommonException with a string error code.
   * This constructor is primarily used for scenarios where a ResponseCode enum is not strictly required.
   *
   * @param errorCode The string representation of the error code.
   * @param message The error message description.
   * @param responseCode The HTTP status code to return to the client.
   */
  public ProjectCommonException(String errorCode, String message, int responseCode) {
    super(message);
    this.errorCode = errorCode;
    this.errorMessage = message;
    this.errorResponseCode = responseCode;
    this.responseCode = null;
  }

  /**
   * Constructs a new ProjectCommonException wrapping another exception, typically for actor operations.
   * Adds service-specific prefixes to the error code.
   *
   * @param pce The original ProjectCommonException to wrap.
   * @param actorOperation The actor operation context to append to the error code prefix.
   */
  public ProjectCommonException(ProjectCommonException pce, String actorOperation) {
    super(pce.getMessage());
    this.setStackTrace(pce.getStackTrace());
    this.errorCode =
        new StringBuilder(JsonKey.USER_ORG_SERVICE_PREFIX)
            .append(actorOperation)
            .append(pce.getErrorCode())
            .toString();
    this.errorResponseCode = pce.getErrorResponseCode();
    this.errorMessage = pce.getMessage();
    this.responseCode = pce.getResponseCodeEnum();
  }

  /**
   * Constructs a new ProjectCommonException with message formatting support.
   * Replaces placeholders in the message with provided values.
   *
   * @param code The ResponseCode enum.
   * @param messageWithPlaceholder The error message pattern containing placeholders.
   * @param responseCode The HTTP status code.
   * @param placeholderValue The values to substitute into the message placeholders.
   */
  public ProjectCommonException(
      ResponseCode code,
      String messageWithPlaceholder,
      int responseCode,
      String... placeholderValue) {
    super(MessageFormat.format(messageWithPlaceholder, placeholderValue));
    this.errorCode = code.getErrorCode();
    this.errorMessage = MessageFormat.format(messageWithPlaceholder, placeholderValue);
    this.errorResponseCode = responseCode;
    this.responseCode = code;
  }

  // --- Getters and Setters ---

  public String getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(String code) {
    this.errorCode = code;
  }

  @Override
  public String getMessage() {
    return errorMessage;
  }

  public void setMessage(String message) {
    this.errorMessage = message;
  }

  /**
   * Gets the HTTP response status code.
   *
   * @return The HTTP status code as an integer.
   */
  public int getErrorResponseCode() {
    return errorResponseCode;
  }

  public void setErrorResponseCode(int responseCode) {
    this.errorResponseCode = responseCode;
  }

  /**
   * Gets the ResponseCode enum.
   * 
   * @return The ResponseCode enum, or null if initialized with the raw string constructor.
   */
  public ResponseCode getResponseCodeEnum() {
    return responseCode;
  }

  public void setResponseCodeEnum(ResponseCode responseCode) {
    this.responseCode = responseCode;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  // --- Backward Compatibility Aliases ---

  /**
   * Gets the error code. Kept for backward compatibility.
   *
   * @return The error code string.
   * @see #getErrorCode()
   */
  public String getCode() {
    return getErrorCode();
  }

  /**
   * Sets the error code. Kept for backward compatibility.
   *
   * @param code The error code string.
   * @see #setErrorCode(String)
   */
  public void setCode(String code) {
    setErrorCode(code);
  }

  /**
   * Gets the HTTP response code. Kept for backward compatibility.
   *
   * @return The integer HTTP response code.
   * @see #getErrorResponseCode()
   */
  public int getResponseCode() {
      return errorResponseCode;
  }
  
  /**
   * Sets the HTTP response code. Kept for backward compatibility.
   *
   * @param responseCode The integer HTTP response code.
   * @see #setErrorResponseCode(int)
   */
  public void setResponseCode(int responseCode) {
    this.errorResponseCode = responseCode;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(errorCode).append(": ");
    builder.append(errorMessage);
    return builder.toString();
  }

  // --- Static Helper Methods ---

  /**
   * Throws a generic client error exception (4xx).
   *
   * @param responseCode The ResponseCode enum details.
   * @param exceptionMessage A custom message to include.
   */
  public static void throwClientErrorException(ResponseCode responseCode, String exceptionMessage) {
    throw new ProjectCommonException(
        responseCode,
        StringUtils.isBlank(exceptionMessage) ? responseCode.getErrorMessage() : exceptionMessage,
        ResponseCode.CLIENT_ERROR.getResponseCode());
  }

  /**
   * Throws a generic Resource Not Found exception (404).
   */
  public static void throwResourceNotFoundException() {
    throw new ProjectCommonException(
        ResponseCode.resourceNotFound,
        MessageFormat.format(ResponseCode.resourceNotFound.getErrorMessage(), ""),
        ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
  }

  /**
   * Throws a Resource Not Found exception (404) with a custom message.
   *
   * @param responseCode The ResponseCode enum details.
   * @param exceptionMessage A custom message to include.
   */
  public static void throwResourceNotFoundException(
      ResponseCode responseCode, String exceptionMessage) {
    throw new ProjectCommonException(
        responseCode,
        StringUtils.isBlank(exceptionMessage) ? responseCode.getErrorMessage() : exceptionMessage,
        ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
  }

  /**
   * Throws a generic Server Error exception (5xx).
   *
   * @param responseCode The ResponseCode enum details.
   * @param exceptionMessage A custom message to include.
   */
  public static void throwServerErrorException(ResponseCode responseCode, String exceptionMessage) {
    throw new ProjectCommonException(
        responseCode,
        StringUtils.isBlank(exceptionMessage) ? responseCode.getErrorMessage() : exceptionMessage,
        ResponseCode.SERVER_ERROR.getResponseCode());
  }

  /**
   * Throws a generic Server Error exception (5xx) using the default enum message.
   *
   * @param responseCode The ResponseCode enum details.
   */
  public static void throwServerErrorException(ResponseCode responseCode) {
    throwServerErrorException(responseCode, responseCode.getErrorMessage());
  }

  /**
   * Throws a generic Client Error exception (4xx) using the default enum message.
   *
   * @param responseCode The ResponseCode enum details.
   */
  public static void throwClientErrorException(ResponseCode responseCode) {
    throwClientErrorException(responseCode, responseCode.getErrorMessage());
  }

  /**
   * Throws the standard Unauthorized exception (401).
   */
  public static void throwUnauthorizedErrorException() {
    throw new ProjectCommonException(
        ResponseCode.unAuthorized,
        ResponseCode.unAuthorized.getErrorMessage(),
        ResponseCode.UNAUTHORIZED.getResponseCode());
  }
}