package org.sunbird.exception;

import org.junit.Assert;
import org.junit.Test;

/** @author Manzarul */
public class ExceptionTest {

  @Test
  public void testProjectCommonException() {
    ProjectCommonException exception =
        new ProjectCommonException(
            ResponseCode.unAuthorized.getErrorCode(),
            ResponseCode.unAuthorized.getErrorMessage(),
            ResponseCode.UNAUTHORIZED.getResponseCode());
    Assert.assertEquals(exception.getCode(), ResponseCode.unAuthorized.getErrorCode());
    Assert.assertEquals(exception.getMessage(), ResponseCode.unAuthorized.getErrorMessage());
    Assert.assertEquals(exception.getResponseCode(), ResponseCode.UNAUTHORIZED.getResponseCode());
  }

  @Test
  public void testProjectCommonExceptionUsingSetters() {
    ProjectCommonException exception =
        new ProjectCommonException(
            ResponseCode.unAuthorized.getErrorCode(),
            ResponseCode.unAuthorized.getErrorMessage(),
            ResponseCode.UNAUTHORIZED.getResponseCode());
    Assert.assertEquals(exception.getCode(), ResponseCode.unAuthorized.getErrorCode());
    Assert.assertEquals(exception.getMessage(), ResponseCode.unAuthorized.getErrorMessage());
    Assert.assertEquals(exception.getResponseCode(), ResponseCode.UNAUTHORIZED.getResponseCode());
    exception.setCode(ResponseCode.dataFormatError.getErrorCode());
    exception.setMessage(ResponseCode.dataFormatError.getErrorMessage());
    exception.setResponseCode(ResponseCode.SERVER_ERROR.getResponseCode());
    Assert.assertEquals(exception.getCode(), ResponseCode.dataFormatError.getErrorCode());
    Assert.assertEquals(exception.getMessage(), ResponseCode.dataFormatError.getErrorMessage());
    Assert.assertEquals(exception.getResponseCode(), ResponseCode.SERVER_ERROR.getResponseCode());
  }
}
