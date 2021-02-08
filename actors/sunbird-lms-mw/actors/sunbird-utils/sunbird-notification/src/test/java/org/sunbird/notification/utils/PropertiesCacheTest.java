package org.sunbird.notification.utils;

import org.junit.Assert;
import org.junit.Test;

public class PropertiesCacheTest {

  @Test
  public void testGetProperty() {
    PropertiesCache cache = PropertiesCache.getInstance();
    String code = cache.getProperty("sunbird.msg.91.country");
    Assert.assertEquals("91", code);
  }

  @Test
  public void testReadProperty() {
    PropertiesCache cache = PropertiesCache.getInstance();
    String code = cache.readProperty("sunbird.msg.91.country");
    Assert.assertEquals("91", code);
  }
}
