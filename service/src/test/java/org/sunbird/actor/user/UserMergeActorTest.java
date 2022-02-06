package org.sunbird.actor.user;

import static akka.testkit.JavaTestKit.duration;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import com.typesafe.config.Config;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.Producer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.auth.verifier.AccessTokenValidator;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.dao.user.impl.UserDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.kafka.KafkaClient;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.User;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.sso.SSOServiceFactory;
import org.sunbird.util.ConfigUtil;
import org.sunbird.util.DataCacheHandler;
import org.sunbird.util.user.KafkaConfigConstants;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  SSOServiceFactory.class,
  DataCacheHandler.class,
  BaseActor.class,
  UserServiceImpl.class,
  UserDaoImpl.class,
  ServiceFactory.class,
  CassandraOperationImpl.class,
  ConfigUtil.class,
  Config.class,
  KafkaClient.class,
  AccessTokenValidator.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
@SuppressStaticInitializationFor("org.sunbird.kafka.KafkaClient")
public class UserMergeActorTest {
  private static int userCounter;
  private static final Props props = Props.create(UserMergeActor.class);
  private static ActorSystem system = ActorSystem.create("system");
  public static UserServiceImpl userService;
  public static UserDaoImpl userDao;
  public static Config config;
  public static Producer producer;
  public static KafkaClient kafkaClient;
  public static CassandraOperationImpl cassandraOperation;
  private static AccessTokenValidator tokenValidator;

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(UserServiceImpl.class);
    PowerMockito.mockStatic(UserDaoImpl.class);
    PowerMockito.mockStatic(ConfigUtil.class);
    PowerMockito.mockStatic(KafkaClient.class);
    PowerMockito.mockStatic(SSOServiceFactory.class);
    PowerMockito.mockStatic(DataCacheHandler.class);
    userService = mock(UserServiceImpl.class);
    userDao = mock(UserDaoImpl.class);
    config = mock(Config.class);
    kafkaClient = mock(KafkaClient.class);
    producer = mock(Producer.class);
    tokenValidator = mock(AccessTokenValidator.class);
    PowerMockito.mockStatic(AccessTokenValidator.class);
    when(ConfigUtil.getConfig()).thenReturn(config);
    when(config.getString(KafkaConfigConstants.SUNBIRD_USER_CERT_KAFKA_TOPIC)).thenReturn("topic");
    when(UserServiceImpl.getInstance()).thenReturn(userService);
    when(UserDaoImpl.getInstance()).thenReturn(userDao);
    when(KafkaClient.getProducer()).thenReturn(producer);
    cassandraOperation = mock(CassandraOperationImpl.class);
    userCounter = 0;
  }

  @Test
  public void testMergeUserIsAlreadyDeleted() {
    when(userService.getUserById(Mockito.anyString(), Mockito.any()))
        .thenReturn(getUserDetails(true))
        .thenReturn(getUserDetails(true));
    when(userDao.updateUser(Mockito.anyMap(), Mockito.any())).thenReturn(getSuccessResponse());
    when(tokenValidator.verifyUserToken(Mockito.anyString(), Mockito.anyMap()))
        .thenReturn("anyUserId");
    when(tokenValidator.verifySourceUserToken(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn("anyUserId");
    when(DataCacheHandler.getConfigSettings()).thenReturn(configSettingsMap());
    when(DataCacheHandler.getConfigSettings()).thenReturn(configSettingsMap());
    boolean result =
        testScenario(getRequest(ActorOperations.MERGE_USER), ResponseCode.invalidIdentifier);
    assertTrue(result);
  }

  @Test
  public void testValidMergeUser() throws Exception {
    when(userService.getUserById(Mockito.anyString(), Mockito.any()))
        .thenReturn(getUserDetails(false))
        .thenReturn(getUserDetails(false));
    when(userDao.updateUser(Mockito.anyMap(), Mockito.any())).thenReturn(getSuccessResponse());
    when(tokenValidator.verifyUserToken(Mockito.anyString(), Mockito.anyMap()))
        .thenReturn("anyUserId");
    when(tokenValidator.verifySourceUserToken(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn("anyUserId");
    when(DataCacheHandler.getConfigSettings()).thenReturn(configSettingsMap());
    boolean result = testScenario(getRequest(ActorOperations.MERGE_USER), null);
    assertTrue(result);
  }

  private Map<String, String> configSettingsMap() {
    Map<String, String> configMap = new HashMap<>();
    configMap.put(JsonKey.CUSTODIAN_ORG_ID, "anyOrgId");
    return configMap;
  }

  public static Response getSuccessResponse() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    return response;
  }

  private User getUserDetails(boolean b) {
    User user = new User();
    if (userCounter == 0) {
      user.setIsDeleted(b);
      user.setRootOrgId("anyOrgId");
    } else {
      user.setIsDeleted(b);
      user.setRootOrgId("orgId");
    }
    userCounter++;
    return user;
  }

  public boolean testScenario(Request reqObj, ResponseCode errorCode) {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    subject.tell(reqObj, probe.getRef());

    if (errorCode == null) {
      Response res = probe.expectMsgClass(duration("10 second"), Response.class);
      return null != res && res.getResponseCode() == ResponseCode.OK;
    } else {
      ProjectCommonException res =
          probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
      return res.getResponseCode().name().equals(errorCode.name())
          || res.getErrorResponseCode() == errorCode.getResponseCode();
    }
  }

  Request getRequest(ActorOperations actorOperation) {
    Request reqObj = new Request();
    Map reqMap = new HashMap<>();
    Map contextMap = new HashMap<>();
    Map header = new HashMap<>();
    String authToken = "authUserToken";
    reqMap.put(JsonKey.FROM_ACCOUNT_ID, "anyUserId");
    reqMap.put(JsonKey.TO_ACCOUNT_ID, "anyUserId");
    header.put(JsonKey.X_AUTHENTICATED_USER_TOKEN, authToken);
    header.put(JsonKey.X_SOURCE_USER_TOKEN, authToken);
    contextMap.put("header", header);
    reqObj.setRequest(reqMap);
    reqObj.setContext(contextMap);
    reqObj.setOperation(actorOperation.getValue());
    reqObj.setRequestContext(new RequestContext());
    return reqObj;
  }
}
