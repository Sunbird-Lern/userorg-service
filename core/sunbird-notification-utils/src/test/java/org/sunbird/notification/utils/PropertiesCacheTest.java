package org.sunbird.notification.utils;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the {@link PropertiesCache} class to ensure properties are loaded and retrieved
 * correctly from the configuration file.
 */
public class PropertiesCacheTest {

  /** Tests the retrieval of a property value using {@link PropertiesCache#getProperty(String)}. */
  @Test
  public void testGetProperty() {
    PropertiesCache cache = PropertiesCache.getInstance();
    String code = cache.getProperty("sunbird.msg.91.country");
    Assert.assertEquals("91", code);
  }

  /** Tests the retrieval of a property value using {@link PropertiesCache#readProperty(String)}. */
  @Test
  public void testReadProperty() {
    PropertiesCache cache = PropertiesCache.getInstance();
    String code = cache.readProperty("sunbird.msg.91.country");
    Assert.assertEquals("91", code);
  }
}
