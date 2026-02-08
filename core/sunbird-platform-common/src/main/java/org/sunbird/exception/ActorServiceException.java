package org.sunbird.exception;

/**
 * Collection of exceptions related to Actor Service operations.
 */
public class ActorServiceException {

  /**
   * Exception thrown when an operation name requested from an actor is invalid or unsupported.
   */
  public static class InvalidOperationName extends BaseException
  {
    public InvalidOperationName(String code, String message, int responseCode) {
      super(code,message,responseCode);
    }
  }

  /**
   * Exception thrown when a request to an actor times out.
   */
  public static class InvalidRequestTimeout extends BaseException
  {
    public InvalidRequestTimeout(String code, String message, int responseCode) {
      super(code,message,responseCode);
    }
  }

  /**
   * Exception thrown when the request data provided to an actor is invalid.
   */
  public static class InvalidRequestData extends BaseException
  {
    public InvalidRequestData(String code, String message, int responseCode) {
      super(code,message,responseCode);
    }
  }

}