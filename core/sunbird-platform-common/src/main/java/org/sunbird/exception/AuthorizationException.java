package org.sunbird.exception;

import org.sunbird.message.ResponseCode;

/**
 * Utility class containing authorization-related exception classes.
 */
public class AuthorizationException {

  /**
   * Exception thrown when a user is not authorized to perform an operation.
   */
  public static class NotAuthorized extends BaseException {
    /**
     * Constructs a NotAuthorized exception using the provided ResponseCode.
     *
     * @param responseCode The response code containing the error code and message.
     */
    public NotAuthorized(ResponseCode responseCode) {
      super(responseCode.getErrorCode(), responseCode.getErrorMessage(), 401);
    }
  }
}
