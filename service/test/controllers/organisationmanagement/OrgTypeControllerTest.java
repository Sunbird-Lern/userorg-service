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

public class OrgTypeControllerTest extends BaseControllerTest {

  private static String orgTypeName = "org-type-name";
  private static String id = "id";

  @Test
  public void testgetOrgTypeList() {

    RequestBuilder req = new RequestBuilder().uri("/v1/org/type/list").method("GET");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains("success"));
    assertEquals(200, result.status());
  }

  @Test
  public void testcreateOrgType() {

    JsonNode json = getRequestedJsonData(true, orgTypeName, false, null);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/org/type/create").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains("success"));
    assertEquals(200, result.status());
  }

  private JsonNode getRequestedJsonData(boolean isName, String name, boolean isId, String id) {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    if (isName) innerMap.put(JsonKey.NAME, name);
    if (isId) innerMap.put(JsonKey.ID, id);
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);
    return Json.parse(data);
  }

  @Test
  public void testcreateOrgTypeFailureWithEmptyName() {

    JsonNode json = getRequestedJsonData(true, null, false, null);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/org/type/create").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testcreateOrgTypeFailureWithoutName() {

    JsonNode json = getRequestedJsonData(false, null, false, null);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/org/type/create").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testupdateSuccessOrgType() {

    JsonNode json = getRequestedJsonData(true, orgTypeName, true, id);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/org/type/update").method("PATCH");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains("success"));
    assertEquals(200, result.status());
  }

  @Test
  public void testupdateOrgTypeFailureWithoutName() {

    JsonNode json = getRequestedJsonData(false, null, true, id);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/org/type/update").method("PATCH");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testupdateOrgTypeFailureWithEmptyName() {

    JsonNode json = getRequestedJsonData(true, null, true, id);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/org/type/update").method("PATCH");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testupdateOrgTypeFailureWithoutId() {

    JsonNode json = getRequestedJsonData(true, orgTypeName, false, null);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/org/type/update").method("PATCH");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }
}
