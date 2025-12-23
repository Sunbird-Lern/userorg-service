package org.sunbird.actor.user;

import java.time.Duration;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

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
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.dao.notification.impl.EmailTemplateDaoImpl;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.util.DataCacheHandler;
import org.sunbird.util.UserUtility;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  UserUtility.class,
  DataCacheHandler.class,
  org.sunbird.datasecurity.impl.ServiceFactory.class,
  EmailTemplateDaoImpl.class,
  EsClientFactory.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class UserExternalIdManagementActorTest {

  private static final Props props = Props.create(UserExternalIdManagementActor.class);
  private ActorSystem system = ActorSystem.create("system");
  private static CassandraOperationImpl cassandraOperation;

  @BeforeClass
  public static void setUp() {

    PowerMockito.mockStatic(ServiceFactory.class);
    PowerMockito.mockStatic(EmailTemplateDaoImpl.class);
    PowerMockito.mockStatic(org.sunbird.datasecurity.impl.ServiceFactory.class);
    PowerMockito.mockStatic(EsClientFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
  }

  private static Response getSuccessResponse() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    return response;
  }

  @Before
  public void beforeTest() {

    PowerMockito.mockStatic(ServiceFactory.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(cassandraOperation.upsertRecord(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.any(RequestContext.class)))
        .thenReturn(cassandraUpsertRecord());
    when(cassandraOperation.getRecordById(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.any(RequestContext.class)))
        .thenReturn(getCassandraRecordsByIndexedProperty());
    cassandraOperation.deleteRecord(
        Mockito.anyString(),
        Mockito.anyString(),
        Mockito.anyMap(),
        Mockito.any(RequestContext.class));
    PowerMockito.mockStatic(UserUtility.class);
    when(UserUtility.encryptData(Mockito.anyString())).thenReturn("userExtId");
  }

  private Response getCassandraRecordsByIndexedProperty() {
    Response response = new Response();
    List list = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ID_TYPE, "anyIdType");
    map.put(JsonKey.PROVIDER, "anyProvider");
    list.add(map);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }

  private Response cassandraUpsertRecord() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    return response;
  }

  @Test
  public void testCreateUserExternalIdentityDetailsSuccess() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request request = new Request();
    request.setOperation(ActorOperations.UPSERT_USER_EXTERNAL_IDENTITY_DETAILS.getValue());

    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.OPERATION_TYPE, "CREATE");

    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> extIdMap = new HashMap<>();
    extIdMap.put(JsonKey.OPERATION, "ADD");
    list.add(extIdMap);
    innerMap.put(JsonKey.EXTERNAL_IDS, list);
    request.setRequest(innerMap);

    subject.tell(request, probe.getRef());
    Response response = probe.expectMsgClass(Duration.ofSeconds(100), Response.class);
    Assert.assertTrue(null != response && response.getResponseCode() == ResponseCode.OK);
  }

  @Test
  public void testUpsertUserExternalIdentityDetailsAddSuccess() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request request = new Request();
    request.setOperation(ActorOperations.UPSERT_USER_EXTERNAL_IDENTITY_DETAILS.getValue());

    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.OPERATION_TYPE, "UPDATE");

    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> extIdMap = new HashMap<>();
    extIdMap.put(JsonKey.OPERATION, "ADD");
    extIdMap.put(JsonKey.ID_TYPE, "anyIdType");
    extIdMap.put(JsonKey.PROVIDER, "anyProvider");
    list.add(extIdMap);
    innerMap.put(JsonKey.EXTERNAL_IDS, list);
    request.setRequest(innerMap);

    subject.tell(request, probe.getRef());
    Response response = probe.expectMsgClass(Duration.ofSeconds(100), Response.class);
    Assert.assertTrue(null != response && response.getResponseCode() == ResponseCode.OK);
  }

  @Test
  public void testUpsertUserExternalIdentityDetailsRemoveSuccess() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request request = new Request();
    request.setOperation(ActorOperations.UPSERT_USER_EXTERNAL_IDENTITY_DETAILS.getValue());

    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.OPERATION_TYPE, "UPDATE");

    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> extIdMap = new HashMap<>();
    extIdMap.put(JsonKey.OPERATION, "REMOVE");
    extIdMap.put(JsonKey.ID_TYPE, "anyIdType");
    extIdMap.put(JsonKey.PROVIDER, "anyProvider");
    list.add(extIdMap);
    innerMap.put(JsonKey.EXTERNAL_IDS, list);
    innerMap.put(JsonKey.USER_ID, "anyUserId");
    request.setRequest(innerMap);

    subject.tell(request, probe.getRef());
    Response response = probe.expectMsgClass(Duration.ofSeconds(100), Response.class);
    Assert.assertTrue(null != response && response.getResponseCode() == ResponseCode.OK);
  }

  @Test
  public void testUpsertUserExternalIdentityDetailsEditSuccess() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request request = new Request();
    request.setOperation(ActorOperations.UPSERT_USER_EXTERNAL_IDENTITY_DETAILS.getValue());

    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.OPERATION_TYPE, "UPDATE");

    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> extIdMap = new HashMap<>();
    extIdMap.put(JsonKey.OPERATION, "EDIT");
    extIdMap.put(JsonKey.ID_TYPE, "anyIdType");
    extIdMap.put(JsonKey.PROVIDER, "anyProvider");
    list.add(extIdMap);
    innerMap.put(JsonKey.EXTERNAL_IDS, list);
    innerMap.put(JsonKey.USER_ID, "anyUserId");
    request.setRequest(innerMap);

    subject.tell(request, probe.getRef());
    Response response = probe.expectMsgClass(Duration.ofSeconds(100), Response.class);
    Assert.assertTrue(null != response && response.getResponseCode() == ResponseCode.OK);
  }
}
