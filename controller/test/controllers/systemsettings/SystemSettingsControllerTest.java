package controllers.systemsettings;

import static controllers.TestUtil.mapToJson;
import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
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

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "jdk.internal.reflect.*", "javax.crypto.*"})
@PrepareForTest(OnRequestHandler.class)
@Ignore
public class SystemSettingsControllerTest extends BaseApplicationTest {

  private static Map<String, String[]> headerMap;

  @Before
  public void before() {
    setup(ACTORS.SYSTEM_SETTINGS_ACTOR, DummyActor.class);
    headerMap = new HashMap<String, String[]>();
    headerMap.put(HeaderParam.X_Consumer_ID.getName(), new String[] {"Service test consumer"});
    headerMap.put(HeaderParam.X_Device_ID.getName(), new String[] {"Some Device Id"});
    headerMap.put(
        HeaderParam.X_Authenticated_Userid.getName(), new String[] {"Authenticated user id"});
    headerMap.put(JsonKey.MESSAGE_ID, new String[] {"Unique Message id"});
  }

  @Test
  public void testGetSystemSettingSuccess() {
    RequestBuilder req =
        new RequestBuilder().uri("/v1/system/settings/get/isRootOrgInitialised").method("GET");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void testGetAllSystemSettingsSuccess() {
    RequestBuilder req = new RequestBuilder().uri("/v1/system/settings/list").method("GET");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void testSetSystemSettingSuccess() {
    JsonNode json = createSetSystemSettingRequest("defaultRootOrgId", "defaultRootOrgId", "org123");
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/system/settings/set").method("POST");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void testSetSystemSettingFailureWithoutId() {
    JsonNode json = createSetSystemSettingRequest(null, "defaultRootOrgId", "org123");
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/system/settings/set").method("POST");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(400, result.status());
  }

  @Test
  public void testSetSystemSettingFailureWithoutField() {
    JsonNode json = createSetSystemSettingRequest("defaultRootOrgId", null, "org123");
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/system/settings/set").method("POST");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(400, result.status());
  }

  @Test
  public void testSetSystemSettingFailureWithoutValue() {
    JsonNode json = createSetSystemSettingRequest("defaultRootOrgId", "defaultRootOrgId", null);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/system/settings/set").method("POST");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);

    assertEquals(400, result.status());
  }

  private JsonNode createSetSystemSettingRequest(String id, String field, String value) {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    if (id != null) innerMap.put(JsonKey.ID, id);
    if (field != null) innerMap.put(JsonKey.FIELD, field);
    if (value != null) innerMap.put(JsonKey.VALUE, value);
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);

    return Json.parse(data);
  }
}
