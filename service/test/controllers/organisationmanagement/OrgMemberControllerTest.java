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
public class OrgMemberControllerTest extends BaseApplicationTest {

  private static String orgId = "someOrgId";
  private static String userId = "someUserId";

  @Before
  public void before() {
    setup(DummyActor.class);
  }

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
