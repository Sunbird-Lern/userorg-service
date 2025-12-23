package org.sunbird.actor.user;

import java.time.Duration;

import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.testkit.javadsl.TestKit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.sso.KeycloakBruteForceAttackUtil;
import org.sunbird.sso.KeycloakUtil;
import org.sunbird.sso.SSOManager;
import org.sunbird.sso.SSOServiceFactory;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.UserUtility;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  CassandraOperationImpl.class,
  UserUtility.class,
  KeycloakBruteForceAttackUtil.class,
  KeycloakUtil.class,
  SSOServiceFactory.class,
  SSOManager.class,
  HttpClientUtil.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class ResetPasswordActorTest {

  private CassandraOperation cassandraOperation = null;
  Props props = Props.create(ResetPasswordActor.class);
  ActorSystem system = ActorSystem.create("ResetPasswordActor");
  private static final String KEYSPACE_NAME = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE);

  private void getRecordByIdNonEmptyResponse() {
    Response response = new Response();
    Map<String, Object> user = new HashMap<>();
    user.put(JsonKey.ID, "ValidUserId");
    user.put(JsonKey.EMAIL, "anyEmail@gmail.com");
    user.put(JsonKey.CHANNEL, "TN");
    user.put(JsonKey.PHONE, "9876543210");
    user.put(JsonKey.MASKED_EMAIL, "any****@gmail.com");
    user.put(JsonKey.MASKED_PHONE, "987*****0");
    user.put(JsonKey.IS_DELETED, false);
    user.put(JsonKey.USER_ID, "ValidUserId");
    user.put(JsonKey.FIRST_NAME, "Demo Name");
    user.put(JsonKey.USERNAME, "validUserName");
    List<Map<String, Object>> userList = new ArrayList<>();
    userList.add(user);
    response.getResult().put(JsonKey.RESPONSE, userList);
    PowerMockito.when(
            cassandraOperation.getRecordById(KEYSPACE_NAME, JsonKey.USER, "ValidUserId", null))
        .thenReturn(response);
  }

  private void getRecordByIdEmptyResponse() {
    Response response = new Response();
    PowerMockito.when(
            cassandraOperation.getRecordById(
                    KEYSPACE_NAME, JsonKey.USER, "invalidParameter", null))
        .thenReturn(response);
  }

  @Before
  public void beforeEachTest() throws Exception {
    PowerMockito.mockStatic(HttpClientUtil.class);
    when(HttpClientUtil.post(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn("{\"link\":\"success\"}");
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    PowerMockito.when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    PowerMockito.mockStatic(UserUtility.class);
    PowerMockito.mockStatic(KeycloakUtil.class);
    PowerMockito.mockStatic(KeycloakBruteForceAttackUtil.class);
    when(UserUtility.decryptUserData(Mockito.anyMap())).thenReturn(getUserDbMap());
    getRecordByIdNonEmptyResponse();
    when(KeycloakUtil.getAdminAccessToken(Mockito.any(RequestContext.class), Mockito.anyString()))
        .thenReturn("accessToken");
    when(KeycloakBruteForceAttackUtil.isUserAccountDisabled(
            Mockito.anyString(), Mockito.any(RequestContext.class)))
        .thenReturn(true);
    when(KeycloakBruteForceAttackUtil.unlockTempDisabledUser(
            Mockito.anyString(), Mockito.any(RequestContext.class)))
        .thenReturn(true);
    PowerMockito.mockStatic(SSOServiceFactory.class);
    SSOManager ssoManager = PowerMockito.mock(SSOManager.class);
    when(SSOServiceFactory.getInstance()).thenReturn(ssoManager);
    when(ssoManager.updatePassword(
            Mockito.anyString(), Mockito.anyString(), Mockito.any(RequestContext.class)))
        .thenReturn(true);
  }

  /*@Test
  public void testResetPasswordWithInvalidUserIdFailure() {
    getRecordByIdEmptyResponse();
    boolean result = testScenario(getInvalidRequest(), ResponseCode.resourceNotFound);
    Assert.assertTrue(result);
  }*/

  @Test
  public void testResetPasswordWithKeyPhoneSuccess() throws Exception {
    when(UserUtility.decryptUserData(Mockito.anyMap())).thenReturn(getUserDbMap());
    boolean result = testScenario(getValidRequestWithKeyPhone(), null);
    Assert.assertTrue(result);
  }

  @Test
  public void testResetPasswordWithKeyEmailSuccess() throws Exception {
    when(UserUtility.decryptUserData(Mockito.anyMap())).thenReturn(getUserDbMap());
    boolean result = testScenario(getValidRequestWithKeyEmail(), null);
    Assert.assertTrue(result);
  }

  @Test
  public void testResetPasswordWithKeyPrevUsedEmailSuccess() throws Exception {
    when(UserUtility.decryptUserData(Mockito.anyMap())).thenReturn(getUserDbMap());
    boolean result = testScenario(getValidRequestWithKeyPrevUsedEmail(), null);
    Assert.assertTrue(result);
  }

  @Test
  public void testResetPasswordWithKeyPrevUsedPhoneSuccess() throws Exception {
    boolean result = testScenario(getValidRequestWithKeyPrevUsedPhone(), null);
    Assert.assertTrue(result);
  }

  private Request getInvalidRequest() {
    Request request = new Request();
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.USER_ID, "invalidParameter");
    reqMap.put(JsonKey.TYPE, "phone");
    request.setRequest(reqMap);
    request.setOperation("resetPassword");
    return request;
  }

  private Request getValidRequestWithKeyPhone() {
    Request request = new Request();
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.USER_ID, "ValidUserId");
    reqMap.put(JsonKey.TYPE, "phone");
    request.setRequest(reqMap);
    request.setOperation("resetPassword");
    return request;
  }

  private Request getValidRequestWithKeyPrevUsedPhone() {
    Request request = new Request();
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.USER_ID, "ValidUserId");
    reqMap.put(JsonKey.TYPE, "prevUsedPhone");
    request.setRequest(reqMap);
    request.setOperation("resetPassword");
    return request;
  }

  private Request getValidRequestWithKeyEmail() {
    Request request = new Request();
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.USER_ID, "ValidUserId");
    reqMap.put(JsonKey.TYPE, "email");
    request.setRequest(reqMap);
    request.setOperation("resetPassword");
    return request;
  }

  private Request getValidRequestWithKeyPrevUsedEmail() {
    Request request = new Request();
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.USER_ID, "ValidUserId");
    reqMap.put(JsonKey.TYPE, "prevUsedEmail");
    request.setRequest(reqMap);
    request.setOperation("resetPassword");
    return request;
  }

  private Map<String, Object> getUserDbMap() {
    Map<String, Object> userDbMap = new HashMap<>();
    userDbMap.put(JsonKey.SET_PASSWORD_LINK, "/password/link/url");
    userDbMap.put(JsonKey.USERNAME, "validUserName");
    userDbMap.put(JsonKey.CHANNEL, "TN");
    userDbMap.put(JsonKey.EMAIL, "anyEmail@gmail.com");
    userDbMap.put(JsonKey.PHONE, "9876543210");
    userDbMap.put(JsonKey.FLAGS_VALUE, 3);
    userDbMap.put(JsonKey.USER_TYPE, "TEACHER");
    userDbMap.put(JsonKey.MASKED_PHONE, "987*****0");
    userDbMap.put(JsonKey.USER_ID, "ValidUserId");
    userDbMap.put(JsonKey.ID, "ValidUserId");
    userDbMap.put(JsonKey.FIRST_NAME, "Demo Name");
    userDbMap.put(JsonKey.REDIRECT_URI, "/resource/url");
    userDbMap.put(JsonKey.IS_DELETED, false);
    return userDbMap;
  }

  public boolean testScenario(Request reqObj, ResponseCode errorCode) {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    subject.tell(reqObj, probe.getRef());
    if (errorCode == null) {
      Response res = probe.expectMsgClass(Duration.ofSeconds(10), Response.class);
      return null != res && res.getResponseCode() == ResponseCode.OK;
    } else {
      ProjectCommonException res =
          probe.expectMsgClass(Duration.ofSeconds(10), ProjectCommonException.class);
      return res.getErrorCode().equals(errorCode.getErrorCode())
          || res.getErrorResponseCode() == errorCode.getResponseCode();
    }
  }
}
