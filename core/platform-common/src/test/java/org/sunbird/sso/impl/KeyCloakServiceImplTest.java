package org.sunbird.sso.impl;

import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.core.Response;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.sso.KeyCloakConnectionProvider;
import org.sunbird.sso.KeycloakRequiredActionLinkUtil;
import org.sunbird.sso.SSOManager;
import org.sunbird.sso.SSOServiceFactory;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.PropertiesCache;

@PrepareForTest({
  ProjectUtil.class,
  KeyCloakConnectionProvider.class,
  KeycloakRequiredActionLinkUtil.class,
  PropertiesCache.class
})
@Ignore
public class KeyCloakServiceImplTest extends BaseHttpTest {

  private SSOManager keyCloakService = SSOServiceFactory.getInstance();

  private static Map<String, String> userId = new HashMap<>();
  private static final String userName = UUID.randomUUID().toString().replaceAll("-", "");
  private static Class t = null;

  private static final Map<String, Object> USER_SUCCESS = new HashMap<>();

  static {
    userId.put(JsonKey.USER_ID, UUID.randomUUID().toString());
    USER_SUCCESS.put(JsonKey.USERNAME, userName);
    USER_SUCCESS.put(JsonKey.PASSWORD, "password");
    USER_SUCCESS.put(JsonKey.FIRST_NAME, "A");
    USER_SUCCESS.put(JsonKey.LAST_NAME, "B");
    USER_SUCCESS.put(JsonKey.PHONE, "9870060000");
    USER_SUCCESS.put(JsonKey.EMAIL, userName.substring(0, 10));
  }

  private static final Map<String, Object> USER_SAME_EMAIL = new HashMap<>();

  static {
    USER_SAME_EMAIL.put(JsonKey.USERNAME, userName);
    USER_SAME_EMAIL.put(JsonKey.PASSWORD, "password");
    USER_SAME_EMAIL.put(JsonKey.FIRST_NAME, "A");
    USER_SAME_EMAIL.put(JsonKey.LAST_NAME, "B");
    USER_SAME_EMAIL.put(JsonKey.PHONE, "9870060000");
    USER_SAME_EMAIL.put(JsonKey.EMAIL, userName.substring(0, 10));
  }

  private static UsersResource usersRes = mock(UsersResource.class);

  @BeforeClass
  public static void init() {
    PowerMockito.mockStatic(PropertiesCache.class);
    PropertiesCache propertiesCache = mock(PropertiesCache.class);
    when(PropertiesCache.getInstance()).thenReturn(propertiesCache);
    PowerMockito.when(propertiesCache.getProperty(Mockito.anyString())).thenReturn("anyString");

    PowerMockito.mockStatic(ProjectUtil.class);
    PowerMockito.when(ProjectUtil.getConfigValue(Mockito.anyString())).thenReturn("somestring");
    try {
      t = Class.forName("org.sunbird.sso.SSOServiceFactory");
    } catch (ClassNotFoundException e) {
    }
    Keycloak kcp = mock(Keycloak.class);
    RealmResource realmRes = mock(RealmResource.class);
    UserResource userRes = mock(UserResource.class);
    UserRepresentation userRep = mock(UserRepresentation.class);
    Response response = mock(Response.class);
    PowerMockito.mockStatic(KeyCloakConnectionProvider.class);
    try {

      doReturn(kcp).when(KeyCloakConnectionProvider.class, "getConnection");
      doReturn(realmRes).when(kcp).realm(Mockito.anyString());
      doReturn(usersRes).when(realmRes).users();
      doReturn(201).when(response).getStatus();
      doReturn("userdata").when(response).getHeaderString(Mockito.eq("Location"));

      doReturn(userRes).when(usersRes).get(Mockito.anyString());
      doReturn(userRep).when(userRes).toRepresentation();
      doNothing().when(userRes).update(Mockito.any(UserRepresentation.class));

      doNothing().when(userRes).remove();

      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.LAST_LOGIN_TIME, Arrays.asList(String.valueOf(System.currentTimeMillis())));
      doReturn(map).when(userRep).getAttributes();
      when(userRep.getUsername()).thenReturn("userName");
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(
          "Failed in initialization of mock rules, underlying error: " + e.getLocalizedMessage());
    }
  }

  @Test
  public void testNewInstanceSucccess() {
    Exception exp = null;
    try {
      Constructor<SSOServiceFactory> constructor = t.getDeclaredConstructor();
      constructor.setAccessible(true);
      SSOServiceFactory application = constructor.newInstance();
      Assert.assertNotNull(application);
    } catch (Exception e) {
      exp = e;
    }
    Assert.assertNull(exp);
  }

  @Test(expected = ProjectCommonException.class)
  public void testDeactivateUserSuccess() {

    Map<String, Object> request = new HashMap<String, Object>();
    request.put(JsonKey.USER_ID, "123");
    request.put(JsonKey.FIRST_NAME, userName);
    keyCloakService.deactivateUser(request, null);
  }

  @Test(expected = ProjectCommonException.class)
  public void testRemoveUserSuccess() {

    Map<String, Object> request = new HashMap<String, Object>();
    request.put(JsonKey.USER_ID, "123");
    keyCloakService.removeUser(request, null);
  }

  @Test(expected = ProjectCommonException.class)
  public void testVerifyTokenSuccess() {
    keyCloakService.verifyToken(
        "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI5emhhVnZDbl81OEtheHpldHBzYXNZQ2lEallkemJIX3U2LV93SDk4SEc0In0.eyJqdGkiOiI5ZmQzNzgzYy01YjZmLTQ3OWQtYmMzYy0yZWEzOGUzZmRmYzgiLCJleHAiOjE1MDUxMTQyNDYsIm5iZiI6MCwiaWF0IjoxNTA1MTEzNjQ2LCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODAvYXV0aC9yZWFsbXMvbWFzdGVyIiwiYXVkIjoic2VjdXJpdHktYWRtaW4tY29uc29sZSIsInN1YiI6ImIzYTZkMTY4LWJjZmQtNDE2MS1hYzVmLTljZjYyODIyNzlmMyIsInR5cCI6IkJlYXJlciIsImF6cCI6InNlY3VyaXR5LWFkbWluLWNvbnNvbGUiLCJub25jZSI6ImMxOGVlMDM2LTAyMWItNGVlZC04NWVhLTc0MjMyYzg2ZmI4ZSIsImF1dGhfdGltZSI6MTUwNTExMzY0Niwic2Vzc2lvbl9zdGF0ZSI6ImRiZTU2NDlmLTY4MDktNDA3NS05Njk5LTVhYjIyNWMwZTkyMiIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOltdLCJyZXNvdXJjZV9hY2Nlc3MiOnt9LCJuYW1lIjoiTWFuemFydWwgaGFxdWUiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJ0ZXN0MTIzNDU2NyIsImdpdmVuX25hbWUiOiJNYW56YXJ1bCBoYXF1ZSIsImVtYWlsIjoidGVzdDEyM0B0LmNvbSJ9.Xdjqe16MSkiR94g-Uj_pVZ2L3gnIdKpkJ6aB82W_w_c3yEmx1mXYBdkxe4zMz3ks4OX_PWwSFEbJECHcnujUwF6Ula0xtXTfuESB9hFyiWHtVAhuh5UlCCwPnsihv5EqK6u-Qzo0aa6qZOiQK3Zo7FLpnPUDxn4yHyo3mRZUiWf76KTl8PhSMoXoWxcR2vGW0b-cPixILTZPV0xXUZoozCui70QnvTgOJDWqr7y80EWDkS4Ptn-QM3q2nJlw63mZreOG3XTdraOlcKIP5vFK992dyyHlYGqWVzigortS9Ah4cprFVuLlX8mu1cQvqHBtW-0Dq_JlcTMaztEnqvJ6XA",
        null);
  }

  @Ignore
  public void testActiveUserSuccess() {
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.USER_ID, userId.get(JsonKey.USER_ID));
    String response = keyCloakService.activateUser(reqMap, null);
    Assert.assertEquals(JsonKey.SUCCESS, response);
  }

  @Test
  public void testActivateUserFailureWithEmptyUserId() {
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.USER_ID, "");
    try {
      keyCloakService.activateUser(reqMap, null);
    } catch (ProjectCommonException e) {
      Assert.assertEquals(ResponseCode.invalidParameterValue.getErrorCode(), e.getCode());
      Assert.assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
    }
  }

  @Test
  public void testGetFederatedUserId()
      throws IllegalAccessException, NoSuchMethodException, SecurityException,
          IllegalArgumentException, InvocationTargetException {
    KeyCloakServiceImpl.class.getDeclaredMethods();
    Method m = KeyCloakServiceImpl.class.getDeclaredMethod("getFederatedUserId", String.class);
    m.setAccessible(true);
    SSOManager keyCloakService = SSOServiceFactory.getInstance();
    String fedUserId = (String) m.invoke(keyCloakService, "userId");
    Assert.assertEquals(
        "f:"
            + ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYCLOAK_USER_FEDERATION_PROVIDER_ID)
            + ":userId",
        fedUserId);
  }

  @Test
  public void testUpdatePassword() throws Exception {
    boolean updated = keyCloakService.updatePassword(userId.get(JsonKey.USER_ID), "password", null);
    Assert.assertNotNull(updated);
  }
}
