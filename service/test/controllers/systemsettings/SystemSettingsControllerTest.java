package controllers.systemsettings;

import static controllers.TestUtil.mapToJson;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.when;
import static play.test.Helpers.route;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseControllerTest;
import java.util.HashMap;
import java.util.Map;
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
import play.libs.Json;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import util.RequestInterceptor;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(PowerMockRunner.class)
@PrepareForTest(RequestInterceptor.class)
@PowerMockIgnore("javax.management.*")
public class SystemSettingsControllerTest extends BaseControllerTest {

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
  public void testGetAllSystemSettingsSuccess() {
    RequestBuilder req = new RequestBuilder().uri("/v1/system/settings/list").method("GET");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
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
}
