package controllers.usermanagement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import controllers.BaseControllerTest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Ignore;
import org.junit.Test;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;
import play.mvc.Result;

@Ignore
public class UserRoleControllerTest extends BaseControllerTest {

  private static String role = "someRole";
  private static String userId = "someUserId";
  private static String orgId = "someOrgId";

  @Test
  public void testAssignRolesSuccess() {
    Result result =
        performTest("/v1/user/assign/role", "POST", createUserRoleRequest(true, true, true, role));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testAssignRolesFailueWithoutOrgId() {
    Result result =
        performTest("/v1/user/assign/role", "POST", createUserRoleRequest(true, false, true, role));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testAssignRolesFailueWithoutUserId() {
    Result result =
        performTest("/v1/user/assign/role", "POST", createUserRoleRequest(false, true, true, role));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testAssignRolesFailureWithoutRoles() {
    Result result =
        performTest("/v1/user/assign/role", "POST", createUserRoleRequest(true, true, false, null));

    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testUpdateAssignedRolesFailureWithEmptyRole() {
    Result result =
        performTest("/v1/user/assign/role", "POST", createUserRoleRequest(true, true, true, null));
    assertEquals(getResponseCode(result), ResponseCode.emptyRolesProvided.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testGetAllRolesSuccess() {
    Result result = performTest("/v1/role/read", "GET", null);
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
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
}
