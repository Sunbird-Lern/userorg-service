package org.sunbird.actor.user;

import static akka.testkit.JavaTestKit.duration;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.testkit.javadsl.TestKit;
import java.util.*;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.client.systemsettings.impl.SystemSettingClientImpl;
import org.sunbird.common.Constants;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.dao.user.UserLookupDao;
import org.sunbird.dao.user.UserOrgDao;
import org.sunbird.dao.user.impl.UserLookupDaoImpl;
import org.sunbird.dao.user.impl.UserOrgDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.util.DataCacheHandler;
import org.sunbird.util.ProjectUtil;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  CassandraOperationImpl.class,
  SystemSettingClientImpl.class,
  DataCacheHandler.class,
  UserLookupDao.class,
  UserLookupDaoImpl.class,
  EsClientFactory.class,
  ElasticSearchRestHighImpl.class,
  UserOrgDao.class,
  UserOrgDaoImpl.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class TenantMigrationActorTest {
  Props props = Props.create(TenantMigrationActor.class);
  ActorSystem system = ActorSystem.create("system");

  private static CassandraOperation cassandraOperation = null;
  private static ElasticSearchService esService;
  private static Response response;

  @BeforeClass
  public static void beforeEachTest() throws Exception {
    PowerMockito.mockStatic(ServiceFactory.class);
    PowerMockito.mockStatic(UserLookupDaoImpl.class);
    PowerMockito.mockStatic(EsClientFactory.class);
    esService = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getListOrgResponse());
    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
    UserLookupDao userLookupDao = PowerMockito.mock(UserLookupDao.class);
    PowerMockito.when(UserLookupDaoImpl.getInstance()).thenReturn(userLookupDao);
    cassandraOperation = mock(CassandraOperationImpl.class);
    response = new Response();
    Map<String, Object> responseMap = new HashMap<>();
    responseMap.put(Constants.RESPONSE, Arrays.asList(getFeedMap()));
    response.getResult().putAll(responseMap);
    PowerMockito.when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    PowerMockito.when(
            cassandraOperation.getRecordsByProperties(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(RequestContext.class)))
        .thenReturn(response);

    Response upsertResponse = new Response();
    Map<String, Object> responseMap2 = new HashMap<>();
    responseMap2.put(Constants.RESPONSE, Constants.SUCCESS);
    upsertResponse.getResult().putAll(responseMap2);
    PowerMockito.when(
            cassandraOperation.upsertRecord(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(RequestContext.class)))
        .thenReturn(upsertResponse);

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
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(response);
    PowerMockito.when(
            userLookupDao.insertExternalIdIntoUserLookup(
                Mockito.anyList(), Mockito.anyString(), Mockito.any()))
        .thenReturn(response);
    List<Map<String, Object>> listMap = new ArrayList<>();
    Map<String, Object> orgs = new HashMap<>();
    orgs.put(JsonKey.USER_ID, "anyUserId");
    orgs.put(JsonKey.ORGANISATION_ID, "anyRootOrgId");
    listMap.add(orgs);
    Map<String, Object> userDetails = new HashMap<>();
    userDetails.put(JsonKey.ROOT_ORG_ID, "anyRootOrgId");
    userDetails.put(JsonKey.ORGANISATIONS, listMap);
    Response response1 = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    list.add(userDetails);
    response1.getResult().put(JsonKey.RESPONSE, list);
    PowerMockito.when(
            cassandraOperation.getRecordById(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(response1);
    PowerMockito.when(
            cassandraOperation.getRecordById(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getSelfDeclarationResponse());
    PowerMockito.when(
            cassandraOperation.updateRecord(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(response);
    Response updateResponse = new Response();
    updateResponse.getResult().put(JsonKey.RESPONSE, "FAILED");
    PowerMockito.when(
            cassandraOperation.updateRecord(
                Mockito.any(), Mockito.any(), Mockito.anyMap(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(updateResponse);
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getOrgFromCassandra());
  }

  public static Map<String, Object> getListOrgResponse() {
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ID, "anyRootOrgId");
    map.put(JsonKey.HASHTAGID, "anyRootOrgId");
    map.put(JsonKey.STATUS, 1);
    map.put(JsonKey.CHANNEL, "anyProvider");
    Map<String, Object> response = new HashMap<>();
    List<Map<String, Object>> content = new ArrayList<>();
    content.add(map);
    response.put(JsonKey.CONTENT, content);
    return response;
  }

  public Request getFailureMigrateReq(ActorOperations actorOperation, String action) {
    Request reqObj = new Request();
    Map reqMap = new HashMap<>();
    reqMap.put(JsonKey.USER_ID, "WrongUserId");
    reqMap.put(JsonKey.USER_EXT_ID, "WrongAnyUserExtId");
    reqMap.put(JsonKey.CHANNEL, "anyChannel");
    reqMap.put(JsonKey.ACTION, action);
    reqMap.put(JsonKey.FEED_ID, "anyFeedId");
    reqObj.setRequest(reqMap);
    reqObj.setOperation(actorOperation.getValue());
    return reqObj;
  }

  public boolean testScenario(Request reqObj, ResponseCode errorCode, Props props) {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    subject.tell(reqObj, probe.getRef());
    if (errorCode == null) {
      Response res = probe.expectMsgClass(duration("100 second"), Response.class);
      return null != res && res.getResponseCode() == ResponseCode.OK;
    } else {
      ProjectCommonException res =
          probe.expectMsgClass(duration("100 second"), ProjectCommonException.class);
      return res.getCode().equals(errorCode.getErrorCode())
          || res.getResponseCode() == errorCode.getResponseCode();
    }
  }

  public Request getMigrateReq(ActorOperations actorOperation, String action) {
    Request reqObj = new Request();
    Map reqMap = new HashMap<>();
    reqMap.put(JsonKey.USER_ID, "anyUserId");
    reqMap.put(JsonKey.USER_EXT_ID, "anyUserExtId");
    reqMap.put(JsonKey.CHANNEL, "anyChannel");
    reqMap.put(JsonKey.ACTION, action);
    reqMap.put(JsonKey.FEED_ID, "anyFeedId");
    reqObj.setRequest(reqMap);
    reqObj.setOperation(actorOperation.getValue());
    return reqObj;
  }

  private static Map<String, Object> getFeedMap() {
    Map<String, Object> fMap = new HashMap<>();
    fMap.put(JsonKey.ID, "123-456-7890");
    fMap.put(JsonKey.USER_ID, "123-456-789");
    fMap.put(JsonKey.CATEGORY, "category");
    return fMap;
  }

  @Test
  public void testUserSelfDeclarationMigrationWithValidatedStatus() {
    PowerMockito.mockStatic(DataCacheHandler.class);
    Map<String, String> dataCache = new HashMap<>();
    dataCache.put(JsonKey.CUSTODIAN_ORG_ID, "anyRootOrgId");
    when(DataCacheHandler.getConfigSettings()).thenReturn(dataCache);
    boolean result =
        testScenario(
            getSelfDeclaredMigrateReq(ActorOperations.USER_SELF_DECLARED_TENANT_MIGRATE),
            ResponseCode.errorUserMigrationFailed,
            props);
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
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    Assert.assertNotNull(exception);
  }

  public Map<String, Object> getOrgandLocation() {
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ORG_ID, "anyOrgId");
    map.put(JsonKey.LOCATION_IDS, new ArrayList<String>(Arrays.asList("anyLocationId")));
    return map;
  }

  public static Response getOrgFromCassandra() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ORG_ID, "anyRootOrgId");
    map.put(JsonKey.LOCATION_IDS, new ArrayList<String>(Arrays.asList("anyLocationId")));
    list.add(map);
    response.put(Constants.RESPONSE, list);
    return response;
  }

  @Test
  public void testUserSelfDeclarationMigrationWithValidatedStatuswithError() {
    Response updateResponse = new Response();
    updateResponse.getResult().put(JsonKey.RESPONSE, "FAILED");
    PowerMockito.when(
            cassandraOperation.updateRecord(
                Mockito.any(), Mockito.any(), Mockito.anyMap(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(updateResponse);
    List<Map<String, Object>> listMap = new ArrayList<>();
    listMap.add(new HashMap<>());
    Map<String, Object> userDetails = new HashMap<>();
    userDetails.put(JsonKey.ROOT_ORG_ID, "");
    userDetails.put(JsonKey.ORGANISATIONS, listMap);

    boolean result =
        testScenario(
            getSelfDeclaredMigrateReq(ActorOperations.USER_SELF_DECLARED_TENANT_MIGRATE),
            ResponseCode.parameterMismatch,
            props);
    assertTrue(result);
  }

  public Request getSelfDeclaredMigrateReq(ActorOperations actorOperation) {
    Request reqObj = new Request();
    Map<String, Object> requestMap = new HashMap();
    Map<String, String> externalIdMap = new HashMap();
    List<Map<String, String>> externalIdLst = new ArrayList();
    requestMap.put(JsonKey.USER_ID, "anyUserID");
    requestMap.put(JsonKey.CHANNEL, "anyChannel");
    requestMap.put(JsonKey.ROOT_ORG_ID, "anyRootOrgId");
    externalIdMap.put(JsonKey.ID, "anyID");
    externalIdMap.put(JsonKey.ID_TYPE, "anyIDtype");
    externalIdMap.put(JsonKey.PROVIDER, "anyProvider");
    externalIdLst.add(externalIdMap);
    requestMap.put(JsonKey.EXTERNAL_IDS, externalIdLst);
    requestMap.put(JsonKey.ORG_EXTERNAL_ID, "anyOrgId");
    requestMap.put(JsonKey.STATUS, ProjectUtil.Status.ACTIVE.getValue());
    reqObj.setRequest(requestMap);
    reqObj.setOperation(actorOperation.getValue());
    return reqObj;
  }

  private static Response getSelfDeclarationResponse() {
    Response response = new Response();

    Map<String, Object> responseMap = new HashMap<>();
    responseMap.put(JsonKey.ORG_ID, "anyOrgID");
    responseMap.put(JsonKey.PERSONA, "anyPersona");
    responseMap.put(Constants.RESPONSE, Arrays.asList(responseMap));
    response.getResult().putAll(responseMap);
    return response;
  }
}
