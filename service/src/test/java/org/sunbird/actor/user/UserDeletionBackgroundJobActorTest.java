package org.sunbird.actor.user;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.User;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.service.user.UserRoleService;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.UserRoleServiceImpl;
import org.sunbird.service.user.impl.UserServiceImpl;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  UserService.class,
  UserServiceImpl.class,
  UserRoleService.class,
  UserRoleServiceImpl.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class UserDeletionBackgroundJobActorTest {
  private static final Props props = Props.create(UserDeletionBackgroundJobActor.class);
  private ActorSystem system = ActorSystem.create("system");

  @Before
  public void beforeEachTest() {
    UserRoleService userRoleService = PowerMockito.mock(UserRoleService.class);
    PowerMockito.mockStatic(UserRoleServiceImpl.class);
    PowerMockito.when(UserRoleServiceImpl.getInstance()).thenReturn(userRoleService);
    PowerMockito.when(
            userRoleService.getUserRoles(Mockito.anyString(), Mockito.any(RequestContext.class)))
        .thenReturn(new ArrayList<>());

    UserServiceImpl userService = PowerMockito.mock(UserServiceImpl.class);
    PowerMockito.mockStatic(UserServiceImpl.class);
    PowerMockito.when(UserServiceImpl.getInstance()).thenReturn(userService);

    User user = new User();
    user.setUserId("46545665465465");
    user.setIsDeleted(false);
    user.setFirstName("firstName");
    user.setStatus(2);
    user.setRootOrgId("rootOrg");
    PowerMockito.when(userService.getUserById(Mockito.anyString(), Mockito.any())).thenReturn(user);
  }

  @Test
  public void callUserDeletionTest() {
    Map<String, Object> userData = new HashMap<>();
    userData.put(JsonKey.USER_ID, "userId");

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.getRequest().putAll(userData);
    reqObj.setOperation("inputKafkaTopic");

    subject.tell(reqObj, probe.getRef());
    probe.expectNoMessage();
  }
}
