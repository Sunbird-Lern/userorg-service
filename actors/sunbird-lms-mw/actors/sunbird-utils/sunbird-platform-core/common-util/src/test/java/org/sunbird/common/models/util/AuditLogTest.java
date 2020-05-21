/** */
package org.sunbird.common.models.util;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/** @author Manzarul */
public class AuditLogTest {

  @Test
  public void createAuditLog() {
    AuditLog log = new AuditLog();
    log.setDate("2017-12-29");
    log.setObjectId("objectId");
    log.setObjectType("User");
    log.setOperationType("create");
    log.setRequestId("requesterId");
    log.setUserId("userId");
    Map<String, Object> map = new HashMap<>();
    log.setLogRecord(map);
    Assert.assertEquals("2017-12-29", log.getDate());
    Assert.assertEquals("objectId", log.getObjectId());
    Assert.assertEquals("User", log.getObjectType());
    Assert.assertEquals("create", log.getOperationType());
    Assert.assertEquals("requesterId", log.getRequestId());
    Assert.assertEquals("userId", log.getUserId());
    Assert.assertEquals(0, log.getLogRecord().size());
  }
}
