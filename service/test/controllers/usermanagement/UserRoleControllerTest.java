package controllers.usermanagement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import controllers.BaseControllerTest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;
import play.mvc.Result;
import play.test.Helpers;

public class UserRoleControllerTest extends BaseControllerTest {

  private static String role = "user";
  private static String userId = "user-id";
  private static String orgId = "org-id";

  @Test
  public void testAssignRolesSuccess() {

    Result result =
        performTest("/v1/user/assign/role", "POST", createUserRoleRequest(true, true, true, role));
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains("success"));
    assertEquals(200, result.status());
  }

  @Test
  public void testAssignRolesFailueWithoutOrgId() {

    Result result =
        performTest("/v1/user/assign/role", "POST", createUserRoleRequest(true, false, true, role));
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testAssignRolesFailueWithoutUserId() {

    Result result =
        performTest("/v1/user/assign/role", "POST", createUserRoleRequest(false, true, true, role));
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testAssignRolesFailueWithoutRoles() {

    Result result =
        performTest("/v1/user/assign/role", "POST", createUserRoleRequest(true, true, false, null));

    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testUpdateAssignedRolesFailureWithEmptyRole() {

    Result result =
        performTest("/v1/user/assign/role", "POST", createUserRoleRequest(true, true, true, null));
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.emptyRolesProvided.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testGetAllRolesSuccess() {

    Result result = performTest("/v1/role/read", "GET", null);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains("success"));
    assertEquals(200, result.status());
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
}
