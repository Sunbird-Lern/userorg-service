package controllers.organisationmanagement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.route;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.BaseControllerTest;
import java.io.IOException;
import java.math.BigInteger;
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

public class OrganisationControllerTest extends BaseControllerTest {

  private static String orgName = "org-name";
  private static String orgId = "org-Id";
  private static String rootOrgId = "root-org-id";
  private static String status = "1";

  @Test
  public void testCreateOrgSuccess() {

    JsonNode json = getRequestedJsonData(true, false, false, false, false, false, null);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/org/create").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains("success"));
    assertEquals(200, result.status());
  }

  @Test
  public void testCreateOrgFailureWithoutOrgName() {

    JsonNode json = getRequestedJsonData(false, false, false, false, false, false, null);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/org/create").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testCreateOrgFailureWithRootOrgWithoutChannel() {

    JsonNode json = getRequestedJsonData(true, true, false, false, false, false, null);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/org/create").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.dependentParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testUpdateOrgSuccess() {

    JsonNode json = getRequestedJsonData(false, false, true, true, false, false, null);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/org/update").method("PATCH");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains("success"));
    assertEquals(200, result.status());
  }

  @Test
  public void testUpdateOrgFailureWithoutOrgId() {

    JsonNode json = getRequestedJsonData(false, false, false, true, false, false, null);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/org/update").method("PATCH");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testupdateOrgStatusSuccess() {

    JsonNode json = getRequestedJsonData(false, false, true, false, true, false, null);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/org/status/update").method("PATCH");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains("success"));
    assertEquals(200, result.status());
  }

  @Test
  public void testupdateOrgStatusFailureWithoutOrgId() {

    JsonNode json = getRequestedJsonData(false, false, false, false, true, false, null);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/org/status/update").method("PATCH");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testgetOrgDetailsSuccess() {

    JsonNode json = getRequestedJsonData(false, false, true, false, true, false, null);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/org/read").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains("success"));
    assertEquals(200, result.status());
  }

  @Test
  public void testgetOrgDetailsFailureWithoutOrgId() {

    JsonNode json = getRequestedJsonData(false, false, false, false, true, false, null);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/org/read").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testsearchOrgsSuccess() {

    JsonNode json = getRequestedJsonData(false, false, false, false, true, true, "filter");
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/org/search").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains("success"));
    assertEquals(200, result.status());
  }

  @Test
  public void testsearchOrgsFailureWithoutFilters() {

    JsonNode json = getRequestedJsonData(false, false, false, false, true, true, null);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/org/search").method("POST");
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

  private JsonNode getRequestedJsonData(
      boolean isOrgName,
      boolean isRootOrg,
      boolean isOrgId,
      boolean isRootOrgId,
      boolean isStatus,
      boolean isFilter,
      String isFilterNull) {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    if (isOrgName) innerMap.put(JsonKey.ORG_NAME, orgName);
    if (isRootOrg) innerMap.put(JsonKey.IS_ROOT_ORG, true);
    if (isOrgId) innerMap.put(JsonKey.ORGANISATION_ID, orgId);
    if (isRootOrgId) innerMap.put(JsonKey.ROOT_ORG_ID, rootOrgId);
    if (isStatus) innerMap.put(JsonKey.STATUS, new BigInteger(status));
    if (isFilter) {
      Map<String, Object> filterMap = new HashMap<>();
      if (isFilterNull != null) {
        innerMap.put(JsonKey.FILTERS, filterMap);
      } else innerMap.put(JsonKey.FILTERS, null);
    }
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);
    return Json.parse(data);
  }
}
