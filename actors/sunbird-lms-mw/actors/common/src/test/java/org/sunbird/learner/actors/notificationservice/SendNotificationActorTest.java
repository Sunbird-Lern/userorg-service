package org.sunbird.learner.actors.notificationservice;

import static akka.testkit.JavaTestKit.duration;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.velocity.VelocityContext;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.actor.service.BaseMWService;
import org.sunbird.actor.service.SunbirdMWService;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.datasecurity.impl.DefaultDecryptionServiceImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.keys.JsonKey;
import org.sunbird.learner.actors.notificationservice.dao.impl.EmailTemplateDaoImpl;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.Util;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import org.sunbird.util.ProjectUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  Util.class,
  DataCacheHandler.class,
  org.sunbird.datasecurity.impl.ServiceFactory.class,
  EmailTemplateDaoImpl.class,
  SunbirdMWService.class,
  HttpClientUtil.class,
  ProjectUtil.class,
  BaseMWService.class
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
  private static DefaultDecryptionServiceImpl defaultDecryptionService;
  private static EmailTemplateDaoImpl emailTemplateDao;
  private ElasticSearchService esService;

  @BeforeClass
  public static void setUp() {
    PowerMockito.mockStatic(SunbirdMWService.class);
    SunbirdMWService.tellToBGRouter(Mockito.any(), Mockito.any());
    PowerMockito.mockStatic(ProjectUtil.class);
    PowerMockito.mockStatic(ServiceFactory.class);
    PowerMockito.mockStatic(EmailTemplateDaoImpl.class);
    PowerMockito.mockStatic(org.sunbird.datasecurity.impl.ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    defaultDecryptionService = mock(DefaultDecryptionServiceImpl.class);
    emailTemplateDao = mock(EmailTemplateDaoImpl.class);
  }

  @Before
  public void beforeTest() {
    PowerMockito.mockStatic(SunbirdMWService.class);
    SunbirdMWService.tellToBGRouter(Mockito.any(), Mockito.any());
    PowerMockito.mockStatic(ProjectUtil.class);
    PowerMockito.mockStatic(ServiceFactory.class);
    PowerMockito.mockStatic(HttpClientUtil.class);
    PowerMockito.mockStatic(org.sunbird.datasecurity.impl.ServiceFactory.class);
    PowerMockito.mockStatic(EmailTemplateDaoImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(org.sunbird.datasecurity.impl.ServiceFactory.getDecryptionServiceInstance(null))
        .thenReturn(defaultDecryptionService);
    when(cassandraOperation.getRecordsByIdsWithSpecifiedColumns(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyList(),
            Mockito.anyList(),
            Mockito.any()))
        .thenReturn(cassandraGetRecordById());

    emailTemplateDao = mock(EmailTemplateDaoImpl.class);
    when(EmailTemplateDaoImpl.getInstance()).thenReturn(emailTemplateDao);
    when(emailTemplateDao.getTemplate(Mockito.anyString(), Mockito.any()))
        .thenReturn("templateName");
  }

  private static Response cassandraGetRecordById() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ID, "anyId");
    map.put(JsonKey.EMAIL, "anyEmailId");
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
    PowerMockito.mockStatic(SunbirdMWService.class);
    SunbirdMWService.tellToBGRouter(Mockito.any(), Mockito.any());
    PowerMockito.mockStatic(BaseMWService.class);
    BaseMWService.getRemoteRouter(Mockito.anyString());
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
    Response response = probe.expectMsgClass(duration("1000 second"), Response.class);
    assertTrue(response != null);
  }

  @Test
  public void testSendEmailFailureWithInvalidParameterValue() {
    PowerMockito.mockStatic(SunbirdMWService.class);
    SunbirdMWService.tellToBGRouter(Mockito.any(), Mockito.any());
    PowerMockito.mockStatic(BaseMWService.class);
    when(cassandraOperation.getRecordsByIdsWithSpecifiedColumns(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyList(),
            Mockito.anyList(),
            Mockito.any()))
        .thenReturn(cassandraGetEmptyRecordById());
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.V2_NOTIFICATION.getValue());

    HashMap<String, Object> innerMap = new HashMap<>();
    Map<String, Object> reqMap = new HashMap<String, Object>();
    List<String> userIdList = new ArrayList<>();
    userIdList.add("001");
    reqMap.put(JsonKey.RECIPIENT_USERIDS, userIdList);
    innerMap.put(JsonKey.EMAIL_REQUEST, reqMap);
    reqObj.setRequest(innerMap);

    subject.tell(reqObj, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    assertTrue(exc.getCode().equals(ResponseCode.notificationNotSent.getErrorCode()));
  }

  @Test
  public void testSendEmailFailureWithBlankTemplateName() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.V2_NOTIFICATION.getValue());

    HashMap<String, Object> innerMap = new HashMap<>();
    Map<String, Object> reqMap = new HashMap<String, Object>();
    List<String> userIdList = new ArrayList<>();
    userIdList.add("001");
    reqMap.put(JsonKey.RECIPIENT_USERIDS, userIdList);
    innerMap.put(JsonKey.EMAIL_REQUEST, reqMap);
    reqObj.setRequest(innerMap);

    when(emailTemplateDao.getTemplate(Mockito.anyString(), Mockito.any())).thenReturn("");
    subject.tell(reqObj, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    assertTrue(exc.getCode().equals(ResponseCode.invalidParameterValue.getErrorCode()));
  }

  @Test
  public void testSendEmailFailureWithInvalidUserIdInList() {
    PowerMockito.mockStatic(SunbirdMWService.class);
    SunbirdMWService.tellToBGRouter(Mockito.any(), Mockito.any());
    when(cassandraOperation.getRecordsByIdsWithSpecifiedColumns(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyList(),
            Mockito.anyList(),
            Mockito.any()))
        .thenReturn(cassandraGetEmptyRecordById());
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.V2_NOTIFICATION.getValue());

    HashMap<String, Object> innerMap = new HashMap<>();
    Map<String, Object> pageMap = new HashMap<String, Object>();
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
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    assertTrue(exc.getCode().equals(ResponseCode.notificationNotSent.getErrorCode()));
  }
}
