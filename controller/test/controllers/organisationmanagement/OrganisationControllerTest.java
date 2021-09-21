package controllers.organisationmanagement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.BaseApplicationTest;
import controllers.DummyActor;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import modules.OnRequestHandler;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.sunbird.exception.ResponseCode;
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
public class OrganisationControllerTest extends BaseApplicationTest {

  private static String orgName = "someOrgName";
  private static String orgId = "someOrgOId";
  private static String rootOrgId = "someRootOrgId";
  private static String status = "1";

  @Before
  public void before() {
    setup(
        Arrays.asList(ACTORS.ORG_MANAGEMENT_ACTOR, ACTORS.SEARCH_HANDLER_ACTOR), DummyActor.class);
  }

  @Test
  public void testCreateOrgSuccess() {
    Result result =
        performTest(
            "/v1/org/create",
            "POST",
            createOrUpdateOrganisationRequest(orgName, null, false, null, null));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testCreateSubOrgWithLicenseSuccess() {
    Map<String, Object> reqMap =
        createOrUpdateOrganisationRequest(orgName, null, false, null, null);
    ((Map<String, Object>) reqMap.get(JsonKey.REQUEST)).put(JsonKey.LICENSE, "Test MIT license");
    Result result = performTest("/v1/org/create", "POST", reqMap);
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testCreateRootOrgWithLicenseSuccess() {
    Map<String, Object> reqMap = createOrUpdateOrganisationRequest(orgName, null, true, null, null);
    ((Map<String, Object>) reqMap.get(JsonKey.REQUEST)).put(JsonKey.CHANNEL, "test-123");
    ((Map<String, Object>) reqMap.get(JsonKey.REQUEST)).put(JsonKey.LICENSE, "Test MIT license");
    Result result = performTest("/v1/org/create", "POST", reqMap);
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testCreateRootOrgWithLicenseEmptyFailure() {
    Map<String, Object> reqMap = createOrUpdateOrganisationRequest(orgName, null, true, null, null);
    ((Map<String, Object>) reqMap.get(JsonKey.REQUEST)).put(JsonKey.CHANNEL, "test-123");
    ((Map<String, Object>) reqMap.get(JsonKey.REQUEST)).put(JsonKey.LICENSE, "");
    Result result = performTest("/v1/org/create", "POST", reqMap);
    assertEquals(getResponseCode(result), ResponseCode.invalidParameterValue.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testCreateOrgFailureWithoutOrgName() {
    Result result =
        performTest(
            "/v1/org/create",
            "POST",
            createOrUpdateOrganisationRequest(null, null, false, null, null));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testCreateOrgFailureWithRootOrgWithoutChannel() {
    Result result =
        performTest(
            "/v1/org/create",
            "POST",
            createOrUpdateOrganisationRequest(orgName, null, true, rootOrgId, null));
    assertEquals(getResponseCode(result), ResponseCode.dependentParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testUpdateOrgSuccess() {
    Result result =
        performTest(
            "/v1/org/update",
            "PATCH",
            createOrUpdateOrganisationRequest(null, orgId, false, rootOrgId, null));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testUpdateOrgFailureWithoutOrgId() {
    Result result =
        performTest(
            "/v1/org/update",
            "PATCH",
            createOrUpdateOrganisationRequest(null, null, false, rootOrgId, null));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testUpdateOrgStatusSuccess() {
    Result result =
        performTest(
            "/v1/org/status/update",
            "PATCH",
            createOrUpdateOrganisationRequest(null, orgId, false, null, status));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testUpdateOrgStatusFailureWithoutOrgId() {
    Result result =
        performTest(
            "/v1/org/status/update",
            "PATCH",
            createOrUpdateOrganisationRequest(null, null, false, null, status));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testGetOrgDetailsSuccess() {
    Result result = performTest("/v1/org/read", "POST", getOrganisationRequest(orgId, status));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testGetOrgDetailsFailureWithoutOrgId() {
    Result result = performTest("/v1/org/read", "POST", getOrganisationRequest(null, status));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testSearchOrgSuccess() {
    Result result =
        performTest("/v1/org/search", "POST", searchOrganisationRequest(status, new HashMap<>()));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testSearchOrgV2Success() {
    Result result =
        performTest("/v2/org/search", "POST", searchOrganisationRequest(status, new HashMap<>()));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testSearchOrgFailureWithoutFilters() {
    Result result = performTest("/v1/org/search", "POST", searchOrganisationRequest(status, null));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  private Map createOrUpdateOrganisationRequest(
      String orgName, String orgId, boolean isRootOrg, String rootOrgId, String status) {
    Map<String, Object> requestMap = new HashMap<>();

    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.ORG_NAME, orgName);
    innerMap.put(JsonKey.ORGANISATION_ID, orgId);
    innerMap.put(JsonKey.ROOT_ORG_ID, rootOrgId);
    innerMap.put(JsonKey.IS_TENANT, isRootOrg);
    innerMap.put(JsonKey.ORG_TYPE, "board");

    if (status != null) innerMap.put(JsonKey.STATUS, new Integer(status));

    requestMap.put(JsonKey.REQUEST, innerMap);

    return requestMap;
  }

  private Map getOrganisationRequest(String orgId, String status) {
    Map<String, Object> requestMap = new HashMap<>();

    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.ORGANISATION_ID, orgId);
    if (status != null) innerMap.put(JsonKey.STATUS, new Integer(status));

    requestMap.put(JsonKey.REQUEST, innerMap);

    return requestMap;
  }

  private Map searchOrganisationRequest(String status, HashMap<Object, Object> filterMap) {
    Map<String, Object> requestMap = new HashMap<>();

    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.FILTERS, filterMap);
    if (status != null) innerMap.put(JsonKey.STATUS, new Integer(status));

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
        return params.getErr();
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
