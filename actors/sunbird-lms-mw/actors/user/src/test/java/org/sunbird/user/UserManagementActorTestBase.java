package org.sunbird.user;

import static akka.testkit.JavaTestKit.duration;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.pattern.Patterns;
import akka.pattern.PipeToSupport;
import akka.testkit.javadsl.TestKit;
import akka.util.Timeout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.actor.service.SunbirdMWService;
import org.sunbird.actorutil.location.impl.LocationClientImpl;
import org.sunbird.actorutil.org.OrganisationClient;
import org.sunbird.actorutil.org.impl.OrganisationClientImpl;
import org.sunbird.actorutil.systemsettings.impl.SystemSettingClientImpl;
import org.sunbird.actorutil.user.UserClient;
import org.sunbird.actorutil.user.impl.UserClientImpl;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.Constants;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.Util;
import org.sunbird.models.organisation.Organisation;
import org.sunbird.models.user.User;
import org.sunbird.models.user.UserType;
import org.sunbird.user.actors.UserManagementActor;
import org.sunbird.user.service.impl.UserServiceImpl;
import org.sunbird.user.util.UserUtil;
import scala.concurrent.Future;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  EsClientFactory.class,
  Util.class,
  SystemSettingClientImpl.class,
  UserServiceImpl.class,
  UserUtil.class,
  Patterns.class,
  LocationClientImpl.class,
  DataCacheHandler.class,
  ElasticSearchRestHighImpl.class,
  SunbirdMWService.class,
  PipeToSupport.PipeableFuture.class,
  UserClientImpl.class,
  OrganisationClientImpl.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public abstract class UserManagementActorTestBase {

  public ActorSystem system = ActorSystem.create("system");
  public static final Props props = Props.create(UserManagementActor.class);
  public static Map<String, Object> reqMap;
  public static UserServiceImpl userService;
  public static CassandraOperationImpl cassandraOperation;
  public static ElasticSearchService esService;
  public static UserClient userClient;
  private static OrganisationClient organisationClient;

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(ServiceFactory.class);
    PowerMockito.mockStatic(EsClientFactory.class);
    PowerMockito.mockStatic(SunbirdMWService.class);
    SunbirdMWService.tellToBGRouter(Mockito.any(), Mockito.any());
    cassandraOperation = mock(CassandraOperationImpl.class);
    esService = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(cassandraOperation.insertRecord(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.any(RequestContext.class)))
        .thenReturn(getSuccessResponse());
    when(cassandraOperation.updateRecord(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.any(RequestContext.class)))
        .thenReturn(getSuccessResponse());
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.any(RequestContext.class)))
        .thenReturn(getOrgFromCassandra());
    when(cassandraOperation.getRecordById(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.any(RequestContext.class)))
        .thenReturn(getOrgandLocationFromCassandra());

    PowerMockito.mockStatic(Patterns.class);
    Future<Object> future = Futures.future(() -> getEsResponse(), system.dispatcher());
    when(Patterns.ask(
            Mockito.any(ActorRef.class), Mockito.any(Request.class), Mockito.any(Timeout.class)))
        .thenReturn(future);

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

    PowerMockito.mockStatic(UserClientImpl.class);
    userClient = mock(UserClientImpl.class);

    PowerMockito.mockStatic(UserServiceImpl.class);
    userService = mock(UserServiceImpl.class);
    when(UserServiceImpl.getInstance()).thenReturn(userService);
    when(userService.getRootOrgIdFromChannel(
            Mockito.anyString(), Mockito.any(RequestContext.class)))
        .thenReturn("anyId");
    when(userService.getCustodianChannel(
            Mockito.anyMap(), Mockito.any(ActorRef.class), Mockito.any(RequestContext.class)))
        .thenReturn("anyChannel");
    when(userService.getRootOrgIdFromChannel(
            Mockito.anyString(), Mockito.any(RequestContext.class)))
        .thenReturn("rootOrgId");

    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getEsResponseMap());
    when(esService.getDataByIdentifier(
            Mockito.anyString(), Mockito.anyString(), Mockito.any(RequestContext.class)))
        .thenReturn(promise.future());
    Map<String, Object> map = new HashMap<>();
    Promise<String> esPromise = Futures.promise();
    esPromise.success("success");
    when(esService.save(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.any(RequestContext.class)))
        .thenReturn(esPromise.future());
    PowerMockito.mockStatic(Util.class);

    PowerMockito.mockStatic(UserUtil.class);
    UserUtil.setUserDefaultValue(Mockito.anyMap(), Mockito.anyString(), Mockito.any());

    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(JsonKey.ROOT_ORG_ID, "rootOrgId");
    requestMap.put(JsonKey.TNC_ACCEPTED_ON, 12345678L);
    requestMap.put(JsonKey.USERNAME, "username");
    requestMap.put(JsonKey.EMAIL, "username@gmail.com");
    requestMap.put(JsonKey.PHONE, "4346345377");
    List externalIds = new ArrayList();
    Map externalId = new HashMap();
    externalId.put(JsonKey.ID, "extid1e2d");
    externalId.put(JsonKey.ID_TYPE, "channel1003");
    externalId.put(JsonKey.PROVIDER, "channel1003");
    externalId.put(JsonKey.OPERATION, "add");
    externalIds.add(externalId);
    requestMap.put(JsonKey.EXTERNAL_IDS, externalIds);
    when(UserUtil.encryptUserData(Mockito.anyMap())).thenReturn(requestMap);
    PowerMockito.mockStatic(DataCacheHandler.class);
    when(DataCacheHandler.getRoleMap()).thenReturn(roleMap(true));
    when(DataCacheHandler.getUserTypesConfig()).thenReturn(getUserTypes());
    when(UserUtil.getActiveUserOrgDetails(Mockito.anyString(), Mockito.any()))
        .thenReturn(getUserOrgDetails());
    Map<String, String> configMap = new HashMap<>();
    configMap.put(JsonKey.CUSTODIAN_ORG_CHANNEL, "channel");
    configMap.put(JsonKey.CUSTODIAN_ORG_ID, "custodianRootOrgId");
    when(DataCacheHandler.getConfigSettings()).thenReturn(configMap);
    reqMap = getMapObject();
    organisationClient = mock(OrganisationClient.class);
    mockStatic(OrganisationClientImpl.class);
    when(OrganisationClientImpl.getInstance()).thenReturn(organisationClient);
    Organisation org = new Organisation();
    org.setRootOrgId("anyOrgId");
    org.setId("anyOrgId");
    when(organisationClient.esGetOrgByExternalId(
            Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(org);
  }

  public Response getOrgFromCassandra() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ORG_ID, "anyOrgId");
    list.add(map);
    response.put(Constants.RESPONSE, list);
    return response;
  }

  public Response getOrgandLocationFromCassandra() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ORG_ID, "anyOrgId");
    map.put(JsonKey.LOCATION_IDS, new ArrayList<String>(Arrays.asList("anyLocationId")));
    list.add(map);
    response.put(Constants.RESPONSE, list);
    return response;
  }

  public void mockForUserOrgUpdate() {
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getListOrgResponse());
    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any(RequestContext.class)))
        .thenReturn(promise.future());
    when(userService.getUserById(Mockito.anyString(), Mockito.any())).thenReturn(getUser(false));
    when(cassandraOperation.insertRecord(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.any(RequestContext.class)))
        .thenReturn(null);
  }

  public Map<String, Object> getListOrgResponse() {
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ID, "org1");
    map.put(JsonKey.HASHTAGID, "hashtagId");
    map.put(JsonKey.CONTACT_DETAILS, "any");
    Map<String, Object> response = new HashMap<>();
    List<Map<String, Object>> content = new ArrayList<>();
    content.add(map);
    response.put(JsonKey.CONTENT, content);
    return response;
  }

  public Map<String, Object> getUserOrgUpdateRequest(boolean validOrgReq) {
    Map<String, Object> req = new HashMap<>();
    req.put(JsonKey.USER_ID, "userId");
    req.put(JsonKey.ORGANISATIONS, "any");
    if (validOrgReq) {
      Map<String, Object> map = new HashMap<>();
      List<Map<String, Object>> list = new ArrayList<>();
      map.put(JsonKey.ORGANISATION_ID, "org1");
      map.put(JsonKey.ROLES, Arrays.asList("PUBLIC"));
      list.add(map);
      req.put(JsonKey.ORGANISATIONS, list);
    }
    return req;
  }

  public List<Map<String, Object>> getUserOrgDetails() {
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ORGANISATION_ID, "any");
    list.add(map);
    return list;
  }

  public Map<String, Object> roleMap(boolean validRole) {
    Map<String, Object> map = new HashMap<>();
    if (validRole) {
      map.put("PUBLIC", "PUBLIC");
    }
    map.put("Invalid", "Invalid");
    return map;
  }

  public Map<String, Object> getAdditionalMapData(Map<String, Object> reqMap) {
    reqMap.put(JsonKey.ORGANISATION_ID, "anyOrgId");
    reqMap.put(JsonKey.CHANNEL, "anyChannel");
    return reqMap;
  }

  public Map<String, List<String>> getUserTypes() {
    Map<String, List<String>> userTypeOrSubTypeConfigMap = new HashMap<>();
    userTypeOrSubTypeConfigMap.put(UserType.STUDENT.getTypeName().toUpperCase(), Arrays.asList());
    userTypeOrSubTypeConfigMap.put(
        UserType.ADMINISTRATOR.getTypeName().toUpperCase(), Arrays.asList("BRC,DAO"));
    userTypeOrSubTypeConfigMap.put(UserType.TEACHER.getTypeName().toUpperCase(), Arrays.asList());
    userTypeOrSubTypeConfigMap.put(UserType.GUARDIAN.getTypeName().toUpperCase(), Arrays.asList());
    return userTypeOrSubTypeConfigMap;
  }

  public boolean testScenario(Request reqObj, ResponseCode errorCode) {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    subject.tell(reqObj, probe.getRef());

    if (errorCode == null) {
      Response res = probe.expectMsgClass(duration("1000 second"), Response.class);
      return null != res && res.getResponseCode() == ResponseCode.OK;
    } else {
      ProjectCommonException res =
          probe.expectMsgClass(duration("1000 second"), ProjectCommonException.class);
      return res.getCode().equals(errorCode.getErrorCode())
          || res.getResponseCode() == errorCode.getResponseCode();
    }
  }

  public Map<String, Object> getExternalIdMap() {

    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.EXTERNAL_ID, "externalId");
    reqMap.put(JsonKey.EXTERNAL_ID_PROVIDER, "externalIdProvider");
    reqMap.put(JsonKey.EXTERNAL_ID_TYPE, "externalIdType");
    return reqMap;
  }

  public HashMap<String, Object> getMapObject() {

    HashMap<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.FIRST_NAME, "firstname");
    reqMap.put(JsonKey.USERNAME, "userName");
    reqMap.put(JsonKey.EMAIL, "email@email.com");
    reqMap.put(JsonKey.LANGUAGE, new ArrayList<>());
    reqMap.put(JsonKey.DOB, "1992-12-31");
    reqMap.put(JsonKey.EMAIL_VERIFIED, true);
    reqMap.put(JsonKey.PHONE_VERIFIED, true);
    reqMap.put(JsonKey.ADDRESS, new ArrayList<>());
    return reqMap;
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
    reqObj.setOperation(actorOperation.getValue());
    return reqObj;
  }

  public Map<String, Object> getUpdateRequestWithLocationCodes() {
    Map<String, Object> reqObj = new HashMap();
    reqObj.put(JsonKey.LOCATION_CODES, Arrays.asList("locationCode"));
    reqObj.put(JsonKey.USER_ID, "userId");
    getUpdateRequestWithDefaultFlags(reqObj);
    return reqObj;
  }

  public Map<String, Object> getUpdateRequestWithLocationCodeSchoolAsOrgExtId() {
    Map<String, Object> reqObj = new HashMap();
    reqObj.put(JsonKey.ORG_EXTERNAL_ID, "orgExtId");
    reqObj.put(JsonKey.USER_ID, "userId");
    reqObj.put("updateUserSchoolOrg", true);
    getUpdateRequestWithDefaultFlags(reqObj);
    return reqObj;
  }

  public Map<String, Object> getUpdateRequestWithDefaultFlags(Map<String, Object> reqObj) {
    reqObj.put(JsonKey.EMAIL_VERIFIED, false);
    reqObj.put(JsonKey.PHONE_VERIFIED, false);
    reqObj.put(JsonKey.STATE_VALIDATED, false);
    return reqObj;
  }

  public static Response getEsResponse() {

    Response response = new Response();
    Map<String, Object> map = new HashMap<>();
    map.put("anyString", new Object());
    response.put(JsonKey.RESPONSE, map);
    return response;
  }

  public static Response getSuccessResponse() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    return response;
  }

  public static Map<String, Object> getEsResponseMap() {
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.IS_ROOT_ORG, true);
    map.put(JsonKey.ID, "rootOrgId");
    map.put(JsonKey.CHANNEL, "anyChannel");
    return map;
  }

  public Object getEsResponseForLocation() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, Arrays.asList("id"));
    return response;
  }

  public User getUser(boolean isCustodian) {
    User user = new User();
    user.setRootOrgId("rootOrgId");
    if (isCustodian) {
      user.setRootOrgId("custodianOrgId");
    }
    return user;
  }
}
