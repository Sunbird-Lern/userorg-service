/** */
package org.sunbird.response;

import org.junit.Assert;
import org.junit.Test;

/** @author Manzarul */
public class ResponseParamsTest {

  @Test
  public void testResponseParamBean() {
    ResponseParams params = new ResponseParams();
    params.setMsgid("test");
    params.setResmsgid("test-1");
    params.setStatus("OK");
    Assert.assertEquals(params.getMsgid(), "test");
    Assert.assertEquals(params.getResmsgid(), "test-1");
    Assert.assertEquals(params.getStatus(), "OK");
    Assert.assertEquals(ResponseParams.StatusType.FAILED.name(), "FAILED");
    Assert.assertEquals(ResponseParams.StatusType.SUCCESSFUL.name(), "SUCCESSFUL");
    Assert.assertEquals(ResponseParams.StatusType.WARNING.name(), "WARNING");
  }
}
