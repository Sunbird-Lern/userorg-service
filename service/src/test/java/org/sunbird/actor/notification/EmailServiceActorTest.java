package org.sunbird.actor.notification;

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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.actor.BackgroundOperations;
import org.sunbird.actor.service.SunbirdMWService;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.notification.NotificationService;
import org.sunbird.service.organisation.OrgService;
import org.sunbird.service.organisation.impl.OrgServiceImpl;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.UserServiceImpl;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  CassandraOperationImpl.class,
  NotificationService.class,
  SunbirdMWService.class,
  UserService.class,
  UserServiceImpl.class,
  OrgService.class,
  OrgServiceImpl.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.net.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*",
  "javax.script.*",
  "javax.xml.*",
  "com.sun.org.apache.xerces.*",
  "org.xml.*"
})
public class EmailServiceActorTest {

  private TestKit probe;
  private ActorRef subject;

  private static final ActorSystem system = ActorSystem.create("system");
  private static final CassandraOperationImpl mockCassandraOperation =
    mock(CassandraOperationImpl.class);
  private Props props = Props.create(EmailServiceActor.class);
  private NotificationService notificationService = null;

  @Before
  public void beforeTest() throws Exception {
    PowerMockito.mockStatic(ServiceFactory.class);
    when(ServiceFactory.getInstance()).thenReturn(mockCassandraOperation);
    probe = new TestKit(system);
    subject = system.actorOf(props);
    PowerMockito.mockStatic(SunbirdMWService.class);
    SunbirdMWService.tellToBGRouter(Mockito.any(), Mockito.any());
    notificationService = PowerMockito.mock(NotificationService.class);
    PowerMockito.whenNew(NotificationService.class).withNoArguments().thenReturn(notificationService);
    PowerMockito.when(notificationService.processSMS(Mockito.anyList(),Mockito.anyList(),Mockito.anyString(),Mockito.any(RequestContext.class))).thenReturn(true);

  }

  private Response getMockCassandraRecordByIdSuccessResponse() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> userResponse = new HashMap<>();
    userResponse.put(JsonKey.ID, "465-546565-731");
    userResponse.put(JsonKey.USER_ID, "465-546565-731");
    userResponse.put(JsonKey.FIRST_NAME, "FirstName");
    userResponse.put(JsonKey.ROOT_ORG_ID,"4845454684684");
    userResponse.put(JsonKey.PHONE,"9999999999");
    list.add(userResponse);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }


  //@Test
  public void testSendEmailSuccess() throws Exception {
    when(mockCassandraOperation.getPropertiesValueById(
      Mockito.anyString(), Mockito.anyString(), Mockito.anyList(),Mockito.anyList(), Mockito.any(RequestContext.class)))
      .thenReturn(getMockCassandraRecordByIdSuccessResponse());

    Request reqObj = new Request();
    reqObj.setOperation(BackgroundOperations.emailService.name());

    HashMap<String, Object> innerMap = new HashMap<>();
    Map<String, Object> reqMap = new HashMap<>();
    List<String> emailIdList = new ArrayList<>();
    emailIdList.add("aaa@gmail.com");
    List<String> userIdList = new ArrayList<>();
    userIdList.add("001");
    Map<String, Object> queryMap = new HashMap<>();
    Map<String, Object> filterMap = new HashMap<>();
    filterMap.put(JsonKey.NAME, "anyName");
    queryMap.put(JsonKey.FILTERS, filterMap);
    reqMap.put(JsonKey.RECIPIENT_EMAILS, emailIdList);
    reqMap.put(JsonKey.RECIPIENT_SEARCH_QUERY, queryMap);
    reqMap.put(JsonKey.EMAIL_TEMPLATE_TYPE, "default");
    reqMap.put(JsonKey.RECIPIENT_USERIDS, userIdList);
    reqMap.put(JsonKey.RECIPIENT_SEARCH_QUERY, queryMap);
    List<String> phoneList = new ArrayList<>();
    phoneList.add("9999999999");
    reqMap.put(JsonKey.RECIPIENT_PHONES, phoneList);
    reqMap.put(JsonKey.MODE,"sms");
    innerMap.put(JsonKey.EMAIL_REQUEST, reqMap);

    reqObj.setRequest(innerMap);
    subject.tell(reqObj, probe.getRef());
    Response response = probe.expectMsgClass(duration("10000 second"), Response.class);
    assertTrue(response != null);
  }

  //@Test
  public void testEmailSuccess() {
    Request reqObj = new Request();
    reqObj.setOperation(BackgroundOperations.emailService.name());

    Map<String, Object> innerMap = new HashMap<>();
    Map<String, Object> pageMap = new HashMap<String, Object>();
    List<String> userIdList = new ArrayList<>();
    userIdList.add("001");
    innerMap.put(JsonKey.EMAIL_REQUEST, pageMap);
    pageMap.put(JsonKey.RECIPIENT_USERIDS, userIdList);
    pageMap.put(JsonKey.FIRST_NAME, "Name");
    pageMap.put(JsonKey.EMAIL_TEMPLATE_TYPE, "default");
    reqObj.setRequest(innerMap);
    subject.tell(reqObj, probe.getRef());
    Response response = probe.expectMsgClass(duration("10 second"), Response.class);
    assertTrue(response != null);
  }

  @Test
  public void testSendEmailFailureWithInvalidParameterValue() {
    Request reqObj = new Request();
    reqObj.setOperation(BackgroundOperations.emailService.name());

    HashMap<String, Object> innerMap = new HashMap<>();
    Map<String, Object> pageMap = new HashMap<String, Object>();
    List<String> emailIdList = new ArrayList<>();
    emailIdList.add("aaa");
    List<String> userIdList = new ArrayList<>();
    userIdList.add("001");
    Map<String, Object> queryMap = new HashMap<>();
    Map<String, Object> filterMap = new HashMap<>();
    queryMap.put(JsonKey.FILTERS, filterMap);
    pageMap.put(JsonKey.RECIPIENT_EMAILS, emailIdList);
    pageMap.put(JsonKey.RECIPIENT_SEARCH_QUERY, queryMap);
    innerMap.put(JsonKey.EMAIL_REQUEST, pageMap);
    innerMap.put(JsonKey.RECIPIENT_USERIDS, userIdList);
    innerMap.put(JsonKey.RECIPIENT_SEARCH_QUERY, queryMap);
    reqObj.setRequest(innerMap);
    subject.tell(reqObj, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    assertTrue(exc.getCode().equals(ResponseCode.invalidParameterValue.getErrorCode()));
  }

  @Test
  public void testSendEmailFailureWithEmptyFilters() {
    Request reqObj = new Request();
    reqObj.setOperation(BackgroundOperations.emailService.name());

    HashMap<String, Object> innerMap = new HashMap<>();
    Map<String, Object> pageMap = new HashMap<String, Object>();
    List<String> emailIdList = new ArrayList<>();
    emailIdList.add("aaa@gmail.com");
    List<String> userIdList = new ArrayList<>();
    userIdList.add("001");
    Map<String, Object> queryMap = new HashMap<>();
    Map<String, Object> filterMap = new HashMap<>();
    queryMap.put(JsonKey.FILTERS, filterMap);
    pageMap.put(JsonKey.RECIPIENT_EMAILS, emailIdList);
    pageMap.put(JsonKey.RECIPIENT_SEARCH_QUERY, queryMap);
    innerMap.put(JsonKey.EMAIL_REQUEST, pageMap);
    innerMap.put(JsonKey.RECIPIENT_USERIDS, userIdList);
    innerMap.put(JsonKey.RECIPIENT_SEARCH_QUERY, queryMap);
    reqObj.setRequest(innerMap);
    subject.tell(reqObj, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    assertTrue(exc.getCode().equals(ResponseCode.invalidParameterValue.getErrorCode()));
  }

  //@Test
  public void testSendEmailFailureWithEmailSizeExceeding() {
    Request reqObj = new Request();
    reqObj.setOperation(BackgroundOperations.emailService.name());

    HashMap<String, Object> innerMap = new HashMap<>();
    Map<String, Object> pageMap = new HashMap<String, Object>();
    List<String> emailIdList = new ArrayList<>();

    for (int i = 0; i < 40; i++) {
      emailIdList.add("aaa" + i + "@gmail.com");
      emailIdList.add("bbb" + i + "@gmail.com");
      emailIdList.add("ccc" + i + "@gmail.com");
    }
    List<String> userIdList = new ArrayList<>();
    userIdList.add("001");
    Map<String, Object> queryMap = new HashMap<>();
    Map<String, Object> filterMap = new HashMap<>();
    filterMap.put(JsonKey.NAME, "anyName");
    queryMap.put(JsonKey.FILTERS, filterMap);
    pageMap.put(JsonKey.RECIPIENT_EMAILS, emailIdList);
    pageMap.put(JsonKey.RECIPIENT_SEARCH_QUERY, queryMap);
    innerMap.put(JsonKey.EMAIL_REQUEST, pageMap);
    innerMap.put(JsonKey.RECIPIENT_USERIDS, userIdList);
    innerMap.put(JsonKey.RECIPIENT_SEARCH_QUERY, queryMap);
    reqObj.setRequest(innerMap);

    subject.tell(reqObj, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    assertTrue(
        exc.getCode().equals(ResponseCode.emailNotSentRecipientsExceededMaxLimit.getErrorCode()));
  }

  //@Test
  public void testSendEmailFailureWithBlankTemplateName() {
    Request reqObj = new Request();
    reqObj.setOperation(BackgroundOperations.emailService.name());

    HashMap<String, Object> innerMap = new HashMap<>();
    Map<String, Object> pageMap = new HashMap<String, Object>();
    List<String> emailIdList = new ArrayList<>();
    emailIdList.add("aaa@gmail.com");
    List<String> userIdList = new ArrayList<>();
    userIdList.add("001");
    Map<String, Object> queryMap = new HashMap<>();
    Map<String, Object> filterMap = new HashMap<>();
    filterMap.put(JsonKey.NAME, "anyName");
    queryMap.put(JsonKey.FILTERS, filterMap);
    pageMap.put(JsonKey.RECIPIENT_EMAILS, emailIdList);
    pageMap.put(JsonKey.RECIPIENT_SEARCH_QUERY, queryMap);
    innerMap.put(JsonKey.EMAIL_REQUEST, pageMap);
    innerMap.put(JsonKey.RECIPIENT_USERIDS, userIdList);
    innerMap.put(JsonKey.RECIPIENT_SEARCH_QUERY, queryMap);
    reqObj.setRequest(innerMap);
    //when(emailTemplateDao.getTemplate(Mockito.anyString(), Mockito.any())).thenReturn("");
    subject.tell(reqObj, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    assertTrue(exc.getCode().equals(ResponseCode.invalidParameterValue.getErrorCode()));
  }

  //@Test
  public void testSendEmailFailureWithInvalidUserIdInList() {
    Request reqObj = new Request();
    reqObj.setOperation(BackgroundOperations.emailService.name());

    HashMap<String, Object> innerMap = new HashMap<>();
    Map<String, Object> pageMap = new HashMap<String, Object>();
    List<String> emailIdList = new ArrayList<>();
    emailIdList.add("aaa@gmail.com");
    List<String> userIdList = new ArrayList<>();
    userIdList.add("001");
    userIdList.add("002");
    Map<String, Object> queryMap = new HashMap<>();
    Map<String, Object> filterMap = new HashMap<>();
    queryMap.put(JsonKey.FILTERS, filterMap);
    pageMap.put(JsonKey.RECIPIENT_EMAILS, emailIdList);

    Map<String, Object> searchQueryMap = new HashMap<>();
    Map<String, Object> userIdMap = new HashMap<>();
    userIdMap.put(JsonKey.RECIPIENT_USERIDS, userIdList);
    userIdMap.put(JsonKey.RECIPIENT_SEARCH_QUERY, searchQueryMap);
    innerMap.put(JsonKey.EMAIL_REQUEST, userIdMap);

    innerMap.put(JsonKey.RECIPIENT_SEARCH_QUERY, queryMap);
    reqObj.setRequest(innerMap);
    subject.tell(reqObj, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    assertTrue(exc.getCode().equals(ResponseCode.invalidParameterValue.getErrorCode()));
  }

  //@Test
  public void testSendEmailFailureWithElasticSearchException() {
    Request reqObj = new Request();
    reqObj.setOperation(BackgroundOperations.emailService.name());

    HashMap<String, Object> innerMap = new HashMap<>();
    Map<String, Object> pageMap = new HashMap<String, Object>();
    List<String> emailIdList = new ArrayList<>();
    emailIdList.add("aaa@gmail.com");
    List<String> userIdList = new ArrayList<>();
    userIdList.add("001");
    Map<String, Object> queryMap = new HashMap<>();
    Map<String, Object> filterMap = new HashMap<>();
    filterMap.put(JsonKey.NAME, "anyName");
    queryMap.put(JsonKey.FILTERS, filterMap);
    pageMap.put(JsonKey.RECIPIENT_EMAILS, emailIdList);
    pageMap.put(JsonKey.RECIPIENT_SEARCH_QUERY, queryMap);
    innerMap.put(JsonKey.EMAIL_REQUEST, pageMap);
    innerMap.put(JsonKey.RECIPIENT_USERIDS, userIdList);
    innerMap.put(JsonKey.RECIPIENT_SEARCH_QUERY, queryMap);
    reqObj.setRequest(innerMap);

    subject.tell(reqObj, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    assertTrue(exc.getCode().equals(ResponseCode.invalidParameterValue.getErrorCode()));
  }
}
