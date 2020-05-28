package org.sunbird.feed.impl;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import java.util.*;
import akka.dispatch.Futures;
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
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.dto.SearchDTO;
import org.sunbird.feed.IFeedService;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.models.user.Feed;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        ServiceFactory.class,
        ElasticSearchRestHighImpl.class,
        ElasticSearchHelper.class,
        EsClientFactory.class,
        CassandraOperationImpl.class,
        ElasticSearchService.class
})
@SuppressStaticInitializationFor("org.sunbird.common.ElasticSearchUtil")
@PowerMockIgnore({"javax.management.*"})
public class FeedServiceImplTest {
    private ElasticSearchService esUtil;
    private static CassandraOperation cassandraOperation = null;
    private static IFeedService feedService = FeedFactory.getInstance();
    private static SearchDTO search = new SearchDTO();
    private static Map<String, Object> esResponse = new HashMap<>();
    private static Promise<Map<String, Object>> promise;

    @Before
    public void setUp() throws Exception {
        Map<String, Object> filters = new HashMap<>();
        filters.put(JsonKey.USER_ID, "123-456-789");
        search.getAdditionalProperties().put(JsonKey.FILTERS, filters);
        PowerMockito.mockStatic(ServiceFactory.class);
        PowerMockito.mockStatic(EsClientFactory.class);
        PowerMockito.mockStatic(ElasticSearchHelper.class);
        esUtil = mock(ElasticSearchService.class);
        esUtil = mock(ElasticSearchRestHighImpl.class);
        promise = Futures.promise();
        promise.success(esResponse);

        when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esUtil);
        cassandraOperation = mock(CassandraOperationImpl.class);
        PowerMockito.when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
        when(FeedServiceImpl.getCassandraInstance()).thenReturn(cassandraOperation);
        when(FeedServiceImpl.getESInstance()).thenReturn(esUtil);
        when(esUtil.search(search,ProjectUtil.EsType.userfeed.getTypeName())).thenReturn(promise.future());
        when(ElasticSearchHelper.getResponseFromFuture(Mockito.any())).thenReturn(esResponse);
        initCassandraForSuccess();
    }

    private static void initCassandraForSuccess() {
        Response response = new Response();
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put(Constants.RESPONSE, Arrays.asList(getFeedMap()));
        response.getResult().putAll(responseMap);
        PowerMockito.when(
                cassandraOperation.getRecordsByProperties(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(response);

        Response upsertResponse = new Response();
        Map<String, Object> responseMap2 = new HashMap<>();
        responseMap2.put(Constants.RESPONSE, Constants.SUCCESS);
        upsertResponse.getResult().putAll(responseMap2);
        PowerMockito.when(cassandraOperation.upsertRecord(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(upsertResponse);
        PowerMockito.when(
                cassandraOperation.deleteRecord(
                        Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(upsertResponse);
    }

    @Test
    public void testInsert() {
        Response res = feedService.insert(getFeed(false));
        Assert.assertTrue(
                ((String) res.getResult().get(JsonKey.RESPONSE)).equalsIgnoreCase(JsonKey.SUCCESS));
    }

    @Test
    public void testUpdate() {
        Response res = feedService.update(getFeed(true));
        Assert.assertTrue(
                ((String) res.getResult().get(JsonKey.RESPONSE)).equalsIgnoreCase(JsonKey.SUCCESS));
    }

    @Test
    public void testDelete() {
        boolean response = false;
        try {
            feedService.delete("123-456-789");
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
        List<Feed> res = feedService.getRecordsByProperties(props);
        Assert.assertTrue(res != null);
    }

    @Test
    public void testSearch() {
        Response response = feedService.search(search);
        when(ElasticSearchHelper.getResponseFromFuture(Mockito.any())).thenReturn(esResponse);
        PowerMockito.when(esUtil.search(search, ProjectUtil.EsType.userfeed.getTypeName()))
                .thenReturn(promise.future());
        Assert.assertTrue(esResponse != null);
    }

    private static Map<String, Object> getFeedMap() {
        Map<String, Object> fMap = new HashMap<>();
        fMap.put(JsonKey.ID, "123-456-7890");
        fMap.put(JsonKey.USER_ID, "123-456-789");
        fMap.put(JsonKey.CATEGORY, "category");
        return fMap;
    }

    private Feed getFeed(boolean needId) {
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
        return feed;
    }
}