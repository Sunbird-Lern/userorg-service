/** */
package org.sunbird.common.exception;

import java.text.MessageFormat;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.responsecode.ResponseCode;

/**
 * This exception will be used across all backend code. This will send status code and error message
 *
 * @author Manzarul.Haque
 */
public class ProjectCommonException extends RuntimeException {

  /** serialVersionUID. */
  private static final long serialVersionUID = 1L;
  /** code String code ResponseCode. */
  private String code;
  /** message String ResponseCode. */
  private String message;
  /** responseCode int ResponseCode. */
  private int responseCode;

  /**
   * This code is for client to identify the error and based on that do the message localization.
   *
   * @return String
   */
  public String getCode() {
    return code;
  }

  /**
   * To set the client code.
   *
   * @param code String
   */
  public void setCode(String code) {
    this.code = code;
  }

  /**
   * message for client in english.
   *
   * @return String
   */
  @Override
  public String getMessage() {
    return message;
  }

  /** @param message String */
  public void setMessage(String message) {
    this.message = message;
  }

  /**
   * This method will provide response code, this code will be used in response header.
   *
   * @return int
   */
  public int getResponseCode() {
    return responseCode;
  }

  /** @param responseCode int */
  public void setResponseCode(int responseCode) {
    this.responseCode = responseCode;
  }

  /**
   * three argument constructor.
   *
   * @param code String
   * @param message String
   * @param responseCode int
   */
  public ProjectCommonException(String code, String message, int responseCode) {
    super();
    this.code = code;
    this.message = message;
    this.responseCode = responseCode;
  }

  public ProjectCommonException(
      String code, String messageWithPlaceholder, int responseCode, String... placeholderValue) {
    super();
    this.code = code;
    this.message = MessageFormat.format(messageWithPlaceholder, placeholderValue);
    this.responseCode = responseCode;
  }

  public static void throwClientErrorException(ResponseCode responseCode, String exceptionMessage) {
    throw new ProjectCommonException(
        responseCode.getErrorCode(),
        StringUtils.isBlank(exceptionMessage) ? responseCode.getErrorMessage() : exceptionMessage,
        ResponseCode.CLIENT_ERROR.getResponseCode());
  }

  public static void throwResourceNotFoundException() {
    throw new ProjectCommonException(
        ResponseCode.resourceNotFound.getErrorCode(),
        ResponseCode.resourceNotFound.getErrorMessage(),
        ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
  }

  public static void throwServerErrorException(ResponseCode responseCode, String exceptionMessage) {
    throw new ProjectCommonException(
        responseCode.getErrorCode(),
        StringUtils.isBlank(exceptionMessage) ? responseCode.getErrorMessage() : exceptionMessage,
        ResponseCode.SERVER_ERROR.getResponseCode());
  }

  public static void throwServerErrorException(ResponseCode responseCode) {
    throwServerErrorException(responseCode, responseCode.getErrorMessage());
  }

  public static void throwClientErrorException(ResponseCode responseCode) {
    throwClientErrorException(responseCode, responseCode.getErrorMessage());
  }

  public static void throwUnauthorizedErrorException() {
    throw new ProjectCommonException(
        ResponseCode.unAuthorized.getErrorCode(),
        ResponseCode.unAuthorized.getErrorMessage(),
        ResponseCode.UNAUTHORIZED.getResponseCode());
  }
}
