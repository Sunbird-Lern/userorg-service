package org.sunbird.learner.actors;

import static akka.testkit.JavaTestKit.duration;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.testkit.javadsl.TestKit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.actor.background.BackgroundOperations;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.datasecurity.impl.DefaultDecryptionServiceImpl;
import org.sunbird.common.models.util.datasecurity.impl.DefaultEncryptionServivceImpl;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.notificationservice.EmailServiceActor;
import org.sunbird.learner.actors.notificationservice.dao.impl.EmailTemplateDaoImpl;
import org.sunbird.learner.util.ContentSearchUtil;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.Util;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  Util.class,
  DataCacheHandler.class,
  PageManagementActor.class,
  ContentSearchUtil.class,
  org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class,
  ElasticSearchRestHighImpl.class,
  EmailTemplateDaoImpl.class,
  EsClientFactory.class,
  ElasticSearchHelper.class
})
@PowerMockIgnore({"javax.management.*"})
public class EmailServiceActorTest {

  private static final Props props = Props.create(EmailServiceActor.class);
  private ActorSystem system = ActorSystem.create("system");
  private static CassandraOperationImpl cassandraOperation;
  private static DefaultDecryptionServiceImpl defaultDecryptionService;
  private static DefaultEncryptionServivceImpl defaultEncryptionServivce;
  private static EmailTemplateDaoImpl emailTemplateDao;
  private ElasticSearchService esService;

  @BeforeClass
  public static void setUp() {

    PowerMockito.mockStatic(ServiceFactory.class);
    PowerMockito.mockStatic(EmailTemplateDaoImpl.class);
    PowerMockito.mockStatic(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    defaultDecryptionService = mock(DefaultDecryptionServiceImpl.class);
    defaultEncryptionServivce = mock(DefaultEncryptionServivceImpl.class);
    emailTemplateDao = mock(EmailTemplateDaoImpl.class);
  }

  @Before
  public void beforeTest() {

    PowerMockito.mockStatic(ServiceFactory.class);
    PowerMockito.mockStatic(ElasticSearchHelper.class);
    esService = mock(ElasticSearchRestHighImpl.class);
    PowerMockito.mockStatic(EsClientFactory.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
    PowerMockito.mockStatic(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class);
    PowerMockito.mockStatic(EmailTemplateDaoImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory
            .getDecryptionServiceInstance(null))
        .thenReturn(defaultDecryptionService);
    when(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory
            .getEncryptionServiceInstance(null))
        .thenReturn(defaultEncryptionServivce);
    when(cassandraOperation.getRecordsByIdsWithSpecifiedColumns(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyList(), Mockito.anyList()))
        .thenReturn(cassandraGetRecordById());

    emailTemplateDao = mock(EmailTemplateDaoImpl.class);
    when(EmailTemplateDaoImpl.getInstance()).thenReturn(emailTemplateDao);
    when(emailTemplateDao.getTemplate(Mockito.anyString())).thenReturn("templateName");

    Map<String, Object> recipientSearchQuery = new HashMap<>();
    recipientSearchQuery.put(JsonKey.FILTERS, "anyName");
    recipientSearchQuery.put(JsonKey.ROOT_ORG_ID, "anyRootId");
    Map<String, Object> esOrgResult = new HashMap<>();
    esOrgResult.put(JsonKey.ORGANISATION_NAME, "anyOrgName");
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(createGetSkillResponse());
    when(esService.search(
            Mockito.eq(ElasticSearchHelper.createSearchDTO(recipientSearchQuery)),
            Mockito.eq(ProjectUtil.EsType.user.getTypeName())))
        .thenReturn(promise.future());
    Promise<Map<String, Object>> promise_recipientSearchQuery = Futures.promise();

    promise_recipientSearchQuery.trySuccess(recipientSearchQuery);
    when(esService.getDataByIdentifier(
            Mockito.eq(ProjectUtil.EsType.user.getTypeName()), Mockito.eq("001")))
        .thenReturn(promise_recipientSearchQuery.future());

    Promise<Map<String, Object>> promise_esOrgResult = Futures.promise();
    promise_esOrgResult.trySuccess(esOrgResult);
    when(esService.getDataByIdentifier(
            Mockito.eq(ProjectUtil.EsType.organisation.getTypeName()), Mockito.eq("anyRootId")))
        .thenReturn(promise_esOrgResult.future());
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

  private static Map<String, Object> createGetSkillResponse() {
    HashMap<String, Object> response = new HashMap<>();
    List<Map<String, Object>> content = new ArrayList<>();
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.EMAIL, "anyEmailId");
    content.add(innerMap);
    response.put(JsonKey.CONTENT, content);
    return response;
  }

  @Test
  public void testSendEmailSuccess() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
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
    Response response = probe.expectMsgClass(duration("10 second"), Response.class);
    assertTrue(response != null);
  }

  @Test
  public void testSendEmailFailureWithInvalidParameterValue() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
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

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
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

  @Test
  public void testSendEmailFailureWithEmailSizeExceeding() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
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
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(createGetSkillResponse());

    when(esService.search(
            Mockito.eq(ElasticSearchHelper.createSearchDTO(new HashMap<>())),
            Mockito.eq(ProjectUtil.EsType.user.getTypeName())))
        .thenReturn(promise.future());

    subject.tell(reqObj, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    assertTrue(
        exc.getCode().equals(ResponseCode.emailNotSentRecipientsExceededMaxLimit.getErrorCode()));
  }

  @Test
  public void testSendEmailFailureWithBlankTemplateName() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
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
    when(emailTemplateDao.getTemplate(Mockito.anyString())).thenReturn("");
    subject.tell(reqObj, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    assertTrue(exc.getCode().equals(ResponseCode.invalidParameterValue.getErrorCode()));
  }

  @Test
  public void testSendEmailFailureWithInvalidUserIdInList() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
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

  @Test
  public void testSendEmailFailureWithElasticSearchException() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
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
    when(esService.search(
            Mockito.eq(ElasticSearchHelper.createSearchDTO(new HashMap<>())),
            Mockito.eq(ProjectUtil.EsType.user.getTypeName())))
        .thenThrow(new ProjectCommonException("", "", 0));
    when(ElasticSearchHelper.getResponseFromFuture(Mockito.any()))
        .thenThrow(new ProjectCommonException("", "", 0));

    subject.tell(reqObj, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    assertTrue(exc.getCode().equals(ResponseCode.invalidParameterValue.getErrorCode()));
  }
}
