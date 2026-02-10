package org.sunbird.common.factory;

import org.junit.Assert;
import org.junit.Test;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.inf.ElasticSearchService;

/**
 * Unit tests for EsClientFactory.
 */
public class EsClientFactoryTest {

  /**
   * Test getInstance method for "rest" client type.
   */
  @Test
  public void testGetRestClient() {
    ElasticSearchService service = EsClientFactory.getInstance("rest");
    Assert.assertTrue(service instanceof ElasticSearchRestHighImpl);
  }

  /**
   * Test getInstance method for invalid client type.
   */
  @Test
  public void testInstanceNull() {
    ElasticSearchService service = EsClientFactory.getInstance("test");
    Assert.assertNull(service);
  }
}