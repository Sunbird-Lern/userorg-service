package util;

import static org.junit.Assert.assertEquals;
import static play.test.Helpers.route;

import controllers.BaseControllerTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;

@RunWith(PowerMockRunner.class)
@PrepareForTest(System.class)
public class FilterTest extends BaseControllerTest {

  @Before
  public void setup() {
    PowerMockito.mockStatic(System.class);
  }

  @Test
  public void testApiResponseWithGzipDisabledSuccess() {
    mockSystemSetting(false);
    headerMap.put("Accept-Encoding", new String[] {"gzip"});
    RequestBuilder req = new RequestBuilder().uri("/v1/user/type/list").method("GET");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals("application/json", result.contentType());
  }

  @Test
  public void testApiResponseWithGzipDisabledSuccessForGzippedResponse() {
    mockSystemSetting(false);
    RequestBuilder req = new RequestBuilder().uri("/v1/user/type/list").method("GET");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals("application/json", result.contentType());
  }

  @Test
  public void testApiResponseFailureGzipEnabledAndRequestedGzipResponse() {
    mockSystemSetting(true);
    RequestBuilder req = new RequestBuilder().uri("/v1/user/type/list").method("GET");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals("application/json", result.contentType());
  }

  private void mockSystemSetting(boolean isGzipEnabled) {
    PowerMockito.when(System.getenv(Mockito.anyString())).thenReturn(String.valueOf(isGzipEnabled));
  }
}
