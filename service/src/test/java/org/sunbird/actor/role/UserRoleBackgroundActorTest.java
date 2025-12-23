package org.sunbird.actor.role;

import java.time.Duration;


import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.testkit.javadsl.TestKit;
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
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.service.user.UserRoleService;
import org.sunbird.service.user.impl.UserRoleServiceImpl;

@RunWith(PowerMockRunner.class)
@PrepareForTest({UserRoleService.class, UserRoleServiceImpl.class})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class UserRoleBackgroundActorTest {

  private static final Props props = Props.create(UserRoleBackgroundActor.class);
  private static ActorSystem system = ActorSystem.create("system");

  @Before
  public void beforeTest() {
    UserRoleService userRoleService = PowerMockito.mock(UserRoleService.class);
    PowerMockito.mockStatic(UserRoleServiceImpl.class);
    PowerMockito.when(UserRoleServiceImpl.getInstance()).thenReturn(userRoleService);
    PowerMockito.when(
            userRoleService.updateUserRoleToES(
                Mockito.anyString(), Mockito.anyMap(), Mockito.any(RequestContext.class)))
        .thenReturn(true);
  }

  @Test
  public void updateUserRoleToESTest() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.UPDATE_USER_ROLES_ES.getValue());
    List<Map<String, Object>> roles = new ArrayList<>();
    Map<String, Object> role = new HashMap<>();
    role.put(JsonKey.ROLE, "role");
    roles.add(role);
    reqObj.getRequest().put(JsonKey.ROLES, roles);
    reqObj.getRequest().put(JsonKey.TYPE, JsonKey.USER);
    reqObj.getRequest().put(JsonKey.USER_ID, "userId");
    subject.tell(reqObj, probe.getRef());
    probe.expectNoMessage();
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
}
