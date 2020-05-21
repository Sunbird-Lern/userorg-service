package org.sunbird.systemsettings.actors;

import static akka.testkit.JavaTestKit.duration;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.ElasticSearchTcpImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import scala.concurrent.duration.FiniteDuration;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ElasticSearchTcpImpl.class,
  CassandraOperationImpl.class,
  ServiceFactory.class,
  EsClientFactory.class
})
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
public class SystemSettingsActorTest {
  private static final FiniteDuration ACTOR_MAX_WAIT_DURATION = duration("10 second");
  private ActorSystem system;
  private Props props;
  private TestKit probe;
  private ActorRef subject;
  private Request actorMessage;
  private CassandraOperation cassandraOperation;
  private static String ROOT_ORG_ID = "defaultRootOrgId";
  private static String FIELD = "someField";
  private static String VALUE = "someValue";
  private ElasticSearchTcpImpl esUtil;
  private static final String KEYSPACE_NAME = JsonKey.SUNBIRD;
  private static final String TABLE_NAME = JsonKey.SYSTEM_SETTINGS_DB;

  @Before
  public void setUp() {
    system = ActorSystem.create("system");
    probe = new TestKit(system);
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = PowerMockito.mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    props = Props.create(SystemSettingsActor.class);
    subject = system.actorOf(props);
    actorMessage = new Request();
    PowerMockito.mockStatic(EsClientFactory.class);
    esUtil = PowerMockito.mock(ElasticSearchTcpImpl.class);
    Response resp = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    list.add(getOrgData());
    resp.put(JsonKey.RESPONSE, list);
    when(cassandraOperation.getRecordsByIndexedProperty(
            KEYSPACE_NAME, TABLE_NAME, JsonKey.FIELD, ROOT_ORG_ID))
        .thenReturn(resp);
    when(cassandraOperation.getRecordsByIndexedProperty(
            KEYSPACE_NAME, TABLE_NAME, JsonKey.FIELD, KEYSPACE_NAME))
        .thenReturn(new Response());
  }

  private Map<String, Object> getOrgData() {
    Map<String, Object> orgData = new HashMap<String, Object>();
    orgData.put(JsonKey.FIELD, ROOT_ORG_ID);
    orgData.put(JsonKey.ID, ROOT_ORG_ID);
    orgData.put(JsonKey.VALUE, VALUE);
    return orgData;
  }

  @Test
  public void testSetSystemSettingSuccess() {
    when(cassandraOperation.upsertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(new Response());
    actorMessage.setOperation(ActorOperations.SET_SYSTEM_SETTING.getValue());
    actorMessage.getRequest().putAll(getSystemSettingMap());
    subject.tell(actorMessage, probe.getRef());
    Response response = probe.expectMsgAnyClassOf(ACTOR_MAX_WAIT_DURATION, Response.class);
    Assert.assertTrue(null != response && response.getResponseCode() == ResponseCode.OK);
  }

  @Test
  public void testGetSystemSettingSuccess() {
    actorMessage.setOperation(ActorOperations.GET_SYSTEM_SETTING.getValue());
    actorMessage.setContext(getOrgData());
    subject.tell(actorMessage, probe.getRef());
    Response response = probe.expectMsgAnyClassOf(ACTOR_MAX_WAIT_DURATION, Response.class);
    Assert.assertTrue(null != response && response.getResponseCode() == ResponseCode.OK);
  }

  @Test
  public void testGetSystemSettingFailure() {
    //  when(cassandraOperation.getRecordsByIndexedProperty(Mockito.anyString(),
    // Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(new Response());
    Map<String, Object> orgData = new HashMap<String, Object>();
    orgData.put(JsonKey.FIELD, KEYSPACE_NAME);
    actorMessage.setOperation(ActorOperations.GET_SYSTEM_SETTING.getValue());
    actorMessage.setContext(orgData);
    subject.tell(actorMessage, probe.getRef());
    ProjectCommonException exception =
        probe.expectMsgAnyClassOf(ACTOR_MAX_WAIT_DURATION, ProjectCommonException.class);
    Assert.assertTrue(
        null != exception
            && exception.getResponseCode() == ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
  }

  @Test
  public void testGetAllSystemSettingsSuccess() {
    when(cassandraOperation.getAllRecords(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(getSystemSettingResponse());
    actorMessage.setOperation(ActorOperations.GET_ALL_SYSTEM_SETTINGS.getValue());
    subject.tell(actorMessage, probe.getRef());
    Response response = probe.expectMsgAnyClassOf(ACTOR_MAX_WAIT_DURATION, Response.class);
    Assert.assertTrue(null != response && response.getResponseCode() == ResponseCode.OK);
  }

  @Test
  public void testGetAllSystemSettingsSuccessWithEmptyResponse() {
    when(cassandraOperation.getAllRecords(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(getSystemSettingEmptyResponse());
    actorMessage.setOperation(ActorOperations.GET_ALL_SYSTEM_SETTINGS.getValue());
    subject.tell(actorMessage, probe.getRef());
    Response response = probe.expectMsgAnyClassOf(ACTOR_MAX_WAIT_DURATION, Response.class);
    Assert.assertTrue(null != response && response.getResponseCode() == ResponseCode.OK);
  }

  private Map<String, Object> getSystemSettingMap() {
    Map<String, Object> orgData = new HashMap<String, Object>();
    orgData.put(JsonKey.ID, ROOT_ORG_ID);
    orgData.put(JsonKey.FIELD, FIELD);
    orgData.put(JsonKey.VALUE, VALUE);
    return orgData;
  }

  private Response getSystemSettingResponse() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    list.add(getSystemSettingMap());
    response.put(JsonKey.RESPONSE, list);
    return response;
  }

  private Response getSystemSettingEmptyResponse() {
    Response response = new Response();
    response.put(
        JsonKey.RESPONSE,
        new ArrayList<Map<String, Object>>(Arrays.asList(new HashMap<String, Object>())));
    return response;
  }
}
