package controllers.usermanagement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.route;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.BaseControllerTest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.responsecode.ResponseCode;
import play.libs.Json;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import play.test.Helpers;

public class UserStatusControllerTest extends BaseControllerTest {

  private static String userId = "user-id";

  @Test
  public void testBlockUserSuccess() {

    JsonNode json = getRequestedJsonData(userId);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/user/block").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains("success"));
    assertEquals(200, result.status());
  }

  @Test
  public void testBlockUserFailureWithouUserId() {

    JsonNode json = getRequestedJsonData(null);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/user/block").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testUnBlockUserSuccess() {

    JsonNode json = getRequestedJsonData(userId);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/user/unblock").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains("success"));
    assertEquals(200, result.status());
  }

  @Test
  public void testUnBlockUserFailureWithoutUserId() {

    JsonNode json = getRequestedJsonData(null);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/user/unblock").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }

  public static String mapToJson(Map map) {
    ObjectMapper mapperObj = new ObjectMapper();
    String jsonResp = "";
    try {
      jsonResp = mapperObj.writeValueAsString(map);
    } catch (IOException e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    return jsonResp;
  }

  private JsonNode getRequestedJsonData(String userId) {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.USER_ID, userId);
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);
    return Json.parse(data);
  }
}
