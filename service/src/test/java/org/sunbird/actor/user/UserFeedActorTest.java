package org.sunbird.actor.user;

import static akka.testkit.JavaTestKit.duration;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
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
import org.sunbird.actor.feed.UserFeedActor;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.Constants;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.feed.IFeedService;
import org.sunbird.service.feed.impl.FeedServiceImpl;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  CassandraOperationImpl.class,
  IFeedService.class,
  FeedServiceImpl.class,
  org.sunbird.datasecurity.impl.ServiceFactory.class
})
@SuppressStaticInitializationFor("org.sunbird.common.ElasticSearchUtil")
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class UserFeedActorTest {
  private static ActorSystem system = ActorSystem.create("system");
  private final Props props = Props.create(UserFeedActor.class);
  private static Response response = null;
  private static Map<String, Object> userFeed = new HashMap<>();
  private static CassandraOperation cassandraOperation = null;

  @Before
  public void setUp() {
    PowerMockito.mockStatic(ServiceFactory.class);
    userFeed.put(JsonKey.ID, "123-456-789");
    response = new Response();
    Map<String, Object> responseMap = new HashMap<>();
    responseMap.put(Constants.RESPONSE, Arrays.asList(userFeed));
    response.getResult().putAll(responseMap);

    cassandraOperation = mock(CassandraOperationImpl.class);
    PowerMockito.when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    Response upsertResponse = new Response();
    Map<String, Object> responseMap2 = new HashMap<>();
    responseMap2.put(Constants.RESPONSE, Constants.SUCCESS);
    upsertResponse.getResult().putAll(responseMap2);
    PowerMockito.when(
            cassandraOperation.insertRecord(
                    Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(upsertResponse);
    PowerMockito.when(
            cassandraOperation.updateRecord(
                    Mockito.any(), Mockito.any(), Mockito.anyMap(), Mockito.anyMap(), Mockito.any()))
            .thenReturn(upsertResponse);
  }

  @Test
  public void getUserFeedTest() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setRequestContext(new RequestContext());
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
    reqObj.setRequestContext(new RequestContext());
    requestMap.put(JsonKey.USER_ID, "someUserId");
    requestMap.put(JsonKey.CATEGORY, "someCategory");
    requestMap.put(JsonKey.PRIORITY, 1);
    requestMap.put(JsonKey.DATA, dataMap);
    reqObj.setRequest(requestMap);
    boolean result = testScenario(reqObj, null);
    assertTrue(result);
  }

  @Test
  public void updateUserFeedTest() {
    Request reqObj = new Request();
    Map<String, Object> requestMap = new HashMap<>();
    reqObj.setOperation(ActorOperations.UPDATE_USER_FEED.getValue());
    reqObj.setRequestContext(new RequestContext());
    requestMap.put(JsonKey.USER_ID, "someUserId");
    requestMap.put(JsonKey.CATEGORY, "someCategory");
    requestMap.put(JsonKey.FEED_ID, "someFeedId");
    reqObj.setRequest(requestMap);
    boolean result = testScenario(reqObj, null);
    assertTrue(result);
  }

  @Test
  public void deleteUserFeedTest() {
    Request reqObj = new Request();
    Map<String, Object> requestMap = new HashMap<>();
    reqObj.setOperation(ActorOperations.DELETE_USER_FEED.getValue());
    reqObj.setRequestContext(new RequestContext());
    requestMap.put(JsonKey.USER_ID, "someUserId");
    requestMap.put(JsonKey.CATEGORY, "someCategory");
    requestMap.put(JsonKey.FEED_ID, "someFeedId");
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
