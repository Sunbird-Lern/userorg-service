package controllers.organisationmanagement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.route;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseControllerTest;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;
import play.libs.Json;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import play.test.Helpers;

public class OrgMemberControllerTest extends BaseControllerTest {

  private static String orgId = "org-id";
  private static String userId = "user-id";

  @Test
  public void testaddMemberToOrganisation() {

    JsonNode json = getRequestedJsonData(true, orgId, true, userId, false);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/org/member/add").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains("success"));
    assertEquals(200, result.status());
  }

  private JsonNode getRequestedJsonData(
      boolean isOrgId, String orgId, boolean isUserId, String userId, boolean isRoleNull) {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    if (isOrgId) innerMap.put(JsonKey.ORGANISATION_ID, orgId);
    if (isUserId) innerMap.put(JsonKey.USER_ID, userId);
    if (isRoleNull) innerMap.put(JsonKey.ROLES, null);
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);
    return Json.parse(data);
  }

  @Test
  public void testaddMemberToOrganisationFailureWithoutOrgId() {

    JsonNode json = getRequestedJsonData(false, null, true, userId, false);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/org/member/add").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testaddMemberToOrganisationFailureWithEmptyOrgId() {

    JsonNode json = getRequestedJsonData(true, null, true, userId, false);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/org/member/add").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testaddMemberToOrganisationFailureWithOutUserId() {

    JsonNode json = getRequestedJsonData(true, orgId, false, null, false);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/org/member/add").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testaddMemberToOrganisationFailureWithEmptyUserId() {

    JsonNode json = getRequestedJsonData(true, orgId, true, null, false);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/org/member/add").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testaddMemberToOrganisationFailureWithRolesOfWrongFormat() {

    JsonNode json = getRequestedJsonData(true, orgId, true, userId, true);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/org/member/add").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.dataTypeError.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testremoveMemberFromOrganisation() {

    JsonNode json = getRequestedJsonData(true, orgId, true, userId, false);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/org/member/remove").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains("success"));
    assertEquals(200, result.status());
  }

  @Test
  public void testremoveMemberFromOrganisationWithoutOrgId() {

    JsonNode json = getRequestedJsonData(false, null, true, userId, false);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/org/member/remove").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testremoveMemberFromOrganisationWithEmptyOrgId() {

    JsonNode json = getRequestedJsonData(true, null, true, userId, false);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/org/member/remove").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testremoveMemberFromOrganisationWithoutUserId() {

    JsonNode json = getRequestedJsonData(true, orgId, false, null, false);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/org/member/remove").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testremoveMemberFromOrganisationWithEmptyUserId() {

    JsonNode json = getRequestedJsonData(true, orgId, true, null, false);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/org/member/remove").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }
}
