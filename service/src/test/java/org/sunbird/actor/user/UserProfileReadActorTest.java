package org.sunbird.actor.user;

import java.time.Duration;

import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.dispatch.Futures;
import org.apache.pekko.testkit.javadsl.TestKit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.dao.user.UserDao;
import org.sunbird.dao.user.impl.UserDaoImpl;
import org.sunbird.datasecurity.impl.DefaultDecryptionServiceImpl;
import org.sunbird.datasecurity.impl.DefaultEncryptionServiceImpl;
import org.sunbird.dto.SearchDTO;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.User;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.organisation.impl.OrgServiceImpl;
import org.sunbird.service.user.UserExternalIdentityService;
import org.sunbird.service.user.UserProfileReadService;
import org.sunbird.service.user.impl.UserExternalIdentityServiceImpl;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.sso.SSOServiceFactory;
import org.sunbird.sso.impl.KeyCloakServiceImpl;
import org.sunbird.util.DataCacheHandler;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.UserUtility;
import org.sunbird.util.Util;
import org.sunbird.util.user.UserUtil;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  org.sunbird.datasecurity.impl.ServiceFactory.class,
  SSOServiceFactory.class,
  ElasticSearchRestHighImpl.class,
  ElasticSearchHelper.class,
  EsClientFactory.class,
  Util.class,
  UserServiceImpl.class,
  OrgServiceImpl.class,
  UserUtil.class,
  DataCacheHandler.class,
  UserExternalIdentityService.class,
  UserExternalIdentityServiceImpl.class,
  UserProfileReadService.class,
  UserDaoImpl.class,
  UserDao.class,
  UserUtility.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class UserProfileReadActorTest {

  private ActorSystem system = ActorSystem.create("system");
  private static final Props props = Props.create(UserProfileReadActor.class);
  private static Map<String, Object> reqMap;
  private static UserServiceImpl userService;
  private static OrgServiceImpl orgService;
  private static CassandraOperationImpl cassandraOperation;
  private static DefaultEncryptionServiceImpl encService;
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
  private static final String KEYSPACE_NAME = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE);

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    PowerMockito.mockStatic(org.sunbird.datasecurity.impl.ServiceFactory.class);
    encService = mock(DefaultEncryptionServiceImpl.class);
    when(org.sunbird.datasecurity.impl.ServiceFactory.getEncryptionServiceInstance())
        .thenReturn(encService);
    decService = mock(DefaultDecryptionServiceImpl.class);
    when(org.sunbird.datasecurity.impl.ServiceFactory.getDecryptionServiceInstance())
        .thenReturn(decService);
    PowerMockito.mockStatic(SSOServiceFactory.class);
    ssoManager = mock(KeyCloakServiceImpl.class);
    when(SSOServiceFactory.getInstance()).thenReturn(ssoManager);

    PowerMockito.mockStatic(OrgServiceImpl.class);
    orgService = mock(OrgServiceImpl.class);
    when(OrgServiceImpl.getInstance()).thenReturn(orgService);
    when(orgService.getRootOrgIdFromChannel(Mockito.anyString(), Mockito.any()))
        .thenReturn("anyId");
    when(orgService.getRootOrgIdFromChannel(Mockito.anyString(), Mockito.any()))
        .thenReturn("rootOrgId");

    PowerMockito.mockStatic(UserServiceImpl.class);
    userService = mock(UserServiceImpl.class);
    when(UserServiceImpl.getInstance()).thenReturn(userService);

    PowerMockito.mockStatic(EsClientFactory.class);
    PowerMockito.mockStatic(Util.class);

    PowerMockito.mockStatic(UserUtil.class);
    UserUtil.setUserDefaultValue(Mockito.anyMap(), Mockito.any());

    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(JsonKey.TNC_ACCEPTED_ON, 12345678L);
    requestMap.put(JsonKey.ROOT_ORG_ID, "rootOrgId");
    when(UserUtil.encryptUserData(Mockito.anyMap())).thenReturn(requestMap);
    PowerMockito.mockStatic(DataCacheHandler.class);
    esService = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
  }

  // @Test
  public void testGetUserProfileFailureWithInvalidUserId() throws Exception {
    reqMap = getUserProfileRequest(INVALID_USER_ID);
    setEsResponse(null);
    UserProfileReadService userProfileReadService = PowerMockito.mock(UserProfileReadService.class);
    PowerMockito.whenNew(UserProfileReadService.class)
        .withNoArguments()
        .thenReturn(userProfileReadService);
    Response response = new Response();
    response.getResult().put(JsonKey.RESPONSE, null);
    PowerMockito.when(
            userProfileReadService.getUserProfileData(
                getRequest(reqMap, ActorOperations.GET_USER_PROFILE_V3)))
        .thenReturn(response);
    boolean result =
        testScenario(
            getRequest(reqMap, ActorOperations.GET_USER_PROFILE_V3), ResponseCode.resourceNotFound);
    assertTrue(result);
  }

  // @Test
  public void testGetUserProfileSuccessWithValidUserId() throws Exception {
    reqMap = getUserProfileRequest(VALID_USER_ID);
    UserProfileReadService userProfileReadService = PowerMockito.mock(UserProfileReadService.class);
    PowerMockito.whenNew(UserProfileReadService.class)
        .withNoArguments()
        .thenReturn(userProfileReadService);
    Response response = new Response();
    response.getResult().put(JsonKey.RESPONSE, getValidUserResponse());
    PowerMockito.when(
            userProfileReadService.getUserProfileData(
                getRequest(reqMap, ActorOperations.GET_USER_PROFILE_V3)))
        .thenReturn(response);
    UserDao userDao = PowerMockito.mock(UserDao.class);
    PowerMockito.mockStatic(UserDaoImpl.class);
    Mockito.when(UserDaoImpl.getInstance()).thenReturn(userDao);
    PowerMockito.mockStatic(UserUtility.class);
    PowerMockito.mockStatic(Util.class);
    Mockito.when(UserUtility.decryptUserData(Mockito.anyMap())).thenReturn(getUserDbMap());
    Mockito.when(userDao.getUserById("ValidUserId", null)).thenReturn(getValidUserResponse());

    /*Response response2 = new Response();
    List<Map<String, Object>> userList = new ArrayList<>();
    userList.add(getUserResponseMap());
    response2.getResult().put(JsonKey.RESPONSE,userList);
    setEsResponse(getUserResponseMap());
    when(cassandraOperation.getRecordById(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any(RequestContext.class)))
        .thenReturn(response2).thenReturn(getUserDeclarationResponse(true));*/
    boolean result = testScenario(getRequest(reqMap, ActorOperations.GET_USER_PROFILE_V3), null);
    assertTrue(result);
  }

  private User getValidUserResponse() {
    User user = new User();
    user.setId("ValidUserId");
    user.setEmail("anyEmail@gmail.com");
    user.setChannel("TN");
    user.setPhone("9876543210");
    user.setMaskedEmail("any****@gmail.com");
    user.setMaskedPhone("987*****0");
    user.setIsDeleted(false);
    user.setFlagsValue(3);
    user.setUserType("TEACHER");
    user.setUserId("ValidUserId");
    user.setFirstName("Demo Name");
    user.setUserName("validUserName");
    return user;
  }

  private Map<String, Object> getUserDbMap() {
    Map<String, Object> userDbMap = new HashMap<>();
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
    userDbMap.put(JsonKey.IS_DELETED, false);
    return userDbMap;
  }

  // @Test
  public void testGetUserProfileV1SuccessWithFieldExternalIds() throws Exception {
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
    when(UserUtil.getExternalIds(Mockito.anyString(), Mockito.anyBoolean(), Mockito.any()))
        .thenReturn(extIdList);

    Request reqObj = getProfileReadV2request(VALID_USER_ID, JsonKey.DECLARATIONS);
    Map<String, Object> req = new HashMap<>();
    req.put(JsonKey.USER_ID, VALID_USER_ID);
    UserProfileReadService userProfileReadService = PowerMockito.mock(UserProfileReadService.class);
    PowerMockito.whenNew(UserProfileReadService.class)
        .withNoArguments()
        .thenReturn(userProfileReadService);
    Response response = new Response();
    response.getResult().put(JsonKey.RESPONSE, getUserResponseMap());
    PowerMockito.when(userProfileReadService.getUserProfileData(reqObj)).thenReturn(response);
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
    boolean result = testScenario(reqObj, ResponseCode.resourceNotFound);
    assertTrue(result);
  }

  @Test
  public void testGetUserProfileV3Failure() throws Exception {
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
    reqObj.setOperation(ActorOperations.GET_USER_PROFILE_V3.getValue());
    setEsResponse(getUserResponseMap());

    Map<String, Object> req = new HashMap<>();
    req.put(JsonKey.USER_ID, VALID_USER_ID);

    UserProfileReadService userProfileReadService = PowerMockito.mock(UserProfileReadService.class);
    PowerMockito.whenNew(UserProfileReadService.class)
        .withNoArguments()
        .thenReturn(userProfileReadService);
    Response response = new Response();
    response.getResult().put(JsonKey.RESPONSE, getUserResponseMap());
    PowerMockito.when(userProfileReadService.getUserProfileData(reqObj)).thenReturn(response);

    boolean result = testScenario(reqObj, ResponseCode.mandatoryParamsMissing);
    assertTrue(result);
  }

  // @Test
  public void testGetUserProfileV3SuccessWithFieldDeclaration() {
    Request reqObj = getProfileReadV3request(VALID_USER_ID, JsonKey.EXTERNAL_IDS);
    Map<String, Object> req = new HashMap<>();
    req.put(JsonKey.USER_ID, VALID_USER_ID);

    boolean result = testScenario(reqObj, null);
    assertTrue(result);
  }

  // @Test
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
            KEYSPACE_NAME, JsonKey.USR_DECLARATION_TABLE, req, null))
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
            KEYSPACE_NAME, JsonKey.USR_DECLARATION_TABLE, req, null))
        .thenReturn(getUserDeclarationResponse(true));
    reqMap = getUserProfileByKeyRequest(JsonKey.EMAIL, INVALID_EMAIL);
    setCassandraResponse(getCassandraResponse(false));
    PowerMockito.mockStatic(ElasticSearchHelper.class);
    when(ElasticSearchHelper.getResponseFromFuture(Mockito.any())).thenReturn(req);
    PowerMockito.mockStatic(UserUtil.class);
    when(UserUtil.getExternalIds(Mockito.anyString(), Mockito.anyBoolean(), Mockito.any()))
        .thenReturn(new ArrayList<>());
    boolean result =
        testScenario(
            getRequest(reqMap, ActorOperations.GET_USER_BY_KEY), ResponseCode.resourceNotFound);
    assertTrue(result);
  }

  @Test
  public void testGetUserByLoginId() {
    reqMap = getUserProfileByKeyRequest(JsonKey.LOGIN_ID, "loginId");
    when(userService.searchUser(Mockito.any(SearchDTO.class), Mockito.any(RequestContext.class)))
        .thenReturn(getUserExistsSearchResponseMap());
    setCassandraResponse(getCassandraResponse(true));
    boolean result =
        testScenario(
            getRequest(reqMap, ActorOperations.GET_USER_BY_KEY), ResponseCode.resourceNotFound);
    assertTrue(result);
  }

  @Test
  public void testGetUserByLoginId2() {
    Request request = new Request();
    request.setOperation(ActorOperations.GET_USER_DETAILS_BY_LOGINID.getValue());
    request.put(JsonKey.LOGIN_ID, "loginId");
    request.getContext().put(JsonKey.PRIVATE, false);
    when(userService.searchUser(Mockito.any(SearchDTO.class), Mockito.any(RequestContext.class)))
        .thenReturn(getUserExistsSearchResponseMap());
    setCassandraResponse(getCassandraResponse(true));
    boolean result = testScenario(request, ResponseCode.resourceNotFound);
    assertTrue(result);
  }

  // @Test
  public void testGetUserByPhoneKeyFailureWithInvalidPhone() {
    reqMap = getUserProfileByKeyRequest(JsonKey.PHONE, INVALID_PHONE);
    setCassandraResponse(getCassandraResponse(false));
    boolean result =
        testScenario(
            getRequest(reqMap, ActorOperations.GET_USER_BY_KEY), ResponseCode.resourceNotFound);
    assertTrue(result);
  }

  // @Test
  public void testGetUserByPhoneKeySuccessWithValidPhone() {
    reqMap = getUserProfileByKeyRequest(JsonKey.PHONE, VALID_PHONE);
    setCassandraResponse(getCassandraResponse(true));
    boolean result = testScenario(getRequest(reqMap, ActorOperations.GET_USER_BY_KEY), null);
    assertTrue(result);
  }

  private static Map<String, Object> getUserExistsSearchResponseMap() {
    Map<String, Object> map = new HashMap<>();
    Map<String, Object> response = new HashMap<>();
    response.put(JsonKey.EXISTS, "true");
    response.put(JsonKey.USER_ID, "userId");
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
      Response res = probe.expectMsgClass(Duration.ofSeconds(100), Response.class);
      return null != res && res.getResponseCode() == ResponseCode.OK;
    } else {
      String errString = ActorOperations.getOperationCodeByActorOperation(reqObj.getOperation());
      ProjectCommonException res =
          probe.expectMsgClass(Duration.ofSeconds(10), ProjectCommonException.class);
      return res.getErrorCode().equals("UOS_" + errString + errorCode.getErrorCode())
          || res.getErrorResponseCode() == errorCode.getResponseCode();
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
    reqObj.setOperation(ActorOperations.GET_USER_PROFILE_V3.getValue());
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
    reqObj.setOperation(ActorOperations.GET_USER_PROFILE_V3.getValue());
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
    when(esService.getDataByIdentifier(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
  }

  public void setEsResponseForSearch(Map<String, Object> esResponse) {
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(esResponse);
    when(esService.search(Mockito.anyObject(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
  }
}
