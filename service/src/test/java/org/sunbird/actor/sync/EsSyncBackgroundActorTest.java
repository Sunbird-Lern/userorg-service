package org.sunbird.actor.sync;

import java.time.Duration;

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
import org.junit.Assert;
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
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  ElasticSearchRestHighImpl.class,
  EsClientFactory.class,
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class EsSyncBackgroundActorTest {
  private ActorSystem system = ActorSystem.create("system");
  private static final Props props = Props.create(EsSyncBackgroundActor.class);
  private static CassandraOperationImpl cassandraOperation;
  private static ElasticSearchService esService;

  @BeforeClass
  public static void beforeEachTest() {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    PowerMockito.mockStatic(EsClientFactory.class);
    esService = mock(ElasticSearchRestHighImpl.class);
    PowerMockito.mockStatic(EsClientFactory.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);

    Promise<Boolean> promise = Futures.promise();
    promise.success(true);

    when(esService.bulkInsert(Mockito.anyString(), Mockito.anyList(), Mockito.any()))
        .thenReturn(promise.future());
    Promise<String> promise2 = Futures.promise();
    promise2.success("anyId");
    when(esService.save(Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(promise2.future());

    Map<String, Object> user = new HashMap<>();
    user.put(JsonKey.FIRST_NAME, "firstName");
    user.put(JsonKey.USER_ID, "897-465-13213");
    user.put(JsonKey.ID, "897-465-13213");
    Response response = new Response();
    List<Map<String, Object>> resList = new ArrayList<>();
    resList.add(user);
    response.getResult().put(JsonKey.RESPONSE, resList);
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(response);
  }

  @Test
  public void testSyncUser() {
    when(cassandraOperation.getRecordsByProperty(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyList(),
            Mockito.any()))
        .thenReturn(cassandraGetUserRecord());
    Map<String, Object> user = new HashMap<>();
    user.put(JsonKey.FIRST_NAME, "firstName");
    user.put(JsonKey.USER_ID, "897-465-13213");
    user.put(JsonKey.ID, "897-465-13213");
    Response response = new Response();
    List<Map<String, Object>> resList = new ArrayList<>();
    resList.add(user);
    response.getResult().put(JsonKey.RESPONSE, resList);
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(response);
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.BACKGROUND_SYNC.getValue());
    Map<String, Object> reqMap = new HashMap<>();
    List<String> ids = new ArrayList<>();
    ids.add("897-465-13213");
    reqMap.put(JsonKey.OBJECT_IDS, ids);
    reqMap.put(JsonKey.OBJECT_TYPE, JsonKey.USER);
    reqMap.put(JsonKey.OPERATION_TYPE, JsonKey.SYNC);
    reqObj.getRequest().put(JsonKey.DATA, reqMap);
    reqObj.setRequestContext(new RequestContext());
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(Duration.ofSeconds(10), Response.class);
    Assert.assertTrue(null != res && res.getResponseCode() == ResponseCode.OK);
  }

  @Test
  public void testSync() {
    when(cassandraOperation.getPropertiesValueById(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyList(),
            Mockito.anyList(),
            Mockito.any()))
        .thenReturn(cassandraGetLocationRecord());
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.BACKGROUND_SYNC.getValue());
    Map<String, Object> reqMap = new HashMap<>();
    List<String> ids = new ArrayList<>();
    ids.add("1544646556");
    reqMap.put(JsonKey.OBJECT_IDS, ids);
    reqMap.put(JsonKey.OBJECT_TYPE, JsonKey.LOCATION);
    reqMap.put(JsonKey.OPERATION_TYPE, JsonKey.SYNC);
    reqObj.getRequest().put(JsonKey.DATA, reqMap);
    reqObj.setRequestContext(new RequestContext());
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(Duration.ofSeconds(100), Response.class);
    Assert.assertTrue(null != res && res.getResponseCode() == ResponseCode.OK);
  }

  @Test
  public void testSyncOrg() {
    when(cassandraOperation.getPropertiesValueById(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyList(),
            Mockito.anyList(),
            Mockito.any()))
        .thenReturn(cassandraGetOrgRecord());
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.BACKGROUND_SYNC.getValue());
    Map<String, Object> reqMap = new HashMap<>();
    List<String> ids = new ArrayList<>();
    ids.add("1544646556");
    reqMap.put(JsonKey.OBJECT_IDS, ids);
    reqMap.put(JsonKey.OBJECT_TYPE, JsonKey.ORGANISATION);
    reqMap.put(JsonKey.OPERATION_TYPE, JsonKey.SYNC);
    reqObj.getRequest().put(JsonKey.DATA, reqMap);
    reqObj.setRequestContext(new RequestContext());
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(Duration.ofSeconds(100), Response.class);
    Assert.assertTrue(null != res && res.getResponseCode() == ResponseCode.OK);
  }

  private static Response cassandraGetOrgRecord() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ID, "anyId");
    map.put(JsonKey.ORG_TYPE, "type");
    map.put(JsonKey.ORG_NAME, "name");
    map.put(JsonKey.CHANNEL, "ch");
    map.put(
        JsonKey.ORG_LOCATION,
        "[{\"id\":\"1\",\"type\":\"state\"},{\"id\":\"2\",\"type\":\"district\"}]");
    list.add(map);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }

  private static Response cassandraGetUserRecord() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ID, "anyId");
    map.put(JsonKey.FIRST_NAME, "name");
    map.put(JsonKey.CHANNEL, "ch");
    map.put(JsonKey.USER_ID, "anyId");
    list.add(map);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }

  private static Response cassandraGetLocationRecord() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ID, "anyId");
    map.put(JsonKey.NAME, "anyLocation");
    map.put(JsonKey.CODE, "code");
    map.put(JsonKey.PARENT_ID, "parentId");
    list.add(map);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }
}
