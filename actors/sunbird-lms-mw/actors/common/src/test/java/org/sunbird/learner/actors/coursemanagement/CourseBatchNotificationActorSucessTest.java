package org.sunbird.learner.actors.coursemanagement;

import static akka.testkit.JavaTestKit.duration;
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
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.actorutil.email.EmailServiceClient;
import org.sunbird.actorutil.email.EmailServiceFactory;
import org.sunbird.actorutil.email.impl.EmailServiceClientImpl;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.coursebatch.CourseBatchNotificationActor;
import org.sunbird.learner.util.EkStepRequestUtil;
import org.sunbird.learner.util.Util;
import org.sunbird.models.course.batch.CourseBatch;

/*
 * @author github.com/iostream04
 *
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  EkStepRequestUtil.class,
  Util.class,
  EmailServiceClientImpl.class,
  EmailServiceClient.class,
  EmailServiceFactory.class
})
@PowerMockIgnore({"javax.management.*", "javax.crypto.*", "javax.net.ssl.*", "javax.security.*"})
public class CourseBatchNotificationActorSucessTest {

  private static final String FIRST_NAME = "Test User";
  private static final String USER_ID = "testUserId";
  private static final String USER_ID_OLD = "testUserIdOld";
  private static final String USER_ID_NEW = "testUserIdNew";
  private static final String emailId = "user@test.com";
  private static final String orgName = "testOrg";
  private static final String TEMPLATE = "template";
  private static ActorSystem system;
  private static final Props props = Props.create(CourseBatchNotificationActor.class);
  private static CassandraOperation cassandraOperation;
  private static EmailServiceClient emailServiceClient;

  @BeforeClass
  public static void setUp() {
    system = ActorSystem.create("system");
    PowerMockito.mockStatic(EkStepRequestUtil.class);
  }

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    Mockito.reset(cassandraOperation);
    PowerMockito.mockStatic(EmailServiceFactory.class);
    emailServiceClient = mock(EmailServiceClientImpl.class);
    when(EmailServiceFactory.getInstance()).thenReturn(emailServiceClient);
    Mockito.reset(emailServiceClient);
  }

  @Test
  public void testCourseBatchUpdateSucess() {
    mockCassandraRequestForReadRecordById();
    mockCassandraRequestForReadTemplate();
    mockEmailServiceSuccess();
    Response response = getUpdateSucessEmailNotificationTestResponse();
    Assert.assertTrue(null != response && response.getResponseCode() == ResponseCode.OK);
  }

  @Test
  public void testCourseBatchEnrollForLearnerSucess() {
    mockCassandraRequestForReadRecordById();
    mockCassandraRequestForReadTemplate();
    mockEmailServiceSuccess();
    Response res = getEnrollSucessEmailNotificationForLearnerTestResponse();
    Assert.assertTrue(null != res && res.getResponseCode() == ResponseCode.OK);
  }

  @Ignore
  public void testCourseBatchBulkRemoveSucess() {
    mockCassandraRequestForReadRecordById();
    mockCassandraRequestForReadTemplate();
    mockEmailServiceSuccess();
    Response response = getBulkSucessEmailNotificationTestResponse(JsonKey.REMOVE);
    Assert.assertTrue(null != response && response.getResponseCode() == ResponseCode.OK);
  }

  @Ignore
  public void testCourseBatchBulkAddSucess() {
    mockCassandraRequestForReadRecordById();
    mockCassandraRequestForReadTemplate();
    mockEmailServiceSuccess();
    Response response = getBulkSucessEmailNotificationTestResponse(JsonKey.ADD);
    Assert.assertTrue(null != response && response.getResponseCode() == ResponseCode.OK);
  }

  private Response getBulkSucessEmailNotificationTestResponse(String operationType) {

    Request request =
        createRequestObjectForBulkOperation(
            createCourseBatchObject(true, JsonKey.SUNBIRD), operationType);
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    subject.tell(request, probe.getRef());
    Response response = probe.expectMsgClass(duration("10 second"), Response.class);
    return response;
  }

  private Response getUpdateSucessEmailNotificationTestResponse() {
    Request request =
        createRequestObjectForUpdateOperation(
            createCourseBatchObject(true, JsonKey.OLD), createMentorParticipantMap());
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    subject.tell(request, probe.getRef());
    Response response = probe.expectMsgClass(duration("20 second"), Response.class);
    return response;
  }

  private Map<String, Object> createMentorParticipantMap() {
    Map<String, Object> mentorParticipantMap = new HashMap<>();
    List<String> prevMentors = new ArrayList<>();
    List<String> newMentors = new ArrayList<>();
    prevMentors.add(USER_ID_OLD);
    newMentors.add(USER_ID_NEW);
    mentorParticipantMap.put(JsonKey.REMOVED_MENTORS, prevMentors);
    mentorParticipantMap.put(JsonKey.ADDED_MENTORS, newMentors);
    mentorParticipantMap.put(JsonKey.ADDED_PARTICIPANTS, newMentors);
    mentorParticipantMap.put(JsonKey.REMOVED_PARTICIPANTS, prevMentors);
    return mentorParticipantMap;
  }

  private Response getEnrollSucessEmailNotificationForLearnerTestResponse() {
    Request request =
        createRequestObjectForEnrollOperation(
            createCourseBatchObject(), createCourseMap(), JsonKey.BATCH_LEARNER_ENROL);
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    subject.tell(request, probe.getRef());
    Response exception = probe.expectMsgClass(duration("10 second"), Response.class);
    return exception;
  }

  private Request createRequestObjectForEnrollOperation(
      CourseBatch courseBatch, Map<String, Object> courseMap, String operationType) {
    Request request = new Request();
    Map<String, Object> requestMap = new HashMap<>();
    request.setOperation(ActorOperations.COURSE_BATCH_NOTIFICATION.getValue());
    requestMap.put(JsonKey.COURSE_BATCH, courseBatch);
    requestMap.put(JsonKey.USER_ID, (String) courseMap.get(JsonKey.USER_ID));
    requestMap.put(JsonKey.OPERATION_TYPE, operationType);
    request.setRequest(requestMap);
    return request;
  }

  private Request createRequestObjectForUpdateOperation(
      CourseBatch CourseBatchOld, Map<String, Object> mentorParticipantsMap) {
    Request request = new Request();
    Map<String, Object> requestMap = new HashMap<>();
    request.setOperation(ActorOperations.COURSE_BATCH_NOTIFICATION.getValue());
    requestMap.put(JsonKey.COURSE_BATCH, CourseBatchOld);
    requestMap.put(JsonKey.UPDATE, true);
    requestMap.put(JsonKey.REMOVED_MENTORS, mentorParticipantsMap.get(JsonKey.REMOVED_MENTORS));
    requestMap.put(JsonKey.ADDED_MENTORS, mentorParticipantsMap.get(JsonKey.ADDED_MENTORS));
    requestMap.put(
        JsonKey.REMOVED_PARTICIPANTS, mentorParticipantsMap.get(JsonKey.REMOVED_PARTICIPANTS));
    requestMap.put(
        JsonKey.ADDED_PARTICIPANTS, mentorParticipantsMap.get(JsonKey.ADDED_PARTICIPANTS));
    request.setRequest(requestMap);
    return request;
  }

  private Request createRequestObjectForBulkOperation(
      CourseBatch courseBatch, String OperationType) {
    Request request = new Request();
    Map<String, Object> requestMap = new HashMap<>();
    request.setOperation(ActorOperations.COURSE_BATCH_NOTIFICATION.getValue());
    requestMap.put(JsonKey.COURSE_BATCH, courseBatch);
    requestMap.put(JsonKey.OPERATION_TYPE, OperationType);
    request.setRequest(requestMap);
    return request;
  }

  private Map<String, Object> createCourseMap() {
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.USER_ID, USER_ID);
    return map;
  }

  private CourseBatch createCourseBatchObject() {
    CourseBatch courseBatch = new CourseBatch();
    List<String> mentors = new ArrayList<>();
    List<String> participants = new ArrayList<>();
    mentors.add(USER_ID);
    participants.add(USER_ID);
    courseBatch.setMentors(mentors);
    courseBatch.setCountDecrementStatus(false);
    courseBatch.setCountIncrementStatus(false);
    courseBatch.setParticipant(participants);
    courseBatch.setStatus(0);
    Map<String, String> courseAdditionalInfo = new HashMap<>();
    courseAdditionalInfo.put(JsonKey.ORG_NAME, orgName);
    courseBatch.setCourseAdditionalInfo(courseAdditionalInfo);

    return courseBatch;
  }

  private CourseBatch createCourseBatchObject(boolean testType, String type) {
    CourseBatch courseBatch = new CourseBatch();
    List<String> mentors = new ArrayList<>();
    List<String> participants = new ArrayList<>();
    if (testType) {
      mentors.add(USER_ID);
      participants.add(USER_ID);
      if (type.equals(JsonKey.OLD)) {
        mentors.add(USER_ID_OLD);
        participants.add(USER_ID_OLD);
      }
      if (type.equals(JsonKey.NEW)) {
        mentors.add(USER_ID_NEW);
        participants.add(USER_ID_NEW);
      }
    }
    courseBatch.setStatus(0);
    courseBatch.setCountDecrementStatus(false);
    courseBatch.setCountIncrementStatus(false);
    courseBatch.setMentors(mentors);
    courseBatch.setParticipant(participants);
    Map<String, String> courseAdditionalInfo = new HashMap<>();
    courseAdditionalInfo.put(JsonKey.ORG_NAME, orgName);
    courseBatch.setCourseAdditionalInfo(courseAdditionalInfo);
    return courseBatch;
  }

  private Response stringTemplateResponse() {
    Response response = new Response();
    List<Map<String, Object>> result = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put(TEMPLATE, "");
    result.add(map);
    response.put(JsonKey.RESPONSE, result);
    return response;
  }

  private Response createUser() {
    Response response = new Response();
    Map<String, Object> userMap = new HashMap<>();
    userMap.put(JsonKey.FIRST_NAME, FIRST_NAME);
    userMap.put(JsonKey.EMAIL, emailId);
    List<Map<String, Object>> result = new ArrayList<>();
    result.add(userMap);
    response.put(JsonKey.RESPONSE, result);
    return response;
  }

  @SuppressWarnings("unchecked")
  private void mockCassandraRequestForReadRecordById() {
    when(cassandraOperation.getRecordsByIdsWithSpecifiedColumns(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyList(), Mockito.anyList()))
        .thenReturn(createUser());
  }

  private void mockCassandraRequestForReadTemplate() {
    when(cassandraOperation.getRecordsByPrimaryKeys(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyList(), Mockito.anyString()))
        .thenReturn(stringTemplateResponse());
  }

  private void mockEmailServiceSuccess() {
    Response res = new Response();
    res.setResponseCode(ResponseCode.OK);
    when(emailServiceClient.sendMail(
            (ActorRef) Mockito.any(), Mockito.anyMapOf(String.class, Object.class)))
        .thenReturn(res);
  }
}
