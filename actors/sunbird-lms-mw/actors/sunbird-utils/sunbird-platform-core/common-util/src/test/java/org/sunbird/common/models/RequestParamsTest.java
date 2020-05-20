/** */
package org.sunbird.common.models;

import org.junit.Assert;
import org.junit.Test;
import org.sunbird.common.request.RequestParams;

/** @author Manzarul */
public class RequestParamsTest {

  @Test
  public void testResponseParamBean() {
    RequestParams params = new RequestParams();
    params.setAuthToken("auth_1233");
    params.setCid("cid");
    params.setDid("deviceId");
    params.setKey("account key");
    params.setMsgid("uniqueMsgId");
    params.setSid("sid");
    params.setUid("UUID");
    Assert.assertEquals(params.getAuthToken(), "auth_1233");
    Assert.assertEquals(params.getCid(), "cid");
    Assert.assertEquals(params.getMsgid(), "uniqueMsgId");
    Assert.assertEquals(params.getDid(), "deviceId");
    Assert.assertEquals(params.getKey(), "account key");
    Assert.assertEquals(params.getSid(), "sid");
    Assert.assertEquals(params.getUid(), "UUID");
  }
}
