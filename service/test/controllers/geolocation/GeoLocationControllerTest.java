package controllers.geolocation;

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
import org.sunbird.common.request.HeaderParam;
import play.libs.Json;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import play.test.Helpers;
import util.RequestInterceptor;

/** Created by arvind on 1/12/17. */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "jdk.internal.reflect.*"})
@PrepareForTest(OnRequestHandler.class)
public class GeoLocationControllerTest extends BaseApplicationTest {

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
  public void testcreateGeoLocationFailure() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.ROOT_ORG_ID, "org123");
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);

    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/location/create").method("POST");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(400, result.status());
  }

  @Test
  public void testupdateGeoLocation() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.ROOT_ORG_ID, "org123");
    innerMap.put(JsonKey.ID, "123");
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);

    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/location/update").method("PATCH");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void testdeleteGeoLocation() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.ROOT_ORG_ID, "org123");
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);

    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/location/delete/123").method("DELETE");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void testsendNotification() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.ROOT_ORG_ID, "org123");
    innerMap.put(JsonKey.TO, "c93");
    Map map = new HashMap();
    map.put(JsonKey.FROM_EMAIL, "fromEmail");
    innerMap.put(JsonKey.DATA, map);
    innerMap.put(JsonKey.TYPE, "fcm");
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);

    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/notification/send").method("POST");
    // req.headers(headerMap);
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
