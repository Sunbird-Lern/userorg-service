package controllers.usermanagement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.route;

import controllers.BaseControllerTest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;

public class UserControllerTest extends BaseControllerTest {

  private static String userId = "someUserId";
  private static String emailId = "someone@someorg.com";
  private static String phoneNumber = "8800088000";
  private static String userName = "someUserName";
  private static String loginId = "someLoginId";
  private static String invalidPhonenumber = "00088000";
  private static String firstName = "someFirstName";
  private static String lastName = "someLastName";
  private static String query = "query";
  private static String language = "any-language";
  private static String role = "user";

  @Test
  public void testCreateUserSuccess() {
    Result result =
        performTest(
            "/v1/user/create",
            "POST",
            (Map) createOrUpdateUserRequest(userName, phoneNumber, null, true));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testCreateUserFailureWithoutContentType() {
    String data = (String) createOrUpdateUserRequest(userName, phoneNumber, null, false);
    RequestBuilder req = new RequestBuilder().bodyText(data).uri("/v1/user/create").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(getResponseCode(result), ResponseCode.contentTypeRequiredError.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testCreateUserFailureWithInvalidPhoneNumber() {
    Result result =
        performTest(
            "/v1/user/create",
            "POST",
            (Map) createOrUpdateUserRequest(userName, invalidPhonenumber, null, true));
    assertEquals(getResponseCode(result), ResponseCode.phoneNoFormatError.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testUpdateUserSuccess() {
    Result result =
        performTest(
            "/v1/user/update",
            "PATCH",
            (Map) createOrUpdateUserRequest(null, phoneNumber, userId, true));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testUpdateUserFailureWithInvalidPhoneNumber() {
    Result result =
        performTest(
            "/v1/user/update",
            "PATCH",
            (Map) createOrUpdateUserRequest(null, invalidPhonenumber, userId, true));
    assertEquals(getResponseCode(result), ResponseCode.phoneNoFormatError.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testGetUserDetailsSuccessByUserId() {
    Result result =
        performTest("/v1/user/read/" + userId, "GET", (Map) getUserRequest(userId, null));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testGetUserDetailsSuccessByLoginId() {
    Result result = performTest("/v1/user/getuser", "POST", (Map) getUserRequest(null, loginId));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testGetUserDetailsFailureWithoutLoginId() {
    Result result = performTest("/v1/user/getuser", "POST", getUserRequest(null, null));
    assertEquals(getResponseCode(result), ResponseCode.loginIdRequired.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testSearchUserSuccess() {
    Result result = performTest("/v1/user/search", "POST", searchUserRequest(new HashMap<>()));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testSearchUserFailureWithoutFilter() {
    Result result = performTest("/v1/user/getuser", "POST", searchUserRequest(null));
    assertEquals(getResponseCode(result), ResponseCode.loginIdRequired.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }
  @Test
  public void testUpdateUserFrameworkSuccess() {
    Result result =
        performTest(
            "/v1/user/update",
            "PATCH",
            (Map) UpdateUserFrameworkRequest(userId, "NCF",true));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }
  @Test
  public void testUpdateUserFrameworkFailure() {
    Result result =
        performTest(
            "/v1/user/update",
            "PATCH",
            (Map) updateUserFrameworkRequest(userId, "NCF",false));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }
  

  private Map updateUserFrameworkRequest(String userId, String frameworkId, boolean success) {
    Map<String, Object> request = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    Map<String, Object> frameworkMap;
    frameworkMap = getFrameworkDetails(frameworkId, success);
    innerMap.put(JsonKey.USER_ID, userId);
    innerMap.put(JsonKey.FRAMEWORK, frameworkMap);
    innerMap.put(JsonKey.PHONE_VERIFIED, true);
    innerMap.put(JsonKey.PHONE, phoneNumber);
    innerMap.put(JsonKey.COUNTRY_CODE, "+91");
    innerMap.put(JsonKey.EMAIL, emailId);
    request.put(JsonKey.REQUEST, innerMap);
    return request;
  }

  private Map<String, Object> getFrameworkDetails(String frameworkId, boolean success) {
    Map<String, Object> frameworkMap = new HashMap<>();
    List<String> medium = new ArrayList<>();
    medium.add("English");
    List<String> gradeLevel = new ArrayList<>();
    gradeLevel.add("Grade 3");
    List<String> board = new ArrayList<>();
    board.add("NCERT");
    if (success) {
      frameworkMap.put(JsonKey.ID, frameworkId);
    } else {
      frameworkMap.put(JsonKey.ID, "");
    }
    frameworkMap.put("medium", medium);
    frameworkMap.put("gradeLevel", gradeLevel);
    frameworkMap.put("board", board);
    return frameworkMap;
  }
  
  
  

  private Object createOrUpdateUserRequest(
      String userName, String phoneNumber, String userId, boolean isContentType) {
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

    if (isContentType) return requestMap;

    return mapToJson(requestMap);
  }

  private Map<String, Object> getUserRequest(String userId, String loginId) {
    Map<String, Object> requestMap = new HashMap<>();

    Map<String, Object> innerMap = new HashMap<>();
    if (userId != null) innerMap.put(JsonKey.USER_ID, userId);
    if (loginId != null) innerMap.put(JsonKey.LOGIN_ID, loginId);

    requestMap.put(JsonKey.REQUEST, innerMap);

    return requestMap;
  }

  private Map<String, Object> searchUserRequest(Map<String, Object> filter) {
    Map<String, Object> requestMap = new HashMap<>();

    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.QUERY, query);
    innerMap.put(JsonKey.FILTERS, filter);

    requestMap.put(JsonKey.REQUEST, innerMap);

    return requestMap;
  }
}
