package controllers.organisationmanagement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.BaseApplicationTest;
import controllers.DummyActor;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import modules.OnRequestHandler;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.response.ResponseParams;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.responsecode.ResponseCode;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;

@PrepareForTest(OnRequestHandler.class)
@PowerMockIgnore({"javax.management.*", "jdk.internal.reflect.*"})
public class OrgTypeControllerTest extends BaseApplicationTest {

  private static String orgTypeName = "org-type-name";
  private static String id = "id";

  @Before
  public void before() {
    setup(DummyActor.class);
  }

  @Test
  public void testCreateOrgTypeSuccess() {
    Result result =
        performTest(
            "/v1/org/type/create", "POST", createOrgTypeRequest(true, orgTypeName, false, null));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testCreateOrgTypeFailureWithEmptyName() {

    Result result =
        performTest("/v1/org/type/create", "POST", createOrgTypeRequest(true, null, false, null));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testCreateOrgTypeFailureWithoutName() {

    Result result =
        performTest("/v1/org/type/create", "POST", createOrgTypeRequest(false, null, false, null));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testListOrgTypeListSuccess() {
    Result result = performTest("/v1/org/type/list", "GET", null);
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testUpdateOrgTypeSuccess() {
    Result result =
        performTest(
            "/v1/org/type/update", "PATCH", createOrgTypeRequest(true, orgTypeName, true, id));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testUpdateOrgTypeFailureWithoutName() {
    Result result =
        performTest("/v1/org/type/update", "PATCH", createOrgTypeRequest(false, null, true, id));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testUpdateOrgTypeFailureWithEmptyName() {
    Result result =
        performTest("/v1/org/type/update", "PATCH", createOrgTypeRequest(true, null, true, id));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testUpdateOrgTypeFailureWithoutId() {
    Result result =
        performTest(
            "/v1/org/type/update", "PATCH", createOrgTypeRequest(true, orgTypeName, false, null));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  private Map createOrgTypeRequest(boolean isName, String name, boolean isId, String id) {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    if (isName) innerMap.put(JsonKey.NAME, name);
    if (isId) innerMap.put(JsonKey.ID, id);
    requestMap.put(JsonKey.REQUEST, innerMap);
    return requestMap;
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

  public String mapToJson(Map map) {
    ObjectMapper mapperObj = new ObjectMapper();
    String jsonResp = "";

    if (map != null) {
      try {
        jsonResp = mapperObj.writeValueAsString(map);
      } catch (IOException e) {
        ProjectLogger.log(e.getMessage(), e);
      }
    }
    return jsonResp;
  }

  public String getResponseCode(Result result) {
    String responseStr = Helpers.contentAsString(result);
    ObjectMapper mapper = new ObjectMapper();

    try {
      Response response = mapper.readValue(responseStr, Response.class);

      if (response != null) {
        ResponseParams params = response.getParams();
        return params.getStatus();
      }
    } catch (Exception e) {
      ProjectLogger.log(
          "BaseControllerTest:getResponseCode: Exception occurred with error message = "
              + e.getMessage(),
          LoggerEnum.ERROR.name());
    }
    return "";
  }

  public int getResponseStatus(Result result) {
    return result.status();
  }
}
