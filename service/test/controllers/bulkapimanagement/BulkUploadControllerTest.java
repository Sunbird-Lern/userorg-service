package controllers.bulkapimanagement;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.BaseApplicationTest;
import controllers.DummyActor;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import modules.OnRequestHandler;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.models.util.GeoLocationJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import play.test.Helpers;
import util.RequestInterceptor;

/** Created by arvind on 4/12/17. */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "jdk.internal.reflect.*"})
@PrepareForTest(OnRequestHandler.class)
public class BulkUploadControllerTest extends BaseApplicationTest {

  @Before
  public void before() {
    setup(DummyActor.class);
  }

  @Test
  public void testUploadUser() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.DATA, "sampleStream".getBytes(Charset.defaultCharset()));
    innerMap.put(JsonKey.ORGANISATION_ID, "org123");
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);

    JsonNode json = Json.parse(data);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/user/upload").method("POST");
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void testUploadOrg() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.DATA, "sampleStream".getBytes(Charset.defaultCharset()));
    // innerMap.put(JsonKey.ORGANISATION_ID , "org123");
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);

    JsonNode json = Json.parse(data);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/org/upload").method("POST");
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void testGetUploadStatus() {
    Http.RequestBuilder req = new Http.RequestBuilder().uri("/v1/upload/status/pid").method("GET");
    Result result = Helpers.route(application, req);
    Assert.assertEquals(200, result.status());
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

  @Test
  public void testUploadLocationWithProperData() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.DATA, "sampleStream".getBytes(Charset.defaultCharset()));
    innerMap.put(GeoLocationJsonKey.LOCATION_TYPE, "State");
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/bulk/location/upload").method("POST");
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void testUploadLocationWithoutMandatoryParamtype() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.DATA, "sampleStream".getBytes(Charset.defaultCharset()));
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/bulk/location/upload").method("POST");
    Result result = Helpers.route(application, req);
    assertEquals(400, result.status());
  }
}
