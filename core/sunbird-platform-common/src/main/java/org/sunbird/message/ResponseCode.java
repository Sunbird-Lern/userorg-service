package org.sunbird.message;

import org.apache.commons.lang3.StringUtils;
import org.sunbird.keys.JsonKey;
/**
 * Enum representing various response codes and their associated metadata (code, status, error message).
 */
public enum ResponseCode {
  unAuthorized(
      IResponseMessage.Key.UNAUTHORIZED, IResponseMessage.Message.UNAUTHORIZED, JsonKey.FAILED),
  mandatoryParameterMissing(
      IResponseMessage.Key.MANDATORY_PARAMETER_MISSING,
      IResponseMessage.Message.MANDATORY_PARAMETER_MISSING,
      JsonKey.FAILED),
  invalidRequestData(
      IResponseMessage.INVALID_REQUESTED_DATA,
      IResponseMessage.Message.INVALID_REQUESTED_DATA,
      JsonKey.FAILED),
  serverError(IResponseMessage.SERVER_ERROR, IResponseMessage.SERVER_ERROR, JsonKey.FAILED),
  internalError(IResponseMessage.INTERNAL_ERROR, IResponseMessage.INTERNAL_ERROR, JsonKey.FAILED),
  templateNotFound(
      IResponseMessage.TEMPLATE_NOT_FOUND, IResponseMessage.Message.TEMPLATE_NOT_FOUND, JsonKey.FAILED),
  OK(200),
  CLIENT_ERROR(400),
  SERVER_ERROR(500),
  RESOURCE_NOT_FOUND(404),
  UNAUTHORIZED(401),
  FORBIDDEN(403),
  REDIRECTION_REQUIRED(302),
  TOO_MANY_REQUESTS(429),
  SERVICE_UNAVAILABLE(503),
  BAD_REQUEST(400);

  private int code;
  private String errorCode;
  private String errorMessage;
  private String status;

  /**
   * Constructor for enum constants with only an integer code.
   *
   * @param code The HTTP response code.
   */
  ResponseCode(int code) {
    this.code = code;
  }

  /**
   * Constructor for enum constants with error code, message, and status.
   *
   * @param errorCode The internal error code string.
   * @param errorMessage The descriptive error message.
   * @param status The status of the response (e.g., failed).
   */
  ResponseCode(String errorCode, String errorMessage, String status) {
    this.errorCode = errorCode;
    this.errorMessage = errorMessage;
    this.status = status;
  }

  /**
   * Gets the status of the response.
   *
   * @return The status string.
   */
  public String getStatus() {
    return status;
  }

  /**
   * Sets the status of the response.
   *
   * @param status The status string to set.
   */
  public void setStatus(String status) {
    this.status = status;
  }

  /**
   * Gets the HTTP response code.
   *
   * @return The integer code.
   */
  public int getCode() {
    return this.code;
  }

  /**
   * Matches an integer HTTP code to its corresponding ResponseCode enum.
   *
   * @param code The HTTP response code.
   * @return The matching ResponseCode enum, or SERVER_ERROR if no match is found.
   */
  public static ResponseCode getHeaderResponseCode(int code) {
    if (code > 0) {
      try {
        ResponseCode[] arr = ResponseCode.values();
        for (ResponseCode rc : arr) {
          if (rc.getResponseCode() == code) return rc;
        }
      } catch (Exception e) {
        return ResponseCode.SERVER_ERROR;
      }
    }
    return ResponseCode.SERVER_ERROR;
  }

  /**
   * Matches an internal error code string to its corresponding ResponseCode enum status.
   *
   * @param errorCode The internal error code string.
   * @return The matching ResponseCode enum, or SERVER_ERROR if no match is found.
   */
  public static ResponseCode getHeaderResponseStatus(String errorCode) {
    if (errorCode != null) {
      try {
        ResponseCode[] arr = ResponseCode.values();
        for (ResponseCode rc : arr) {
          if (rc.getErrorCode() != null && rc.getErrorCode().equals(errorCode)) return rc;
        }
      } catch (Exception e) {
        return ResponseCode.SERVER_ERROR;
      }
    }
    return ResponseCode.SERVER_ERROR;
  }

  /**
   * Gets a ResponseCode based on an internal error code string.
   *
   * @param errorCode The internal error code string.
   * @return The matching ResponseCode enum, or null if not found.
   */
  public static ResponseCode getResponse(String errorCode) {
    if (StringUtils.isBlank(errorCode)) {
      return null;
    } else if (JsonKey.UNAUTHORIZED.equals(errorCode)) {
      return ResponseCode.unAuthorized;
    } else {
      ResponseCode[] responseCodes = ResponseCode.values();
      for (ResponseCode response : responseCodes) {
        if (response.getErrorCode() != null && response.getErrorCode().equals(errorCode)) {
          return response;
        }
      }
      return null;
    }
  }

  /**
   * Gets the internal integer response code.
   *
   * @return The response code.
   */
  public int getResponseCode() {
    return code;
  }

  /**
   * Gets the internal error code string.
   *
   * @return The error code string.
   */
  public String getErrorCode() {
    return errorCode;
  }

  /**
   * Gets the descriptive error message.
   *
   * @return The error message string.
   */
  public String getErrorMessage() {
    return errorMessage;
  }

  /**
   * Gets a ResponseCode based on an integer response code.
   *
   * @param code The HTTP response code.
   * @return The matching ResponseCode enum, or RESOURCE_NOT_FOUND if no match is found.
   */
  public static ResponseCode getResponseCode(int code) {
    ResponseCode[] codes = ResponseCode.values();
    for (ResponseCode res : codes) {
      if (res.code == code) {
        return res;
      }
    }
    return ResponseCode.RESOURCE_NOT_FOUND;
  }
}
