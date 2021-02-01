package org.sunbird.feed;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

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
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.actorutil.org.OrganisationClient;
import org.sunbird.actorutil.org.impl.OrganisationClientImpl;
import org.sunbird.bean.ShadowUser;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.Constants;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.feed.impl.FeedFactory;
import org.sunbird.feed.impl.FeedServiceImpl;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.models.organisation.Organisation;
import org.sunbird.models.user.Feed;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  ElasticSearchRestHighImpl.class,
  ElasticSearchHelper.class,
  EsClientFactory.class,
  CassandraOperationImpl.class,
  ElasticSearchService.class,
  IFeedService.class,
  FeedServiceImpl.class,
  FeedFactory.class,
  ShadowUser.class,
  OrganisationClient.class,
  OrganisationClientImpl.class
})
@SuppressStaticInitializationFor("org.sunbird.common.ElasticSearchUtil")
@PowerMockIgnore({"javax.management.*"})
public class FeedUtilTest {
  private ElasticSearchService esUtil;
  private CassandraOperation cassandraOperation = null;
  private static Response response;
  private static IFeedService feedService;
  private static OrganisationClient organisationClient;

  @Before
  public void setUp() throws Exception {
    PowerMockito.mockStatic(FeedServiceImpl.class);
    PowerMockito.mockStatic(FeedFactory.class);
    feedService = mock(FeedServiceImpl.class);
    organisationClient = mock(OrganisationClient.class);
    mockStatic(OrganisationClientImpl.class);
    when(FeedFactory.getInstance()).thenReturn(feedService);
    when(FeedServiceImpl.getCassandraInstance()).thenReturn(cassandraOperation);
    when(feedService.getFeedsByProperties(Mockito.anyMap(), Mockito.any()))
        .thenReturn(getFeedList(true))
        .thenReturn(getFeedList(false));
    when(feedService.insert(Mockito.any(), Mockito.any())).thenReturn(new Response());
    when(feedService.update(Mockito.any(), Mockito.any())).thenReturn(new Response());

    when(OrganisationClientImpl.getInstance()).thenReturn(organisationClient);
    when(organisationClient.esSearchOrgByFilter(Mockito.anyMap(), Mockito.any()))
        .thenReturn(getFeedOrgs());

    PowerMockito.mockStatic(ServiceFactory.class);
    PowerMockito.mockStatic(EsClientFactory.class);
    PowerMockito.mockStatic(ElasticSearchHelper.class);
    esUtil = mock(ElasticSearchService.class);
    esUtil = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esUtil);
    cassandraOperation = mock(CassandraOperationImpl.class);
    response = new Response();
    Map<String, Object> responseMap = new HashMap<>();
    responseMap.put(Constants.RESPONSE, Arrays.asList(getFeedMap()));
    response.getResult().putAll(responseMap);
    PowerMockito.when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    Response upsertResponse = new Response();
    Map<String, Object> responseMap2 = new HashMap<>();
    responseMap2.put(Constants.RESPONSE, Constants.SUCCESS);
    upsertResponse.getResult().putAll(responseMap2);
    PowerMockito.when(
            cassandraOperation.upsertRecord(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(upsertResponse);
  }

  private List<Feed> getFeedList(boolean needId) {
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
    List<Feed> feedList = new ArrayList<>();
    feedList.add(feed);
    return feedList;
  }

  private Map<String, Object> getFeedMap() {
    Map<String, Object> fMap = new HashMap<>();
    fMap.put(JsonKey.ID, "123-456-7890");
    fMap.put(JsonKey.USER_ID, "123-456-789");
    fMap.put(JsonKey.CATEGORY, "category");
    return fMap;
  }

  @Test
  public void saveFeedInsertTest() {
    List<String> userIds = new ArrayList<>();
    userIds.add("123-456-7890");
    Response response = FeedUtil.saveFeed(getShadowUser(), userIds, null);
    Assert.assertNotNull(response);
  }

  public static ShadowUser getShadowUser() {
    ShadowUser.ShadowUserBuilder user = new ShadowUser.ShadowUserBuilder();
    user.setChannel("SI");
    return user.build();
  }

  private List<Organisation> getFeedOrgs() {
    Organisation org = new Organisation();
    org.setChannel("dummyChannel");
    org.setId("dummyId");
    List<Organisation> orgList = new ArrayList<>();
    orgList.add(org);
    return orgList;
  }
}
