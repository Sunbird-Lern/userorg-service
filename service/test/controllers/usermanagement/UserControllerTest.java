package controllers.usermanagement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.route;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseControllerTest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;
import play.libs.Json;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import play.test.Helpers;

public class UserControllerTest extends BaseControllerTest {

  private static String userId = "user-id";
  private static String emailId = "abbc@gmail.com";
  private static String phoneNumber = "8800088000";
  private static String userName = "userName";
  private static String loginId = "login-id";
  private static String invalidPhonenumber = "00088000";
  private static String firstName = "firstName";
  private static String lastName = "lastName";
  private static String query = "query";
  private static String language = "any-language";
  private static String role = "user";

  @Test
  public void testCreateUserFailureWithoutContentType() {

    String data = getRequestedData(userName, phoneNumber, null);
    RequestBuilder req = new RequestBuilder().bodyText(data).uri("/v1/user/create").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.contentTypeRequiredError.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testCreateUserSuccess() {

    String data = getRequestedData(userName, phoneNumber, null);
    JsonNode json = Json.parse(data);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/user/create").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains("success"));
    assertEquals(200, result.status());
  }

  @Test
  public void testCreateUserFailureWithInvalidPhoneNumber() {

    String data = getRequestedData(userName, invalidPhonenumber, null);
    JsonNode json = Json.parse(data);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/user/create").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.phoneNoFormatError.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testUpdateUserProfileSuccess() {

    String data = getRequestedData(null, phoneNumber, userId);
    JsonNode json = Json.parse(data);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/user/update").method("PATCH");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains("success"));
    assertEquals(200, result.status());
  }

  @Test
  public void testUpdateUserFailureWithInvalidPhoneNumber() {

    String data = getRequestedData(null, invalidPhonenumber, userId);
    JsonNode json = Json.parse(data);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/user/update").method("PATCH");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.phoneNoFormatError.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testGetUserDetailsSuccessByUserId() {
    RequestBuilder req = new RequestBuilder().uri("/v1/user/read/user-id").method("GET");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains("success"));
    assertEquals(200, result.status());
  }

  @Test
  public void testGetUserDetailsSuccessByLoginId() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.LOGIN_ID, loginId);
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/user/getuser").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains("success"));
    assertEquals(200, result.status());
  }

  @Test
  public void testGetUserDetailsFailureWithoutLoginId() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.LOGIN_ID, null);
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/user/getuser").method("POST");
    req.headers(headerMap);
    Result result = route(req);

    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.loginIdRequired.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testSearchUserSuccess() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    Map<String, Object> filters = new HashMap<>();
    innerMap.put(JsonKey.QUERY, query);
    innerMap.put(JsonKey.FILTERS, filters);
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/user/search").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains("success"));
    assertEquals(200, result.status());
  }

  @Test
  public void testSearchUserFailureWithoutFilters() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/user/search").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }

  private String getRequestedData(String userName, String phoneNumber, String userId) {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.PHONE_VERIFIED, true);
    innerMap.put(JsonKey.PHONE, phoneNumber);
    innerMap.put(JsonKey.COUNTRY_CODE, "+91");
    innerMap.put(JsonKey.EMAIL, emailId);
    if (userName != null) {
      innerMap.put(JsonKey.USERNAME, userName);
    }
    if (userId != null) {
      innerMap.put(JsonKey.USER_ID, userId);
    }
    innerMap.put(JsonKey.FIRST_NAME, firstName);
    innerMap.put(JsonKey.LAST_NAME, lastName);
    List<String> roles = new ArrayList<>();
    roles.add(role);
    List languages = new ArrayList<>();
    languages.add(language);

    innerMap.put(JsonKey.ROLES, roles);
    innerMap.put(JsonKey.LANGUAGE, languages);

    requestMap.put(JsonKey.REQUEST, innerMap);
    return mapToJson(requestMap);
  }
}
