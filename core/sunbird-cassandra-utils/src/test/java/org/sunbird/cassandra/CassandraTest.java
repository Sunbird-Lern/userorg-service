/*
package org.sunbird.cassandra;

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.helper.CassandraConnectionManagerImpl;
import org.sunbird.helper.CassandraConnectionMngrFactory;

@Ignore
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CassandraTest {

  private static PropertiesCache cach = PropertiesCache.getInstance();
  private static String host = cach.getProperty("contactPoint");
  private static String port = cach.getProperty("port");
  private static CassandraConnectionManagerImpl connectionManager =
      (CassandraConnectionManagerImpl) CassandraConnectionMngrFactory.getObject(JsonKey.EMBEDDED);

  @Test
  public void testConnection() {
    boolean bool = connectionManager.createConnection(host, port, "", "", "sunbird1");
    assertEquals(true, bool);
  }

  @Test
  public void testConnectionB() {
    boolean bool = connectionManager.createConnection(host, port, "", "", "sunbird12");
    assertEquals(true, bool);
  }

  @AfterClass
  public static void shutdownhook() {
    connectionManager.registerShutDownHook();
  }
}
*/
