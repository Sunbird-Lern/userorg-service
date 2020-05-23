/** */
package org.sunbird.common.request;

import java.util.HashMap;
import org.junit.Assert;
import org.junit.Test;

/** @author Manzarul */
public class RequestTest {

  @Test
  public void testRequestBeanWithDefaultConstructor() {
    Request request = new Request();
    request.setEnv(1);
    long val = System.currentTimeMillis();
    request.setId(val + "");
    request.setManagerName("name");
    request.setOperation("operation name");
    request.setRequestId("unique req id");
    request.setTs(val + "");
    request.setVer("v1");
    request.setContext(new HashMap<>());
    request.setRequest(new HashMap<>());
    request.setParams(new RequestParams());
    Assert.assertEquals(request.getEnv(), 1);
    Assert.assertEquals(request.getId(), val + "");
    Assert.assertEquals(request.getManagerName(), "name");
    Assert.assertEquals(request.getOperation(), "operation name");
    Assert.assertEquals(request.getRequestId(), "unique req id");
    Assert.assertEquals(request.getTs(), val + "");
    Assert.assertEquals(request.getVer(), "v1");
    Assert.assertEquals(request.getContext().size(), 0);
    Assert.assertEquals(request.getRequest().size(), 0);
    Assert.assertNotNull(request.getParams());
    Assert.assertNotNull(request.toString());
  }
}
