package org.sunbird.user;

import static akka.testkit.JavaTestKit.duration;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.testkit.javadsl.TestKit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.actor.router.RequestRouter;
import org.sunbird.actorutil.location.impl.LocationClientImpl;
import org.sunbird.actorutil.systemsettings.impl.SystemSettingClientImpl;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.datasecurity.impl.DefaultDecryptionServiceImpl;
import org.sunbird.common.models.util.datasecurity.impl.DefaultEncryptionServivceImpl;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.Util;
import org.sunbird.services.sso.SSOServiceFactory;
import org.sunbird.services.sso.impl.KeyCloakServiceImpl;
import org.sunbird.user.actors.UserProfileReadActor;
import org.sunbird.user.service.impl.UserServiceImpl;
import org.sunbird.user.util.UserUtil;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class,
  SSOServiceFactory.class,
  ElasticSearchRestHighImpl.class,
  EsClientFactory.class,
  Util.class,
  RequestRouter.class,
  SystemSettingClientImpl.class,
  UserServiceImpl.class,
  UserUtil.class,
  LocationClientImpl.class,
  DataCacheHandler.class
})
@PowerMockIgnore({"javax.management.*"})
public class UserProfileReadActorTest {

  private ActorSystem system = ActorSystem.create("system");
  private static final Props props = Props.create(UserProfileReadActor.class);
  private static Map<String, Object> reqMap;
  private static UserServiceImpl userService;
  private static CassandraOperationImpl cassandraOperation;
  private static DefaultEncryptionServivceImpl encService;
  private static DefaultDecryptionServiceImpl decService;
  private static KeyCloakServiceImpl ssoManager;
  private static final String VALID_USER_ID = "VALID-USER-ID";
  private static final String INVALID_USER_ID = "INVALID-USER-ID";
  private static final String VALID_EMAIL = "someEmail@someDomain.com";
  private static final String INVALID_EMAIL = "someInvalidEmail";
  private static final String VALID_PHONE = "000000000";
  private static final String INVALID_PHONE = "000";
  private static final String VALID_USERNAME = "USERNAME";
  private static ElasticSearchService esService;

  @Before
  public void beforeEachTest() {

    ActorRef actorRef = mock(ActorRef.class);
    PowerMockito.mockStatic(RequestRouter.class);
    when(RequestRouter.getActor(Mockito.anyString())).thenReturn(actorRef);

    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    PowerMockito.mockStatic(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class);
    encService = mock(DefaultEncryptionServivceImpl.class);
    when(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory
            .getEncryptionServiceInstance(null))
        .thenReturn(encService);
    decService = mock(DefaultDecryptionServiceImpl.class);
    when(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory
            .getDecryptionServiceInstance(null))
        .thenReturn(decService);
    PowerMockito.mockStatic(SSOServiceFactory.class);
    ssoManager = mock(KeyCloakServiceImpl.class);
    when(SSOServiceFactory.getInstance()).thenReturn(ssoManager);
    PowerMockito.mockStatic(SystemSettingClientImpl.class);
    SystemSettingClientImpl systemSettingClient = mock(SystemSettingClientImpl.class);
    when(SystemSettingClientImpl.getInstance()).thenReturn(systemSettingClient);
    when(systemSettingClient.getSystemSettingByFieldAndKey(
            Mockito.any(ActorRef.class),
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyObject()))
        .thenReturn(new HashMap<>());

    PowerMockito.mockStatic(UserServiceImpl.class);
    userService = mock(UserServiceImpl.class);
    when(UserServiceImpl.getInstance()).thenReturn(userService);
    when(userService.getRootOrgIdFromChannel(Mockito.anyString())).thenReturn("anyId");
    when(userService.getCustodianChannel(Mockito.anyMap(), Mockito.any(ActorRef.class)))
        .thenReturn("anyChannel");
    when(userService.getRootOrgIdFromChannel(Mockito.anyString())).thenReturn("rootOrgId");

    PowerMockito.mockStatic(EsClientFactory.class);
    PowerMockito.mockStatic(Util.class);
    Util.getUserProfileConfig(Mockito.any(ActorRef.class));

    PowerMockito.mockStatic(UserUtil.class);
    UserUtil.setUserDefaultValue(Mockito.anyMap(), Mockito.anyString());

    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(JsonKey.TNC_ACCEPTED_ON, 12345678L);
    requestMap.put(JsonKey.ROOT_ORG_ID, "rootOrgId");
    when(UserUtil.encryptUserData(Mockito.anyMap())).thenReturn(requestMap);
    PowerMockito.mockStatic(DataCacheHandler.class);
    when(ssoManager.getUsernameById(Mockito.anyString())).thenReturn(VALID_USERNAME);
    esService = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
  }

  @Test
  public void testGetUserProfileFailureWithInvalidUserId() {
    reqMap = getUserProfileRequest(INVALID_USER_ID);
    setEsResponse(null);
    boolean result =
        testScenario(
            getRequest(reqMap, ActorOperations.GET_USER_PROFILE), ResponseCode.userNotFound);
    assertTrue(result);
  }

  @Test
  public void testGetUserProfileSuccessWithValidUserId() {
    reqMap = getUserProfileRequest(VALID_USER_ID);
    setEsResponse(getUserResponseMap());
    boolean result = testScenario(getRequest(reqMap, ActorOperations.GET_USER_PROFILE), null);
    assertTrue(result);
  }

  @Test
  @Ignore
  public void testGetUserByEmailKeyFailureWithInvalidEmail() {
    reqMap = getUserProfileByKeyRequest(JsonKey.EMAIL, INVALID_EMAIL);
    setCassandraResponse(getCassandraResponse(false));
    boolean result =
        testScenario(
            getRequest(reqMap, ActorOperations.GET_USER_BY_KEY), ResponseCode.userNotFound);
    assertTrue(result);
  }

  @Test
  @Ignore
  public void testGetUserByEmailKeySuccessWithValidEmail() {
    reqMap = getUserProfileByKeyRequest(JsonKey.EMAIL, VALID_EMAIL);
    setCassandraResponse(getCassandraResponse(true));
    boolean result = testScenario(getRequest(reqMap, ActorOperations.GET_USER_BY_KEY), null);
    assertTrue(result);
  }

  @Test
  @Ignore
  public void testGetUserByPhoneKeyFailureWithInvalidPhone() {
    reqMap = getUserProfileByKeyRequest(JsonKey.PHONE, INVALID_PHONE);
    setCassandraResponse(getCassandraResponse(false));
    boolean result =
        testScenario(
            getRequest(reqMap, ActorOperations.GET_USER_BY_KEY), ResponseCode.userNotFound);
    assertTrue(result);
  }

  @Test
  @Ignore
  public void testGetUserByPhoneKeySuccessWithValidPhone() {
    reqMap = getUserProfileByKeyRequest(JsonKey.PHONE, VALID_PHONE);
    setCassandraResponse(getCassandraResponse(true));
    boolean result = testScenario(getRequest(reqMap, ActorOperations.GET_USER_BY_KEY), null);
    assertTrue(result);
  }

  private void setCassandraResponse(Response cassandraResponse) {
    when(cassandraOperation.getRecordsByProperties(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(cassandraResponse);
  }

  private Response getCassandraResponse(boolean success) {
    Response response = new Response();
    if (success) {
      List<Map<String, Object>> userList = new ArrayList<>();
      userList.add(getUserResponseMap());
      response.put(JsonKey.RESPONSE, userList);
    }
    return response;
  }

  private boolean testScenario(Request reqObj, ResponseCode errorCode) {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    subject.tell(reqObj, probe.getRef());
    if (errorCode == null) {
      Response res = probe.expectMsgClass(duration("10 second"), Response.class);
      return null != res && res.getResponseCode() == ResponseCode.OK;
    } else {

      ProjectCommonException res =
          probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
      return res.getCode().equals(errorCode.getErrorCode())
          || res.getResponseCode() == errorCode.getResponseCode();
    }
  }

  private Map<String, Object> getUserProfileRequest(String userId) {
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.USER_ID, userId);
    return reqMap;
  }

  private Map<String, Object> getUserProfileByKeyRequest(String key, String value) {
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.KEY, key);
    reqMap.put(JsonKey.VALUE, value);
    return reqMap;
  }

  private Request getRequest(Map<String, Object> reqMap, ActorOperations actorOperation) {
    Request reqObj = new Request();
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.REQUESTED_BY, "requestedBy");
    innerMap.put(JsonKey.PRIVATE, false);
    reqObj.setRequest(reqMap);
    reqObj.setContext(innerMap);
    reqObj.setOperation(actorOperation.getValue());
    return reqObj;
  }

  private static Map<String, Object> getUserResponseMap() {
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.USER_ID, VALID_USER_ID);
    map.put(JsonKey.CHANNEL, "anyChannel");
    return map;
  }

  public void setEsResponse(Map<String, Object> esResponse) {
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(esResponse);
    when(esService.getDataByIdentifier(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(promise.future());
  }
}
