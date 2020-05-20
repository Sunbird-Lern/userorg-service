package org.sunbird.helper;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.util.concurrent.FutureUtils;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregations;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.models.util.HttpUtil;

/** @author manzarul */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
@PrepareForTest({
  ConnectionManager.class,
  TransportClient.class,
  AcknowledgedResponse.class,
  GetRequestBuilder.class,
  HttpUtil.class,
  BulkProcessor.class,
  FutureUtils.class,
  SearchHit.class,
  SearchHits.class,
  Aggregations.class
})
public class ConnectionManagerTest {

  @Test
  public void testInitialiseConnection() {
    TransportClient client = ConnectionManager.getClient();
    Assert.assertNotNull(client);
  }

  @Test
  public void testGetRestClientNull() {
    RestHighLevelClient client = ConnectionManager.getRestClient();
    Assert.assertNull(client);
  }

  @Test
  @Ignore
  public void testInitialiseConnectionFromPropertiesFile() {
    boolean response =
        ConnectionManager.initialiseConnectionFromPropertiesFile("test", "localhost", "9200");
    Assert.assertTrue(response);
  }

  @Test
  public void testInitialiseConnectionFromPropertiesFileFailWithEmpty() {
    boolean response =
        ConnectionManager.initialiseConnectionFromPropertiesFile("test", "localhost", "");
    Assert.assertFalse(response);
  }

  @Test
  public void testInitialiseConnectionFromPropertiesFileFailWithNull() {
    boolean response =
        ConnectionManager.initialiseConnectionFromPropertiesFile("test", "localhost", null);
    Assert.assertFalse(response);
  }

  @Test
  public void testCloseConnection() {
    ConnectionManager.closeClient();
    Assert.assertTrue(true);
  }
}
