package org.sunbird.response;

/**
 * A simple wrapper class for HTTP responses, holding the response body and status code.
 * This class is typically used by utility methods handling raw HTTP interactions.
 */
public class HttpUtilResponse {
  
  /** The raw response body string. */
  private String body;
  
  /** The HTTP status code of the response. */
  private int statusCode;

  /**
   * Default constructor.
   */
  public HttpUtilResponse() {}

  /**
   * Constructs a new HttpUtilResponse with the specified body and status code.
   *
   * @param body The response body as a string.
   * @param statusCode The integer HTTP status code.
   */
  public HttpUtilResponse(String body, int statusCode) {
    this.body = body;
    this.statusCode = statusCode;
  }

  /**
   * Gets the response body.
   *
   * @return The response body string.
   */
  public String getBody() {
    return body;
  }

  /**
   * Sets the response body.
   *
   * @param body The response body string to set.
   */
  public void setBody(String body) {
    this.body = body;
  }

  /**
   * Gets the HTTP status code.
   *
   * @return The status code.
   */
  public int getStatusCode() {
    return statusCode;
  }

  /**
   * Sets the HTTP status code.
   *
   * @param statusCode The status code to set.
   */
  public void setStatusCode(int statusCode) {
    this.statusCode = statusCode;
  }
}
