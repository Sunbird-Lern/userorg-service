package org.sunbird.common.factory;

import org.junit.Assert;
import org.junit.Test;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.ElasticSearchTcpImpl;
import org.sunbird.common.inf.ElasticSearchService;

public class EsClientFactoryTest {

  @Test
  public void testGetTcpClient() {
    ElasticSearchService service = EsClientFactory.getInstance("tcp");
    Assert.assertTrue(service instanceof ElasticSearchTcpImpl);
  }

  @Test
  public void testGetRestClient() {
    ElasticSearchService service = EsClientFactory.getInstance("rest");
    Assert.assertTrue(service instanceof ElasticSearchRestHighImpl);
  }

  @Test
  public void testInstanceNull() {
    ElasticSearchService service = EsClientFactory.getInstance("test");
    Assert.assertNull(service);
  }
}
