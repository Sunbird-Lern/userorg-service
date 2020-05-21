/** */
package org.sunbird.common.models;

import java.util.HashMap;
import org.junit.Assert;
import org.junit.Test;
import org.sunbird.common.models.response.ResponseParams;
import org.sunbird.common.responsecode.ResponseCode;

/** @author Manzarul */
public class ResponseTest {

  @Test
  public void responseCreate() {
    org.sunbird.common.models.response.Response response =
        new org.sunbird.common.models.response.Response();
    response.setId("test");
    response.setResponseCode(ResponseCode.SERVER_ERROR);
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
    Assert.assertEquals(response.getResponseCode(), ResponseCode.SERVER_ERROR);
    Assert.assertEquals(response.getParams().getErr(), params.getErr());
    Assert.assertEquals(response.getParams().getErrmsg(), params.getErrmsg());
    Assert.assertEquals(response.getParams().getMsgid(), params.getMsgid());
    Assert.assertEquals(response.getParams().getResmsgid(), params.getResmsgid());
    Assert.assertEquals(response.getParams().getStatus(), params.getStatus());
    Assert.assertEquals(response.getResult().size(), 0);
    Assert.assertNotEquals(response.get("Test"), "test");
    response.putAll(new HashMap<String, Object>());
    response.put("test", "test123");
    org.sunbird.common.models.response.Response responseClone = response.clone(response);
    Assert.assertNotEquals(response, responseClone);
  }
}
