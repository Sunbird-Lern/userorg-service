package org.sunbird.actor.notes;

import java.time.Duration;

import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.dispatch.Futures;
import org.apache.pekko.testkit.javadsl.TestKit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
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
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  ElasticSearchRestHighImpl.class,
  EsClientFactory.class,
  ElasticSearchHelper.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class NotesManagementActorTest {

  private static String userId = "userId-example";
  private static String noteId = "";
  private ActorSystem system = ActorSystem.create("system");
  private static final Props props = Props.create(NotesManagementActor.class);
  private static CassandraOperationImpl cassandraOperation;
  private ElasticSearchService esUtil;

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(EsClientFactory.class);
    esUtil = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esUtil);
    Promise<Boolean> booleanPromise = Futures.promise();
    booleanPromise.success(true);
    when(esUtil.update(Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(booleanPromise.future());
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getSuccessResponse());
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getSuccessResponse());
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(getUserResponse());
  }

  @Ignore
  public void testCreateNoteSuccess() {
    Request req = new Request();
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.USER_ID, userId);
    req.setRequest(reqMap);
    req.setOperation(ActorOperations.CREATE_NOTE.getValue());
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(reqMap);
    when(esUtil.getDataByIdentifier(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getSuccessResponse());
    boolean result = testScenario(req, null);
    assertTrue(result);
  }

  @Test
  public void testCreateNoteFailure() {
    Request req = new Request();
    Map<String, Object> reqMap = new HashMap<>();
    req.setRequest(reqMap);
    req.setOperation(ActorOperations.CREATE_NOTE.getValue());
    boolean result = testScenario(req, ResponseCode.invalidParameter);
    assertTrue(result);
  }

  @Test
  public void testCreateNoteFailureWithInvalidUserId() {
    Request req = new Request();
    Map<String, Object> reqMap = new HashMap<>();
    req.setRequest(reqMap);
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(new HashMap<>());
    when(esUtil.getDataByIdentifier(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
    req.setOperation(ActorOperations.CREATE_NOTE.getValue());
    boolean result = testScenario(req, ResponseCode.invalidParameter);
    assertTrue(result);
  }

  @Ignore
  public void testUpdateNoteSuccess() {
    Request req = new Request();
    req.getContext().put(JsonKey.REQUESTED_BY, userId);
    req.getContext().put(JsonKey.NOTE_ID, noteId);
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.USER_ID, userId);
    req.setRequest(reqMap);
    req.setOperation(ActorOperations.UPDATE_NOTE.getValue());
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(reqMap);
    when(esUtil.getDataByIdentifier(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future())
        .thenReturn(promise.future());
    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getSuccessResponse());
    boolean result = testScenario(req, null);
    assertTrue(result);
  }

  @Test
  public void testSearchNoteSuccess() {
    Request req = new Request();
    req.getContext().put(JsonKey.REQUESTED_BY, userId);
    req.getContext().put(JsonKey.NOTE_ID, noteId);
    Map<String, Object> reqMap = new HashMap<>();
    req.setRequest(reqMap);
    req.setOperation(ActorOperations.SEARCH_NOTE.getValue());
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(reqMap);
    when(esUtil.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
    boolean result = testScenario(req, null);
    assertTrue(result);
  }

  @Test
  public void testGetNoteFailureWithInvalidUserId() {
    Request req = new Request();
    Map<String, Object> reqMap = new HashMap<>();
    req.setRequest(reqMap);
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(new HashMap<>());
    when(esUtil.getDataByIdentifier(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
    req.setOperation(ActorOperations.GET_NOTE.getValue());
    boolean result = testScenario(req, ResponseCode.invalidParameterValue);
    assertTrue(result);
  }

  @Ignore
  public void testDeleteNoteSuccess() {
    Request req = new Request();
    req.getContext().put(JsonKey.REQUESTED_BY, userId);
    req.getContext().put(JsonKey.NOTE_ID, noteId);
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.USER_ID, userId);
    req.setRequest(reqMap);
    req.setOperation(ActorOperations.DELETE_NOTE.getValue());
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(reqMap);
    when(esUtil.getDataByIdentifier(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future())
        .thenReturn(promise.future());
    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getSuccessResponse());
    boolean result = testScenario(req, null);
    assertTrue(result);
  }

  private boolean testScenario(Request reqObj, ResponseCode errorCode) {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    subject.tell(reqObj, probe.getRef());

    if (errorCode == null) {
      Response res = probe.expectMsgClass(Duration.ofSeconds(100), Response.class);
      return null != res && res.getResponseCode() == ResponseCode.OK;
    } else {
      ProjectCommonException res =
          probe.expectMsgClass(Duration.ofSeconds(100), ProjectCommonException.class);
      return res.getResponseCode().name().equals(errorCode.name())
          || res.getErrorResponseCode() == errorCode.getResponseCode();
    }
  }

  private static Response getSuccessResponse() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    return response;
  }

  private static Response getUserResponse() {
    Response response = new Response();
    Map<String, Object> user = new HashMap<>();
    user.put(JsonKey.ID, "46545665465465");
    user.put(JsonKey.IS_DELETED, false);
    user.put(JsonKey.FIRST_NAME, "firstName");
    List<Map<String, Object>> userList = new ArrayList<>();
    userList.add(user);
    response.getResult().put(JsonKey.RESPONSE, userList);
    return response;
  }
}
