package org.sunbird.common.models.response;

import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.responsecode.ResponseCode;

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
