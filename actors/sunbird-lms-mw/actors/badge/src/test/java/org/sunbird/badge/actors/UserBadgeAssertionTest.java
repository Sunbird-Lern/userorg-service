package org.sunbird.badge.actors;

import static akka.testkit.JavaTestKit.duration;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.testkit.javadsl.TestKit;
import java.util.ArrayList;
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
import org.sunbird.badge.BadgeOperations;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.BadgingJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.learner.util.Util.DbInfo;
import scala.concurrent.Promise;
import scala.concurrent.duration.FiniteDuration;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  EsClientFactory.class,
  Util.class,
  ElasticSearchHelper.class,
  ElasticSearchRestHighImpl.class
})
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
public class UserBadgeAssertionTest {

  @SuppressWarnings("deprecation")
  private static final FiniteDuration ACTOR_MAX_WAIT_DURATION = duration("100 second");

  private ActorSystem system;
  private TestKit probe;
  private ActorRef subject;
  private Request actorMessage;
  private CassandraOperation cassandraOperation;
  private DbInfo dbInfo = Util.dbInfoMap.get(BadgingJsonKey.USER_BADGE_ASSERTION_DB);
  private HashMap<String, Object> tempMap;
  private Map<String, Object> result;
  private Map<String, Object> badge;
  private ElasticSearchService esService;

  @Before
  public void setUp() {
    system = ActorSystem.create("system");
    probe = new TestKit(system);
    actorMessage = new Request();
    badge = new HashMap<>();
    badge.put(BadgingJsonKey.ASSERTION_ID, "aslug123");
    badge.put(BadgingJsonKey.BADGE_ID, "bslug123");
    badge.put(BadgingJsonKey.ISSUER_ID, "islug123");
    badge.put(BadgingJsonKey.BADGE_CLASS_NANE, "cert123");
    badge.put(
        BadgingJsonKey.BADGE_CLASS_IMAGE,
        "http://localhost:8000/public/badges/java-se-8-programmer/image");
    badge.put(BadgingJsonKey.CREATED_TS, "1520586333");

    Map<String, Object> req = new HashMap<>();
    req.put(JsonKey.ID, "userId-123");
    req.put(BadgingJsonKey.BADGE_ASSERTION, badge);
    actorMessage.setRequest(req);
    tempMap = new HashMap<>();
    cassandraOperation = PowerMockito.mock(CassandraOperation.class);
    esService = PowerMockito.mock(ElasticSearchRestHighImpl.class);
    PowerMockito.mockStatic(ServiceFactory.class);
    PowerMockito.mockStatic(ElasticSearchHelper.class);
    PowerMockito.when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    PowerMockito.mockStatic(EsClientFactory.class);
    PowerMockito.when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
    Props props = Props.create(UserBadgeAssertion.class);
    subject = system.actorOf(props);
  }

  @Test
  public void checkTelemetryKeyFailure() throws Exception {
    result = new HashMap<>();
    String telemetryEnvKey = "user";
    Promise<Boolean> promise = Futures.promise();
    promise.success(true);
    PowerMockito.when(
            esService.update(ProjectUtil.EsType.user.getTypeName(), "userId-123", tempMap))
        .thenReturn(promise.future());

    PowerMockito.mockStatic(Util.class);
    PowerMockito.doNothing()
        .when(
            Util.class,
            "initializeContext",
            Mockito.any(Request.class),
            Mockito.eq(telemetryEnvKey));
    List<Map<String, Object>> badgeAssertionsList = new ArrayList<>();
    Map<String, Object> tempMap = new HashMap<>();
    tempMap.put(JsonKey.ID, getUserBadgeAssertionId(badge));
    badgeAssertionsList.add(tempMap);
    result.put(BadgingJsonKey.BADGE_ASSERTIONS, badgeAssertionsList);
    Promise<Map<String, Object>> promise1 = Futures.promise();
    promise1.success(result);
    PowerMockito.when(
            esService.getDataByIdentifier(ProjectUtil.EsType.user.getTypeName(), "userId-123"))
        .thenReturn(promise1.future());
    PowerMockito.when(
            cassandraOperation.insertRecord(dbInfo.getKeySpace(), dbInfo.getTableName(), tempMap))
        .thenReturn(new Response());

    actorMessage.setOperation(BadgeOperations.assignBadgeToUser.name());

    subject.tell(actorMessage, probe.getRef());

    probe.expectMsgClass(ACTOR_MAX_WAIT_DURATION, Response.class);
    Assert.assertTrue(!(telemetryEnvKey.charAt(0) >= 65 && telemetryEnvKey.charAt(0) <= 90));
  }

  @Test
  public void testAssignBadgeToUser() {
    result = new HashMap<>();
    Promise<Boolean> promise = Futures.promise();
    promise.success(false);
    PowerMockito.when(
            esService.update(ProjectUtil.EsType.user.getTypeName(), "userId-123", tempMap))
        .thenReturn(promise.future());
    List<Map<String, Object>> badgeAssertionsList = new ArrayList<>();
    Map<String, Object> tempMap = new HashMap<>();
    tempMap.put(JsonKey.ID, getUserBadgeAssertionId(badge));
    badgeAssertionsList.add(tempMap);
    result.put(BadgingJsonKey.BADGE_ASSERTIONS, badgeAssertionsList);
    Promise<Map<String, Object>> promise1 = Futures.promise();
    promise1.success(result);
    PowerMockito.when(
            esService.getDataByIdentifier(ProjectUtil.EsType.user.getTypeName(), "userId-123"))
        .thenReturn(promise1.future());
    PowerMockito.when(
            cassandraOperation.insertRecord(dbInfo.getKeySpace(), dbInfo.getTableName(), tempMap))
        .thenReturn(new Response());

    actorMessage.setOperation(BadgeOperations.assignBadgeToUser.name());

    subject.tell(actorMessage, probe.getRef());

    Response response = probe.expectMsgClass(ACTOR_MAX_WAIT_DURATION, Response.class);
    Assert.assertTrue(null != response);
  }

  @Test
  public void testAssignBadgeToUser2() {
    Promise<Boolean> promise = Futures.promise();
    promise.success(false);
    PowerMockito.when(
            esService.update(ProjectUtil.EsType.user.getTypeName(), "userId-123", tempMap))
        .thenReturn(promise.future());
    result = new HashMap<>();
    List<Map<String, Object>> badgeAssertionsList = new ArrayList<>();
    Map<String, Object> tempMap = new HashMap<>();
    tempMap.put(JsonKey.ID, "132");
    badgeAssertionsList.add(tempMap);
    result.put(BadgingJsonKey.BADGE_ASSERTIONS, badgeAssertionsList);
    Promise<Map<String, Object>> promise1 = Futures.promise();
    promise1.success(result);
    PowerMockito.when(esService.getDataByIdentifier(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(promise1.future());

    PowerMockito.when(
            cassandraOperation.insertRecord(dbInfo.getKeySpace(), dbInfo.getTableName(), tempMap))
        .thenReturn(new Response());

    actorMessage.setOperation(BadgeOperations.assignBadgeToUser.name());

    subject.tell(actorMessage, probe.getRef());

    Response response = probe.expectMsgClass(ACTOR_MAX_WAIT_DURATION, Response.class);
    Assert.assertTrue(null != response);
  }

  @Test
  public void testRevokeBadgeToUser() {
    Promise<Boolean> promise = Futures.promise();
    promise.success(true);
    PowerMockito.when(
            esService.update(ProjectUtil.EsType.user.getTypeName(), "userId-123", tempMap))
        .thenReturn(promise.future());

    PowerMockito.when(
            cassandraOperation.deleteRecord(
                dbInfo.getKeySpace(), dbInfo.getTableName(), "userId-123"))
        .thenReturn(new Response());

    actorMessage.setOperation(BadgeOperations.revokeBadgeFromUser.name());

    subject.tell(actorMessage, probe.getRef());

    Response response = probe.expectMsgClass(ACTOR_MAX_WAIT_DURATION, Response.class);
    Assert.assertTrue(null != response);
  }

  /**
   * @param badge
   * @return String
   */
  private String getUserBadgeAssertionId(Map<String, Object> badge) {
    return ((String) badge.get(BadgingJsonKey.ASSERTION_ID)
        + JsonKey.PRIMARY_KEY_DELIMETER
        + (String) badge.get(BadgingJsonKey.ISSUER_ID)
        + JsonKey.PRIMARY_KEY_DELIMETER
        + (String) badge.get(BadgingJsonKey.BADGE_CLASS_ID));
  }
}
