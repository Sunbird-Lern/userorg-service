package util;

import controllers.BaseApplicationTest;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import play.test.Helpers;

import static org.junit.Assert.assertEquals;

@Ignore
@RunWith(PowerMockRunner.class)
@PrepareForTest(System.class)
public class FilterTest extends BaseApplicationTest {

  @Before
  public void setup() {
    PowerMockito.mockStatic(System.class);
  }

  @Test
  public void testApiResponseWithGzipDisabledSuccess() {
    mockSystemSetting(false);
    //headerMap.put("Accept-Encoding", new String[] {"gzip"});
    RequestBuilder req = new RequestBuilder().uri("/v1/user/type/list").method("GET");
    //req.headers(headerMap);
    Result result = Helpers.route(application,req);
    assertEquals("application/json", result.contentType());
  }

  @Test
  public void testApiResponseWithGzipDisabledSuccessForGzippedResponse() {
    mockSystemSetting(false);
    RequestBuilder req = new RequestBuilder().uri("/v1/user/type/list").method("GET");
    //req.headers(headerMap);
    Result result = Helpers.route(application,req);
    assertEquals("application/json", result.contentType());
  }

  @Test
  public void testApiResponseFailureGzipEnabledAndRequestedGzipResponse() {
    mockSystemSetting(true);
    RequestBuilder req = new RequestBuilder().uri("/v1/user/type/list").method("GET");
    //req.headers(headerMap);
    Result result = Helpers.route(application,req);
    assertEquals("application/json", result.contentType());
  }

  private void mockSystemSetting(boolean isGzipEnabled) {
    PowerMockito.when(System.getenv(Mockito.anyString())).thenReturn(String.valueOf(isGzipEnabled));
  }
}
