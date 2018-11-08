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
            (Map) getRequestedDataByPhoneNumber(userName, phoneNumber, null, true));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testCreateUserFailureWithoutContentType() {
    String data = (String) getRequestedDataByPhoneNumber(userName, phoneNumber, null, false);
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
            (Map) getRequestedDataByPhoneNumber(userName, invalidPhonenumber, null, true));
    assertEquals(getResponseCode(result), ResponseCode.phoneNoFormatError.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testUpdateUserSuccess() {
    Result result =
        performTest(
            "/v1/user/update",
            "PATCH",
            (Map) getRequestedDataByPhoneNumber(null, phoneNumber, userId, true));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testUpdateUserFailureWithInvalidPhoneNumber() {
    Result result =
        performTest(
            "/v1/user/update",
            "PATCH",
            (Map) getRequestedDataByPhoneNumber(null, invalidPhonenumber, userId, true));
    assertEquals(getResponseCode(result), ResponseCode.phoneNoFormatError.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testGetUserDetailsSuccessByUserId() {
    Result result =
        performTest(
            "/v1/user/read/" + userId,
            "GET",
            (Map) getRequestedDataByRequiredParams(userId, null, false));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testGetUserDetailsSuccessByLoginId() {
    Result result =
        performTest(
            "/v1/user/getuser",
            "POST",
            (Map) getRequestedDataByRequiredParams(null, loginId, false));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testGetUserDetailsFailureWithoutRequiredParams() {
    Result result =
        performTest(
            "/v1/user/getuser", "POST", (Map) getRequestedDataByRequiredParams(null, null, false));
    assertEquals(getResponseCode(result), ResponseCode.loginIdRequired.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testSearchUserSuccess() {
    Result result =
        performTest(
            "/v1/user/search", "POST", (Map) getRequestedDataByRequiredParams(null, null, true));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  private Object getRequestedDataByPhoneNumber(
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

  private Object getRequestedDataByRequiredParams(
      String userId, String loginId, boolean isFilterReq) {
    Map<String, Object> requestMap = new HashMap<>();

    Map<String, Object> innerMap = new HashMap<>();
    if (userId != null) innerMap.put(JsonKey.USER_ID, userId);
    if (loginId != null) innerMap.put(JsonKey.LOGIN_ID, loginId);
    if (isFilterReq) {
      Map<String, Object> filters = new HashMap<>();
      innerMap.put(JsonKey.QUERY, query);
      innerMap.put(JsonKey.FILTERS, filters);
    }
    requestMap.put(JsonKey.REQUEST, innerMap);
    return requestMap;
  }
}
