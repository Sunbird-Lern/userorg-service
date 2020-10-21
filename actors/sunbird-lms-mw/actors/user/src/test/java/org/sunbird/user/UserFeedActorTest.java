package org.sunbird.user;

import static akka.testkit.JavaTestKit.duration;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.testkit.javadsl.TestKit;
import java.util.Arrays;
import java.util.HashMap;
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
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.Constants;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.dto.SearchDTO;
import org.sunbird.feed.IFeedService;
import org.sunbird.feed.impl.FeedServiceImpl;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.user.actors.UserFeedActor;
import scala.concurrent.Promise;

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
  org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class
})
@SuppressStaticInitializationFor("org.sunbird.common.ElasticSearchUtil")
@PowerMockIgnore({"javax.management.*"})
public class UserFeedActorTest {
  private static ActorSystem system = ActorSystem.create("system");
  private final Props props = Props.create(UserFeedActor.class);
  private static Response response = null;
  private static Map<String, Object> esResponse = new HashMap<>();
  private static ElasticSearchService esService;
  private static Map<String, Object> userFeed = new HashMap<>();
  private static ElasticSearchService esUtil;
  private static EsClientFactory esFactory;
  private static CassandraOperation cassandraOperation = null;

  @Before
  public void setUp() {
    PowerMockito.mockStatic(ServiceFactory.class);
    PowerMockito.mockStatic(EsClientFactory.class);
    PowerMockito.mockStatic(ElasticSearchHelper.class);
    esService = mock(ElasticSearchService.class);
    esUtil = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esUtil);

    userFeed.put(JsonKey.ID, "123-456-789");
    response = new Response();
    Map<String, Object> responseMap = new HashMap<>();
    responseMap.put(Constants.RESPONSE, Arrays.asList(userFeed));
    response.getResult().putAll(responseMap);
    esResponse.put(JsonKey.CONTENT, Arrays.asList(userFeed));

    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.USER_ID, "123-456-789");
    SearchDTO search = new SearchDTO();
    search.getAdditionalProperties().put(JsonKey.FILTERS, filters);
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(esResponse);
    when(ElasticSearchHelper.getResponseFromFuture(Mockito.any())).thenReturn(esResponse);
    PowerMockito.when(esService.search(search, ProjectUtil.EsType.userfeed.getTypeName(), null))
        .thenReturn(promise.future());
    cassandraOperation = mock(CassandraOperationImpl.class);
    PowerMockito.when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(FeedServiceImpl.getCassandraInstance()).thenReturn(cassandraOperation);
    Response upsertResponse = new Response();
    Map<String, Object> responseMap2 = new HashMap<>();
    responseMap2.put(Constants.RESPONSE, Constants.SUCCESS);
    upsertResponse.getResult().putAll(responseMap2);
    PowerMockito.when(
            cassandraOperation.upsertRecord(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(upsertResponse);
  }

  @Test
  public void getUserFeedTest() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.GET_USER_FEED_BY_ID.getValue());
    reqObj.put(JsonKey.USER_ID, "123-456-789");
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(duration("10 second"), Response.class);
    Assert.assertTrue(null != res && res.getResponseCode() == ResponseCode.OK);
  }

  @Test
  public void saveUserFeedTest() {
    Request reqObj = new Request();
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> dataMap = new HashMap<>();
    reqObj.setOperation(ActorOperations.CREATE_USER_FEED.getValue());
    requestMap.put(JsonKey.USER_ID, "someUserId");
    requestMap.put(JsonKey.CATEGORY, "someCategory");
    requestMap.put(JsonKey.PRIORITY, 1);
    requestMap.put(JsonKey.DATA, dataMap);
    reqObj.setRequest(requestMap);
    boolean result = testScenario(reqObj, null);
    assertTrue(result);
  }

  public boolean testScenario(Request reqObj, ResponseCode errorCode) {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    subject.tell(reqObj, probe.getRef());

    if (errorCode == null) {
      Response res = probe.expectMsgClass(duration("100 second"), Response.class);
      return null != res && res.getResponseCode() == ResponseCode.OK;
    } else {
      ProjectCommonException res =
          probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
      return res.getCode().equals(errorCode.getErrorCode())
          || res.getResponseCode() == errorCode.getResponseCode();
    }
  }
}
