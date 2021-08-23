package org.sunbird.actor.role;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.actor.service.BaseMWService;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static akka.testkit.JavaTestKit.duration;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  BaseMWService.class,
  CassandraOperationImpl.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class GetUserRoleTest {
  private ActorSystem system = ActorSystem.create("system");
  private final Props props = Props.create(UserRoleActor.class);

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(BaseMWService.class);
    PowerMockito.mockStatic(ServiceFactory.class);
    CassandraOperationImpl cassandraOperation = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(cassandraOperation.getRecordById(
      Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
      .thenReturn(getCassandraUserRoleResponse());
    PowerMockito.when(cassandraOperation.getRecordsByPrimaryKeys(
      Mockito.anyString(), Mockito.anyString(), Mockito.anyList(), Mockito.anyString(), Mockito.any(RequestContext.class))).thenReturn(getRecordsByIds(false));

  }

  @Test
  public void testGetUserRoles() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request request = new Request();
    request.getRequest().put(JsonKey.USER_ID,"userId");
    request.getContext().put(JsonKey.FIELDS,"orgName");
    request.setOperation("getUserRolesById");
    request.setRequestContext(new RequestContext());
    subject.tell(request, probe.getRef());
    Response response = probe.expectMsgClass(duration("100 second"), Response.class);
    Assert.assertNotNull(response);
  }

  private Response getRecordsByIds(boolean empty) {
    Response res = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    if (!empty) {
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.ID, "id1");
      map.put(JsonKey.IS_DELETED, true);
      map.put(JsonKey.CHANNEL, "channel1");
      map.put(JsonKey.IS_TENANT, true);
      map.put(JsonKey.ORG_NAME,"OrgName1");
      list.add(map);

      Map<String, Object> map2 = new HashMap<>();
      map2.put(JsonKey.ID, "id2");
      map2.put(JsonKey.IS_DELETED, true);
      map2.put(JsonKey.CHANNEL, "channel2");
      map2.put(JsonKey.IS_TENANT, true);
      map2.put(JsonKey.ORG_NAME,"OrgName2");
      list.add(map2);
    }
    res.put(JsonKey.RESPONSE, list);
    return res;
  }

  private Response getCassandraUserRoleResponse() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> orgMap = new HashMap<>();
    orgMap.put(JsonKey.ID, "ORGANISATION_ID");
    orgMap.put(JsonKey.USER_ID, "USER_ID");
    orgMap.put(JsonKey.ROLE, "anyRole1");
    orgMap.put(
      JsonKey.SCOPE,
      "[{\"organisationId\":\"id1\"},{\"organisationId\":\"id2\"}]");
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
    orgMap.put(
      JsonKey.SCOPE,
      "[{\"organisationId\":\"id1\"},{\"organisationId\":\"id2\"}]");
    list.add(orgMap);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }

}
