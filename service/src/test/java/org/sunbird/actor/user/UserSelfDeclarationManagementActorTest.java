package org.sunbird.actor.user;

import java.time.Duration;

import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.dispatch.Futures;
import org.apache.pekko.testkit.javadsl.TestKit;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.dao.notification.impl.EmailTemplateDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.UserDeclareEntity;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.util.DataCacheHandler;
import org.sunbird.util.UserUtility;
import org.sunbird.util.user.UserUtil;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  DataCacheHandler.class,
  org.sunbird.datasecurity.impl.ServiceFactory.class,
  EmailTemplateDaoImpl.class,
  UserUtility.class,
  EsClientFactory.class,
  ElasticSearchRestHighImpl.class,
  UserUtil.class,
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class UserSelfDeclarationManagementActorTest {

  private static final Props props = Props.create(UserSelfDeclarationManagementActor.class);
  private ActorSystem system = ActorSystem.create("system");
  private static CassandraOperationImpl cassandraOperation;
  private static ElasticSearchService esService;

  @BeforeClass
  public static void setUp() {

    PowerMockito.mockStatic(ServiceFactory.class);
    PowerMockito.mockStatic(EmailTemplateDaoImpl.class);
    PowerMockito.mockStatic(org.sunbird.datasecurity.impl.ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
  }

  @Before
  public void beforeTest() {

    PowerMockito.mockStatic(ServiceFactory.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(cassandraInsertRecord());
    when(cassandraOperation.getRecordsByProperties(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getCassandraRecordsByProperties());
    cassandraOperation.deleteRecord(
        Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any());
    cassandraOperation.updateRecord(
        Mockito.anyString(),
        Mockito.anyString(),
        Mockito.anyMap(),
        Mockito.anyMap(),
        Mockito.any());

    List userOrgLst = new LinkedList<HashMap<String, Object>>();
    Map userOrg = new HashMap();
    userOrg.put(JsonKey.ORGANISATION_ID, "someStateSubOrgId");
    userOrg.put(JsonKey.EXTERNAL_ID, "someStateExternalId");
    userOrgLst.add(userOrg);
    Response response = new Response();
    response.put(JsonKey.RESPONSE, userOrgLst);
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(response);

    PowerMockito.mockStatic(UserUtility.class);
    when(UserUtility.encryptData(Mockito.anyString())).thenReturn("userExtId");

    PowerMockito.mockStatic(UserUtil.class);
    UserDeclareEntity userDeclareEntity = new UserDeclareEntity();
    Map userInfo = new HashMap();
    userInfo.put(JsonKey.DECLARED_EMAIL, "9909090909");
    userInfo.put(JsonKey.DECLARED_PHONE, "abc@tenant.com");
    userDeclareEntity.setUserId("userid");
    userDeclareEntity.setOperation("add");
    userDeclareEntity.setPersona("somePersona");
    userDeclareEntity.setUserInfo(userInfo);
    Mockito.when(UserUtil.createUserDeclaredObject(Mockito.anyMap(), Mockito.anyString()))
        .thenReturn(userDeclareEntity);

    PowerMockito.mockStatic(EsClientFactory.class);
    esService = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getEsResponseMap());
    when(esService.getDataByIdentifier(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
    PowerMockito.mockStatic(DataCacheHandler.class);
    when(DataCacheHandler.getConfigSettings()).thenReturn(configSettingsMap());
  }

  public static Map<String, Object> getEsResponseMap() {
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.IS_TENANT, false);
    map.put(JsonKey.ID, "rootOrgId");
    map.put(JsonKey.CHANNEL, "anyChannel");
    return map;
  }

  private Response cassandraInsertRecord() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    return response;
  }

  private Response getCassandraRecordsByProperties() {
    Response response = new Response();
    List list = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.CREATED_BY, "createdBy");
    map.put(JsonKey.PROVIDER, "anyProvider");
    map.put(JsonKey.USER_ID, "userid1");
    map.put(JsonKey.ORG_ID, "org1");
    map.put(JsonKey.PERSONA, JsonKey.TEACHER_PERSONA);
    list.add(map);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }

  @Test
  public void testAddUserSelfDeclaredDetailsSuccess() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request request = new Request();
    request.setOperation(ActorOperations.UPSERT_USER_SELF_DECLARATIONS.getValue());

    List<UserDeclareEntity> list = new ArrayList<>();
    list.add(addUserDeclaredEntity());
    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(JsonKey.DECLARATIONS, list);
    request.setRequest(requestMap);

    subject.tell(request, probe.getRef());
    Response response = probe.expectMsgClass(Duration.ofSeconds(100), Response.class);
    Assert.assertTrue(null != response && response.getResponseCode() == ResponseCode.OK);
    Assert.assertEquals(JsonKey.SUCCESS, response.getResult().get(JsonKey.RESPONSE));
  }

  @Test
  public void testAddRemoveEditProvidersUserSelfDeclaredDetailsSuccess() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request request = new Request();
    request.setOperation(ActorOperations.UPSERT_USER_SELF_DECLARATIONS.getValue());
    List<UserDeclareEntity> list = new ArrayList<>();
    list.add(addUserDeclaredEntity());
    list.add(removeUserDeclaredEntity());
    list.add(editRoleChangeUserDeclaredEntity());
    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(JsonKey.DECLARATIONS, list);
    request.setRequest(requestMap);
    subject.tell(request, probe.getRef());
    Response response = probe.expectMsgClass(Duration.ofSeconds(100), Response.class);
    Assert.assertTrue(null != response && response.getResponseCode() == ResponseCode.OK);
    Assert.assertEquals(JsonKey.SUCCESS, response.getResult().get(JsonKey.RESPONSE));
  }

  @Test
  public void testEditOrgChangeUserSelfDeclaredDetailsSuccess() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request request = new Request();
    request.setOperation(ActorOperations.UPSERT_USER_SELF_DECLARATIONS.getValue());
    List<UserDeclareEntity> list = new ArrayList<>();
    list.add(editOrgChangeUserDeclaredEntity());
    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(JsonKey.DECLARATIONS, list);
    request.setRequest(requestMap);
    subject.tell(request, probe.getRef());
    Response response = probe.expectMsgClass(Duration.ofSeconds(100), Response.class);
    Assert.assertTrue(null != response && response.getResponseCode() == ResponseCode.OK);
    Assert.assertEquals(JsonKey.SUCCESS, response.getResult().get(JsonKey.RESPONSE));
  }

  @Test
  public void testUpdateUserSelfDeclaredDetailsSuccess() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request request = new Request();
    request.setOperation(ActorOperations.UPSERT_USER_SELF_DECLARATIONS.getValue());
    List<UserDeclareEntity> list = new ArrayList<>();
    list.add(editOrgChangeUserDeclaredEntity());
    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(JsonKey.DECLARATIONS, list);
    request.setRequest(requestMap);
    subject.tell(request, probe.getRef());
    Response response = probe.expectMsgClass(Duration.ofSeconds(100), Response.class);
    Assert.assertTrue(null != response && response.getResponseCode() == ResponseCode.OK);
    Assert.assertEquals(JsonKey.SUCCESS, response.getResult().get(JsonKey.RESPONSE));
  }

  @Test
  public void testUpdateUserSelfDeclaredDetails() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request request = new Request();
    request.setOperation(ActorOperations.UPDATE_USER_DECLARATIONS.getValue());
    List<Map<String, Object>> list = new ArrayList<>();
    UserDeclareEntity userDeclareEntity = editOrgChangeUserDeclaredEntity();
    userDeclareEntity.setOrgId("anyOrgId");
    ObjectMapper mapper = new ObjectMapper();

    list.add(mapper.convertValue(userDeclareEntity, new TypeReference<Map<String, Object>>() {}));
    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(JsonKey.DECLARATIONS, list);
    request.setRequest(requestMap);
    subject.tell(request, probe.getRef());
    probe.expectMsgClass(Duration.ofSeconds(100), ProjectCommonException.class);
  }

  private UserDeclareEntity addUserDeclaredEntity() {
    UserDeclareEntity userDeclareEntity = new UserDeclareEntity();
    userDeclareEntity.setOrgId("01234848481");
    userDeclareEntity.setPersona(JsonKey.TEACHER_PERSONA);
    userDeclareEntity.setStatus(JsonKey.SUBMITTED);
    Map<String, Object> userInfo = new HashMap<>();
    userInfo.put(JsonKey.DECLARED_EMAIL, "dsadaddasdadadadadE^JD");
    userInfo.put(JsonKey.DECLARED_PHONE, "0890321830");
    userDeclareEntity.setUserInfo(userInfo);
    userDeclareEntity.setOperation(JsonKey.ADD);
    return userDeclareEntity;
  }

  private UserDeclareEntity removeUserDeclaredEntity() {
    UserDeclareEntity userDeclareEntity = new UserDeclareEntity();
    userDeclareEntity.setOrgId("018329328293892");
    userDeclareEntity.setPersona(JsonKey.TEACHER_PERSONA);
    userDeclareEntity.setStatus(JsonKey.SUBMITTED);
    Map<String, Object> userInfo = new HashMap<>();
    userInfo.put(JsonKey.DECLARED_EMAIL, "dsadaddasdadadadadE^JD");
    userInfo.put(JsonKey.DECLARED_PHONE, "0890321830");
    userDeclareEntity.setUserInfo(userInfo);
    userDeclareEntity.setOperation(JsonKey.REMOVE);
    return userDeclareEntity;
  }

  private UserDeclareEntity editRoleChangeUserDeclaredEntity() {
    UserDeclareEntity userDeclareEntity = new UserDeclareEntity();
    userDeclareEntity.setOrgId("org1");
    userDeclareEntity.setPersona("volunteer");
    userDeclareEntity.setStatus(JsonKey.SUBMITTED);
    Map<String, Object> userInfo = new HashMap<>();
    userInfo.put(JsonKey.DECLARED_EMAIL, "dsadaddasdadadadadE^JD");
    userInfo.put(JsonKey.DECLARED_PHONE, "0890321830");
    userDeclareEntity.setUserInfo(userInfo);
    userDeclareEntity.setOperation(JsonKey.EDIT);
    return userDeclareEntity;
  }

  private UserDeclareEntity editOrgChangeUserDeclaredEntity() {
    UserDeclareEntity userDeclareEntity = new UserDeclareEntity();
    userDeclareEntity.setOrgId("org2");
    userDeclareEntity.setPersona(JsonKey.TEACHER_PERSONA);
    userDeclareEntity.setStatus(JsonKey.SUBMITTED);
    Map<String, Object> userInfo = new HashMap<>();
    userInfo.put(JsonKey.DECLARED_EMAIL, "dsadaddasdadadadadE^JD");
    userInfo.put(JsonKey.DECLARED_PHONE, "0890321830");
    userDeclareEntity.setUserInfo(userInfo);
    userDeclareEntity.setOperation(JsonKey.EDIT);
    return userDeclareEntity;
  }

  @Test
  public void updateUserSelfDeclaredErrorStatus() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request request = new Request();
    request.setOperation(ActorOperations.UPDATE_USER_SELF_DECLARATIONS_ERROR_TYPE.getValue());
    UserDeclareEntity userDeclareEntity = userDeclaredEntityWithErrorStatus(true);
    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(JsonKey.DECLARATIONS, userDeclareEntity);
    request.setRequest(requestMap);
    subject.tell(request, probe.getRef());
    Response response = probe.expectMsgClass(Duration.ofSeconds(10), Response.class);
    Assert.assertTrue(null != response && response.getResponseCode() == ResponseCode.OK);
    Assert.assertEquals(JsonKey.SUCCESS, response.getResult().get(JsonKey.RESPONSE));
  }

  @Test
  public void testUpdateUserSelfDeclaredErrorStatusWithOtherStatus() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request request = new Request();
    request.setOperation(ActorOperations.UPDATE_USER_SELF_DECLARATIONS_ERROR_TYPE.getValue());
    UserDeclareEntity userDeclareEntity = userDeclaredEntityWithErrorStatus(false);
    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(JsonKey.DECLARATIONS, userDeclareEntity);
    request.setRequest(requestMap);
    subject.tell(request, probe.getRef());
    ProjectCommonException projectCommonException =
        probe.expectMsgClass(Duration.ofSeconds(10), ProjectCommonException.class);
    Assert.assertEquals(
        ResponseCode.declaredUserErrorStatusNotUpdated.getErrorMessage(),
        projectCommonException.getMessage());
  }

  private UserDeclareEntity userDeclaredEntityWithErrorStatus(boolean errorStatus) {
    UserDeclareEntity userDeclareEntity = new UserDeclareEntity();
    userDeclareEntity.setOrgId("org2");
    userDeclareEntity.setPersona(JsonKey.TEACHER_PERSONA);
    userDeclareEntity.setUserId("someUserID");
    if (errorStatus) {
      userDeclareEntity.setStatus(JsonKey.SELF_DECLARED_ERROR);
    } else {
      userDeclareEntity.setStatus(JsonKey.VALIDATED);
    }
    userDeclareEntity.setErrorType("ERROR-PHONE");
    return userDeclareEntity;
  }

  @Test
  public void testUpdateUserSelfDeclarations() throws Exception {
    Response response = new Response();
    List<Map<String, Object>> orgList = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ID, "id");
    map.put(JsonKey.IS_TENANT, false);
    orgList.add(map);
    response.put(JsonKey.RESPONSE, orgList);
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(response);
    Map<String, Object> reqMap = createUpdateUserDeclrationRequests();

    doNothing()
        .when(
            UserUtil.class,
            "encryptDeclarationFields",
            Mockito.anyList(),
            Mockito.anyMap(),
            Mockito.any());

    boolean result =
        testScenario(
            getRequest(true, false, false, reqMap, ActorOperations.UPDATE_USER_DECLARATIONS), null);
    assertTrue(result);
  }

  @Test
  public void testWithInvalidRequest() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request request = new Request();
    request.setOperation("invalidOperation");
    subject.tell(request, probe.getRef());
    ProjectCommonException exception =
        probe.expectMsgClass(Duration.ofSeconds(10), ProjectCommonException.class);
    Assert.assertNotNull(exception);
  }

  public Map createUpdateUserDeclrationRequests() {
    Map<String, Object> request = new HashMap<>();
    Map<String, Object> userInfo = new HashMap<>();
    userInfo.put(JsonKey.DECLARED_EMAIL, "abc@tenant.com");
    userInfo.put(JsonKey.DECLARED_PHONE, "9909090909");
    Map<String, Object> userDeclareFieldMap = new HashMap<>();
    userDeclareFieldMap.put(JsonKey.USER_ID, "userid");
    userDeclareFieldMap.put(JsonKey.ORG_ID, "orgID");
    userDeclareFieldMap.put(JsonKey.PERSONA, "somePersona");
    userDeclareFieldMap.put(JsonKey.OPERATION, JsonKey.ADD);
    userDeclareFieldMap.put(JsonKey.INFO, userInfo);
    List<Map<String, Object>> userDeclareEntityList = new ArrayList<>();
    userDeclareEntityList.add(userDeclareFieldMap);
    request.put(JsonKey.DECLARATIONS, userDeclareEntityList);
    return request;
  }

  private Map<String, String> configSettingsMap() {
    Map<String, String> configMap = new HashMap<>();
    configMap.put(JsonKey.CUSTODIAN_ORG_ID, "anyOrgId");
    return configMap;
  }

  public boolean testScenario(Request reqObj, ResponseCode errorCode) {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    subject.tell(reqObj, probe.getRef());

    if (errorCode == null) {
      Response res = probe.expectMsgClass(Duration.ofSeconds(1000), Response.class);
      return null != res && res.getResponseCode() == ResponseCode.OK;
    } else {
      ProjectCommonException res =
          probe.expectMsgClass(Duration.ofSeconds(1000), ProjectCommonException.class);
      return res.getErrorCode().equals(errorCode.getErrorCode())
          || res.getErrorResponseCode() == errorCode.getResponseCode();
    }
  }

  public Request getRequest(
      boolean isCallerIdReq,
      boolean isRootOrgIdReq,
      boolean isVersionReq,
      Map<String, Object> reqMap,
      ActorOperations actorOperation) {

    Request reqObj = new Request();
    HashMap<String, Object> innerMap = new HashMap<>();
    if (isCallerIdReq) innerMap.put(JsonKey.CALLER_ID, "anyCallerId");
    if (isVersionReq) innerMap.put(JsonKey.VERSION, "v2");
    if (isRootOrgIdReq) innerMap.put(JsonKey.ROOT_ORG_ID, "MY_ROOT_ORG_ID");
    innerMap.put(JsonKey.REQUESTED_BY, "requestedBy");
    reqObj.setRequest(reqMap);
    reqObj.setContext(innerMap);
    reqObj.setRequestContext(new RequestContext());
    reqObj.setOperation(actorOperation.getValue());
    return reqObj;
  }
}
