package controllers.usermanagement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.route;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.BaseControllerTest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.responsecode.ResponseCode;
import play.libs.Json;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import play.test.Helpers;

public class UserRoleControllerTest extends BaseControllerTest {

  private static String role = "user";
  private static String userId = "user-id";
  private static String orgId = "org-id";

  @Test
  public void testAssignRolesSuccess() {

    String data = getRequestedJsonData(true, true, true, role);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/user/assign/role").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains("success"));
    assertEquals(200, result.status());
  }

  @Test
  public void testAssignRolesFailueWithoutOrgId() {

    String data = getRequestedJsonData(true, false, true, role);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/user/assign/role").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testAssignRolesFailueWithoutUserId() {

    String data = getRequestedJsonData(false, true, true, role);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/user/assign/role").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testAssignRolesFailueWithoutRoles() {

    String data = getRequestedJsonData(true, true, false, null);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/user/assign/role").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testUpdateAssignedRolesFailureWithEmptyRole() {

    String data = getRequestedJsonData(true, true, true, null);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/user/assign/role").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.emptyRolesProvided.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testGetAllRolesSuccess() {
    RequestBuilder req = new RequestBuilder().uri("/v1/role/read").method("GET");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains("success"));
    assertEquals(200, result.status());
  }

  private String getRequestedJsonData(
      boolean isUserIdReq, boolean isOrgReq, boolean isRoleReq, String role) {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();

    if (isUserIdReq) innerMap.put(JsonKey.USER_ID, userId);
    if (isOrgReq) innerMap.put(JsonKey.ORGANISATION_ID, orgId);
    if (isRoleReq) {
      List<String> roles = new ArrayList<>();
      if (role != null) roles.add(role);
      innerMap.put(JsonKey.ROLES, roles);
    }
    requestMap.put(JsonKey.REQUEST, innerMap);
    return mapToJson(requestMap);
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
}
