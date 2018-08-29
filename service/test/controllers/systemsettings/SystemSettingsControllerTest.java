package controllers.systemsettings;

import static controllers.TestUtil.mapToJson;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.when;
import static play.test.Helpers.route;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import controllers.DummyActor;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
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

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(PowerMockRunner.class)
@PrepareForTest(RequestInterceptor.class)
@PowerMockIgnore("javax.management.*")
public class SystemSettingsControllerTest {

  private static FakeApplication app;
  private static ActorSystem system;
  private static final Props props = Props.create(DummyActor.class);
  private static Map<String, String[]> headerMap;

  @BeforeClass
  public static void startApp() {
    app = Helpers.fakeApplication();
    Helpers.start(app);
    headerMap = new HashMap<String, String[]>();
    headerMap.put(HeaderParam.X_Consumer_ID.getName(), new String[] {"Service test consumer"});
    headerMap.put(HeaderParam.X_Device_ID.getName(), new String[] {"Some Device Id"});
    headerMap.put(
        HeaderParam.X_Authenticated_Userid.getName(), new String[] {"Authenticated user id"});
    headerMap.put(JsonKey.MESSAGE_ID, new String[] {"Unique Message id"});
    system = ActorSystem.create("system");
    ActorRef subject = system.actorOf(props);
    BaseController.setActorRef(subject);
  }

  @Before
  public void beforeTest() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
  }

  @Test
  public void testGetSystemSettingSuccess() {
    RequestBuilder req =
        new RequestBuilder().uri("/v1/system/setting/get/isRootOrgInitialised").method("GET");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
  }

  @Test
  public void testGetSystemSettingFailureWithInvalidId() {
    RequestBuilder req =
        new RequestBuilder().uri("/v1/system/setting/get/notvalidsettingid").method("GET");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(404, result.status());
  }

  @Test
  public void testGetAllSystemSettingsSuccess() {
    RequestBuilder req = new RequestBuilder().uri("/v1/system/settings/list").method("GET");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
  }

  public JsonNode createSetSystemRequest(String id, String field, String value) {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    if (id != null) innerMap.put(JsonKey.ID, id);
    if (field != null) innerMap.put(JsonKey.FIELD, field);
    if (value != null) innerMap.put(JsonKey.VALUE, value);
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);

    return Json.parse(data);
  }

  @Test
  public void testSetSystemSettingSuccess() {
    JsonNode json = createSetSystemRequest("defaultRootOrgId", "defaultRootOrgId", "org123");
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/system/setting/set").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
  }

  @Test
  public void testSetSystemSettingFailureWithoutId() {
    JsonNode json = createSetSystemRequest(null, "defaultRootOrgId", "org123");
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/system/setting/set").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(400, result.status());
  }

  @Test
  public void testSetSystemSettingFailureWithoutField() {
    JsonNode json = createSetSystemRequest("defaultRootOrgId", null, "org123");
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/system/setting/set").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(400, result.status());
  }

  @Test
  public void testSetSystemSettingFailureWithoutValue() {
    JsonNode json = createSetSystemRequest("defaultRootOrgId", "defaultRootOrgId", null);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/system/setting/set").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(400, result.status());
  }
}
