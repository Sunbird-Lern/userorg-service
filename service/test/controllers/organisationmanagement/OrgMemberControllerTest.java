package controllers.organisationmanagement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import controllers.BaseControllerTest;
import java.util.HashMap;
import java.util.Map;
import org.junit.Ignore;
import org.junit.Test;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;
import play.mvc.Result;

@Ignore
public class OrgMemberControllerTest extends BaseControllerTest {

  private static String orgId = "someOrgId";
  private static String userId = "someUserId";

  @Test
  public void testAddMemberToOrganisationSuccess() {
    Result result =
        performTest(
            "/v1/org/member/add", "POST", createMemberRequest(true, orgId, true, userId, false));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testAddMemberToOrganisationFailureWithoutOrgId() {
    Result result =
        performTest(
            "/v1/org/member/add", "POST", createMemberRequest(false, null, true, userId, false));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testAddMemberToOrganisationFailureWithEmptyOrgId() {
    Result result =
        performTest(
            "/v1/org/member/add", "POST", createMemberRequest(true, null, true, userId, false));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testAddMemberToOrganisationFailureWithoutUserId() {
    Result result =
        performTest(
            "/v1/org/member/add", "POST", createMemberRequest(true, orgId, false, null, false));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testAddMemberToOrganisationFailureWithEmptyUserId() {
    Result result =
        performTest(
            "/v1/org/member/add", "POST", createMemberRequest(true, orgId, true, null, false));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testAddMemberToOrganisationFailureWithNullRoles() {
    Result result =
        performTest(
            "/v1/org/member/add", "POST", createMemberRequest(true, orgId, true, userId, true));
    assertEquals(getResponseCode(result), ResponseCode.dataTypeError.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testRemoveMemberFromOrganisationSuccess() {
    Result result =
        performTest(
            "/v1/org/member/remove", "POST", createMemberRequest(true, orgId, true, userId, false));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testRemoveMemberFromOrganisationFailureWithoutOrgId() {
    Result result =
        performTest(
            "/v1/org/member/remove", "POST", createMemberRequest(false, null, true, userId, false));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testRemoveMemberFromOrganisationFailureWithEmptyOrgId() {
    Result result =
        performTest(
            "/v1/org/member/remove", "POST", createMemberRequest(true, null, true, userId, false));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testRemoveMemberFromOrganisationFailureWithoutUserId() {
    Result result =
        performTest(
            "/v1/org/member/remove", "POST", createMemberRequest(true, orgId, false, null, false));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testRemoveMemberFromOrganisationFailureWithEmptyUserId() {
    Result result =
        performTest(
            "/v1/org/member/remove", "POST", createMemberRequest(true, orgId, true, null, false));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  private Map createMemberRequest(
      boolean isOrgId, String orgId, boolean isUserId, String userId, boolean isRoleNull) {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    if (isOrgId) innerMap.put(JsonKey.ORGANISATION_ID, orgId);
    if (isUserId) innerMap.put(JsonKey.USER_ID, userId);
    if (isRoleNull) innerMap.put(JsonKey.ROLES, null);
    requestMap.put(JsonKey.REQUEST, innerMap);
    return requestMap;
  }
}
