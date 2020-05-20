package org.sunbird.learner.actors.skill;

import static akka.testkit.JavaTestKit.duration;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.testkit.javadsl.TestKit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import scala.concurrent.Promise;
import scala.concurrent.duration.FiniteDuration;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  EsClientFactory.class,
  ElasticSearchHelper.class,
  ElasticSearchRestHighImpl.class
})
@PowerMockIgnore("javax.management.*")
public class UserSkillManagementActorTest {

  private static ActorSystem system;
  private static final Props props = Props.create(UserSkillManagementActor.class);
  private CassandraOperation cassandraOperation;
  private static final String USER_ID = "userId";
  private static final String ROOT_ORG_ID = "someRootOrgId";
  private static final String ENDORSED_USER_ID = "someEndorsedUserId";
  private static final String ENDORSED_SKILL_NAME = "someEndorsedSkillName";
  private FiniteDuration duration = duration("10 second");
  private ElasticSearchService esService;

  @Before
  public void beforeEachTest() {
    system = ActorSystem.create("system");
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    PowerMockito.mockStatic(EsClientFactory.class);
    PowerMockito.mockStatic(ElasticSearchHelper.class);
    esService = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
  }

  @Test
  public void testAddSkillSuccess() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Response insertResponse = createCassandraSuccessResponse();
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(createGetUserSuccessResponse());
    when(cassandraOperation.getRecordsByProperty(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(createGetSkillsSuccessResponse());
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(insertResponse);
    mockCassandraRequestForGetUser(false);
    subject.tell(
        createAddSkillRequest(USER_ID, ENDORSED_USER_ID, Arrays.asList(ENDORSED_SKILL_NAME)),
        probe.getRef());
    Response response = probe.expectMsgClass(duration, Response.class);
    Assert.assertTrue(null != response && response.getResponseCode() == ResponseCode.OK);
  }

  @Test
  public void testAddSkillFailureWithInvalidUserId() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    mockCassandraRequestForGetUser(true);
    subject.tell(
        createAddSkillRequest(USER_ID, ENDORSED_USER_ID, Collections.emptyList()), probe.getRef());

    ProjectCommonException exception = probe.expectMsgClass(duration, ProjectCommonException.class);
    Assert.assertTrue(
        null != exception
            && exception.getResponseCode() == ResponseCode.CLIENT_ERROR.getResponseCode());
  }

  @Test
  public void testUpdateSkillSuccess() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request actorMessage = createUpdateSkillRequest(USER_ID, Collections.emptyList());
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(createGetUserSuccessResponse());
    when(cassandraOperation.getRecordsByProperty(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(createGetSkillsSuccessResponse());

    mockGetSkillResponse(ENDORSED_USER_ID);
    subject.tell(actorMessage, probe.getRef());
    Response response = probe.expectMsgClass(duration, Response.class);
    Assert.assertTrue(null != response && response.getResponseCode() == ResponseCode.OK);
  }

  @Test
  public void testUpdateUserSkillSuccess() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request actorMessage = createUpdateSkillRequest(USER_ID, Arrays.asList(ENDORSED_SKILL_NAME));
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(createGetUserSuccessResponse());
    when(cassandraOperation.getRecordsByProperty(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(createGetSkillsSuccessResponse());

    mockGetSkillEmptyResponse(ENDORSED_USER_ID);
    subject.tell(actorMessage, probe.getRef());
    Response response = probe.expectMsgClass(duration, Response.class);
    Assert.assertTrue(null != response && response.getResponseCode() == ResponseCode.OK);
  }

  @Test
  public void testUpdateSkillFailureWithInvalidUserId() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = createUpdateSkillRequest(USER_ID, Collections.emptyList());
    mockCassandraRequestForGetUser(true);
    subject.tell(actorMessage, probe.getRef());
    ProjectCommonException exception = probe.expectMsgClass(duration, ProjectCommonException.class);
    Assert.assertTrue(
        null != exception
            && exception.getResponseCode() == ResponseCode.CLIENT_ERROR.getResponseCode());
  }

  @Test
  public void testGetSkillSuccess() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    mockGetSkillResponse(USER_ID);
    subject.tell(createGetSkillRequest(USER_ID, ENDORSED_USER_ID), probe.getRef());
    Response response = probe.expectMsgClass(duration, Response.class);
    Assert.assertTrue(null != response && response.getResponseCode() == ResponseCode.OK);
  }

  @Test
  public void testGetSkillWithEmptyEndorsedUserId() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    mockGetSkillResponse(USER_ID);
    subject.tell(createGetSkillRequest(USER_ID, null), probe.getRef());
    ProjectCommonException ex = probe.expectMsgClass(duration, ProjectCommonException.class);
    Assert.assertTrue(null != ex);
  }

  @Ignore
  public void testGetSkillFailureWithInvalidUserId() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    subject.tell(createGetSkillRequest(USER_ID, ENDORSED_USER_ID), probe.getRef());
    ProjectCommonException exception = probe.expectMsgClass(duration, ProjectCommonException.class);
    Assert.assertTrue(
        null != exception
            && exception.getResponseCode() == ResponseCode.CLIENT_ERROR.getResponseCode());
  }

  @Test
  public void testGetAllSkillsSuccess() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(createGetSkillsSuccessResponse());
    Request actorMessage = new Request();
    actorMessage.setOperation(ActorOperations.GET_SKILLS_LIST.getValue());

    subject.tell(actorMessage, probe.getRef());
    Response response = probe.expectMsgClass(duration, Response.class);
    Assert.assertTrue(null != response && response.getResponseCode() == ResponseCode.OK);
  }

  @Test
  public void testGetSkillsFailureWithInvalidOperationName() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = new Request();
    actorMessage.setOperation(ActorOperations.GET_SKILLS_LIST.getValue() + "INVALID");
    subject.tell(actorMessage, probe.getRef());
    ProjectCommonException exception = probe.expectMsgClass(duration, ProjectCommonException.class);
    Assert.assertTrue(null != exception);
  }

  @Test
  public void testAddSkillEndorsementFailureWithInvalidEndorsedUserId() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    mockCassandraRequestForGetUser(true);
    subject.tell(
        createSkillEndorsementRequest(USER_ID, ENDORSED_USER_ID, ENDORSED_SKILL_NAME),
        probe.getRef());
    ProjectCommonException result = probe.expectMsgClass(duration, ProjectCommonException.class);
    Assert.assertTrue(
        null != result && result.getResponseCode() == ResponseCode.CLIENT_ERROR.getResponseCode());
  }

  @Test
  public void testAddSkillEndorsementFailureWithInvalidUserId() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    mockCassandraRequestForGetUser(true);
    subject.tell(
        createSkillEndorsementRequest(USER_ID, ENDORSED_USER_ID, ENDORSED_SKILL_NAME),
        probe.getRef());
    ProjectCommonException exception = probe.expectMsgClass(duration, ProjectCommonException.class);
    Assert.assertTrue(
        null != exception
            && exception.getResponseCode() == ResponseCode.CLIENT_ERROR.getResponseCode());
  }

  @Test
  @Ignore
  public void testAddSkillEndorsementSuccess() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(createSkillEndorsementResponse());

    when(cassandraOperation.getRecordsByProperty(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(endorsementSkillResponse());
    subject.tell(
        createSkillEndorsementRequest(USER_ID, ENDORSED_USER_ID, ENDORSED_SKILL_NAME),
        probe.getRef());
    Response response = probe.expectMsgClass(duration, Response.class);
    Assert.assertTrue(null != response && response.getResponseCode() == ResponseCode.OK);
  }

  private Request createAddSkillRequest(
      String userId, String endorseUserId, List<String> skillsList) {

    Request actorMessage = new Request();
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.REQUESTED_BY, userId);
    actorMessage.setContext(innerMap);
    actorMessage.put(JsonKey.ENDORSED_USER_ID, endorseUserId);
    actorMessage.put(JsonKey.SKILL_NAME, skillsList);
    actorMessage.setOperation(ActorOperations.ADD_SKILL.getValue());

    return actorMessage;
  }

  private Request createUpdateSkillRequest(String userid, List<String> skills) {
    Request actorMessage = new Request();
    actorMessage.put(JsonKey.USER_ID, userid);
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.REQUESTED_BY, userid);
    actorMessage.setContext(innerMap);
    actorMessage.put(JsonKey.SKILLS, skills);
    actorMessage.setOperation(ActorOperations.UPDATE_SKILL.getValue());

    return actorMessage;
  }

  private Request createGetSkillRequest(String userId, String endorseUserId) {
    Request actorMessage = new Request();
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.REQUESTED_BY, userId);
    actorMessage.setContext(innerMap);
    actorMessage.put(JsonKey.ENDORSED_USER_ID, endorseUserId);
    actorMessage.setOperation(ActorOperations.GET_SKILL.getValue());
    return actorMessage;
  }

  private Request createSkillEndorsementRequest(
      String userId, String endorsedUserId, String skillName) {
    Request actorMessage = new Request();
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.REQUESTED_BY, userId);
    actorMessage.setContext(innerMap);
    actorMessage.put(JsonKey.SKILL_NAME, skillName);
    actorMessage.put(JsonKey.ENDORSED_USER_ID, endorsedUserId);
    actorMessage.put(JsonKey.USER_ID, userId);
    actorMessage.setOperation(ActorOperations.ADD_USER_SKILL_ENDORSEMENT.getValue());
    return actorMessage;
  }

  private Map<String, Object> createGetSkillResponse() {
    HashMap<String, Object> response = new HashMap<>();
    List<Map<String, Object>> content = new ArrayList<>();
    List<Map<String, Object>> skillList = new ArrayList<>();
    HashMap<String, Object> skillMap = new HashMap<>();
    skillMap.put(JsonKey.SKILL_NAME, "some-skill");
    skillList.add(skillMap);
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.SKILLS, skillList);
    content.add(innerMap);
    response.put(JsonKey.CONTENT, content);
    return response;
  }

  private Map<String, Object> createGetSkillEmptyContentResponse() {
    HashMap<String, Object> response = new HashMap<>();
    List<Map<String, Object>> content = new ArrayList<>();
    List<Map<String, Object>> skillList = new ArrayList<>();
    HashMap<String, Object> skillMap = new HashMap<>();
    skillMap.put(JsonKey.SKILL_NAME, "some-skill");
    skillList.add(skillMap);
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.SKILLS, skillList);
    response.put(JsonKey.CONTENT, content);
    return response;
  }

  private Response createGetSkillsSuccessResponse() {
    Response response = new Response();
    Map<String, Object> userMap = new HashMap<>();
    List<Map<String, Object>> result = new ArrayList<>();
    result.add(userMap);
    response.put(JsonKey.RESPONSE, result);
    return response;
  }

  private Response createSkillEndorsementResponse() {
    Response response = new Response();
    List<Map<String, Object>> result = new ArrayList<>();
    Map<String, Object> skill = new HashMap<>();
    skill.put(JsonKey.SKILL_NAME, ENDORSED_SKILL_NAME);
    skill.put(JsonKey.SKILL_NAME_TO_LOWERCASE, ENDORSED_SKILL_NAME);
    skill.put(JsonKey.ENDORSERS_LIST, new ArrayList<>());
    result.add(skill);
    response.put(JsonKey.RESPONSE, result);
    return response;
  }

  private Response createCassandraSuccessResponse() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    return response;
  }

  private Response createGetUserSuccessResponse() {
    Response response = new Response();
    Map<String, Object> userMap = new HashMap<>();
    userMap.put(JsonKey.ID, USER_ID);
    userMap.put(JsonKey.ROOT_ORG_ID, ROOT_ORG_ID);
    userMap.put(JsonKey.ENDORSERS_LIST, new ArrayList<>());
    userMap.put(JsonKey.ENDORSEMENT_COUNT, 2);
    List<Map<String, Object>> result = new ArrayList<>();
    result.add(userMap);
    response.put(JsonKey.RESPONSE, result);
    return response;
  }

  private Response createGetUserFailureResponse() {
    Response response = new Response();
    List<Map<String, Object>> result = new ArrayList<>();
    response.put(JsonKey.RESPONSE, result);
    return response;
  }

  private void mockGetSkillResponse(String userId) {
    Map<String, Object> esDtoMap = new HashMap<>();
    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.USER_ID, userId);
    esDtoMap.put(JsonKey.FILTERS, filters);
    List<String> fields = new ArrayList<>();
    fields.add(JsonKey.SKILLS);
    esDtoMap.put(JsonKey.FIELDS, fields);
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(createGetSkillResponse());
    when(esService.search(
            Mockito.eq(ElasticSearchHelper.createSearchDTO(esDtoMap)),
            Mockito.eq(ProjectUtil.EsType.user.getTypeName())))
        .thenReturn(promise.future());
    Promise<Map<String, Object>> promise_esDtoMap = Futures.promise();
    promise_esDtoMap.success(esDtoMap);

    when(esService.getDataByIdentifier(
            Mockito.eq(ProjectUtil.EsType.user.getTypeName()), Mockito.eq(userId)))
        .thenReturn(promise_esDtoMap.future());
    when(ElasticSearchHelper.getResponseFromFuture(promise_esDtoMap.future())).thenReturn(esDtoMap);
    when(ElasticSearchHelper.getResponseFromFuture(promise.future()))
        .thenReturn(createGetSkillResponse());
  }

  private void mockGetSkillEmptyResponse(String userId) {
    Map<String, Object> esDtoMap = new HashMap<>();
    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.USER_ID, userId);
    esDtoMap.put(JsonKey.FILTERS, filters);
    List<String> fields = new ArrayList<>();
    fields.add(JsonKey.SKILLS);
    esDtoMap.put(JsonKey.FIELDS, fields);
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(createGetSkillResponse());
    when(esService.search(
            Mockito.eq(ElasticSearchHelper.createSearchDTO(esDtoMap)),
            Mockito.eq(ProjectUtil.EsType.user.getTypeName())))
        .thenReturn(promise.future());
    Promise<Map<String, Object>> promise_esDtoMap = Futures.promise();
    promise_esDtoMap.success(esDtoMap);
    when(esService.getDataByIdentifier(
            Mockito.eq(ProjectUtil.EsType.user.getTypeName()), Mockito.eq(userId)))
        .thenReturn(promise_esDtoMap.future());
    when(ElasticSearchHelper.getResponseFromFuture(promise_esDtoMap.future())).thenReturn(esDtoMap);
    when(ElasticSearchHelper.getResponseFromFuture(promise.future()))
        .thenReturn(createGetSkillEmptyContentResponse());
  }

  private Response endorsementSkillResponse() {
    Response response = new Response();
    List<Map<String, Object>> result = new ArrayList<>();
    response.put(JsonKey.RESPONSE, result);
    return response;
  }

  private void mockCassandraRequestForGetUser(boolean isFailure) {
    if (isFailure)
      when(cassandraOperation.getRecordById(
              Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
          .thenReturn(createGetUserFailureResponse());
    else
      when(cassandraOperation.getRecordById(
              Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
          .thenReturn(createGetUserSuccessResponse());
  }
}
