package org.sunbird.response;

import java.util.HashMap;
import org.junit.Assert;
import org.junit.Test;
import org.sunbird.exception.ResponseCode;

public class ClientErrorResponseTest {

  @Test
  public void responseCreate() {
    Response response = new ClientErrorResponse();
    response.setId("test");
    response.setTs("1233444555");
    response.setVer("v1");
    ResponseParams params = new ResponseParams();
    params.setErr("Server Error");
    params.setErrmsg("test msg");
    params.setMsgid("123");
    params.setResmsgid("4566");
    params.setStatus("OK");
    response.setParams(params);
    Assert.assertEquals(response.getId(), "test");
    Assert.assertEquals(response.getTs(), "1233444555");
    Assert.assertEquals(response.getVer(), "v1");
    Assert.assertEquals(response.getParams(), params);
    Assert.assertEquals(response.getResponseCode(), ResponseCode.CLIENT_ERROR);
    Assert.assertEquals(response.getParams().getErr(), params.getErr());
    Assert.assertEquals(response.getParams().getErrmsg(), params.getErrmsg());
    Assert.assertEquals(response.getParams().getMsgid(), params.getMsgid());
    Assert.assertEquals(response.getParams().getResmsgid(), params.getResmsgid());
    Assert.assertEquals(response.getParams().getStatus(), params.getStatus());
    Assert.assertEquals(response.getResult().size(), 0);
    Assert.assertNotEquals(response.get("Test"), "test");
    response.putAll(new HashMap<String, Object>());
    response.put("test", "test123");
    Response responseClone = response.clone(response);
    Assert.assertNotEquals(response, responseClone);
  }
}
