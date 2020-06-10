package controllers.metrics;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.when;

import controllers.BaseApplicationTest;
import controllers.DummyActor;
import java.util.HashMap;
import java.util.Map;
import modules.OnRequestHandler;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.HeaderParam;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import play.test.Helpers;
import util.RequestInterceptor;

/** Created by arvind on 4/12/17. */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "jdk.internal.reflect.*"})
@PrepareForTest(OnRequestHandler.class)
@Ignore
public class OrganisationMetricsControllerTest extends BaseApplicationTest {

  private static Map<String, String[]> headerMap;

  @Before
  public void before() {
    setup(DummyActor.class);
    headerMap = new HashMap<String, String[]>();
    headerMap.put(HeaderParam.X_Consumer_ID.getName(), new String[] {"Service test consumer"});
    headerMap.put(HeaderParam.X_Device_ID.getName(), new String[] {"Some Device Id"});
    headerMap.put(
        HeaderParam.X_Authenticated_Userid.getName(), new String[] {"Authenticated user id"});
    headerMap.put(JsonKey.MESSAGE_ID, new String[] {"Unique Message id"});
  }

  @Test
  public void testcourseProgress() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
    RequestBuilder req =
        new RequestBuilder().uri("/v1/dashboard/creation/org/orgId?period=7d").method("GET");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void testorgConsumption() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
    RequestBuilder req =
        new RequestBuilder().uri("/v1/dashboard/consumption/org/orgId?period=7d").method("GET");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void testorgCreationReport() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
    RequestBuilder req =
        new RequestBuilder().uri("/v1/dashboard/creation/org/orgId/export?period=7d").method("GET");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void testorgConsumptionReport() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
    RequestBuilder req =
        new RequestBuilder()
            .uri("/v1/dashboard/consumption/org/orgId/export?period=7d")
            .method("GET");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }
}
