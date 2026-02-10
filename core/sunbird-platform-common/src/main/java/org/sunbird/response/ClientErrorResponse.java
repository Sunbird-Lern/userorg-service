package org.sunbird.response;

import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;

/**
 * Represents a client-side error response.
 * <p>
 * This class extends the standard {@link Response} to include details about the exception
 * that caused the error, typically encapsulating a {@link ProjectCommonException}.
 * It defaults the response code to {@link ResponseCode#CLIENT_ERROR}.
 */
public class ClientErrorResponse extends Response {

  private static final long serialVersionUID = 1L;

  /** The exception details associated with this client error. */
  private ProjectCommonException exception = null;

  /**
   * Default constructor.
   * Initializes the response code to {@link ResponseCode#CLIENT_ERROR}.
   */
  public ClientErrorResponse() {
    this.responseCode = ResponseCode.CLIENT_ERROR;
  }

  /**
   * Gets the exception associated with this response.
   *
   * @return The {@link ProjectCommonException} causing the error.
   */
  public ProjectCommonException getException() {
    return exception;
  }

  /**
   * Sets the exception associated with this response.
   *
   * @param exception The {@link ProjectCommonException} to set.
   */
  public void setException(ProjectCommonException exception) {
    this.exception = exception;
  }
}
