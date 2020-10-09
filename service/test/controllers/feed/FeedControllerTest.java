package controllers.feed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import akka.http.javadsl.model.HttpMethods;
import controllers.BaseApplicationTest;
import controllers.DummyActor;
import controllers.feed.validator.FeedRequestValidator;
import java.util.HashMap;
import java.util.Map;
import modules.OnRequestHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest({OnRequestHandler.class, FeedRequestValidator.class})
public class FeedControllerTest extends BaseApplicationTest {

  String SAVE_FEED_URL = "/v1/user/feed/create";
  String UPDATE_FEED_URL = "/v1/user/feed/update";

  @Before
  public void before() {
    setup(DummyActor.class);
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
            FeedRequestValidator.userIdValidation(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(true)
        .thenReturn(false);
    Http.RequestBuilder req =
        new Http.RequestBuilder().uri("/v1/user/feed/1234567890").method("GET");
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void testUpdateUserFeed() {
    Http.RequestBuilder req = new Http.RequestBuilder().uri("/v1/user/feed/update").method("PATCH");
    Result result = performTest(UPDATE_FEED_URL, HttpMethods.PATCH.name(), updateFeedRequest(true));
    assertEquals(getResponseCode(result), ResponseCode.success.name());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testUpdateUserFeedFailureForUserID() {
    Http.RequestBuilder req = new Http.RequestBuilder().uri("/v1/user/feed/update").method("PATCH");
    Result result =
        performTest(UPDATE_FEED_URL, HttpMethods.PATCH.name(), updateFeedRequest(false));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  private Map updateFeedRequest(boolean setUserid) {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> dataMap = new HashMap<>();
    if (setUserid) {
      dataMap.put(JsonKey.USER_ID, "someUserId");
    }
    dataMap.put(JsonKey.FEED_ID, "someFeedId");
    dataMap.put(JsonKey.CATEGORY, "someCategory");
    dataMap.put(JsonKey.STATUS, "someStatus");
    requestMap.put(JsonKey.REQUEST, dataMap);
    return requestMap;
  }
}
