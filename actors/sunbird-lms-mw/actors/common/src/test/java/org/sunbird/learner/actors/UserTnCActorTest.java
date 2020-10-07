package org.sunbird.learner.actors;

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
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.actor.router.RequestRouter;
import org.sunbird.actor.service.SunbirdMWService;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.tac.UserTnCActor;
import org.sunbird.learner.util.DataCacheHandler;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  RequestRouter.class,
  EsClientFactory.class,
  ElasticSearchRestHighImpl.class,
  SunbirdMWService.class,
  DataCacheHandler.class
})
@PowerMockIgnore({"javax.management.*", "javax.crypto.*", "javax.net.ssl.*", "javax.security.*"})
public class UserTnCActorTest {
  private static ActorSystem system;
  private String tncConfig =
      "{\"latestVersion\":\"v1\",\"v1\":{\"url\":\"http://dev/terms.html\"},\"v2\":{\"url\":\"http://dev/terms.html\"},\"v4\":{\"url\":\"http://dev/terms.html\"}}";

  private static final Props props = Props.create(UserTnCActor.class);
  private static final CassandraOperation cassandraOperation = mock(CassandraOperationImpl.class);;
  private static final String LATEST_VERSION = "v1";
  private static final String ACCEPTED_CORRECT_VERSION = "v1";
  private static final String ACCEPTED_INVALID_VERSION = "invalid";

  @BeforeClass
  public static void setUp() {
    system = ActorSystem.create("system");
  }

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(DataCacheHandler.class);
    PowerMockito.mockStatic(ServiceFactory.class);
    PowerMockito.mockStatic(RequestRouter.class);
    ActorRef actorRef = mock(ActorRef.class);

    PowerMockito.mockStatic(SunbirdMWService.class);
    SunbirdMWService.tellToBGRouter(Mockito.any(), Mockito.any());
    when(RequestRouter.getActor(Mockito.anyString())).thenReturn(actorRef);

    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    Map<String, String> config = new HashMap<>();
    config.put(JsonKey.TNC_CONFIG, tncConfig);
    when(DataCacheHandler.getConfigSettings()).thenReturn(config);
  }

  @Test
  public void testAcceptUserTcnSuccessWithAcceptFirstTime() {
    Response userResponse = new Response();
    List<Map<String, Object>> userList = new ArrayList<>();
    userList.add(getUser(null));
    userResponse.put(JsonKey.RESPONSE, userList);
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(userResponse);
    Response response =
        setRequest(ACCEPTED_CORRECT_VERSION).expectMsgClass(duration("10 second"), Response.class);
    Assert.assertTrue(
        null != response && "SUCCESS".equals(response.getResult().get(JsonKey.RESPONSE)));
  }

  @Test
  public void testAcceptUserTncSuccessAlreadyAccepted() {
    Response userResponse = new Response();
    List<Map<String, Object>> userList = new ArrayList<>();
    userList.add(getUser(LATEST_VERSION));
    userResponse.put(JsonKey.RESPONSE, userList);
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(userResponse);

    Response response =
        setRequest(ACCEPTED_CORRECT_VERSION).expectMsgClass(duration("10 second"), Response.class);
    Assert.assertTrue(
        null != response && "SUCCESS".equals(response.getResult().get(JsonKey.RESPONSE)));
  }

  @Test
  public void testAcceptUserTncForBlockedUser() {
    Response userResponse = new Response();
    List<Map<String, Object>> userList = new ArrayList<>();
    Map<String, Object> user = new HashMap<>();
    user.put(JsonKey.ROOT_ORG_ID, "anyRootId");
    user.put(JsonKey.IS_DELETED, true);
    userList.add(user);
    userResponse.put(JsonKey.RESPONSE, userList);
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(userResponse);

    ProjectCommonException response =
        setRequest(ACCEPTED_CORRECT_VERSION)
            .expectMsgClass(duration("10 second"), ProjectCommonException.class);
    Assert.assertEquals(ResponseCode.userAccountlocked.getErrorCode(), response.getCode());
    Assert.assertEquals("User account has been blocked .", response.getMessage());
  }

  @Test
  public void testAcceptUserTncFailureWithInvalidVersion() {
    ProjectCommonException exception =
        setRequest(ACCEPTED_INVALID_VERSION)
            .expectMsgClass(duration("10 second"), ProjectCommonException.class);
    Assert.assertTrue(
        null != exception
            && exception
                .getCode()
                .equalsIgnoreCase(ResponseCode.invalidParameterValue.getErrorCode()));
  }

  private TestKit setRequest(String version) {
    mockCassandraOperation();
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.USER_TNC_ACCEPT.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.VERSION, version);
    reqObj.setRequest(innerMap);
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    subject.tell(reqObj, probe.getRef());
    return probe;
  }

  private void mockCassandraOperation() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(response);
  }

  private Map<String, Object> getUser(String lastAcceptedVersion) {
    Map<String, Object> user = new HashMap<>();
    user.put(JsonKey.NAME, "someName");
    user.put(JsonKey.IS_DELETED, false);
    if (lastAcceptedVersion != null) {
      user.put(JsonKey.TNC_ACCEPTED_VERSION, lastAcceptedVersion);
    }
    return user;
  }
}
