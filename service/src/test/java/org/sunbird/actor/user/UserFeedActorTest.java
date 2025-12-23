package org.sunbird.actor.user;

import java.time.Duration;

import static org.junit.Assert.assertTrue;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.testkit.javadsl.TestKit;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.sunbird.actor.feed.UserFeedActor;
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
  IFeedService.class,
  FeedServiceImpl.class,
  org.sunbird.datasecurity.impl.ServiceFactory.class,
  HttpClientUtil.class
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

  @Before
  public void setUp() throws JsonProcessingException {
    PowerMockito.mockStatic(ServiceFactory.class);
    userFeed.put(JsonKey.ID, "123-456-789");
    response = new Response();
    Map<String, Object> responseMap = new HashMap<>();
    responseMap.put(Constants.RESPONSE, Arrays.asList(userFeed));
    response.getResult().putAll(responseMap);

    Response upsertResponse = new Response();
    Map<String, Object> responseMap2 = new HashMap<>();
    responseMap2.put(Constants.RESPONSE, Constants.SUCCESS);
    upsertResponse.getResult().putAll(responseMap2);
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
  }

  @Test
  public void getUserFeedTest() throws JsonProcessingException {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setRequestContext(new RequestContext());
    reqObj.setOperation(ActorOperations.GET_USER_FEED_BY_ID.getValue());
    reqObj.put(JsonKey.USER_ID, "123-456-789");
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(Duration.ofSeconds(10), Response.class);
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
      Response res = probe.expectMsgClass(Duration.ofSeconds(100), Response.class);
      return null != res && res.getResponseCode() == ResponseCode.OK;
    } else {
      ProjectCommonException res =
          probe.expectMsgClass(Duration.ofSeconds(10), ProjectCommonException.class);
      return res.getErrorCode().equals(errorCode.getErrorCode())
          || res.getErrorResponseCode() == errorCode.getResponseCode();
    }
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
      Assert.assertFalse(false);
    }
    return jsonStr;
  }
}
