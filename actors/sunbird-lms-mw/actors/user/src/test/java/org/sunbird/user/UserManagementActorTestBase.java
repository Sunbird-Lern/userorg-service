package org.sunbird.user;

import static akka.testkit.JavaTestKit.duration;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Futures;
import scala.concurrent.Future;
import akka.testkit.javadsl.TestKit;
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
import org.sunbird.actor.router.RequestRouter;
import org.sunbird.actor.service.SunbirdMWService;
import org.sunbird.actorutil.InterServiceCommunication;
import org.sunbird.actorutil.InterServiceCommunicationFactory;
import org.sunbird.actorutil.impl.InterServiceCommunicationImpl;
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
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.Util;
import org.sunbird.models.user.User;
import org.sunbird.user.actors.UserManagementActor;
import org.sunbird.user.service.impl.UserServiceImpl;
import org.sunbird.user.util.UserUtil;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  EsClientFactory.class,
  Util.class,
  RequestRouter.class,
  SystemSettingClientImpl.class,
  UserServiceImpl.class,
  UserUtil.class,
  InterServiceCommunicationFactory.class,
  LocationClientImpl.class,
  DataCacheHandler.class,
  ElasticSearchRestHighImpl.class,
  SunbirdMWService.class
})
@PowerMockIgnore({"javax.management.*"})
public abstract class UserManagementActorTestBase {

  public ActorSystem system = ActorSystem.create("system");
  public static final Props props = Props.create(UserManagementActor.class);
  public static Map<String, Object> reqMap;
  static InterServiceCommunication interServiceCommunication =
      mock(InterServiceCommunicationImpl.class);;
  public static UserServiceImpl userService;
  public static CassandraOperationImpl cassandraOperation;
  public static ElasticSearchService esService;

  @Before
  public void beforeEachTest() {

    ActorRef actorRef = mock(ActorRef.class);
    PowerMockito.mockStatic(RequestRouter.class);
    when(RequestRouter.getActor(Mockito.anyString())).thenReturn(actorRef);

    PowerMockito.mockStatic(ServiceFactory.class);
    PowerMockito.mockStatic(EsClientFactory.class);
    PowerMockito.mockStatic(SunbirdMWService.class);
    SunbirdMWService.tellToBGRouter(Mockito.any(), Mockito.any());
    cassandraOperation = mock(CassandraOperationImpl.class);
    esService = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(getSuccessResponse());
    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(getSuccessResponse());

    PowerMockito.mockStatic(InterServiceCommunicationFactory.class);
    when(InterServiceCommunicationFactory.getInstance()).thenReturn(interServiceCommunication);
    when(interServiceCommunication.getResponse(
            Mockito.any(ActorRef.class), Mockito.any(Request.class)))
        .thenReturn(getEsResponse());

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

    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getEsResponseMap());
    when(esService.getDataByIdentifier(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(promise.future());
    Map<String,Object>map=new HashMap<>();
    Promise<String> esPromise = Futures.promise();
    esPromise.success("success");
    when(esService.save(Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
      .thenReturn(esPromise.future());
    PowerMockito.mockStatic(Util.class);
    Util.getUserProfileConfig(Mockito.any(ActorRef.class));

    PowerMockito.mockStatic(UserUtil.class);
    UserUtil.setUserDefaultValue(Mockito.anyMap(), Mockito.anyString());

    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(JsonKey.ROOT_ORG_ID, "rootOrgId");
    requestMap.put(JsonKey.TNC_ACCEPTED_ON, 12345678L);
    when(UserUtil.encryptUserData(Mockito.anyMap())).thenReturn(requestMap);
    PowerMockito.mockStatic(DataCacheHandler.class);
    when(DataCacheHandler.getRoleMap()).thenReturn(roleMap(true));
    when(UserUtil.getActiveUserOrgDetails(Mockito.anyString())).thenReturn(getUserOrgDetails());
    reqMap = getMapObject();
  }

  public void mockForUserOrgUpdate() {
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getListOrgResponse());
    when(esService.search(Mockito.any(), Mockito.anyString())).thenReturn(promise.future());
    when(userService.getUserById(Mockito.anyString())).thenReturn(getUser(false));
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
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

  public boolean testScenario(Request reqObj, ResponseCode errorCode) {

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
