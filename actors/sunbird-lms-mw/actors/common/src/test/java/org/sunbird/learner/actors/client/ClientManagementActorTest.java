package org.sunbird.learner.actors.client;

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
import org.junit.Test;
import org.junit.runner.RunWith;
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
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ServiceFactory.class, Util.class})
@PowerMockIgnore({"javax.management.*"})
public class ClientManagementActorTest {

  private static ActorSystem system = ActorSystem.create("system");
  private static final Props props = Props.create(ClientManagementActor.class);
  private static final String masterKey = "anyMasterKey";
  private static final String clientId = "anyClientId";
  private static final String clientName = "anyClientName";
  private static final String channel = "anyChannel";
  private static final String id = "anyId";
  private static final CassandraOperationImpl cassandraOperation =
      mock(CassandraOperationImpl.class);
  private Map<String, Object> request = new HashMap<>();
  private Map<String, Object> orgMap = new HashMap<>();

  @Before
  public void init() {
    PowerMockito.mockStatic(ServiceFactory.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(getSuccessResponse());
    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(getSuccessResponse());
    request.clear();
    orgMap.clear();
  }

  @Test
  public void checkTelemetryKeyFailure() throws Exception {

    String telemetryEnvKey = "masterKey";
    PowerMockito.mockStatic(Util.class);
    PowerMockito.doNothing()
        .when(
            Util.class,
            "initializeContext",
            Mockito.any(Request.class),
            Mockito.eq(telemetryEnvKey));

    request.put(JsonKey.CLIENT_NAME, clientName);
    orgMap.put(JsonKey.CLIENT_ID, clientId);
    boolean result = testScenario(request, orgMap, ActorOperations.REGISTER_CLIENT, null);
    Assert.assertTrue(!(telemetryEnvKey.charAt(0) >= 65 && telemetryEnvKey.charAt(0) <= 90));
  }

  @Test
  public void testRegisterClientSuccess() {

    request.put(JsonKey.CLIENT_NAME, clientName);
    orgMap.put(JsonKey.CLIENT_ID, clientId);
    boolean result = testScenario(request, orgMap, ActorOperations.REGISTER_CLIENT, null);
    Assert.assertTrue(result);
  }

  @Test
  public void testRegisterClientFailureWithDuplicateChannel() {

    orgMap.put(JsonKey.MASTER_KEY, masterKey);
    request.put(JsonKey.CLIENT_NAME, clientName);
    request.put(JsonKey.CHANNEL, channel);
    boolean result =
        testScenario(
            request,
            orgMap,
            ActorOperations.REGISTER_CLIENT,
            ResponseCode.channelUniquenessInvalid);
    Assert.assertTrue(result);
  }

  @Test
  public void testRegisterClientFailureWithInvalidClientName() {

    orgMap.put(JsonKey.ID, id);
    request.put(JsonKey.CLIENT_NAME, clientName);
    boolean result =
        testScenario(
            request, orgMap, ActorOperations.REGISTER_CLIENT, ResponseCode.invalidClientName);
    Assert.assertTrue(result);
  }

  @Test
  public void testGetClientKeySuccess() {

    request.put(JsonKey.CLIENT_ID, clientId);
    request.put(JsonKey.TYPE, JsonKey.CLIENT_ID);
    orgMap.put(JsonKey.ID, id);
    boolean result = testScenario(request, orgMap, ActorOperations.GET_CLIENT_KEY, null);
    Assert.assertTrue(result);
  }

  @Test
  public void testGetClientKeyFailureWithInvalidClientIdType() {

    request.put(JsonKey.CLIENT_ID, clientId);
    request.put(JsonKey.TYPE, JsonKey.CLIENT_ID);
    boolean result =
        testScenario(
            request, orgMap, ActorOperations.GET_CLIENT_KEY, ResponseCode.invalidRequestData);
    Assert.assertTrue(result);
  }

  @Test
  public void testGetClientKeyFailureWithInvalidChannelType() {

    request.put(JsonKey.CLIENT_ID, clientId);
    request.put(JsonKey.TYPE, JsonKey.CHANNEL);
    boolean result =
        testScenario(
            request, orgMap, ActorOperations.GET_CLIENT_KEY, ResponseCode.invalidRequestData);
    Assert.assertTrue(result);
  }

  @Test
  public void testGetClientKeyFailureWithoutType() {

    request.put(JsonKey.CLIENT_ID, clientId);
    boolean result =
        testScenario(
            request, orgMap, ActorOperations.GET_CLIENT_KEY, ResponseCode.invalidRequestData);
    Assert.assertTrue(result);
  }

  @Test
  public void testUpdateClientKeySuccess() {

    orgMap.put(JsonKey.MASTER_KEY, masterKey);
    boolean result = testScenario(orgMap, orgMap, ActorOperations.UPDATE_CLIENT_KEY, null);
    Assert.assertTrue(result);
  }

  @Test
  public void testUpdateClientKeyFailureWithInvalidRequestedData() {

    request.put(JsonKey.CLIENT_ID, clientId);
    request.put(JsonKey.MASTER_KEY, masterKey);
    boolean result =
        testScenario(
            request, orgMap, ActorOperations.UPDATE_CLIENT_KEY, ResponseCode.invalidRequestData);
    Assert.assertTrue(result);
  }

  @Test
  public void testUpdateClientKeyFailureWithDuplicateChannel() {

    request.put(JsonKey.CLIENT_ID, clientId);
    request.put(JsonKey.CHANNEL, channel);
    orgMap.put(JsonKey.CHANNEL, "differentChannel");
    boolean result =
        testScenario(
            request,
            orgMap,
            ActorOperations.UPDATE_CLIENT_KEY,
            ResponseCode.channelUniquenessInvalid);
    Assert.assertTrue(result);
  }

  private static Response getSuccessResponse() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    return response;
  }

  private static Response getCassandraResponse(Map<String, Object> orgMap) {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    if (!orgMap.isEmpty()) {
      list.add(orgMap);
    }
    response.put(JsonKey.RESPONSE, list);
    return response;
  }

  private boolean testScenario(
      Map<String, Object> request,
      Map<String, Object> orgMap,
      ActorOperations actorOperations,
      ResponseCode errorResponse) {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request actorMessage = new Request();
    actorMessage.setRequest(request);
    actorMessage.setOperation(actorOperations.getValue());
    when(cassandraOperation.getRecordsByProperty(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(getCassandraResponse(orgMap));
    subject.tell(actorMessage, probe.getRef());
    if (errorResponse == null) {
      Response res = probe.expectMsgClass(duration("10 second"), Response.class);
      return null != res && res.getResponseCode() == ResponseCode.OK;
    } else {
      ProjectCommonException res =
          probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);

      return res.getCode().equals(errorResponse.getErrorCode())
          || res.getResponseCode() == errorResponse.getResponseCode();
    }
  }
}
