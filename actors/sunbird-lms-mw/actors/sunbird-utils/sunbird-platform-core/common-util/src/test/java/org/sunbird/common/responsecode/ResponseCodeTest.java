package org.sunbird.common.responsecode;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;

public class ResponseCodeTest {

  @Test
  public void testGetHeaderResponseCodeClientError() {
    ResponseCode respCode =
        ResponseCode.getHeaderResponseCode(ResponseCode.CLIENT_ERROR.getResponseCode());
    assertEquals(ResponseCode.CLIENT_ERROR, respCode);
  }

  @Test
  public void testGetHeaderResponseCodeServerError() {
    ResponseCode respCode = ResponseCode.getHeaderResponseCode(0);
    assertEquals(ResponseCode.SERVER_ERROR, respCode);
  }

  @Test
  public void testGetResponse() {
    ResponseCode respCode = ResponseCode.getResponse(ResponseCode.invalidData.getErrorCode());
    assertEquals(ResponseCode.invalidData, respCode);
  }

  @Test
  public void testGetResponseNullCheck() {
    ResponseCode respCode = ResponseCode.getResponse(null);
    Assert.assertNull(respCode);
  }

  @Test
  public void testGetResponseMessage() {
    String respMsg = ResponseCode.getResponseMessage(ResponseCode.unAuthorized.getErrorCode());
    assertEquals(ResponseCode.unAuthorized.getErrorMessage(), respMsg);
  }

  @Test
  public void testGetResponseMessageEmpty() {
    String respMsg = ResponseCode.getResponseMessage("");
    assertEquals("", respMsg);
  }

  @Test
  public void testInvalidElementValueSuccess() {
    ResponseCode respCode =
        ResponseCode.getResponse(ResponseCode.invalidElementInList.getErrorCode());
    assertEquals(ResponseCode.invalidElementInList, respCode);
  }
}
