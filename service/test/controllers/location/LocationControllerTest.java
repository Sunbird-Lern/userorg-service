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
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.HeaderParam;
import play.libs.Json;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import play.test.FakeApplication;
import play.test.Helpers;
import util.RequestInterceptor;

/** Created by arvind on 19/4/18. */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(PowerMockRunner.class)
@PrepareForTest(RequestInterceptor.class)
@PowerMockIgnore("javax.management.*")
public class LocationControllerTest {

  private static FakeApplication app;
  private static Map<String, String[]> headerMap;
  private static ActorSystem system;
  private static final Props props = Props.create(DummyActor.class);

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
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
  }

  @Test
  public void testcreateLocation() {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> requestBody = new HashMap<>();
    Map<String, Object> locationData = new HashMap<>();
    locationData.put(JsonKey.NAME, "CAL");
    locationData.put(JsonKey.CODE, "CA");
    locationData.put(JsonKey.TYPE, "STATE");
    requestBody.put(JsonKey.DATA, locationData);
    requestMap.put(JsonKey.REQUEST, requestBody);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/location/create").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
  }

  @Test
  public void testcreateLocationWithInvalidRequest() {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> requestBody = new HashMap<>();
    Map<String, Object> locationData = new HashMap<>();
    locationData.put(JsonKey.NAME, "CAL");
    locationData.put(JsonKey.CODE, "CA");

    requestBody.put(JsonKey.DATA, locationData);
    requestMap.put(JsonKey.REQUEST, requestBody);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/location/create").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(400, result.status());
  }

  @Test
  public void testUpdateLocation() {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put(JsonKey.NAME, "CAL");
    requestBody.put(JsonKey.CODE, "CA");
    requestBody.put(JsonKey.TYPE, "STATE");
    requestBody.put(JsonKey.ID, "123");

    requestMap.put(JsonKey.REQUEST, requestBody);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/location/update").method("PATCH");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
  }

  @Test
  public void testUpdateLocationWithInvalidRequest() {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put(JsonKey.NAME, "CAL");
    requestBody.put(JsonKey.CODE, "CA");
    requestBody.put(JsonKey.TYPE, "STATE");

    requestMap.put(JsonKey.REQUEST, requestBody);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/location/update").method("PATCH");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(400, result.status());
  }

  @Test
  public void testDeleteLocation() {
    RequestBuilder req = new RequestBuilder().uri("/v1/location/delete/123").method("DELETE");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
  }

  @Test
  public void testDeleteLocationWithInvalidRequest() {
    RequestBuilder req = new RequestBuilder().uri("/v1/location/delete/  ").method("DELETE");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(400, result.status());
  }

  @Test
  public void testSearchLocation() {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> requestBody = new HashMap<>();
    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.NAME, "CAL");
    filters.put(JsonKey.CODE, "CA");
    filters.put(JsonKey.TYPE, "STATE");
    requestBody.put(JsonKey.FILTERS, filters);
    requestMap.put(JsonKey.REQUEST, requestBody);
    String data = TestUtil.mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/location/search").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
  }
}
