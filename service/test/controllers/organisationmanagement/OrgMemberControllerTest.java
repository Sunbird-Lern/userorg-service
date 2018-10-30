package controllers.organisationmanagement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import controllers.BaseControllerTest;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;
import play.mvc.Result;
import play.test.Helpers;

public class OrgMemberControllerTest extends BaseControllerTest {

  private static String orgId = "org-id";
  private static String userId = "user-id";

  @Test
  public void testAddMemberToOrganisation() {

    Result result =
        performTest(
            "/v1/org/member/add", "POST", createMemberRequest(true, orgId, true, userId, false));
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains("success"));
    assertEquals(200, result.status());
  }

  @Test
  public void testAddMemberToOrganisationFailureWithoutOrgId() {

    Result result =
        performTest(
            "/v1/org/member/add", "POST", createMemberRequest(false, null, true, userId, false));
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testAddMemberToOrganisationFailureWithEmptyOrgId() {

    Result result =
        performTest(
            "/v1/org/member/add", "POST", createMemberRequest(true, null, true, userId, false));
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testAddMemberToOrganisationFailureWithOutUserId() {

    Result result =
        performTest(
            "/v1/org/member/add", "POST", createMemberRequest(true, orgId, false, null, false));
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testAddMemberToOrganisationFailureWithEmptyUserId() {

    Result result =
        performTest(
            "/v1/org/member/add", "POST", createMemberRequest(true, orgId, true, null, false));
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testAddMemberToOrganisationFailureWithRolesOfWrongFormat() {

    Result result =
        performTest(
            "/v1/org/member/add", "POST", createMemberRequest(true, orgId, true, userId, true));
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.dataTypeError.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testRemoveMemberFromOrganisation() {

    Result result =
        performTest(
            "/v1/org/member/remove", "POST", createMemberRequest(true, orgId, true, userId, false));
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains("success"));
    assertEquals(200, result.status());
  }

  @Test
  public void testRemoveMemberFromOrganisationWithoutOrgId() {

    Result result =
        performTest(
            "/v1/org/member/remove", "POST", createMemberRequest(false, null, true, userId, false));

    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testRemoveMemberFromOrganisationWithEmptyOrgId() {

    Result result =
        performTest(
            "/v1/org/member/remove", "POST", createMemberRequest(true, null, true, userId, false));
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testRemoveMemberFromOrganisationWithoutUserId() {

    Result result =
        performTest(
            "/v1/org/member/remove", "POST", createMemberRequest(true, orgId, false, null, false));
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testRemoveMemberFromOrganisationWithEmptyUserId() {

    Result result =
        performTest(
            "/v1/org/member/remove", "POST", createMemberRequest(true, orgId, true, null, false));
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
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
