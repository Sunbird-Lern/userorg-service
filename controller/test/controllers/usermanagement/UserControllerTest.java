package controllers.usermanagement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.BaseApplicationTest;
import controllers.DummyActor;
import controllers.TestUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import modules.OnRequestHandler;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.sunbird.response.ResponseCode;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.HeaderParam;
import org.sunbird.response.Response;
import org.sunbird.response.ResponseParams;
import org.sunbird.common.ProjectUtil;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import play.test.Helpers;
import util.ACTORS;
import util.CaptchaHelper;

@PrepareForTest({
  OnRequestHandler.class,
  CaptchaHelper.class,
  ProjectUtil.class,
  HttpClientUtil.class
})
@PowerMockIgnore({"javax.management.*", "jdk.internal.reflect.*", "javax.crypto.*"})
@Ignore
public class UserControllerTest extends BaseApplicationTest {

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
  private static final String UPDATE_URL = "/v1/user/update";
  public static final String USER_EXISTS_API = "/v1/user/exists/";

  public static Map<String, List<String>> headerMap;

  @Before
  public void before() {
    setup(
        Arrays.asList(
            ACTORS.SSO_USER_CREATE_ACTOR,
            ACTORS.SSU_USER_CREATE_ACTOR,
            ACTORS.MANAGED_USER_ACTOR,
            ACTORS.USER_UPDATE_ACTOR,
            ACTORS.USER_PROFILE_READ_ACTOR,
            ACTORS.SEARCH_HANDLER_ACTOR,
            ACTORS.USER_LOOKUP_ACTOR,
            ACTORS.CHECK_USER_EXIST_ACTOR,
            ACTORS.USER_SELF_DECLARATION_MANAGEMENT_ACTOR),
        DummyActor.class);
    headerMap = new HashMap<>();
    headerMap.put(HeaderParam.X_Consumer_ID.getName(), Arrays.asList("Some consumer ID"));
    headerMap.put(HeaderParam.X_Device_ID.getName(), Arrays.asList("Some device ID"));
    headerMap.put(
        HeaderParam.X_Authenticated_Userid.getName(), Arrays.asList("Some authenticated user ID"));
    headerMap.put(JsonKey.MESSAGE_ID, Arrays.asList("Some message ID"));
    headerMap.put(HeaderParam.X_APP_ID.getName(), Arrays.asList("Some app Id"));
  }

  @Test
  public void testCreateUserSuccess() {

    Result result =
        performTest(
            "/v1/user/create",
            "POST",
            (Map) createOrUpdateUserRequest(userName, phoneNumber, null, true, ""));
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
  }

  @Test
  public void testCreateUserV3Success() {

    Result result =
        performTest(
            "/v1/user/signup",
            "POST",
            (Map) createOrUpdateUserRequest(userName, phoneNumber, null, true, ""));
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
  }

  @Test
  public void testCreateUserV3SyncSuccess() {
    Result result =
        performTest(
            "/v3/user/create",
            "POST",
            (Map) createOrUpdateUserRequest(userName, phoneNumber, null, true, ""));
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
  }

  @Test
  public void testCreateUserV2SyncSuccess() {
    Result result =
        performTest(
            "/v1/ssouser/create",
            "POST",
            (Map) createOrUpdateUserRequest(userName, phoneNumber, null, true, ""));
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
  }

  @Test
  public void testCreateUserV3WithInvalidPassLength() {
    Result result =
        performTest(
            "/v1/user/signup",
            "POST",
            (Map) createOrUpdateUserRequest(userName, phoneNumber, null, true, "Ab@1214"));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testCreateUserWithInvalidPassLength() {
    Result result =
        performTest(
            "/v1/user/create",
            "POST",
            (Map) createOrUpdateUserRequest(userName, phoneNumber, null, true, "Ab@1214"));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testCreateUserWithOutUpperCasePass() {
    Result result =
        performTest(
            "/v1/user/create",
            "POST",
            (Map) createOrUpdateUserRequest(userName, phoneNumber, null, true, "ab@12148"));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testCreateUserWithOutSpecialCharPass() {
    Result result =
        performTest(
            "/v1/user/create",
            "POST",
            (Map) createOrUpdateUserRequest(userName, phoneNumber, null, true, "ab312148"));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testCreateUserWithCorrectPass() {
    Result result =
        performTest(
            "/v1/user/create",
            "POST",
            (Map) createOrUpdateUserRequest(userName, phoneNumber, null, true, "Ab3#$2148"));
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
  }

  @Test
  public void testCreateUserV3WithCorrectPass() {
    Result result =
        performTest(
            "/v1/user/signup",
            "POST",
            (Map) createOrUpdateUserRequest(userName, phoneNumber, null, true, "Ab3#$2148"));
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
  }

  @Test
  public void testCreateUserV4Success() {
    Result result =
        performTest(
            "/v4/user/create",
            "POST",
            (Map) createOrUpdateUserRequest(userName, phoneNumber, null, true, "Ab3#$2148"));
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
  }

  @Test
  public void testCreateUserV3V2WithCorrectPass() {
    Result result =
        performTest(
            "/v2/user/signup",
            "POST",
            (Map) createOrUpdateUserRequest(userName, phoneNumber, null, true, "Ab3#$2148"));
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
  }

  @Test
  public void testCreateUserV4V2WithCorrectPass() {
    Result result =
        performTest(
            "/v1/manageduser/create",
            "POST",
            (Map) createOrUpdateUserRequest(userName, phoneNumber, null, true, "Ab3#$2148"));
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
  }
  // @Test
  public void testCreateUserFailureWithoutContentType() {
    String data = (String) createOrUpdateUserRequest(userName, phoneNumber, null, false, null);
    RequestBuilder req = new RequestBuilder().bodyText(data).uri("/v1/user/create").method("POST");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testCreateUserFailureWithInvalidPhoneNumber() {
    Result result =
        performTest(
            "/v1/user/create",
            "POST",
            (Map) createOrUpdateUserRequest(userName, invalidPhonenumber, null, true, null));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testUpdateUserSuccess() throws Exception {
    Result result =
        performTest(
            "/v1/user/update",
            "PATCH",
            (Map) createOrUpdateUserRequest(null, phoneNumber, userId, true, null));
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testUpdateUserV2Success() throws Exception {
    Result result =
        performTest(
            "/v2/user/update",
            "PATCH",
            (Map) createOrUpdateUserRequest(null, phoneNumber, userId, true, null));
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testUpdateUserV3Success() throws Exception {
    Result result =
        performTest(
            "/v3/user/update",
            "PATCH",
            (Map) createOrUpdateUserRequest(null, phoneNumber, userId, true, null));
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testUpdateUserFailureWithInvalidPhoneNumber() {
    Result result =
        performTest(
            "/v1/user/update",
            "PATCH",
            (Map) createOrUpdateUserRequest(null, invalidPhonenumber, userId, true, null));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testGetUserDetailsSuccessByUserId() {
    Result result =
        performTest("/v1/user/read/" + userId, "GET", (Map) getUserRequest(userId, null));
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testGetUserDetailsSuccessByUserIdV5() {
    Result result =
        performTest("/v5/user/read/" + userId, "GET", (Map) getUserRequest(userId, null));
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testGetUserDetailsSuccessByUserIdV4() {
    Result result =
        performTest("/v4/user/read/" + userId, "GET", (Map) getUserRequest(userId, null));
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testGetUserDetailsV3SuccessByUserId() {
    Result result =
        performTest("/v3/user/read/" + userId, "GET", (Map) getUserRequest(userId, null));
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testGetUserDetailsSuccessByKey() {
    Result result =
        performTest("/v1/user/get/loginId/testloginid", "GET", (Map) getUserRequest(null, loginId));
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testGetUserDetailsSuccessByLoginId() {
    Result result = performTest("/v1/user/getuser", "POST", (Map) getUserRequest(null, loginId));
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testGetUserDetailsFailureWithoutLoginId() {
    Result result = performTest("/v1/user/getuser", "POST", getUserRequest(null, null));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testSearchUserSuccess() {
    Result result = performTest("/v1/user/search", "POST", searchUserRequest(new HashMap<>()));
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testSearchUserSuccessV2() {
    Result result = performTest("/v2/user/search", "POST", searchUserRequest(new HashMap<>()));
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testSearchUserSuccessV3() {
    Result result = performTest("/v3/user/search", "POST", searchUserRequest(new HashMap<>()));
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testUserLookupFailure() {
    Map<String, Object> reqMap = new HashMap<>();
    Result result = TestUtil.performTest("/private/user/v1/lookup", "POST", reqMap, application);
    assertEquals(ResponseCode.CLIENT_ERROR.name(), TestUtil.getResponseCode(result));
  }

  @Test
  public void testSearchUserFailureWithoutFilter() {
    Result result = performTest("/v1/user/getuser", "POST", searchUserRequest(null));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testUpdateUserFrameworkSuccess() {
    Result result =
        performTest(UPDATE_URL, "PATCH", (Map) updateUserFrameworkRequest(userId, "NCF", true));
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testUpdateUserFrameworkFailure() {
    Result result =
        performTest(UPDATE_URL, "PATCH", (Map) updateUserFrameworkRequest(userId, "NCF", false));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertTrue(getResponseStatus(result) == 400);
  }

  private Map updateUserFrameworkRequest(String userId, String frameworkId, boolean success) {
    Map<String, Object> request = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    Map<String, Object> frameworkMap;
    frameworkMap = getFrameworkDetails(frameworkId, success);
    innerMap.put(JsonKey.USER_ID, userId);
    innerMap.put(JsonKey.FRAMEWORK, frameworkMap);
    innerMap.put(JsonKey.PHONE, phoneNumber);
    innerMap.put(JsonKey.COUNTRY_CODE, "+91");
    innerMap.put(JsonKey.EMAIL, emailId);
    request.put(JsonKey.REQUEST, innerMap);
    return request;
  }

  private Map<String, Object> getFrameworkDetails(String frameworkId, boolean success) {
    Map<String, Object> frameworkMap = new HashMap<>();
    List<String> medium = Arrays.asList("English");
    List<String> gradeLevel = Arrays.asList("Grade 3");
    List<String> board = Arrays.asList("NCERT");
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
      String userName, String phoneNumber, String userId, boolean isContentType, String password) {
    Map<String, Object> requestMap = new HashMap<>();

    Map<String, Object> innerMap = new HashMap<>();
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
    if (StringUtils.isNotBlank(password)) {
      innerMap.put(JsonKey.PASSWORD, password);
    }

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
        return response.getResponseCode().name();
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
