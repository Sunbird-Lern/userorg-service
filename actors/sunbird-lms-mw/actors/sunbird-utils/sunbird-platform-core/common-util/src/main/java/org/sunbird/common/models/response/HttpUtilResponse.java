package org.sunbird.common.models.response;

public class HttpUtilResponse {
  private String body;
  private int statusCode;

  public HttpUtilResponse() {}

  public HttpUtilResponse(String body, int statusCode) {
    this.body = body;
    this.statusCode = statusCode;
  }

  /** @return the body */
  public String getBody() {
    return body;
  }

  /** @param body the body to set */
  public void setBody(String body) {
    this.body = body;
  }

  /** @return the statusCode */
  public int getStatusCode() {
    return statusCode;
  }

  /** @param statusCode the statusCode to set */
  public void setStatusCode(int statusCode) {
    this.statusCode = statusCode;
  }
}
