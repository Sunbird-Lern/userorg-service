package controllers.feed;

import static org.junit.Assert.assertEquals;

import controllers.BaseApplicationTest;
import controllers.DummyActor;
import controllers.feed.validator.FeedRequestValidator;
import modules.OnRequestHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "jdk.internal.reflect.*"})
@PrepareForTest({OnRequestHandler.class, FeedRequestValidator.class})
public class FeedControllerTest extends BaseApplicationTest {

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
}
