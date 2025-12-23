package org.sunbird.actor.systemsettings;

import static org.powermock.api.mockito.PowerMockito.when;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.testkit.javadsl.TestKit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import org.sunbird.util.ProjectUtil;
import scala.concurrent.duration.FiniteDuration;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ElasticSearchRestHighImpl.class,
  CassandraOperationImpl.class,
  ServiceFactory.class,
  EsClientFactory.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class SystemSettingsActorTest {
  private static final FiniteDuration ACTOR_MAX_WAIT_DURATION = FiniteDuration.apply(100, TimeUnit.SECONDS);
  private ActorSystem system;
  private Props props;
  private TestKit probe;
  private ActorRef subject;
  private Request actorMessage;
  private CassandraOperation cassandraOperation;
  private static String ROOT_ORG_ID = "defaultRootOrgId";
  private static String FIELD = "someField";
  private static String VALUE = "someValue";
  private ElasticSearchRestHighImpl esUtil;
  private static final String KEYSPACE_NAME = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE);
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
    esUtil = PowerMockito.mock(ElasticSearchRestHighImpl.class);
    Response resp = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    list.add(getOrgData());
    resp.put(JsonKey.RESPONSE, list);
    when(cassandraOperation.getRecordById(KEYSPACE_NAME, TABLE_NAME, ROOT_ORG_ID, null))
        .thenReturn(resp);
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
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
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
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(new Response());
    Map<String, Object> orgData = new HashMap<String, Object>();
    orgData.put(JsonKey.FIELD, KEYSPACE_NAME);
    actorMessage.setOperation(ActorOperations.GET_SYSTEM_SETTING.getValue());
    actorMessage.setContext(orgData);
    subject.tell(actorMessage, probe.getRef());
    ProjectCommonException exception =
        probe.expectMsgAnyClassOf(ACTOR_MAX_WAIT_DURATION, ProjectCommonException.class);
    Assert.assertTrue(
        null != exception
            && exception
                .getErrorCode()
                .equals("UOS_SYSRED" + ResponseCode.resourceNotFound.getErrorCode()));
  }

  @Test
  public void testGetAllSystemSettingsSuccess() {
    when(cassandraOperation.getAllRecords(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(getSystemSettingResponse());
    actorMessage.setOperation(ActorOperations.GET_ALL_SYSTEM_SETTINGS.getValue());
    subject.tell(actorMessage, probe.getRef());
    Response response = probe.expectMsgAnyClassOf(ACTOR_MAX_WAIT_DURATION, Response.class);
    Assert.assertTrue(null != response && response.getResponseCode() == ResponseCode.OK);
  }

  @Test
  public void testGetAllSystemSettingsSuccessWithEmptyResponse() {
    when(cassandraOperation.getAllRecords(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(getSystemSettingEmptyResponse());
    actorMessage.setOperation(ActorOperations.GET_ALL_SYSTEM_SETTINGS.getValue());
    subject.tell(actorMessage, probe.getRef());
    Response response = probe.expectMsgAnyClassOf(ACTOR_MAX_WAIT_DURATION, Response.class);
    Assert.assertTrue(null != response && response.getResponseCode() == ResponseCode.OK);
  }

  @Test
  public void testWithInvalidRequest() {
    Request request = new Request();
    request.setOperation("invalidOperation");
    subject.tell(request, probe.getRef());
    ProjectCommonException exception =
        probe.expectMsgClass(java.time.Duration.ofSeconds(10), ProjectCommonException.class);
    Assert.assertNotNull(exception);
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
