package org.sunbird.exception;

/**
 * Base exception class for the application, extending RuntimeException.
 * Used for standardized error handling and response generation.
 */
public class BaseException extends RuntimeException {

  private static final long serialVersionUID = 1L;
  private String code;
  private String message;
  private int responseCode;

  /**
   * Gets the internal error code for client-side localization.
   *
   * @return The error code string.
   */
  public String getCode() {
    return code;
  }

  /**
   * Sets the internal error code.
   *
   * @param code The error code string.
   */
  public void setCode(String code) {
    this.code = code;
  }

  /**
   * Gets the descriptive error message.
   *
   * @return The error message string.
   */
  @Override
  public String getMessage() {
    return message;
  }

  /**
   * Sets the descriptive error message.
   *
   * @param message The error message string.
   */
  public void setMessage(String message) {
    this.message = message;
  }

  /**
   * Gets the HTTP response code associated with this exception.
   *
   * @return The integer response code.
   */
  public int getResponseCode() {
    return responseCode;
  }

  /**
   * Sets the HTTP response code for this exception.
   *
   * @param responseCode The integer response code.
   */
  public void setResponseCode(int responseCode) {
    this.responseCode = responseCode;
  }

  /**
   * Constructs a BaseException with code, message, and response code.
   *
   * @param code The internal error code string.
   * @param message The descriptive error message.
   * @param responseCode The HTTP response code.
   */
  public BaseException(String code, String message, int responseCode) {
    super();
    this.code = code;
    this.message = message;
    this.responseCode = responseCode;
  }

  /**
   * Constructs a BaseException from an existing BaseException instance.
   *
   * @param ex The source BaseException.
   */
  public BaseException(BaseException ex) {
    super();
    this.code = ex.code;
    this.message = ex.getMessage();
    this.responseCode = ex.getResponseCode();
  }

  /**
   * Constructs a BaseException with code and message.
   *
   * @param code The internal error code string.
   * @param message The descriptive error message.
   */
  public BaseException(String code, String message) {
    super();
    this.code = code;
    this.message = message;
  }
}
