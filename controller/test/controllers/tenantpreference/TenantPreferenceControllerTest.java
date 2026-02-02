package controllers.tenantpreference;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.BaseApplicationTest;
import controllers.DummyActor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import modules.OnRequestHandler;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.HeaderParam;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;
import util.ACTORS;

@PrepareForTest(OnRequestHandler.class)
@Ignore
public class TenantPreferenceControllerTest extends BaseApplicationTest {

  public static Map<String, List<String>> headerMap;

  @Before
  public void before() {
    setup(ACTORS.TENANT_PREFERENCE_ACTOR, DummyActor.class);
    headerMap = new HashMap<>();
    headerMap.put(HeaderParam.X_Consumer_ID.getName(), Arrays.asList("Some consumer ID"));
    headerMap.put(HeaderParam.X_Device_ID.getName(), Arrays.asList("Some device ID"));
    headerMap.put(
        HeaderParam.X_Authenticated_Userid.getName(), Arrays.asList("Some authenticated user ID"));
    headerMap.put(JsonKey.MESSAGE_ID, Arrays.asList("Some message ID"));
    headerMap.put(HeaderParam.X_APP_ID.getName(), Arrays.asList("Some app Id"));
  }

  @Test
  public void testCreateTenantPreferenceSuccess() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.KEY, "teacher");
    innerMap.put(JsonKey.ORG_ID, "organisationId");
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.FIELDS, new ArrayList<>());
    innerMap.put(JsonKey.DATA, map);
    requestMap.put(JsonKey.REQUEST, innerMap);
    Result result = performTest("/v2/org/preferences/create", "POST", requestMap);
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
  }

  @Test
  public void testCreatePreferenceWithMandatoryParamOrgId() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.KEY, "teacher");
    requestMap.put(JsonKey.REQUEST, innerMap);
    Result result = performTest("/v2/org/preferences/create", "POST", requestMap);
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
  }

  @Test
  public void testCreateTenantPreferenceWithDataTypeError() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.KEY, "teacher");
    innerMap.put(JsonKey.ORG_ID, "organisationId");
    innerMap.put(JsonKey.DATA, "data");
    requestMap.put(JsonKey.REQUEST, innerMap);
    Result result = performTest("/v2/org/preferences/create", "POST", requestMap);
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
  }

  @Test
  public void testUpdateTenantPreferenceSuccess() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.KEY, "teacher");
    innerMap.put(JsonKey.ORG_ID, "organisationId");
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.FIELDS, new ArrayList<>());
    innerMap.put(JsonKey.DATA, map);
    requestMap.put(JsonKey.REQUEST, innerMap);
    Result result = performTest("/v2/org/preferences/update", "PATCH", requestMap);
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
  }

  @Test
  public void testReadTenantPreferenceSuccess() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.KEY, "teacher");
    innerMap.put(JsonKey.ORG_ID, "organisationId");
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.FIELDS, new ArrayList<>());
    requestMap.put(JsonKey.REQUEST, innerMap);
    Result result = performTest("/v2/org/preferences/read", "POST", requestMap);
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
  }

  @Test
  public void testReadPreferenceWithoutMandatoryParamOrgId() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.KEY, "teacher");
    innerMap.put(JsonKey.ORG_ID, "");
    requestMap.put(JsonKey.REQUEST, innerMap);
    Result result = performTest("/v2/org/preferences/read", "POST", requestMap);
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
  }

  public String mapToJson(Map map) {
    ObjectMapper mapperObj = new ObjectMapper();
    String jsonResp = "";

    if (map != null) {
      try {
        jsonResp = mapperObj.writeValueAsString(map);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return jsonResp;
  }

  public Result performTest(String url, String method, Map map) {
    String data = mapToJson(map);
    Http.RequestBuilder req;
    if (StringUtils.isNotBlank(data)) {
      JsonNode json = Json.parse(data);
      req = new Http.RequestBuilder().bodyJson(json).uri(url).method(method);
    } else {
      req = new Http.RequestBuilder().uri(url).method(method);
    }
    // req.headers(new Http.Headers(headerMap));
    Result result = Helpers.route(application, req);
    return result;
  }
}
