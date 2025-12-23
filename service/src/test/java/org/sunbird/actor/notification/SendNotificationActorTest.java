package org.sunbird.actor.notification;

import java.time.Duration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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
import org.apache.velocity.VelocityContext;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import org.sunbird.util.DataCacheHandler;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  Util.class,
  DataCacheHandler.class,
  HttpClientUtil.class,
  ProjectUtil.class,
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class SendNotificationActorTest {

  private static final Props props = Props.create(SendNotificationActor.class);
  private ActorSystem system = ActorSystem.create("system");
  private static CassandraOperationImpl cassandraOperation;

  @InjectMocks
  ActorRef backGroundNotificationActor =
      system.actorOf(Props.create(BackGroundNotificationActor.class));

  @BeforeClass
  public static void setUp() {
    PowerMockito.mockStatic(ProjectUtil.class);
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
  }

  @Before
  public void beforeTest() {
    // backGroundNotificationActor =
    // system.actorOf(Props.create(BackGroundNotificationActor.class));
    PowerMockito.mockStatic(ProjectUtil.class);
    PowerMockito.mockStatic(ServiceFactory.class);
    PowerMockito.mockStatic(HttpClientUtil.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(cassandraOperation.getRecordsByIdsWithSpecifiedColumns(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyList(),
            Mockito.anyList(),
            Mockito.any()))
        .thenReturn(cassandraGetRecordById());
    when(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE)).thenReturn("sunbird");
  }

  private static Response cassandraGetRecordById() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ID, "anyId");
    map.put(JsonKey.EMAIL, "xyz@xyz.com");
    map.put(JsonKey.PHONE, "9999999999");
    map.put("template", "some template Id");
    list.add(map);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }

  private static Response cassandraGetEmptyRecordById() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ID, "anyId");
    map.put(JsonKey.EMAIL, "");
    list.add(map);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }

  @Test
  public void testSendEmailSuccess() {
    when(cassandraOperation.getPropertiesValueById(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyList(),
            Mockito.anyList(),
            Mockito.any()))
        .thenReturn(cassandraGetRecordById());
    when(cassandraOperation.getRecordsByPrimaryKeys(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyList(),
            Mockito.anyString(),
            Mockito.any()))
        .thenReturn(cassandraGetRecordById());
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.V2_NOTIFICATION.getValue());
    VelocityContext context = PowerMockito.mock(VelocityContext.class);
    when(ProjectUtil.getContext(Mockito.anyMap())).thenReturn(context);
    Object[] arr = new Object[1];
    arr[0] = "name";
    when(context.getKeys()).thenReturn(arr);
    HashMap<String, Object> innerMap = new HashMap<>();
    Map<String, Object> reqMap = new HashMap<String, Object>();
    List<String> userIdList = new ArrayList<>();
    userIdList.add("001");
    reqMap.put(JsonKey.RECIPIENT_USERIDS, userIdList);
    innerMap.put(JsonKey.EMAIL_REQUEST, reqMap);
    reqMap.put(JsonKey.EMAIL_TEMPLATE_TYPE, "default");
    reqObj.getRequest().put(JsonKey.EMAIL_REQUEST, reqMap);
    subject.tell(reqObj, probe.getRef());
    Response response = probe.expectMsgClass(Duration.ofSeconds(1000), Response.class);
    assertTrue(response != null);
  }

  @Test
  @Ignore
  public void testSendEmailFailureWithInvalidParameterValue() {
    when(cassandraOperation.getPropertiesValueById(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyList(),
            Mockito.anyList(),
            Mockito.any()))
        .thenReturn(cassandraGetRecordById());
    when(cassandraOperation.getRecordsByPrimaryKeys(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyList(),
            Mockito.anyString(),
            Mockito.any()))
        .thenReturn(cassandraGetEmptyRecordById());
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.V2_NOTIFICATION.getValue());
    VelocityContext context = PowerMockito.mock(VelocityContext.class);
    when(ProjectUtil.getContext(Mockito.anyMap())).thenReturn(context);
    Object[] arr = new Object[1];
    arr[0] = "name";
    when(context.getKeys()).thenReturn(arr);
    HashMap<String, Object> innerMap = new HashMap<>();
    Map<String, Object> reqMap = new HashMap<String, Object>();
    List<String> userIdList = new ArrayList<>();
    userIdList.add("001");
    reqMap.put(JsonKey.RECIPIENT_USERIDS, userIdList);
    innerMap.put(JsonKey.EMAIL_REQUEST, reqMap);
    reqObj.setRequest(innerMap);

    subject.tell(reqObj, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(Duration.ofSeconds(10), ProjectCommonException.class);
    assertTrue(exc.getErrorCode().equals(ResponseCode.invalidParameterValue.getErrorCode()));
  }

  @Test
  public void testSendEmailFailureWithInvalidUserIdInList() {
    when(cassandraOperation.getPropertiesValueById(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyList(),
            Mockito.anyList(),
            Mockito.any()))
        .thenReturn(cassandraGetEmptyRecordById());
    when(cassandraOperation.getRecordsByPrimaryKeys(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyList(),
            Mockito.anyString(),
            Mockito.any()))
        .thenReturn(cassandraGetRecordById());
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.V2_NOTIFICATION.getValue());
    VelocityContext context = PowerMockito.mock(VelocityContext.class);
    when(ProjectUtil.getContext(Mockito.anyMap())).thenReturn(context);
    Object[] arr = new Object[1];
    arr[0] = "name";
    when(context.getKeys()).thenReturn(arr);
    HashMap<String, Object> innerMap = new HashMap<>();
    List<String> emailIdList = new ArrayList<>();
    emailIdList.add("aaa@gmail.com");
    List<String> userIdList = new ArrayList<>();
    userIdList.add("001");
    userIdList.add("002");
    Map<String, Object> userIdMap = new HashMap<>();
    userIdMap.put(JsonKey.RECIPIENT_USERIDS, userIdList);
    innerMap.put(JsonKey.EMAIL_REQUEST, userIdMap);

    reqObj.setRequest(innerMap);
    subject.tell(reqObj, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(Duration.ofSeconds(10), ProjectCommonException.class);
    assertEquals(
        "UOS_NOTI" + ResponseCode.invalidParameterValue.getErrorCode(), exc.getErrorCode());
  }

  @Test
  public void testSendSMSSuccess() {
    when(cassandraOperation.getPropertiesValueById(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyList(),
            Mockito.anyList(),
            Mockito.any()))
        .thenReturn(cassandraGetRecordById());

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.V2_NOTIFICATION.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    Map<String, Object> reqMap = new HashMap<String, Object>();
    List<String> userIdList = new ArrayList<>();
    userIdList.add("001");
    reqMap.put(JsonKey.RECIPIENT_USERIDS, userIdList);
    reqMap.put(JsonKey.MODE, JsonKey.SMS);
    innerMap.put(JsonKey.EMAIL_REQUEST, reqMap);
    reqMap.put(JsonKey.EMAIL_TEMPLATE_TYPE, "default");
    reqObj.getRequest().put(JsonKey.EMAIL_REQUEST, reqMap);
    subject.tell(reqObj, probe.getRef());
    Response response = probe.expectMsgClass(Duration.ofSeconds(1000), Response.class);
    assertNotNull(response);
  }

  @Test
  public void testWithInvalidRequest() {
    Request request = new Request();
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    request.setOperation("invalidOperation");
    subject.tell(request, probe.getRef());
    ProjectCommonException exception =
        probe.expectMsgClass(Duration.ofSeconds(10), ProjectCommonException.class);
    Assert.assertNotNull(exception);
  }
}
