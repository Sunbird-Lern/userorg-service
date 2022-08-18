package controllers.location;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseApplicationTest;
import controllers.DummyActor;
import controllers.TestUtil;
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
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.HeaderParam;
import play.libs.Json;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import play.test.Helpers;
import util.ACTORS;
import util.RequestInterceptor;

/** @author arvind on 19/4/18. */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "jdk.internal.reflect.*", "javax.crypto.*"})
@PrepareForTest(OnRequestHandler.class)
@Ignore
public class LocationControllerTest extends BaseApplicationTest {

  private static Map<String, String[]> headerMap;
  private static final String LOCATION_NAME = "Laddakh";
  private static final String LOCATION_CODE = "LOC_01";
  private static final String LOCATION_TYPE = "State";
  private static final String LOCATION_ID = "123";
  private static final String CREATE_LOCATION_URL = "/v1/location/create";
  private static final String UPDATE_LOCATION_URL = "/v1/location/update";
  private static final String DELETE_LOCATION_URL = "/v1/location/delete";
  private static final String SEARCH_LOCATION_URL = "/v1/location/search";

  @Before
  public void before() {
    setup(ACTORS.LOCATION_ACTOR, DummyActor.class);
    headerMap = new HashMap<String, String[]>();
    headerMap.put(
        HeaderParam.X_Authenticated_User_Token.getName(),
        new String[] {"Authenticated user token"});
    headerMap.put(JsonKey.MESSAGE_ID, new String[] {"Unique Message id"});
  }

  @Test
  public void testCreateLocation() {
    Map userAuthentication = new HashMap<String, String>();
    userAuthentication.put(JsonKey.USER_ID, "uuiuhcf784508 8y8c79-fhh");
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject(), Mockito.anyMap()))
        .thenReturn(userAuthentication);
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> locationData = new HashMap<>();
    locationData.put(JsonKey.NAME, LOCATION_NAME);
    locationData.put(JsonKey.CODE, LOCATION_CODE);
    locationData.put(JsonKey.LOCATION_TYPE, LOCATION_TYPE);
    requestMap.put(JsonKey.REQUEST, locationData);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri(CREATE_LOCATION_URL).method("POST");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void testCreateLocationWithoutType() {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> locationData = new HashMap<>();
    locationData.put(JsonKey.NAME, LOCATION_NAME);
    locationData.put(JsonKey.CODE, LOCATION_CODE);
    requestMap.put(JsonKey.REQUEST, locationData);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri(CREATE_LOCATION_URL).method("POST");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(400, result.status());
  }

  @Test
  public void testUpdateLocation() {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put(JsonKey.NAME, LOCATION_NAME);
    requestBody.put(JsonKey.CODE, LOCATION_CODE);
    requestBody.put(JsonKey.ID, LOCATION_ID);

    requestMap.put(JsonKey.REQUEST, requestBody);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri(UPDATE_LOCATION_URL).method("PATCH");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void testUpdateLocationWithType() {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put(JsonKey.LOCATION_TYPE, LOCATION_TYPE);
    requestBody.put(JsonKey.ID, LOCATION_ID);
    requestMap.put(JsonKey.REQUEST, requestBody);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri(UPDATE_LOCATION_URL).method("PATCH");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(400, result.status());
  }

  @Test
  public void testDeleteLocation() {
    RequestBuilder req =
        new RequestBuilder().uri(DELETE_LOCATION_URL + "/" + LOCATION_ID).method("DELETE");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void testSearchLocation() {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> requestBody = new HashMap<>();
    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.NAME, LOCATION_NAME);
    filters.put(JsonKey.CODE, LOCATION_CODE);
    filters.put(JsonKey.LOCATION_TYPE, LOCATION_TYPE);
    requestBody.put(JsonKey.FILTERS, filters);
    requestMap.put(JsonKey.REQUEST, requestBody);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri(SEARCH_LOCATION_URL).method("POST");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }
}
