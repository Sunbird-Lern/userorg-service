package controllers.badging;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.BaseApplicationTest;
import controllers.DummyActor;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import modules.OnRequestHandler;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import play.libs.Json;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import play.test.Helpers;
import util.RequestInterceptor;

/**
 * Test class for BadgeIssuerController.
 *
 * @author Arvind, B Vinaya Kumar
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "jdk.internal.reflect.*"})
@PrepareForTest(OnRequestHandler.class)
public class BadgeIssuerControllerTest extends BaseApplicationTest {

  @Before
  public void before() {
    setup(DummyActor.class);
  }

  @Test
  public void testCreateBadgeIssuer() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.NAME, "goldBadge");
    innerMap.put(JsonKey.DESCRIPTION, "golden award");
    innerMap.put(JsonKey.EMAIL, "abc@gmail.com");
    innerMap.put(JsonKey.URL, "http://example.com");
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);

    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/issuer/create").method("POST");
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void testCreateBadgeIssuerMissingUrl() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.NAME, "goldBadge");
    innerMap.put(JsonKey.DESCRIPTION, "golden award");
    innerMap.put(JsonKey.EMAIL, "abc@gmail.com");
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);

    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/issuer/create").method("POST");
    Result result = Helpers.route(application, req);
    assertEquals(400, result.status());
  }

  @Test
  public void testCreateBadgeIssuerInvalidUrl() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.NAME, "goldBadge");
    innerMap.put(JsonKey.DESCRIPTION, "golden award");
    innerMap.put(JsonKey.EMAIL, "abc@gmail.com");
    innerMap.put(JsonKey.URL, "https://mail.");
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);

    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/issuer/create").method("POST");
    Result result = Helpers.route(application, req);
    assertEquals(400, result.status());
  }

  @Test
  public void testGetBadgeIssuer() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");

    RequestBuilder req = new RequestBuilder().uri("/v1/issuer/read/123").method("GET");
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void testGetAllIssuer() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");

    RequestBuilder req = new RequestBuilder().uri("/v1/issuer/list").method("GET");
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void testDeleteBadgeIssuer() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");

    RequestBuilder req = new RequestBuilder().uri("/v1/issuer/delete/123").method("DELETE");
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  private static String mapToJson(Map map) {
    ObjectMapper mapperObj = new ObjectMapper();
    String jsonResp = "";
    try {
      jsonResp = mapperObj.writeValueAsString(map);
    } catch (IOException e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    return jsonResp;
  }
}
