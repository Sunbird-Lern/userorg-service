package org.sunbird.response;

import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;

public class ClientErrorResponse extends Response {

  private ProjectCommonException exception = null;

  public ClientErrorResponse() {
    responseCode = ResponseCode.CLIENT_ERROR;
  }

  public ProjectCommonException getException() {
    return exception;
  }

  public void setException(ProjectCommonException exception) {
    this.exception = exception;
  }
}
