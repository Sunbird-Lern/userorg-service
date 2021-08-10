package org.sunbird.dao.feed.impl;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.Constants;
import org.sunbird.dao.feed.IFeedDao;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.response.Response;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  CassandraOperationImpl.class,
})
@SuppressStaticInitializationFor("org.sunbird.common.ElasticSearchUtil")
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class FeedDaoImplTest {

  private static CassandraOperation cassandraOperation = null;

  @Before
  public void setUp() throws Exception {
    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.USER_ID, "123-456-789");
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(FeedDaoImpl.getCassandraInstance()).thenReturn(cassandraOperation);
    initCassandraForSuccess();
  }

  private static void initCassandraForSuccess() {
    Response response = new Response();
    Map<String, Object> responseMap = new HashMap<>();
    responseMap.put(Constants.RESPONSE, Arrays.asList(getFeedMap()));
    response.getResult().putAll(responseMap);
    when(cassandraOperation.getRecordById(
            Mockito.any(), Mockito.any(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(response);

    Response upsertResponse = new Response();
    Map<String, Object> responseMap2 = new HashMap<>();
    responseMap2.put(Constants.RESPONSE, Constants.SUCCESS);
    upsertResponse.getResult().putAll(responseMap2);
    when(cassandraOperation.insertRecord(
            Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(upsertResponse);
    when(cassandraOperation.updateRecord(
            Mockito.any(), Mockito.any(), Mockito.anyMap(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(upsertResponse);
    when(cassandraOperation.deleteRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(upsertResponse);
  }

  @Test
  public void testInsert() {
    IFeedDao iFeedDao = new FeedDaoImpl();
    Response res = iFeedDao.insert(getFeed(false), null);
    Assert.assertTrue(
        ((String) res.getResult().get(JsonKey.RESPONSE)).equalsIgnoreCase(JsonKey.SUCCESS));
  }

  @Test
  public void testUpdate() {
    IFeedDao iFeedDao = new FeedDaoImpl();
    Response res = iFeedDao.update(getFeed(true), null);
    Assert.assertTrue(
        ((String) res.getResult().get(JsonKey.RESPONSE)).equalsIgnoreCase(JsonKey.SUCCESS));
  }

  @Test
  public void testDelete() {
    boolean response = false;
    IFeedDao iFeedDao = new FeedDaoImpl();
    try {
      iFeedDao.delete("123-456-789", null, null, null);
      response = true;
    } catch (Exception ex) {
      Assert.assertTrue(response);
    }
    Assert.assertTrue(response);
  }

  @Test
  public void testGetRecordsByProperties() {
    Map<String, Object> props = new HashMap<>();
    props.put(JsonKey.USER_ID, "123-456-789");
    IFeedDao iFeedDao = new FeedDaoImpl();
    Response res = iFeedDao.getFeedsByProperties(props, null);
    Assert.assertNotNull(res);
  }

  private static Map<String, Object> getFeedMap() {
    Map<String, Object> fMap = new HashMap<>();
    fMap.put(JsonKey.ID, "123-456-7890");
    fMap.put(JsonKey.USER_ID, "123-456-789");
    fMap.put(JsonKey.CATEGORY, "category");
    return fMap;
  }

  private Map<String, Object> getFeed(boolean needId) {
    Map feed = new HashMap<String, Object>();
    feed.put(JsonKey.ID, "123-456-7890");
    feed.put(JsonKey.CATEGORY, "category");
    if (needId) {
      feed.put(JsonKey.USER_ID, "123-456-789");
    }
    Map<String, Object> map = new HashMap<>();
    List<String> channelList = new ArrayList<>();
    channelList.add("SI");
    map.put(JsonKey.PROSPECT_CHANNELS, channelList);
    feed.put(JsonKey.DATA, map);
    return feed;
  }
}
