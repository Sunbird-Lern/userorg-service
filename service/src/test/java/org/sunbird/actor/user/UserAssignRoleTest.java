package org.sunbird.actor.user;

import static org.junit.Assert.assertTrue;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.dispatch.Futures;
import org.apache.pekko.testkit.javadsl.TestKit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.actor.role.UserRoleActor;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.Constants;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.util.DataCacheHandler;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import scala.concurrent.Promise;
import scala.concurrent.duration.FiniteDuration;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  ElasticSearchRestHighImpl.class,
  EsClientFactory.class,
  CassandraOperationImpl.class,
  DataCacheHandler.class,
  org.sunbird.datasecurity.impl.ServiceFactory.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
@SuppressStaticInitializationFor("org.sunbird.common.ElasticSearchUtil")
public class UserAssignRoleTest {

  private static final FiniteDuration ACTOR_MAX_WAIT_DURATION = FiniteDuration.apply(120, TimeUnit.SECONDS);
  private static String ID = "id001";
  private static String orgId = "testOrg001";
  private static String userId = "testUser001";
  private static String externalId = "testExternal001";
  private static String provider = "testProvider001";
  private static String hashtagId = "hashTagId001";
  private static List<String> ALL_ROLES =
      Arrays.asList(
          "CONTENT_CREATOR",
          "COURSE_MENTOR",
          "BOOK_CREATOR",
          "BOOK_REVIEWER",
          "ANNOUNCEMENT_SENDER",
          "CONTENT_REVIEWER",
          "FLAG_REVIEWER",
          "PUBLIC");
  private static Map<String, Object> userOrg = new HashMap<>();

  private static ActorSystem system;
  private static Props props;
  private static CassandraOperation cassandraOperation = null;
  private static Response response = null;
  private static Map<String, Object> esRespone = new HashMap<>();
  private static ElasticSearchService esService;

  @BeforeClass
  public static void setUp() throws Exception {
    system = ActorSystem.create("system");
    props = Props.create(UserRoleActor.class);

    userOrg.put(JsonKey.ID, ID);
    userOrg.put(JsonKey.ORGANISATION_ID, orgId);
    userOrg.put(JsonKey.USER_ID, userId);
    userOrg.put(JsonKey.HASHTAGID, hashtagId);
    userOrg.put(JsonKey.EXTERNAL_ID, externalId);
    userOrg.put(JsonKey.PROVIDER, provider);
    userOrg.put(
        JsonKey.ROLES,
        Arrays.asList(
            "CONTENT_CREATOR", "COURSE_MENTOR", "BOOK_CREATOR", "BOOK_REVIEWER", "PUBLIC"));

    response = new Response();
    Map<String, Object> responseMap = new HashMap<>();

    responseMap.put(Constants.RESPONSE, Arrays.asList(userOrg));
    response.getResult().putAll(responseMap);

    esRespone.put(JsonKey.CONTENT, Arrays.asList(userOrg));
  }

  @Before
  public void mockClasses() throws Exception {

    PowerMockito.mockStatic(ServiceFactory.class);
    PowerMockito.mockStatic(EsClientFactory.class);
    cassandraOperation = PowerMockito.mock(CassandraOperationImpl.class);
    PowerMockito.when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    esService = PowerMockito.mock(ElasticSearchRestHighImpl.class);
    PowerMockito.when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
    Map<String, Object> roleMap = new HashMap<>();
    for (String role : ALL_ROLES) roleMap.put(role, role);
    PowerMockito.mockStatic(DataCacheHandler.class);
    PowerMockito.when(DataCacheHandler.getRoleMap()).thenReturn(roleMap);
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(userOrg);
    Promise<Map<String, Object>> promise_es = Futures.promise();
    promise_es.success(esRespone);
    PowerMockito.when(esService.getDataByIdentifier(Mockito.any(), Mockito.any(), null))
        .thenReturn(promise.future());
    PowerMockito.when(esService.search(Mockito.any(), Mockito.any(), null))
        .thenReturn(promise_es.future());
  }

  private static void initCassandraForSuccess() {
    PowerMockito.when(
            cassandraOperation.getRecordsByProperties(
                Mockito.any(), Mockito.any(), Mockito.any(), null))
        .thenReturn(response);

    Response updateResponse = new Response();
    Map<String, Object> responseMap = new HashMap<>();
    responseMap.put(Constants.RESPONSE, Constants.SUCCESS);
    updateResponse.getResult().putAll(responseMap);

    PowerMockito.when(
            cassandraOperation.updateRecord(Mockito.any(), Mockito.any(), Mockito.any(), null))
        .thenReturn(updateResponse);
  }

  @Ignore
  public void testAssignInvalidRolesFailure() throws Exception {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.ASSIGN_ROLES.getValue());
    Map<String, Object> request = new HashMap<String, Object>();
    request.put(JsonKey.USER_ID, userId);
    request.put(JsonKey.ORGANISATION_ID, orgId);
    request.put(JsonKey.ROLES, new ArrayList<>(Arrays.asList("DUMMY_ROLE")));
    reqObj.setRequest(request);

    initCassandraForSuccess();
    subject.tell(reqObj, probe.getRef());
    ProjectCommonException ex =
        probe.expectMsgClass(ACTOR_MAX_WAIT_DURATION, ProjectCommonException.class);
    assertTrue(null != ex);
  }

  @Ignore
  public void testAssignValidRolesSuccess() throws Exception {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.ASSIGN_ROLES.getValue());
    Map<String, Object> request = new HashMap<String, Object>();
    request.put(JsonKey.USER_ID, userId);
    request.put(JsonKey.ORGANISATION_ID, orgId);
    List<String> roles = new ArrayList<>(Arrays.asList("CONTENT_CREATOR", "COURSE_MENTOR"));
    request.put(JsonKey.ROLES, roles);
    reqObj.setRequest(request);

    initCassandraForSuccess();
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(ACTOR_MAX_WAIT_DURATION, Response.class);
    assertTrue(null != res);
  }

  @Ignore
  public void testAssignEmptyRoleSuccess() throws Exception {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.ASSIGN_ROLES.getValue());
    Map<String, Object> request = new HashMap<String, Object>();
    request.put(JsonKey.USER_ID, userId);
    request.put(JsonKey.ORGANISATION_ID, orgId);
    List<String> roles = new ArrayList<>();
    request.put(JsonKey.ROLES, roles);
    reqObj.setRequest(request);

    initCassandraForSuccess();
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(ACTOR_MAX_WAIT_DURATION, Response.class);
    assertTrue(null != res);
  }

  @Ignore
  public void testAssignValidRolesSuccessWithoutOrgID() throws Exception {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.ASSIGN_ROLES.getValue());
    Map<String, Object> request = new HashMap<String, Object>();
    request.put(JsonKey.USER_ID, userId);
    request.put(JsonKey.EXTERNAL_ID, externalId);
    request.put(JsonKey.PROVIDER, provider);
    List<String> roles = new ArrayList<>(Arrays.asList("CONTENT_CREATOR", "COURSE_MENTOR"));
    request.put(JsonKey.ROLES, roles);
    reqObj.setRequest(request);

    initCassandraForSuccess();
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(ACTOR_MAX_WAIT_DURATION, Response.class);
    assertTrue(null != res);
  }
}
