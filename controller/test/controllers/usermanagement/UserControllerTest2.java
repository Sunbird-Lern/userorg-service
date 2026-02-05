package controllers.usermanagement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.BaseApplicationTest;
import controllers.DummyActor;
import java.util.*;
import modules.OnRequestHandler;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.sunbird.response.ResponseCode;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.UserDeclareEntity;
import org.sunbird.request.HeaderParam;
import org.sunbird.request.RequestContext;
import org.sunbird.common.ProjectUtil;
import play.mvc.Result;
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
public class UserControllerTest2 extends BaseApplicationTest {
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
  public void testUserExistsWithValidEmail() {
    Result result = performTest(USER_EXISTS_API.concat("email/demo@gmail.com"), "GET", null);
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testUserExistsWithInValidEmail() {
    Result result = performTest(USER_EXISTS_API.concat("email/demogmail.com"), "GET", null);
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testUserExistsWithValidPhone() {
    Result result = performTest(USER_EXISTS_API.concat("phone/9876543210"), "GET", null);
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testUserExistsWithInValidPhone() {
    Result result = performTest(USER_EXISTS_API.concat("phone/98765432103"), "GET", null);
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testUpdateUserDeclarations() {
    Result result =
        performTest("/v1/user/declarations", "PATCH", (Map) createUpdateUserDeclrationRequests());
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
  }

  private Map createUpdateUserDeclrationRequests() {
    Map<String, Object> request = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    Map<String, Object> userInfo = new HashMap<>();
    userInfo.put(JsonKey.DECLARED_PHONE, "abc@tenant.com");
    userInfo.put(JsonKey.DECLARED_PHONE, "9909090909");
    UserDeclareEntity userDeclareEntity =
        new UserDeclareEntity("userid", "orgid", JsonKey.TEACHER_PERSONA, userInfo);
    List<UserDeclareEntity> userDeclareEntityList = new ArrayList<>();
    userDeclareEntityList.add(userDeclareEntity);
    innerMap.put(JsonKey.DECLARATIONS, userDeclareEntityList);
    request.put(JsonKey.REQUEST, innerMap);
    return request;
  }

  @Test
  public void testGetManagedUsersSuccess() {
    Result result =
        performTest(
            "/v1/user/managed/102fcbd2-8ec1-4870-b9e1-5dc01f2acc75?withTokens=false", "GET", null);
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testCaptchaUserExists2() throws Exception {
    CaptchaHelper captchaHelper = mock(CaptchaHelper.class);
    when(captchaHelper.validate(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
    PowerMockito.mockStatic(HttpClientUtil.class);
    PowerMockito.mockStatic(ProjectUtil.class);
    Map map = new HashMap<String, String>();
    map.put("success", true);
    ObjectMapper objectMapper = new ObjectMapper();
    String s = objectMapper.writeValueAsString(map);
    when(ProjectUtil.getConfigValue(Mockito.anyString())).thenReturn("anyString");
    when(HttpClientUtil.postFormData(
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.anyMap(),
            Mockito.any(RequestContext.class)))
        .thenReturn(s);
    Result result = performTest("/v2/user/exists/email/demo@gmail.com", "GET", null);
    assertTrue(getResponseStatus(result) == 200);
  }
}
