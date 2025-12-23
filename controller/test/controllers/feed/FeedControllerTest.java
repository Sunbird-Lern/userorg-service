package controllers.feed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.pekko.http.javadsl.model.HttpMethods;
import controllers.BaseApplicationTest;
import controllers.DummyActor;
import controllers.feed.validator.FeedRequestValidator;
import java.util.HashMap;
import java.util.Map;
import modules.OnRequestHandler;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;
import util.ACTORS;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "jdk.internal.reflect.*", "javax.crypto.*"})
@PrepareForTest({OnRequestHandler.class, FeedRequestValidator.class})
@Ignore
public class FeedControllerTest extends BaseApplicationTest {

  String SAVE_FEED_URL = "/v1/user/feed/create";
  String UPDATE_FEED_URL = "/v1/user/feed/update";
  String DELETE_FEED_URL = "/v1/user/feed/delete";

  @Before
  public void before() {
    setup(ACTORS.USER_FEED_ACTOR, DummyActor.class);
  }

  @Test
  public void testGetUserFeedUnAuthorized() {
    Http.RequestBuilder req =
        new Http.RequestBuilder().uri("/v1/user/feed/1234567890").method("GET");
    Result result = Helpers.route(application, req);
    assertEquals(401, result.status());
  }

  @Test
  public void testGetUserFeed() {
    PowerMockito.mockStatic(FeedRequestValidator.class);
    PowerMockito.when(
            FeedRequestValidator.userIdValidation(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(true)
        .thenReturn(false);
    Http.RequestBuilder req =
        new Http.RequestBuilder().uri("/v1/user/feed/1234567890").method("GET");
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void testUpdateUserFeed() {
    Result result = performTest(UPDATE_FEED_URL, HttpMethods.PATCH.name(), updateFeedRequest(true));
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
    assertTrue(getResponseStatus(result) == 200);
  }

  public void testCreateUserFeed() {
    Result result = performTest(SAVE_FEED_URL, HttpMethods.POST.name(), createFeedRequest(true));
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testUpdateUserFeedFailureForUserID() {
    Http.RequestBuilder req = new Http.RequestBuilder().uri("/v1/user/feed/update").method("PATCH");
    Result result =
        performTest(UPDATE_FEED_URL, HttpMethods.PATCH.name(), updateFeedRequest(false));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testDeleteUserFeed() {
    Result result = performTest(DELETE_FEED_URL, HttpMethods.POST.name(), updateFeedRequest(true));
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
    assertTrue(getResponseStatus(result) == 200);
  }

  private Map updateFeedRequest(boolean setUserid) {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> dataMap = new HashMap<>();
    if (setUserid) {
      dataMap.put(JsonKey.USER_ID, "userId");
    }
    dataMap.put(JsonKey.CATEGORY, "someCategory");
    dataMap.put(JsonKey.FEED_ID, "someFeedId");
    requestMap.put(JsonKey.REQUEST, dataMap);
    return requestMap;
  }

  private Map createFeedRequest(boolean setUserid) {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> dataMap = new HashMap<>();
    if (setUserid) {
      dataMap.put(JsonKey.USER_ID, "someUserId");
    }
    dataMap.put(JsonKey.CATEGORY, "someCategory");
    dataMap.put(JsonKey.PRIORITY, 1);
    requestMap.put(JsonKey.REQUEST, dataMap);
    return requestMap;
  }
}
