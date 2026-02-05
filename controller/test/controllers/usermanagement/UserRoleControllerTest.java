package controllers.usermanagement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.BaseApplicationTest;
import controllers.DummyActor;
import java.io.IOException;
import java.util.*;
import modules.OnRequestHandler;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.response.Response;
import org.sunbird.response.ResponseParams;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;
import util.ACTORS;

@PrepareForTest(OnRequestHandler.class)
@PowerMockIgnore({"javax.management.*", "jdk.internal.reflect.*", "javax.crypto.*"})
@Ignore
public class UserRoleControllerTest extends BaseApplicationTest {

  private static String role = "someRole";
  private static String userId = "someUserId";
  private static String orgId = "someOrgId";

  @Before
  public void before() throws Exception {
    setup(Arrays.asList(ACTORS.USER_ROLE_ACTOR, ACTORS.FETCH_USER_ROLE_ACTOR), DummyActor.class);
  }

  @Test
  public void testAssignRolesSuccess() {
    // setup(DummyActor.class);
    Result result =
        performTest("/v1/user/assign/role", "POST", createUserRoleRequest(true, true, true, role));
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testAssignRolesV2Success() {
    Result result =
        performTest(
            "/v2/user/assign/role", "POST", createUserRoleRequestV2(true, true, true, true));
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testAssignRolesV2SuccessReq1() {
    Result result =
        performTest(
            "/v2/user/assign/role", "POST", createUserRoleRequestV2Req3(true, true, true, true));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testAssignRolesV2SuccessReq2() {
    Result result =
        performTest(
            "/v2/user/assign/role", "POST", createUserRoleRequestV2Req2(true, true, true, true));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testAssignRolesV2SuccessReq3() {
    Result result =
        performTest(
            "/v2/user/assign/role", "POST", createUserRoleRequestV2Req4(true, true, true, true));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testAssignRolesV2FailueWithoutUserId() {
    Result result =
        performTest(
            "/v2/user/assign/role", "POST", createUserRoleRequestV2(false, true, true, true));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testAssignRolesV2FailureWithoutRoles() {
    Result result =
        performTest(
            "/v2/user/assign/role", "POST", createUserRoleRequestV2(true, true, false, true));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testAssignRolesV2FailureWithoutOp() {
    Result result =
        performTest(
            "/v2/user/assign/role", "POST", createUserRoleRequestV2(true, true, true, false));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testAssignRolesV2FailureWithoutScope() {
    Result result =
        performTest(
            "/v2/user/assign/role", "POST", createUserRoleRequestV2(true, false, true, true));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testAssignRolesFailueWithoutOrgId() {
    Result result =
        performTest("/v1/user/assign/role", "POST", createUserRoleRequest(true, false, true, role));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testAssignRolesFailueWithoutUserId() {
    Result result =
        performTest("/v1/user/assign/role", "POST", createUserRoleRequest(false, true, true, role));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testAssignRolesFailureWithoutRoles() {
    Result result =
        performTest("/v1/user/assign/role", "POST", createUserRoleRequest(true, true, false, null));

    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testUpdateAssignedRolesFailureWithEmptyRole() {
    Result result =
        performTest("/v1/user/assign/role", "POST", createUserRoleRequest(true, true, true, null));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testGetAllRolesSuccess() {
    Result result = performTest("/v1/role/read", "GET", null);
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
    assertTrue(getResponseStatus(result) == 200);
  }

  private Map createUserRoleRequest(
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

    return requestMap;
  }

  private Map createUserRoleRequestV2Req3(
      boolean isUserIdReq, boolean isScopeReq, boolean isRoleReq, boolean isOpReq) {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> requestMapObj = new HashMap<>();

    if (isUserIdReq) requestMapObj.put(JsonKey.USER_ID, userId);
    if (isRoleReq) {
      List<Map<String, Object>> scopeList = new ArrayList<>();
      Map<String, Object> scopeMap = new HashMap<>();
      scopeMap.put(JsonKey.ORGANISATION_ID, Arrays.asList("545667489132"));
      scopeList.add(scopeMap);

      List<Map<String, Object>> rolesList = new ArrayList<>();
      Map<String, Object> roleMap = new HashMap<>();
      roleMap.put(JsonKey.ROLE, "someRole1");
      if (isOpReq) roleMap.put(JsonKey.OPERATION, JsonKey.ADD);
      if (isScopeReq) roleMap.put(JsonKey.SCOPE, scopeList);
      rolesList.add(roleMap);

      requestMapObj.put(JsonKey.ROLES, rolesList);
    }
    requestMap.put(JsonKey.REQUEST, requestMapObj);

    return requestMap;
  }

  private Map createUserRoleRequestV2Req2(
      boolean isUserIdReq, boolean isScopeReq, boolean isRoleReq, boolean isOpReq) {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> requestMapObj = new HashMap<>();

    if (isUserIdReq) requestMapObj.put(JsonKey.USER_ID, userId);
    if (isRoleReq) {
      List<Map<String, Object>> scopeList = new ArrayList<>();
      Map<String, Object> scopeMap = new HashMap<>();
      scopeMap.put(JsonKey.ORGANISATION_ID, "");
      scopeList.add(scopeMap);

      List<Map<String, Object>> rolesList = new ArrayList<>();
      Map<String, Object> roleMap = new HashMap<>();
      roleMap.put(JsonKey.ROLE, "someRole1");
      if (isOpReq) roleMap.put(JsonKey.OPERATION, JsonKey.ADD);
      if (isScopeReq) roleMap.put(JsonKey.SCOPE, scopeList);
      rolesList.add(roleMap);

      requestMapObj.put(JsonKey.ROLES, rolesList);
    }
    requestMap.put(JsonKey.REQUEST, requestMapObj);

    return requestMap;
  }

  private Map createUserRoleRequestV2Req4(
      boolean isUserIdReq, boolean isScopeReq, boolean isRoleReq, boolean isOpReq) {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> requestMapObj = new HashMap<>();

    if (isUserIdReq) requestMapObj.put(JsonKey.USER_ID, userId);
    if (isRoleReq) {
      List<Map<String, Object>> scopeList = new ArrayList<>();
      Map<String, Object> scopeMap = new HashMap<>();
      scopeMap.put(JsonKey.ORGANISATION_ID, null);
      scopeList.add(scopeMap);

      List<Map<String, Object>> rolesList = new ArrayList<>();
      Map<String, Object> roleMap = new HashMap<>();
      roleMap.put(JsonKey.ROLE, "someRole1");
      if (isOpReq) roleMap.put(JsonKey.OPERATION, JsonKey.ADD);
      if (isScopeReq) roleMap.put(JsonKey.SCOPE, scopeList);
      rolesList.add(roleMap);

      requestMapObj.put(JsonKey.ROLES, rolesList);
    }
    requestMap.put(JsonKey.REQUEST, requestMapObj);

    return requestMap;
  }

  private Map createUserRoleRequestV2(
      boolean isUserIdReq, boolean isScopeReq, boolean isRoleReq, boolean isOpReq) {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> requestMapObj = new HashMap<>();

    if (isUserIdReq) requestMapObj.put(JsonKey.USER_ID, userId);
    if (isRoleReq) {
      List<Map<String, Object>> scopeList = new ArrayList<>();
      Map<String, Object> scopeMap = new HashMap<>();
      scopeMap.put(JsonKey.ORGANISATION_ID, "someOrgId");
      scopeList.add(scopeMap);

      List<Map<String, Object>> rolesList = new ArrayList<>();
      Map<String, Object> roleMap = new HashMap<>();
      roleMap.put(JsonKey.ROLE, "someRole1");
      if (isOpReq) roleMap.put(JsonKey.OPERATION, JsonKey.ADD);
      if (isScopeReq) roleMap.put(JsonKey.SCOPE, scopeList);
      rolesList.add(roleMap);

      roleMap = new HashMap<>();
      roleMap.put(JsonKey.ROLE, "someRole11");
      roleMap.put(JsonKey.OPERATION, JsonKey.ADD);
      roleMap.put(JsonKey.SCOPE, scopeList);
      rolesList.add(roleMap);

      roleMap = new HashMap<>();
      roleMap.put(JsonKey.ROLE, "someRole2");
      roleMap.put(JsonKey.OPERATION, JsonKey.REMOVE);
      roleMap.put(JsonKey.SCOPE, scopeList);
      rolesList.add(roleMap);
      requestMapObj.put(JsonKey.ROLES, rolesList);
    }
    requestMap.put(JsonKey.REQUEST, requestMapObj);

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
    //    req.headers(new Http.Headers(headerMap));
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
        e.printStackTrace();
      }
    }
    return jsonResp;
  }

  public String getResponseCode(Result result) {
    String responseStr = Helpers.contentAsString(result);
    ObjectMapper mapper = new ObjectMapper();

    try {
      Response response = mapper.readValue(responseStr, Response.class);
      ResponseParams params = response.getParams();
      if (result.status() != 200) {
        return response.getResponseCode().name();
      } else {
        return params.getStatus();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "";
  }

  public int getResponseStatus(Result result) {
    return result.status();
  }
}
