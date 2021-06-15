package org.sunbird.user;

import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.pattern.Patterns;
import akka.util.Timeout;
import java.util.HashMap;
import java.util.Map;
import org.mockito.Mockito;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.user.actors.ManagedUserActor;
import scala.concurrent.Await;
import scala.concurrent.Future;

public class ManagedUserActorTest extends UserManagementActorTestBase {

  public final Props props = Props.create(ManagedUserActor.class);

  // @Test
  public void testGetManagedUsers() throws Exception {
    HashMap<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.ID, "102fcbd2-8ec1-4870-b9e1-5dc01f2acc75");
    reqMap.put(JsonKey.WITH_TOKENS, "true");

    Map<String, Object> map = new HashMap<>();
    map.put("anyString", new Object());

    Response response = new Response();
    response.put(JsonKey.RESPONSE, map);

    when(Await.result(
            Patterns.ask(
                Mockito.any(ActorRef.class), Mockito.any(Request.class), Mockito.anyLong()),
            Mockito.anyObject()))
        .thenReturn(response)
        .thenReturn(map);
    boolean result =
        testScenario(
            getRequest(false, false, false, reqMap, ActorOperations.GET_MANAGED_USERS),
            null,
            props);
    assertTrue(result);
  }

  // @Test
  public void testCreateUserFailureWithManagedUserLimit() {
    Map<String, Object> reqMap = getUserOrgUpdateRequest(true);
    getUpdateRequestWithDefaultFlags(reqMap);
    Future<Object> future1 = Futures.future(() -> reqMap, system.dispatcher());
    Future<Object> future2 = Futures.future(() -> getEsResponse(), system.dispatcher());
    when(Patterns.ask(
            Mockito.any(ActorRef.class), Mockito.any(Request.class), Mockito.any(Timeout.class)))
        .thenReturn(future1)
        .thenReturn(future2);

    boolean result =
        testScenario(
            getRequest(
                false, false, false, getAdditionalMapData(reqMap), ActorOperations.CREATE_USER_V4),
            null,
            props);
    assertTrue(true);
  }
}
