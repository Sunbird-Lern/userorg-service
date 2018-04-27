package controllers.location;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.when;
import static play.test.Helpers.route;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import controllers.DummyActor;
import controllers.TestUtil;
import java.util.HashMap;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.models.util.GeoLocationJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.HeaderParam;
import play.libs.Json;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import play.test.FakeApplication;
import play.test.Helpers;
import util.RequestInterceptor;

/** @author arvind on 19/4/18. */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(PowerMockRunner.class)
@PrepareForTest(RequestInterceptor.class)
@PowerMockIgnore("javax.management.*")
public class LocationControllerTest {

  private static FakeApplication app;
  private static Map<String, String[]> headerMap;
  private static ActorSystem system;
  private static final Props props = Props.create(DummyActor.class);
  private static final String USER_TOKEN = "{userId} uuiuhcf784508 8y8c79-fhh";
  private static final String LOCATION_NAME = "Laddakh";
  private static final String LOCATION_CODE = "LOC_01";
  private static final String LOCATION_TYPE = "State";
  private static final String LOCATION_ID = "123";
  private static final String CREATE_LOCATION_URL = "/v1/location/create";
  private static final String UPDATE_LOCATION_URL = "/v1/location/update";
  private static final String DELETE_LOCATION_URL = "/v1/location/delete";
  private static final String SEARCH_LOCATION_URL = "/v1/location/search";

  @BeforeClass
  public static void startApp() {
    app = Helpers.fakeApplication();
    Helpers.start(app);
    headerMap = new HashMap<String, String[]>();

    headerMap.put(
        HeaderParam.X_Access_TokenId.getName(), new String[] {"Authenticated user token"});
    headerMap.put(JsonKey.MESSAGE_ID, new String[] {"Unique Message id"});

    system = ActorSystem.create("system");
    ActorRef subject = system.actorOf(props);
    BaseController.setActorRef(subject);

    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject())).thenReturn(USER_TOKEN);
  }

  @Test
  public void testCreateLocation() {

    when(RequestInterceptor.verifyRequestData(Mockito.anyObject())).thenReturn(USER_TOKEN);
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> locationData = new HashMap<>();
    locationData.put(JsonKey.NAME, LOCATION_NAME);
    locationData.put(JsonKey.CODE, LOCATION_CODE);
    locationData.put(GeoLocationJsonKey.LOCATION_TYPE, LOCATION_TYPE);
    requestMap.put(JsonKey.REQUEST, locationData);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri(CREATE_LOCATION_URL).method("POST");
    req.headers(headerMap);
    Result result = route(req);
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
    req.headers(headerMap);
    Result result = route(req);
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
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
  }

  @Test
  public void testUpdateLocationWithType() {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put(GeoLocationJsonKey.LOCATION_TYPE, LOCATION_TYPE);
    requestBody.put(JsonKey.ID, LOCATION_ID);
    requestMap.put(JsonKey.REQUEST, requestBody);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri(UPDATE_LOCATION_URL).method("PATCH");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(400, result.status());
  }

  @Test
  public void testDeleteLocation() {
    RequestBuilder req =
        new RequestBuilder().uri(DELETE_LOCATION_URL + "/" + LOCATION_ID).method("DELETE");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
  }

  @Test
  public void testSearchLocation() {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> requestBody = new HashMap<>();
    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.NAME, LOCATION_NAME);
    filters.put(JsonKey.CODE, LOCATION_CODE);
    filters.put(GeoLocationJsonKey.LOCATION_TYPE, LOCATION_TYPE);
    requestBody.put(JsonKey.FILTERS, filters);
    requestMap.put(JsonKey.REQUEST, requestBody);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri(SEARCH_LOCATION_URL).method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
  }
}
