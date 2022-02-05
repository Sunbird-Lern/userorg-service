package org.sunbird.exception;

import org.junit.Assert;
import org.junit.Test;

/** @author Manzarul */
public class ExceptionTest {

  @Test
  public void testProjectCommonException() {
    ProjectCommonException exception =
        new ProjectCommonException(
            ResponseCode.unAuthorized,
            ResponseCode.unAuthorized.getErrorMessage(),
            ResponseCode.UNAUTHORIZED.getResponseCode());
    Assert.assertEquals(exception.getErrorCode(), ResponseCode.unAuthorized.getErrorCode());
    Assert.assertEquals(exception.getMessage(), ResponseCode.unAuthorized.getErrorMessage());
    Assert.assertEquals(
        exception.getErrorResponseCode(), ResponseCode.UNAUTHORIZED.getResponseCode());
  }

  @Test
  public void testProjectCommonExceptionUsingSetters() {
    ProjectCommonException exception =
        new ProjectCommonException(
            ResponseCode.unAuthorized,
            ResponseCode.unAuthorized.getErrorMessage(),
            ResponseCode.UNAUTHORIZED.getResponseCode());
    Assert.assertEquals(exception.getErrorCode(), ResponseCode.unAuthorized.getErrorCode());
    Assert.assertEquals(exception.getMessage(), ResponseCode.unAuthorized.getErrorMessage());
    Assert.assertEquals(
        exception.getErrorResponseCode(), ResponseCode.UNAUTHORIZED.getResponseCode());
    exception.setErrorCode(ResponseCode.emailFormatError.getErrorCode());
    exception.setMessage(ResponseCode.emailFormatError.getErrorMessage());
    exception.setErrorResponseCode(ResponseCode.SERVER_ERROR.getResponseCode());
    Assert.assertEquals(exception.getErrorCode(), ResponseCode.emailFormatError.getErrorCode());
    Assert.assertEquals(exception.getMessage(), ResponseCode.emailFormatError.getErrorMessage());
    Assert.assertEquals(
        exception.getErrorResponseCode(), ResponseCode.SERVER_ERROR.getResponseCode());
  }
}
