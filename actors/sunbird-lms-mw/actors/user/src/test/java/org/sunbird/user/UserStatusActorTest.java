package org.sunbird.user;

import static akka.testkit.JavaTestKit.duration;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
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
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.KeyCloakConnectionProvider;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.models.user.User;
import org.sunbird.user.actors.UserStatusActor;
import org.sunbird.user.service.UserService;
import org.sunbird.user.service.impl.UserServiceImpl;

@RunWith(PowerMockRunner.class)
@PrepareForTest({UserServiceImpl.class, KeyCloakConnectionProvider.class, ServiceFactory.class})
@PowerMockIgnore("javax.management.*")
public class UserStatusActorTest {

  private static final Props props = Props.create(UserStatusActor.class);
  private static final ActorSystem system = ActorSystem.create("system");
  private static final User user = mock(User.class);
  private static final CassandraOperationImpl cassandraOperation =
      mock(CassandraOperationImpl.class);;

  @BeforeClass
  public static void beforeClass() {
    PowerMockito.mockStatic(ServiceFactory.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    Response response = createCassandraUpdateSuccessResponse();
    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(response);
  }

  @Before
  public void init() {

    UserRepresentation userRepresentation = mock(UserRepresentation.class);
    RealmResource realmResource = mock(RealmResource.class);
    Keycloak keycloak = mock(Keycloak.class);

    PowerMockito.mockStatic(UserServiceImpl.class);
    UserService userService = mock(UserService.class);
    when(UserServiceImpl.getInstance()).thenReturn(userService);

    PowerMockito.mockStatic(KeyCloakConnectionProvider.class);
    when(KeyCloakConnectionProvider.getConnection()).thenReturn(keycloak);
    when(keycloak.realm(Mockito.anyString())).thenReturn(realmResource);

    UsersResource usersResource = mock(UsersResource.class);
    when(realmResource.users()).thenReturn(usersResource);

    UserResource userResource = mock(UserResource.class);
    when(usersResource.get(Mockito.any())).thenReturn(userResource);
    when(userResource.toRepresentation()).thenReturn(userRepresentation);
    when(userService.getUserById(Mockito.anyString())).thenReturn(user);
  }

  @Test
  public void testBlockUserSuccess() {
    boolean result = testScenario(false, ActorOperations.BLOCK_USER, true, null);
    assertTrue(result);
  }

  @Test
  public void testBlockUserFailureWithUserAlreadyInactive() {
    boolean result =
        testScenario(
            true,
            ActorOperations.BLOCK_USER,
            false,
            ResponseCode.userAlreadyInactive.getErrorCode());
    assertTrue(result);
  }

  @Test
  public void testUnblockUserSuccess() {
    boolean result = testScenario(true, ActorOperations.UNBLOCK_USER, true, null);
    assertTrue(result);
  }

  @Test
  public void testUnblockUserFailureWithUserAlreadyActive() {
    boolean result =
        testScenario(
            false,
            ActorOperations.UNBLOCK_USER,
            false,
            ResponseCode.userAlreadyActive.getErrorCode());
    assertTrue(result);
  }

  private Request getRequestObject(String operation) {

    Request reqObj = new Request();
    String userId = "someUserId";
    reqObj.setOperation(operation);
    reqObj.put(JsonKey.USER_ID, userId);
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
      res = probe.expectMsgClass(duration("10 second"), Response.class);
      return (res != null && "SUCCESS".equals(res.getResult().get(JsonKey.RESPONSE)));
    } else {
      ProjectCommonException exception =
          probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
      return (((ProjectCommonException) exception).getCode().equals(expectedErrorResponse));
    }
  }
}
