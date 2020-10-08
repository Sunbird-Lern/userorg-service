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
import org.sunbird.common.ElasticSearchHelper;
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
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.Util;
import org.sunbird.services.sso.SSOServiceFactory;
import org.sunbird.services.sso.impl.KeyCloakServiceImpl;
import org.sunbird.user.actors.UserProfileReadActor;
import org.sunbird.user.service.UserExternalIdentityService;
import org.sunbird.user.service.impl.UserExternalIdentityServiceImpl;
import org.sunbird.user.service.impl.UserServiceImpl;
import org.sunbird.user.util.UserUtil;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class,
  SSOServiceFactory.class,
  ElasticSearchRestHighImpl.class,
  ElasticSearchHelper.class,
  EsClientFactory.class,
  Util.class,
  RequestRouter.class,
  SystemSettingClientImpl.class,
  UserServiceImpl.class,
  UserUtil.class,
  LocationClientImpl.class,
  DataCacheHandler.class,
  UserExternalIdentityService.class,
  UserExternalIdentityServiceImpl.class
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
            Mockito.anyObject(),
            Mockito.any()))
        .thenReturn(new HashMap<>());

    PowerMockito.mockStatic(UserServiceImpl.class);
    userService = mock(UserServiceImpl.class);
    when(UserServiceImpl.getInstance()).thenReturn(userService);
    when(userService.getRootOrgIdFromChannel(Mockito.anyString(), Mockito.any()))
        .thenReturn("anyId");
    when(userService.getCustodianChannel(
            Mockito.anyMap(), Mockito.any(ActorRef.class), Mockito.any()))
        .thenReturn("anyChannel");
    when(userService.getRootOrgIdFromChannel(Mockito.anyString(), Mockito.any()))
        .thenReturn("rootOrgId");

    PowerMockito.mockStatic(EsClientFactory.class);
    PowerMockito.mockStatic(Util.class);

    PowerMockito.mockStatic(UserUtil.class);
    UserUtil.setUserDefaultValue(Mockito.anyMap(), Mockito.anyString(), Mockito.any());

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
    when(cassandraOperation.getRecordById(JsonKey.SUNBIRD, JsonKey.ORGANISATION, "rootOrgId", null))
        .thenReturn(getUserDeclarationResponse(true));
    boolean result = testScenario(getRequest(reqMap, ActorOperations.GET_USER_PROFILE), null);
    assertTrue(result);
  }

  @Test
  public void testGetUserProfileV1SuccessWithFieldExternalIds() {
    PowerMockito.mockStatic(UserUtil.class);
    List<Map<String, String>> extIdList = new ArrayList<>();
    Map<String, String> extId = new HashMap<>();
    extId.put(JsonKey.USER_ID, "userId");
    extId.put(JsonKey.EXTERNAL_ID, "extrnalId");
    extId.put(JsonKey.ID_TYPE, "rootOrgId");
    extId.put(JsonKey.PROVIDER, "rootOrgId");
    extId.put(JsonKey.ORIGINAL_EXTERNAL_ID, "extrnalId");
    extId.put(JsonKey.ORIGINAL_ID_TYPE, "rootOrgId");
    extId.put(JsonKey.ORIGINAL_PROVIDER, "rootOrgId");
    extIdList.add(extId);
    when(UserUtil.getExternalIds(Mockito.anyString(), Mockito.any())).thenReturn(extIdList);

    Request reqObj = getProfileReadV2request(VALID_USER_ID, JsonKey.DECLARATIONS);
    Map<String, Object> req = new HashMap<>();
    req.put(JsonKey.USER_ID, VALID_USER_ID);
    boolean result = testScenario(reqObj, null);
    assertTrue(result);
  }

  // @Test
  public void testGetUserProfileV1SuccessWithExternalIds() throws Exception {
    UserExternalIdentityServiceImpl externalIdentityService =
        PowerMockito.mock(UserExternalIdentityServiceImpl.class);
    PowerMockito.whenNew(UserExternalIdentityServiceImpl.class)
        .withNoArguments()
        .thenReturn(externalIdentityService);
    when(externalIdentityService.getUserV1(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(VALID_USER_ID);

    Request reqObj = getProfileReadV1request(VALID_USER_ID);
    Map<String, Object> req = new HashMap<>();
    req.put(JsonKey.USER_ID, VALID_USER_ID);
    boolean result = testScenario(reqObj, null);
    assertTrue(result);
  }

  @Test
  public void testGetUserProfileV1FailureWithExternalIds() throws Exception {
    UserExternalIdentityServiceImpl externalIdentityService =
        PowerMockito.mock(UserExternalIdentityServiceImpl.class);
    PowerMockito.whenNew(UserExternalIdentityServiceImpl.class)
        .withNoArguments()
        .thenReturn(externalIdentityService);
    when(externalIdentityService.getUserV1(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(null);

    Request reqObj = getProfileReadV1request(VALID_USER_ID);
    Map<String, Object> req = new HashMap<>();
    req.put(JsonKey.USER_ID, VALID_USER_ID);
    boolean result = testScenario(reqObj, ResponseCode.externalIdNotFound);
    assertTrue(result);
  }

  @Test
  public void testGetUserProfileV1Failure() {
    Request reqObj = new Request();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.REQUESTED_BY, VALID_USER_ID);
    innerMap.put(JsonKey.PRIVATE, false);
    innerMap.put(JsonKey.VERSION, JsonKey.VERSION_2);
    innerMap.put(JsonKey.PROVIDER, JsonKey.VERSION_2);
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.USER_ID, VALID_USER_ID);
    reqMap.put(JsonKey.ROOT_ORG_ID, "validRootOrgId");
    reqObj.setRequest(reqMap);
    reqObj.setContext(innerMap);
    reqObj.setOperation(ActorOperations.GET_USER_PROFILE.getValue());
    setEsResponse(getUserResponseMap());

    Map<String, Object> req = new HashMap<>();
    req.put(JsonKey.USER_ID, VALID_USER_ID);
    boolean result = testScenario(reqObj, ResponseCode.mandatoryParamsMissing);
    assertTrue(result);
  }

  @Test
  public void testGetUserProfileV3SuccessWithFieldDeclaration() {
    Request reqObj = getProfileReadV3request(VALID_USER_ID, JsonKey.EXTERNAL_IDS);
    Map<String, Object> req = new HashMap<>();
    req.put(JsonKey.USER_ID, VALID_USER_ID);

    boolean result = testScenario(reqObj, null);
    assertTrue(result);
  }

  @Test
  public void testGetUserProfileSuccessV3WithFieldDeclarationAndExternalIds() throws Exception {
    UserExternalIdentityServiceImpl externalIdentityService =
        PowerMockito.mock(UserExternalIdentityServiceImpl.class);
    PowerMockito.whenNew(UserExternalIdentityServiceImpl.class)
        .withNoArguments()
        .thenReturn(externalIdentityService);
    List<Map<String, String>> extIdList = new ArrayList<>();
    Map<String, String> extId = new HashMap<>();
    extId.put(JsonKey.USER_ID, "userId");
    extId.put(JsonKey.EXTERNAL_ID, "extrnalId");
    extId.put(JsonKey.ID_TYPE, "rootOrgId");
    extId.put(JsonKey.PROVIDER, "rootOrgId");
    extId.put(JsonKey.ORIGINAL_EXTERNAL_ID, "extrnalId");
    extId.put(JsonKey.ORIGINAL_ID_TYPE, "rootOrgId");
    extId.put(JsonKey.ORIGINAL_PROVIDER, "rootOrgId");
    extIdList.add(extId);
    when(externalIdentityService.getUserExternalIds(Mockito.anyString(), Mockito.any()))
        .thenReturn(extIdList);
    when(cassandraOperation.getPropertiesValueById(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyList(),
            Mockito.anyList(),
            Mockito.any()))
        .thenReturn(getUserDeclarationResponse(true));
    Request reqObj =
        getProfileReadV3request(
            VALID_USER_ID,
            JsonKey.DECLARATIONS
                .concat(",")
                .concat(JsonKey.EXTERNAL_IDS)
                .concat(",")
                .concat(JsonKey.TOPIC)
                .concat(",")
                .concat(JsonKey.ORGANISATIONS)
                .concat(",")
                .concat(JsonKey.ROLES)
                .concat(",")
                .concat(JsonKey.LOCATIONS));
    Response response1 = new Response();
    List<Map<String, Object>> responseList = new ArrayList<>();
    response1.getResult().put(JsonKey.RESPONSE, responseList);
    when(cassandraOperation.getRecordsByPrimaryKeys(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyList(),
            Mockito.anyString(),
            Mockito.any()))
        .thenReturn(response1);
    Map<String, Object> req = new HashMap<>();
    req.put(JsonKey.USER_ID, VALID_USER_ID);
    when(cassandraOperation.getRecordById(
            JsonKey.SUNBIRD, JsonKey.USR_DECLARATION_TABLE, req, null))
        .thenReturn(getUserDeclarationResponse(true));
    boolean result = testScenario(reqObj, null);
    assertTrue(result);
  }

  private Request getProfileReadV3request(String userId, String fields) {
    Request reqObj = new Request();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.REQUESTED_BY, VALID_USER_ID);
    innerMap.put(JsonKey.PRIVATE, false);
    innerMap.put(JsonKey.VERSION, JsonKey.VERSION_3);
    innerMap.put(JsonKey.FIELDS, fields);
    reqMap = getUserProfileRequest(userId);
    reqObj.setRequest(reqMap);
    reqObj.setContext(innerMap);
    reqObj.setOperation(ActorOperations.GET_USER_PROFILE_V3.getValue());
    setEsResponse(getUserResponseMap());
    return reqObj;
  }

  private Response getUserDeclarationResponse(boolean success) {
    Response response = new Response();
    if (success) {
      List<Map<String, Object>> userList = new ArrayList<>();
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.USER_ID, VALID_USER_ID);
      List<String> locIds = new ArrayList<>();
      locIds.add("13213");
      map.put(JsonKey.LOCATION_IDS, locIds);
      map.put(JsonKey.LOCATION_ID, "1216564");
      map.put(JsonKey.TOPIC, "1216564");
      map.put(JsonKey.ID, "123165464");
      // todo This can be a role(persona) id/name Needs sync up with portal team
      map.put(JsonKey.ROLE, "teacher");
      Map<String, Object> userInfo = new HashMap<>();
      userInfo.put(
          JsonKey.DECLARED_EMAIL,
          "ProZzR7/VhnWAewS3XjCHxVi5U8iDstpxBfgO89Ao/oHqBn9cmyQ9CnA5pT7//KkvF9QvRKpGkqv\n"
              + "F9wmXYKjHs7J2mhT1MnarGOD2wDtVfFVEjPufgzbUvpwbSRgb0R+TQtMGOn7lhkDdxs1iV8l8A==");
      map.put(JsonKey.USER_INFO, userInfo);
      userList.add(map);
      response.put(JsonKey.RESPONSE, userList);
    }
    return response;
  }

  @Test
  public void testGetUserByEmailKey() throws Exception {
    Response response1 = new Response();
    Map<String, Object> userMap = new HashMap<>();
    userMap.put(JsonKey.USER_ID, "123-456-7890");
    userMap.put(JsonKey.FIRST_NAME, "Name");
    userMap.put(JsonKey.LAST_NAME, "Name");
    userMap.put(JsonKey.IS_DELETED, false);
    userMap.put(JsonKey.ROOT_ORG_ID, "1234567890");
    List<Map<String, Object>> responseList = new ArrayList<>();
    responseList.add(userMap);
    response1.getResult().put(JsonKey.RESPONSE, responseList);
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any()))
        .thenReturn(response1);
    setEsResponse(userMap);
    UserExternalIdentityServiceImpl externalIdentityService =
        PowerMockito.mock(UserExternalIdentityServiceImpl.class);
    PowerMockito.whenNew(UserExternalIdentityServiceImpl.class)
        .withNoArguments()
        .thenReturn(externalIdentityService);
    List<Map<String, String>> extIdList = new ArrayList<>();
    Map<String, String> extId = new HashMap<>();
    extId.put(JsonKey.USER_ID, "userId");
    extId.put(JsonKey.EXTERNAL_ID, "extrnalId");
    extId.put(JsonKey.ID_TYPE, "rootOrgId");
    extId.put(JsonKey.PROVIDER, "rootOrgId");
    extId.put(JsonKey.ORIGINAL_EXTERNAL_ID, "extrnalId");
    extId.put(JsonKey.ORIGINAL_ID_TYPE, "rootOrgId");
    extId.put(JsonKey.ORIGINAL_PROVIDER, "rootOrgId");
    extIdList.add(extId);
    when(externalIdentityService.getUserExternalIds(Mockito.anyString(), Mockito.any()))
        .thenReturn(extIdList);
    when(cassandraOperation.getPropertiesValueById(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyList(),
            Mockito.anyList(),
            Mockito.any()))
        .thenReturn(getUserDeclarationResponse(true));
    Request reqObj =
        getProfileReadV3request(
            VALID_USER_ID,
            JsonKey.DECLARATIONS
                .concat(",")
                .concat(JsonKey.EXTERNAL_IDS)
                .concat(",")
                .concat(JsonKey.TOPIC)
                .concat(",")
                .concat(JsonKey.ORGANISATIONS)
                .concat(",")
                .concat(JsonKey.ROLES)
                .concat(",")
                .concat(JsonKey.LOCATIONS));
    Response response2 = new Response();
    List<Map<String, Object>> response2List = new ArrayList<>();
    response2.getResult().put(JsonKey.RESPONSE, response2List);
    when(cassandraOperation.getRecordsByPrimaryKeys(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyList(),
            Mockito.anyString(),
            Mockito.any()))
        .thenReturn(response1);
    Map<String, Object> req = new HashMap<>();
    req.put(JsonKey.USER_ID, VALID_USER_ID);
    when(cassandraOperation.getRecordById(
            JsonKey.SUNBIRD, JsonKey.USR_DECLARATION_TABLE, req, null))
        .thenReturn(getUserDeclarationResponse(true));
    reqMap = getUserProfileByKeyRequest(JsonKey.EMAIL, INVALID_EMAIL);
    setCassandraResponse(getCassandraResponse(false));
    PowerMockito.mockStatic(ElasticSearchHelper.class);
    when(ElasticSearchHelper.getResponseFromFuture(Mockito.any())).thenReturn(req);
    PowerMockito.mockStatic(UserUtil.class);
    when(UserUtil.getExternalIds(Mockito.anyString(), Mockito.any())).thenReturn(new ArrayList<>());
    boolean result = testScenario(getRequest(reqMap, ActorOperations.GET_USER_BY_KEY), null);
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

  @Test
  public void testCheckUserExistenceV1WithEmail() {
    Response response1 = new Response();
    Map<String, Object> userMap = new HashMap<>();
    userMap.put(JsonKey.FIRST_NAME, "Name");
    userMap.put(JsonKey.LAST_NAME, "Name");
    List<Map<String, Object>> responseList = new ArrayList<>();
    response1.getResult().put(JsonKey.RESPONSE, responseList);
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any()))
        .thenReturn(response1);
    setEsResponse(userMap);
    reqMap = getUserProfileByKeyRequest(JsonKey.EMAIL, VALID_EMAIL);
    setEsSearchResponse(getUserExistsSearchResponseMap());
    boolean result = testScenario(getRequest(reqMap, "checkUserExistence"), null);
    assertTrue(result);
  }

  @Test
  public void testCheckUserExistenceV2WithEmail() {
    Response response1 = new Response();
    Map<String, Object> userMap = new HashMap<>();
    userMap.put(JsonKey.USER_ID, "123456790-789456-741258");
    userMap.put(JsonKey.FIRST_NAME, "Name");
    userMap.put(JsonKey.LAST_NAME, "Name");
    List<Map<String, Object>> responseList = new ArrayList<>();
    responseList.add(userMap);
    response1.getResult().put(JsonKey.RESPONSE, responseList);
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any()))
        .thenReturn(response1);
    setEsResponse(userMap);
    reqMap = getUserProfileByKeyRequest(JsonKey.EMAIL, VALID_EMAIL);
    setEsSearchResponse(getUserExistsSearchResponseMap());
    boolean result = testScenario(getRequest(reqMap, ActorOperations.CHECK_USER_EXISTENCEV2), null);
    assertTrue(result);
  }

  @Test
  public void testCheckUserExistenceV2WithLoginid() {
    reqMap = getUserProfileByKeyRequest(JsonKey.LOGIN_ID, VALID_EMAIL);
    setEsSearchResponse(getUserExistsSearchResponseMap());
    boolean result = testScenario(getRequest(reqMap, ActorOperations.CHECK_USER_EXISTENCEV2), null);
    assertTrue(result);
  }

  public void setEsSearchResponse(Map<String, Object> esResponse) {
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(esResponse);
    when(esService.search(
            Mockito.anyObject(), Mockito.anyString(), Mockito.any(RequestContext.class)))
        .thenReturn(promise.future());
  }

  private static Map<String, Object> getUserExistsSearchResponseMap() {
    Map<String, Object> map = new HashMap<>();
    Map<String, Object> response = new HashMap<>();
    response.put(JsonKey.EXISTS, "true");
    response.put(JsonKey.FIRST_NAME, "Name");
    response.put(JsonKey.LAST_NAME, "Name");
    List contentList = new ArrayList<>();
    contentList.add(response);
    map.put(JsonKey.CONTENT, contentList);
    return map;
  }

  private void setCassandraResponse(Response cassandraResponse) {
    when(cassandraOperation.getRecordsByProperties(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
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
    reqMap.put(JsonKey.ROOT_ORG_ID, "validRootOrgId");
    return reqMap;
  }

  private Request getProfileReadV1request(String userId) {
    Request reqObj = new Request();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.REQUESTED_BY, VALID_USER_ID);
    innerMap.put(JsonKey.PRIVATE, false);
    innerMap.put(JsonKey.VERSION, JsonKey.VERSION_2);
    innerMap.put(JsonKey.ID_TYPE, JsonKey.VERSION_2);
    innerMap.put(JsonKey.PROVIDER, JsonKey.VERSION_2);
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.USER_ID, userId);
    reqMap.put(JsonKey.ROOT_ORG_ID, "validRootOrgId");
    reqObj.setRequest(reqMap);
    reqObj.setContext(innerMap);
    reqObj.setOperation(ActorOperations.GET_USER_PROFILE.getValue());
    setEsResponse(getUserResponseMap());
    return reqObj;
  }

  private Request getProfileReadV2request(String userId, String fields) {
    Request reqObj = new Request();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.REQUESTED_BY, VALID_USER_ID);
    innerMap.put(JsonKey.PRIVATE, false);
    innerMap.put(JsonKey.VERSION, JsonKey.VERSION_2);
    innerMap.put(JsonKey.FIELDS, fields);
    reqMap = getUserProfileRequest(userId);
    reqObj.setRequest(reqMap);
    reqObj.setContext(innerMap);
    reqObj.setOperation(ActorOperations.GET_USER_PROFILE.getValue());
    setEsResponse(getUserResponseMap());
    return reqObj;
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

  private Request getRequest(Map<String, Object> reqMap, String actorOperation) {
    Request reqObj = new Request();
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.REQUESTED_BY, "requestedBy");
    innerMap.put(JsonKey.PRIVATE, false);
    reqObj.setRequest(reqMap);
    reqObj.setContext(innerMap);
    reqObj.setOperation(actorOperation);
    return reqObj;
  }

  private static Map<String, Object> getUserResponseMap() {
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.USER_ID, VALID_USER_ID);
    map.put(JsonKey.CHANNEL, "anyChannel");
    map.put(JsonKey.ROOT_ORG_ID, "rootOrgId");
    List<String> locIds = new ArrayList<>();
    locIds.add("12346578900");
    map.put(JsonKey.LOCATION_IDS, locIds);
    map.put(JsonKey.LOCATION_ID, "1216564");
    map.put(JsonKey.TOPIC, "1216564");
    List<Map<String, Object>> orgList = new ArrayList<>();
    Map<String, Object> organisations = new HashMap<>();
    orgList.add(organisations);
    organisations.put(JsonKey.ORGANISATION_ID, "123165464");
    map.put(JsonKey.ORGANISATIONS, orgList);
    return map;
  }

  public void setEsResponse(Map<String, Object> esResponse) {
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(esResponse);
    when(esService.getDataByIdentifier(
            Mockito.anyString(), Mockito.anyString(), Mockito.any(RequestContext.class)))
        .thenReturn(promise.future());
  }

  public void setEsResponseForSearch(Map<String, Object> esResponse) {
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(esResponse);
    when(esService.search(
            Mockito.anyObject(), Mockito.anyString(), Mockito.any(RequestContext.class)))
        .thenReturn(promise.future());
  }
}
