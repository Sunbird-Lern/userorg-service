package org.sunbird.service.feed.impl;

import static org.junit.Assert.assertFalse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.Constants;
import org.sunbird.dao.feed.IFeedDao;
import org.sunbird.dao.feed.impl.FeedDaoImpl;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.Feed;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import org.sunbird.service.feed.FeedFactory;
import org.sunbird.service.feed.IFeedService;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  CassandraOperationImpl.class,
  FeedDaoImpl.class,
  IFeedDao.class,
  HttpClientUtil.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class FeedServiceImplTest {
  private static IFeedService feedService;

  @Before
  public void setUp() throws JsonProcessingException {
    PowerMockito.mockStatic(FeedDaoImpl.class);
    IFeedDao iFeedDao = PowerMockito.mock(FeedDaoImpl.class);
    PowerMockito.when(FeedDaoImpl.getInstance()).thenReturn(iFeedDao);
    Response upsertResponse = new Response();
    Map<String, Object> responseMap2 = new HashMap<>();
    responseMap2.put(Constants.RESPONSE, Constants.SUCCESS);
    upsertResponse.getResult().putAll(responseMap2);
    Response response = new Response();
    Map<String, Object> responseMap = new HashMap<>();
    responseMap.put(Constants.RESPONSE, Arrays.asList(getFeedMap()));
    response.getResult().putAll(responseMap);
    ObjectMapper Obj = new ObjectMapper();
    String jsonStr = Obj.writeValueAsString(upsertResponse);
    PowerMockito.mockStatic(HttpClientUtil.class);
    PowerMockito.when(
            HttpClientUtil.post(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(jsonStr);
    PowerMockito.when(
            HttpClientUtil.patch(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(jsonStr);
    PowerMockito.when(HttpClientUtil.get(Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getUserFeedData());

    feedService = FeedFactory.getInstance();
  }

  @Test
  public void testInsert() {
    Response res = feedService.insert(getFeed(false), null);
    Assert.assertTrue(
        ((String) res.getResult().get(JsonKey.RESPONSE)).equalsIgnoreCase(JsonKey.SUCCESS));
  }

  @Test
  public void testUpdate() {
    Response res = feedService.update(getFeedUpdate(true), null);
    Assert.assertTrue(
        ((String) res.getResult().get(JsonKey.RESPONSE)).equalsIgnoreCase(JsonKey.SUCCESS));
  }

  @Test
  public void testGetRecordsByProperties() {
    Map<String, Object> props = new HashMap<>();
    props.put(JsonKey.USER_ID, "123-456-789");
    List<Feed> res = feedService.getFeedsByProperties(props, null);
    Assert.assertTrue(res != null);
  }

  private static Map<String, Object> getFeedMap() {
    Map<String, Object> fMap = new HashMap<>();
    fMap.put(JsonKey.ID, "123-456-7890");
    fMap.put(JsonKey.USER_ID, "123-456-789");
    fMap.put(JsonKey.CATEGORY, "category");
    return fMap;
  }

  private Request getFeed(boolean needId) {
    Request request = new Request();
    Feed feed = new Feed();
    feed.setUserId("123-456-7890");
    feed.setCategory("category");
    if (needId) {
      feed.setId("123-456-789");
    }
    Map<String, Object> map = new HashMap<>();
    List<String> channelList = new ArrayList<>();
    channelList.add("SI");
    map.put(JsonKey.PROSPECT_CHANNELS, channelList);
    feed.setData(map);
    request.setRequest(new ObjectMapper().convertValue(feed, Map.class));
    return request;
  }

  private Request getFeedUpdate(boolean needId) {
    Request request = new Request();
    Feed feed = new Feed();
    feed.setUserId("123-456-7890");
    feed.setCategory("category");
    if (needId) {
      feed.setId("123-456-789");
    }
    Map<String, Object> map = new HashMap<>();
    List<String> channelList = new ArrayList<>();
    channelList.add("SI");
    map.put(JsonKey.PROSPECT_CHANNELS, channelList);
    feed.setData(map);
    request.setRequest(new ObjectMapper().convertValue(feed, Map.class));
    return request;
  }

  public String getUserFeedData() {
    Response response = new Response();
    Map<String, Object> result = new HashMap<>();
    List<Map<String, Object>> feeds = new ArrayList<>();
    Map<String, Object> feed = new HashMap<>();
    feed.put(JsonKey.ID, "12312312");
    feeds.add(feed);
    result.put(JsonKey.FEEDS, feeds);
    response.putAll(result);
    ObjectMapper Obj = new ObjectMapper();
    String jsonStr = null;
    try {
      jsonStr = Obj.writeValueAsString(response);
    } catch (Exception e) {
      assertFalse(false);
    }
    return jsonStr;
  }
}
