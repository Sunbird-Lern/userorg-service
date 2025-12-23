package org.sunbird.actor.user;

import java.time.Duration;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.*;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.dispatch.Futures;
import org.apache.pekko.testkit.javadsl.TestKit;
import java.util.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
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
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.User;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.user.UserExternalIdentityService;
import org.sunbird.service.user.UserRoleService;
import org.sunbird.service.user.impl.UserExternalIdentityServiceImpl;
import org.sunbird.service.user.impl.UserRoleServiceImpl;
import org.sunbird.sso.KeyCloakConnectionProvider;
import org.sunbird.sso.SSOManager;
import org.sunbird.sso.SSOServiceFactory;
import org.sunbird.util.ProjectUtil;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  KeyCloakConnectionProvider.class,
  ServiceFactory.class,
  SSOServiceFactory.class,
  SSOManager.class,
  ElasticSearchRestHighImpl.class,
  ElasticSearchService.class,
  EsClientFactory.class,
  ServiceFactory.class,
  CassandraOperationImpl.class,
  UserExternalIdentityService.class,
  UserExternalIdentityServiceImpl.class,
  UserRoleService.class,
  UserRoleServiceImpl.class,
  ProjectUtil.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class UserStatusActorTest {

  private static final Props props = Props.create(UserStatusActor.class);
  private static final ActorSystem system = ActorSystem.create("system");
  private static final User user = mock(User.class);
  private static CassandraOperationImpl cassandraOperation = mock(CassandraOperationImpl.class);

  @BeforeClass
  public static void beforeClass() {
    PowerMockito.mockStatic(ServiceFactory.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    Response response = createCassandraUpdateSuccessResponse();
    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(response);

    PowerMockito.mockStatic(EsClientFactory.class);
    ElasticSearchService esService = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
    Promise<Boolean> promise2 = Futures.promise();
    promise2.success(true);
    when(esService.update(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.any(RequestContext.class)))
        .thenReturn(promise2.future());
  }

  @Before
  public void init() {

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

    UserRoleService userRoleService = PowerMockito.mock(UserRoleService.class);
    PowerMockito.mockStatic(UserRoleServiceImpl.class);
    PowerMockito.when(UserRoleServiceImpl.getInstance()).thenReturn(userRoleService);

    PowerMockito.mockStatic(ProjectUtil.class);
    when(ProjectUtil.getConfigValue(Mockito.anyString())).thenReturn("public");

    List<Map<String, Object>> roleList = new ArrayList<>();
    Map<String, Object> orgMap = new HashMap<>();
    orgMap.put(JsonKey.ID, "ORGANISATION_ID");
    orgMap.put(JsonKey.USER_ID, "USER_ID");
    orgMap.put(JsonKey.ROLE, "public");
    orgMap.put(
        JsonKey.SCOPE,
        "[{\"organisationId\":\"ORGANISATION_ID1\"},{\"organisationId\":\"ORGANISATION_ID\"}]");
    roleList.add(orgMap);

    when(userRoleService.getUserRoles(Mockito.anyString(), Mockito.any())).thenReturn(roleList);
  }

  @Test
  public void testBlockUserSuccess() {
    try {
      Response response2 = new Response();
      Map<String, Object> user = new HashMap<>();
      user.put(JsonKey.ID, "46545665465465");
      user.put(JsonKey.IS_DELETED, false);
      user.put(JsonKey.FIRST_NAME, "firstName");
      user.put(JsonKey.STATUS, 1);
      List<Map<String, Object>> userList = new ArrayList<>();
      userList.add(user);
      response2.getResult().put(JsonKey.RESPONSE, userList);
      when(cassandraOperation.getRecordById(
              Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
          .thenReturn(response2);

      PowerMockito.mockStatic(SSOServiceFactory.class);
      SSOManager ssoManager = PowerMockito.mock(SSOManager.class);
      PowerMockito.when(SSOServiceFactory.getInstance()).thenReturn(ssoManager);
      PowerMockito.when(ssoManager.deactivateUser(Mockito.anyMap(), Mockito.any()))
          .thenReturn(JsonKey.SUCCESS);
      boolean result = testScenario(false, ActorOperations.BLOCK_USER, true, null);
      assertTrue(result);
    } catch (Exception ex) {
      assertNotNull(ex);
    }
  }

  @Test
  public void testBlockUserFailureWithUserAlreadyInactive() {
    try {
      Response response2 = new Response();
      Map<String, Object> user = new HashMap<>();
      user.put(JsonKey.ID, "46545665465465");
      user.put(JsonKey.IS_DELETED, true);
      user.put(JsonKey.FIRST_NAME, "firstName");
      user.put(JsonKey.STATUS, 0);
      List<Map<String, Object>> userList = new ArrayList<>();
      userList.add(user);
      response2.getResult().put(JsonKey.RESPONSE, userList);
      when(cassandraOperation.getRecordById(
              Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
          .thenReturn(response2);
      boolean result =
          testScenario(
              true, ActorOperations.BLOCK_USER, false, ResponseCode.userStatusError.getErrorCode());
      assertTrue(result);
    } catch (Exception ex) {
      assertNotNull(ex);
    }
  }

  @Test
  public void testUnblockUserSuccess() {
    try {
      Response response2 = new Response();
      Map<String, Object> user = new HashMap<>();
      user.put(JsonKey.ID, "46545665465465");
      user.put(JsonKey.IS_DELETED, true);
      user.put(JsonKey.FIRST_NAME, "firstName");
      user.put(JsonKey.STATUS, 0);
      List<Map<String, Object>> userList = new ArrayList<>();
      userList.add(user);
      response2.getResult().put(JsonKey.RESPONSE, userList);
      when(cassandraOperation.getRecordById(
              Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
          .thenReturn(response2);

      PowerMockito.mockStatic(SSOServiceFactory.class);
      SSOManager ssoManager = PowerMockito.mock(SSOManager.class);
      PowerMockito.when(SSOServiceFactory.getInstance()).thenReturn(ssoManager);
      PowerMockito.when(ssoManager.activateUser(Mockito.anyMap(), Mockito.any()))
          .thenReturn(JsonKey.SUCCESS);
      boolean result = testScenario(true, ActorOperations.UNBLOCK_USER, true, null);
      assertTrue(result);
    } catch (Exception ex) {
      assertNotNull(ex);
    }
  }

  @Test
  public void testUnblockUserFailureWithUserAlreadyActive() {
    try {
      Response response2 = new Response();
      Map<String, Object> user = new HashMap<>();
      user.put(JsonKey.ID, "46545665465465");
      user.put(JsonKey.IS_DELETED, false);
      user.put(JsonKey.FIRST_NAME, "firstName");
      user.put(JsonKey.STATUS, 1);
      List<Map<String, Object>> userList = new ArrayList<>();
      userList.add(user);
      response2.getResult().put(JsonKey.RESPONSE, userList);
      when(cassandraOperation.getRecordById(
              Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
          .thenReturn(response2);
      boolean result =
          testScenario(
              false,
              ActorOperations.UNBLOCK_USER,
              false,
              ResponseCode.userStatusError.getErrorCode());
      assertTrue(result);
    } catch (Exception ex) {
      assertNotNull(ex);
    }
  }

  @Test
  public void testWithInvalidRequest() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request request = new Request();
    request.setOperation("invalidOperation");
    subject.tell(request, probe.getRef());
    ProjectCommonException exception =
        probe.expectMsgClass(Duration.ofSeconds(10), ProjectCommonException.class);
    Assert.assertNotNull(exception);
  }

  @Test
  public void testDeleteUserSuccess() {
    try {
      Response userDetailsById = new Response();
      Map<String, Object> user = new HashMap<>();
      user.put(JsonKey.ID, "46545665465465");
      user.put(JsonKey.IS_DELETED, false);
      user.put(JsonKey.FIRST_NAME, "firstName");
      user.put(JsonKey.STATUS, 1);
      user.put(JsonKey.EMAIL, "test@test.com");
      user.put(JsonKey.PHONE, "9876543210");
      user.put(JsonKey.EXTERNAL_ID, "9876");
      List<Map<String, Object>> userList = new ArrayList<>();
      userList.add(user);
      userDetailsById.getResult().put(JsonKey.RESPONSE, userList);
      when(cassandraOperation.getRecordById(
              Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
          .thenReturn(userDetailsById);

      PowerMockito.mockStatic(SSOServiceFactory.class);
      SSOManager ssoManager = PowerMockito.mock(SSOManager.class);
      PowerMockito.when(SSOServiceFactory.getInstance()).thenReturn(ssoManager);
      PowerMockito.when(ssoManager.removeUser(Mockito.anyMap(), Mockito.any()))
          .thenReturn(JsonKey.SUCCESS);

      when(cassandraOperation.deleteRecord(
              Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
          .thenReturn(userDetailsById);

      Map<String, String> userExtIdMap = new HashMap<>();
      userExtIdMap.put("provider", "0132818330295992324");
      userExtIdMap.put("idtype", "teacherId");
      userExtIdMap.put("externalid", "cedd456");
      userExtIdMap.put("userid", "46545665465465");

      List<Map<String, String>> userExtIdRespList = new ArrayList<>();
      userExtIdRespList.add(userExtIdMap);

      UserExternalIdentityServiceImpl externalIdentityService =
          PowerMockito.mock(UserExternalIdentityServiceImpl.class);
      PowerMockito.whenNew(UserExternalIdentityServiceImpl.class)
          .withNoArguments()
          .thenReturn(externalIdentityService);

      when(externalIdentityService.getUserExternalIds(Mockito.anyString(), Mockito.any()))
          .thenReturn(userExtIdRespList);

      when(externalIdentityService, "deleteUserExternalIds", Mockito.any(), Mockito.any())
          .thenReturn(true);

      boolean result = testScenario(false, ActorOperations.DELETE_USER, true, null);
      assertTrue(result);
    } catch (Exception ex) {
      assertNotNull(ex);
    }
  }

  private Request getRequestObject(String operation) {
    Request reqObj = new Request();
    String userId = "someUserId";
    reqObj.setOperation(operation);
    reqObj.put(JsonKey.USER_ID, userId);
    reqObj.setContext(new HashMap<>());
    reqObj.setRequestContext(new RequestContext());
    return reqObj;
  }

  private static Response createCassandraUpdateSuccessResponse() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    return response;
  }

  private boolean testScenario(
      boolean isDeleted,
      ActorOperations operation,
      boolean isSuccess,
      String expectedErrorResponse) {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    when(user.getIsDeleted()).thenReturn(isDeleted);
    subject.tell(getRequestObject(operation.getValue()), probe.getRef());
    Response res;
    if (isSuccess) {
      res = probe.expectMsgClass(Duration.ofSeconds(100), Response.class);
      return (res != null && "SUCCESS".equals(res.getResult().get(JsonKey.RESPONSE)));
    } else {
      String errString = ActorOperations.getOperationCodeByActorOperation(operation.getValue());
      ProjectCommonException exception =
          probe.expectMsgClass(Duration.ofSeconds(100), ProjectCommonException.class);
      return (exception.getErrorCode().equals("UOS_" + errString + expectedErrorResponse));
    }
  }
}
