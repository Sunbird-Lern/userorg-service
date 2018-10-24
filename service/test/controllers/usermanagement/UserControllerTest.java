package controllers.usermanagement;

import static org.junit.Assert.assertEquals;
import static play.test.Helpers.route;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.BaseControllerTest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import play.libs.Json;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;

public class UserControllerTest extends BaseControllerTest {

  private static String userId = "2a8316e3-7da4-4001-8494-90f40b1853e8";
  private static String emailId = "abbc@gmail.com";
  private static String phoneNumber = "8800088000";
  private static String userName = "userName";
  private static String orgId = "0126180523645747200";
  private static String loginId = "loginid";
  private static String invalidPhonenumber = "00088000";
  private static String firstName = "firstName";
  private static String lastName = "lastName";
  private static String query = "query";
  private static String language = "hindi";
  private static String role = "user";

  @Test
  public void testCreateUserFailureWithoutContentType() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.PHONE_VERIFIED, true);
    innerMap.put(JsonKey.PHONE, phoneNumber);
    innerMap.put(JsonKey.COUNTRY_CODE, "+91");
    innerMap.put(JsonKey.EMAIL, emailId);
    innerMap.put(JsonKey.USERNAME, userName);
    innerMap.put(JsonKey.FIRST_NAME, firstName);
    innerMap.put(JsonKey.LAST_NAME, lastName);
    List<String> roles = new ArrayList<>();
    roles.add(role);
    List languages = new ArrayList<>();
    languages.add(language);

    innerMap.put(JsonKey.ROLES, roles);
    innerMap.put(JsonKey.LANGUAGE, languages);

    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);
    RequestBuilder req = new RequestBuilder().bodyText(data).uri("/v1/user/create").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(400, result.status());
  }

  @Test
  public void testCreateUserSuccess() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.PHONE_VERIFIED, true);
    innerMap.put(JsonKey.PHONE, phoneNumber);
    innerMap.put(JsonKey.COUNTRY_CODE, "+91");
    innerMap.put(JsonKey.EMAIL, emailId);
    innerMap.put(JsonKey.USERNAME, userName);
    innerMap.put(JsonKey.FIRST_NAME, firstName);
    innerMap.put(JsonKey.LAST_NAME, lastName);
    List<String> roles = new ArrayList<>();
    roles.add(role);
    List languages = new ArrayList<>();
    languages.add(language);

    innerMap.put(JsonKey.ROLES, roles);
    innerMap.put(JsonKey.LANGUAGE, languages);

    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);

    JsonNode json = Json.parse(data);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/user/create").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
  }

  @Test
  public void testCreateUserFailureWithInvalidPhoneNumber() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.PHONE, invalidPhonenumber);
    innerMap.put(JsonKey.COUNTRY_CODE, "+91");
    innerMap.put(JsonKey.EMAIL, emailId);
    innerMap.put(JsonKey.USERNAME, userName);
    innerMap.put(JsonKey.FIRST_NAME, firstName);
    innerMap.put(JsonKey.LAST_NAME, lastName);
    List<String> roles = new ArrayList<>();
    roles.add(role);
    List languages = new ArrayList<>();
    languages.add(language);
    innerMap.put(JsonKey.ROLES, roles);
    innerMap.put(JsonKey.LANGUAGE, languages);
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/user/create").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(400, result.status());
  }

  @Test
  public void testUpdateUserProfileSuccess() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.USER_ID, userId);
    innerMap.put(JsonKey.EMAIL, emailId);
    innerMap.put(JsonKey.FIRST_NAME, firstName);
    innerMap.put(JsonKey.LAST_NAME, lastName);
    List<String> roles = new ArrayList<>();
    roles.add(role);
    List languages = new ArrayList<>();
    languages.add(language);
    innerMap.put(JsonKey.ROLES, roles);
    innerMap.put(JsonKey.LANGUAGE, languages);
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/user/update").method("PATCH");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
  }

  @Test
  public void testUpdateUserFailureWithInvalidPhoneNumber() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.USER_ID, userId);
    innerMap.put(JsonKey.PHONE, invalidPhonenumber);
    innerMap.put(JsonKey.EMAIL, emailId);
    innerMap.put(JsonKey.FIRST_NAME, firstName);
    innerMap.put(JsonKey.LAST_NAME, lastName);
    List<String> roles = new ArrayList<>();
    roles.add(role);
    List languages = new ArrayList<>();
    languages.add(language);
    innerMap.put(JsonKey.ROLES, roles);
    innerMap.put(JsonKey.LANGUAGE, languages);
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/user/update").method("PATCH");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(400, result.status());
  }

  @Test
  public void testGetUserDetailsSuccessByUserId() {
    RequestBuilder req =
        new RequestBuilder()
            .uri("/v1/user/read/2a8316e3-7da4-4001-8494-90f40b1853e8")
            .method("GET");
    req.headers(headerMap);
    Result result = route(req);
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
    assertEquals(200, result.status());
  }

  @Test
  public void testGetUserDetailsFailureByInvalidLoginId() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.LOGIN_ID, loginId);
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/user/getuser").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
  }

  @Test
  public void testGetRoles() {
    RequestBuilder req = new RequestBuilder().uri("/v1/role/read").method("GET");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
  }

  @Test
  public void testBlockUserSuccess() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.USER_ID, userId);
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/user/block").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
  }

  @Test
  public void testBlockUserFailureWithouUserId() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/user/block").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(400, result.status());
  }

  @Test
  public void testAssignRoles() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.USER_ID, userId);
    innerMap.put(JsonKey.ORGANISATION_ID, orgId);
    List<String> roles = new ArrayList<>();
    roles.add(role);
    innerMap.put(JsonKey.ROLES, roles);
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/user/assign/role").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
  }

  @Test
  public void testRemoveAssignedRolesSuccess() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.USER_ID, userId);
    innerMap.put(JsonKey.ORGANISATION_ID, orgId);
    List<String> roles = new ArrayList<>();
    innerMap.put(JsonKey.ROLES, roles);
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/user/assign/role").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
  }

  @Test
  public void testUnBlockUserSuccess() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.USER_ID, userId);
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/user/unblock").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
  }

  @Test
  public void testUnBlockUserFailureWithoutUserId() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/user/unblock").method("POST");
    req.headers(headerMap);
    Result result = route(req);
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
    assertEquals(400, result.status());
  }

  @Test
  public void testUpdateLoginTimeSuccess() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.USER_ID, userId);
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/user/update/logintime").method("PATCH");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
  }

  @Test
  public void testUpdateLoginTimeFailureWithoutUserId() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/user/update/logintime").method("PATCH");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(400, result.status());
  }

  @Test
  public void testGetMediaTypes() {
    RequestBuilder req = new RequestBuilder().uri("/v1/user/mediatype/list").method("GET");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
  }

  @Test
  public void testProfileVisibilitySuccess() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.USER_ID, "{userId} uuiuhcf784508 8y8c79-fhh");
    innerMap.put(JsonKey.PRIVATE_FIELDS, new ArrayList<>());
    innerMap.put(JsonKey.PUBLIC_FIELDS, new ArrayList<>());
    List<String> privateFields = new ArrayList<>();
    privateFields.add(JsonKey.PHONE);
    List<String> publicFields = new ArrayList<>();
    publicFields.add(JsonKey.EMAIL);

    innerMap.put(JsonKey.PRIVATE, privateFields);
    innerMap.put(JsonKey.PUBLIC, publicFields);
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);

    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/user/profile/visibility").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
  }

  @Test
  public void testProfileVisibilityFailureWithDifferentUsedIdAndRequestedId() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.USER_ID, userId);
    innerMap.put(JsonKey.PRIVATE_FIELDS, new ArrayList<>());
    innerMap.put(JsonKey.PUBLIC_FIELDS, new ArrayList<>());
    List<String> privateFields = new ArrayList<>();
    privateFields.add(JsonKey.PHONE);
    List<String> publicFields = new ArrayList<>();
    publicFields.add(JsonKey.EMAIL);

    innerMap.put(JsonKey.PRIVATE, privateFields);
    innerMap.put(JsonKey.PUBLIC, publicFields);
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);

    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/user/profile/visibility").method("POST");
    req.headers(headerMap);
    Result result = route(req);
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
}
