package org.sunbird.actor.user;

import java.time.Duration;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.CassandraUtil;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.sso.KeyCloakConnectionProvider;
import org.sunbird.sso.SSOManager;
import org.sunbird.sso.SSOServiceFactory;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  KeyCloakConnectionProvider.class,
  SSOServiceFactory.class,
  SSOManager.class,
  UserServiceImpl.class,
  ServiceFactory.class,
  ElasticSearchRestHighImpl.class,
  ElasticSearchHelper.class,
  EsClientFactory.class,
  CassandraOperationImpl.class,
  CassandraOperation.class,
  CassandraUtil.class,
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class IdentifierFreeUpActorTest {
  private ElasticSearchService elasticSearchService;
  public CassandraOperation cassandraOperation;
  Props props = Props.create(IdentifierFreeUpActor.class);
  ActorSystem system = ActorSystem.create("IdentifierFreeUpActor");

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(ElasticSearchRestHighImpl.class);
    elasticSearchService = PowerMockito.mock(ElasticSearchService.class);
    PowerMockito.mockStatic(EsClientFactory.class);
    when(EsClientFactory.getInstance(JsonKey.REST)).thenReturn(elasticSearchService);
    PowerMockito.mockStatic(ElasticSearchHelper.class);
    cassandraOperation = PowerMockito.mock(CassandraOperation.class);
    PowerMockito.mockStatic(ServiceFactory.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);

    UserRepresentation userRepresentation = mock(UserRepresentation.class);
    RealmResource realmResource = mock(RealmResource.class);
    Keycloak keycloak = mock(Keycloak.class);
    PowerMockito.mockStatic(KeyCloakConnectionProvider.class);
    when(KeyCloakConnectionProvider.getConnection()).thenReturn(keycloak);
    when(keycloak.realm(Mockito.anyString())).thenReturn(realmResource);

    UsersResource usersResource = mock(UsersResource.class);
    when(realmResource.users()).thenReturn(usersResource);

    UserResource userResource = mock(UserResource.class);
    when(usersResource.get(Mockito.any())).thenReturn(userResource);
    when(userResource.toRepresentation()).thenReturn(userRepresentation);
    PowerMockito.mockStatic(SSOServiceFactory.class);
    SSOManager ssoManager = PowerMockito.mock(SSOManager.class);
    PowerMockito.when(SSOServiceFactory.getInstance()).thenReturn(ssoManager);
    PowerMockito.when(ssoManager.deactivateUser(Mockito.anyMap(), Mockito.any()))
        .thenReturn(JsonKey.SUCCESS);
  }

  @Test
  public void testFreeUpWhenOnlyFreeUpEmail() {
    String id = "anyUserId";
    Request reqObj = new Request();
    Map reqMap = new HashMap<>();
    reqMap.put(JsonKey.ID, "anyUserId");
    reqMap.put(JsonKey.IDENTIFIER, new ArrayList<>(Arrays.asList("email", "phone")));
    reqObj.setRequest(reqMap);
    reqObj.setOperation(ActorOperations.FREEUP_USER_IDENTITY.getValue());
    Response response = new Response();
    List<Map<String, Object>> responseList = new ArrayList<>();
    Map<String, Object> userDbMap = new HashMap<>();
    userDbMap.put(JsonKey.EMAIL, "userPrimaryEmail");
    userDbMap.put(JsonKey.PHONE, "9876543210");
    userDbMap.put(JsonKey.PREV_USED_EMAIL, null);
    userDbMap.put(JsonKey.PREV_USED_PHONE, null);
    userDbMap.put(JsonKey.MASKED_EMAIL, "user*******");
    userDbMap.put(JsonKey.MASKED_PHONE, "98***08908");
    userDbMap.put(JsonKey.FLAGS_VALUE, 3);
    userDbMap.put(JsonKey.ID, id);
    responseList.add(userDbMap);
    response.put(JsonKey.RESPONSE, responseList);
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(response);
    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(new Response());
    doNothing()
        .when(cassandraOperation)
        .deleteRecord(Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any());
    Promise<Boolean> promise = Futures.promise();
    promise.success(true);
    when(elasticSearchService.update(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(promise.future());
    when(ElasticSearchHelper.getResponseFromFuture(promise.future())).thenReturn(true);
    boolean result = testScenario(reqObj, null);
    assertTrue(result);
  }

  public boolean testScenario(Request reqObj, ResponseCode errorCode) {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    subject.tell(reqObj, probe.getRef());

    if (errorCode == null) {
      Response res = probe.expectMsgClass(Duration.ofSeconds(100), Response.class);
      return null != res && res.getResponseCode() == ResponseCode.OK;
    } else {
      ProjectCommonException res =
          probe.expectMsgClass(Duration.ofSeconds(100), ProjectCommonException.class);
      return res.getErrorCode().equals(errorCode.getErrorCode())
          || res.getErrorResponseCode() == errorCode.getResponseCode();
    }
  }
}
