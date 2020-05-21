package org.sunbird.learner.actors.tenantpreference;

import static akka.testkit.JavaTestKit.duration;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import java.util.*;
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
import org.sunbird.actor.router.RequestRouter;
import org.sunbird.actor.service.BaseMWService;
import org.sunbird.actorutil.InterServiceCommunicationFactory;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.datasecurity.DecryptionService;
import org.sunbird.common.request.Request;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.Util;

// import org.sunbird.user.dao.impl.UserOrgDaoImpl;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  BaseMWService.class,
  RequestRouter.class,
  InterServiceCommunicationFactory.class,
  ElasticSearchHelper.class,
  Util.class,
  //  UserOrgDaoImpl.class,
  DecryptionService.class,
  DataCacheHandler.class,
})
@PowerMockIgnore({"javax.management.*"})
public class TenantPreferenceManagementActorTest {

  private ActorSystem system = ActorSystem.create("system");
  private static final Props props = Props.create(TenantPreferenceManagementActor.class);
  private static CassandraOperationImpl cassandraOperation;
  private static final String orgId = "hhjcjr79fw4p89";
  private static final String USER_ID = "vcurc633r8911";

  @BeforeClass
  public static void beforeClass() {

    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
  }

  @Before
  public void beforeEachTest() {

    PowerMockito.mockStatic(ServiceFactory.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);

    when(cassandraOperation.getRecordsByProperty(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(cassandraGetRecordByProperty());
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(createCassandraInsertSuccessResponse());
    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(createCassandraInsertSuccessResponse());
  }

  private Response createCassandraInsertSuccessResponse() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    return response;
  }

  @Test
  public void testCreateSuccessWithTenantPreferenceAlreadyExists() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = new Request();
    List<Map<String, Object>> reqList = new ArrayList<>();

    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.KEY, "anyKey");
    reqList.add(map);

    actorMessage.getRequest().put(JsonKey.TENANT_PREFERENCE, reqList);
    actorMessage.getRequest().put(JsonKey.ROOT_ORG_ID, orgId);
    actorMessage.getRequest().put(JsonKey.REQUESTED_BY, USER_ID);
    actorMessage.setOperation(ActorOperations.CREATE_TENANT_PREFERENCE.getValue());

    subject.tell(actorMessage, probe.getRef());
    Response res = probe.expectMsgClass(duration("10 second"), Response.class);
    Assert.assertTrue(null != res.get(JsonKey.RESPONSE));
  }

  @Test
  public void testCreateSuccessWithTenantPreferenceDoesNotExists() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = new Request();
    List<Map<String, Object>> reqList = new ArrayList<>();

    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.KEY, "differentKey");
    reqList.add(map);

    actorMessage.getRequest().put(JsonKey.TENANT_PREFERENCE, reqList);
    actorMessage.getRequest().put(JsonKey.ROOT_ORG_ID, orgId);
    actorMessage.getRequest().put(JsonKey.REQUESTED_BY, USER_ID);
    actorMessage.setOperation(ActorOperations.CREATE_TENANT_PREFERENCE.getValue());

    subject.tell(actorMessage, probe.getRef());
    Response res = probe.expectMsgClass(duration("10 second"), Response.class);
    Assert.assertTrue(null != res.get(JsonKey.RESPONSE));
  }

  @Test
  public void testCreateTanentPreferenceFailureWithInvalidOrgId() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = new Request();
    List<Map<String, Object>> reqList = new ArrayList<>();

    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ROLE, "admin");
    reqList.add(map);

    actorMessage.getRequest().put(JsonKey.TENANT_PREFERENCE, reqList);
    actorMessage.getRequest().put(JsonKey.ROOT_ORG_ID, "");
    actorMessage.getRequest().put(JsonKey.REQUESTED_BY, USER_ID);
    actorMessage.setOperation(ActorOperations.CREATE_TENANT_PREFERENCE.getValue());

    subject.tell(actorMessage, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    Assert.assertTrue(null != exc);
  }

  @Test
  public void testCreateTanentPreferenceWithInvalidReqData() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = new Request();
    List<Map<String, Object>> reqList = new ArrayList<>();

    actorMessage.getRequest().put(JsonKey.TENANT_PREFERENCE, reqList);
    actorMessage.getRequest().put(JsonKey.ROOT_ORG_ID, orgId);
    actorMessage.getRequest().put(JsonKey.REQUESTED_BY, USER_ID);
    actorMessage.setOperation(ActorOperations.CREATE_TENANT_PREFERENCE.getValue());

    subject.tell(actorMessage, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    Assert.assertTrue(null != exc);
  }

  @Test
  public void testUpdateTanentPreferenceSuccessWithoutKeyValue() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = new Request();
    List<Map<String, Object>> reqList = new ArrayList<>();

    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ROLE, "admin");
    reqList.add(map);

    actorMessage.getRequest().put(JsonKey.TENANT_PREFERENCE, reqList);
    actorMessage.getRequest().put(JsonKey.ROOT_ORG_ID, orgId);
    actorMessage.getRequest().put(JsonKey.REQUESTED_BY, USER_ID);
    actorMessage.setOperation(ActorOperations.UPDATE_TENANT_PREFERENCE.getValue());

    subject.tell(actorMessage, probe.getRef());
    Response res = probe.expectMsgClass(duration("10 second"), Response.class);
    Assert.assertTrue(null != res.get(JsonKey.RESPONSE));
  }

  @Test
  public void testUpdateTanentPreferenceSuccessWithSameKey() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = new Request();
    List<Map<String, Object>> reqList = new ArrayList<>();

    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.KEY, "anyKey");
    map.put(JsonKey.DATA, "anyData");
    reqList.add(map);

    actorMessage.getRequest().put(JsonKey.TENANT_PREFERENCE, reqList);
    actorMessage.getRequest().put(JsonKey.ROOT_ORG_ID, orgId);
    actorMessage.getRequest().put(JsonKey.REQUESTED_BY, USER_ID);
    actorMessage.setOperation(ActorOperations.UPDATE_TENANT_PREFERENCE.getValue());

    subject.tell(actorMessage, probe.getRef());
    Response res = probe.expectMsgClass(duration("10 second"), Response.class);
    Assert.assertTrue(null != res.get(JsonKey.RESPONSE));
  }

  @Test
  public void testUpdateTanentPreferenceSuccessWithDifferentKey() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = new Request();
    List<Map<String, Object>> reqList = new ArrayList<>();

    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.KEY, "differentKey");
    map.put(JsonKey.DATA, "anyData");
    reqList.add(map);

    actorMessage.getRequest().put(JsonKey.TENANT_PREFERENCE, reqList);
    actorMessage.getRequest().put(JsonKey.ROOT_ORG_ID, orgId);
    actorMessage.getRequest().put(JsonKey.REQUESTED_BY, USER_ID);
    actorMessage.setOperation(ActorOperations.UPDATE_TENANT_PREFERENCE.getValue());

    subject.tell(actorMessage, probe.getRef());
    Response res = probe.expectMsgClass(duration("10 second"), Response.class);
    Assert.assertTrue(null != res.get(JsonKey.RESPONSE));
  }

  @Test
  public void testUpdateTanentPreferenceFailureWithInvalidRequestData() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = new Request();
    List<Map<String, Object>> reqList = new ArrayList<>();

    actorMessage.getRequest().put(JsonKey.TENANT_PREFERENCE, reqList);
    actorMessage.getRequest().put(JsonKey.ROOT_ORG_ID, orgId);
    actorMessage.getRequest().put(JsonKey.REQUESTED_BY, USER_ID);
    actorMessage.setOperation(ActorOperations.UPDATE_TENANT_PREFERENCE.getValue());

    subject.tell(actorMessage, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    Assert.assertTrue(null != exc);
  }

  @Test
  public void testUpdateTanentPreferenceFailureWithInvalidOrgId() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = new Request();
    List<Map<String, Object>> reqList = new ArrayList<>();

    actorMessage.getRequest().put(JsonKey.TENANT_PREFERENCE, reqList);
    actorMessage.getRequest().put(JsonKey.ROOT_ORG_ID, "");
    actorMessage.getRequest().put(JsonKey.REQUESTED_BY, USER_ID);
    actorMessage.setOperation(ActorOperations.UPDATE_TENANT_PREFERENCE.getValue());

    subject.tell(actorMessage, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    Assert.assertTrue(null != exc);
  }

  @Test
  public void testGetTanentPreferenceSuccessWithoutKey() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = new Request();

    actorMessage.getRequest().put(JsonKey.ROOT_ORG_ID, orgId);
    actorMessage.getRequest().put(JsonKey.REQUESTED_BY, USER_ID);
    actorMessage.setOperation(ActorOperations.GET_TENANT_PREFERENCE.getValue());

    subject.tell(actorMessage, probe.getRef());
    Response res = probe.expectMsgClass(duration("10 second"), Response.class);
    Assert.assertTrue(null != res);
  }

  @Test
  public void testGetTanentPreferenceSuccessWithKeysDiff() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = new Request();

    actorMessage.getRequest().put(JsonKey.ROOT_ORG_ID, orgId);
    actorMessage.getRequest().put(JsonKey.KEYS, Arrays.asList("anyKey"));
    actorMessage.getRequest().put(JsonKey.REQUESTED_BY, USER_ID);
    actorMessage.setOperation(ActorOperations.GET_TENANT_PREFERENCE.getValue());

    subject.tell(actorMessage, probe.getRef());
    Response res = probe.expectMsgClass(duration("10 second"), Response.class);
    Assert.assertTrue(null != res);
  }

  @Test
  public void testGetTanentPreferenceWithInvalidOrgId() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = new Request();

    actorMessage.getRequest().put(JsonKey.ROOT_ORG_ID, "");
    actorMessage.getRequest().put(JsonKey.REQUESTED_BY, USER_ID);
    actorMessage.setOperation(ActorOperations.GET_TENANT_PREFERENCE.getValue());

    subject.tell(actorMessage, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    Assert.assertTrue(null != exc);
  }

  @Test
  public void testWithInvalidOperationType() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = new Request();

    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.TERM_AND_CONDITION_STATUS, "ACCEPTED");

    actorMessage.getRequest().put(JsonKey.TENANT_PREFERENCE, map);
    actorMessage.getRequest().put(JsonKey.ROOT_ORG_ID, orgId);
    actorMessage.getRequest().put(JsonKey.REQUESTED_BY, USER_ID);
    actorMessage.setOperation("InvalidOperation");

    subject.tell(actorMessage, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    Assert.assertTrue(null != exc);
  }

  private static Response cassandraGetRecordByProperty() {
    Response response = new Response();
    List list = new ArrayList();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.KEY, "anyKey");
    list.add(map);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }
}
