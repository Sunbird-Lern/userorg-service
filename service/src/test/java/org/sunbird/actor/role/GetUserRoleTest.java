package org.sunbird.actor.role;

import java.time.Duration;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

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
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ServiceFactory.class, CassandraOperationImpl.class})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class GetUserRoleTest {
  private ActorSystem system = ActorSystem.create("system");
  private final Props props = Props.create(FetchUserRoleActor.class);

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(ServiceFactory.class);
    CassandraOperationImpl cassandraOperation = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getCassandraUserRoleResponse())
        .thenReturn(getUserResponse());

    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(getUserResponse());
  }

  @Test
  public void testGetUserRoles() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request request = new Request();
    request.getRequest().put(JsonKey.USER_ID, "userId");
    request.setOperation("getUserRolesById");
    request.setRequestContext(new RequestContext());
    subject.tell(request, probe.getRef());
    Response response = probe.expectMsgClass(Duration.ofSeconds(100), Response.class);
    Assert.assertNotNull(response);
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

  private Response getUserResponse() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList<>();

    Map<String, Object> userMap = new HashMap<>();
    userMap.put(JsonKey.FIRST_NAME, "firstName");
    userMap.put(JsonKey.LAST_NAME, "lastName");
    list.add(userMap);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }

  private Response getCassandraUserRoleResponse() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList<>();

    Map<String, Object> orgMap = new HashMap<>();
    orgMap.put(JsonKey.ID, "ORGANISATION_ID");
    orgMap.put(JsonKey.USER_ID, "USER_ID");
    orgMap.put(JsonKey.ROLE, "anyRole1");
    orgMap.put(JsonKey.SCOPE, "[{\"organisationId\":\"id1\"},{\"organisationId\":\"id2\"}]");
    list.add(orgMap);
    orgMap = new HashMap<>();
    orgMap.put(JsonKey.ID, "ORGANISATION_ID");
    orgMap.put(JsonKey.USER_ID, "USER_ID");
    orgMap.put(JsonKey.ROLE, "anyRole2");
    orgMap.put(JsonKey.SCOPE, "[{\"organisationId\":\"id2\"}]");
    list.add(orgMap);
    orgMap = new HashMap<>();
    orgMap.put(JsonKey.ID, "ORGANISATION_ID");
    orgMap.put(JsonKey.USER_ID, "USER_ID");
    orgMap.put(JsonKey.ROLE, "anyRole3");
    orgMap.put(JsonKey.SCOPE, "[{\"organisationId\":\"id1\"},{\"organisationId\":\"id2\"}]");
    list.add(orgMap);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }
}
