package org.sunbird.learner.actors;

import static akka.testkit.JavaTestKit.duration;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.testkit.javadsl.TestKit;
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
import org.sunbird.actor.router.RequestRouter;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  Util.class,
  RequestRouter.class,
  ElasticSearchRestHighImpl.class,
  EsClientFactory.class
})
@PowerMockIgnore({"javax.management.*", "javax.crypto.*", "javax.net.ssl.*", "javax.security.*"})
public class NotesManagementActorTest {

  private static String userId = "userId-example";
  private static String noteId = "";
  private ActorSystem system = ActorSystem.create("system");
  private static final Props props = Props.create(NotesManagementActor.class);
  private static CassandraOperationImpl cassandraOperation;
  private ElasticSearchService esUtil;

  @Before
  public void beforeEachTest() {
    ActorRef actorRef = mock(ActorRef.class);
    PowerMockito.mockStatic(RequestRouter.class);
    when(RequestRouter.getActor(Mockito.anyString())).thenReturn(actorRef);
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    esUtil = mock(ElasticSearchRestHighImpl.class);
    PowerMockito.mockStatic(EsClientFactory.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esUtil);
  }

  @Test
  public void testCreateNoteSuccess() {
    Request req = new Request();
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.USER_ID, userId);
    req.setRequest(reqMap);
    req.setOperation(ActorOperations.CREATE_NOTE.getValue());
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(reqMap);
    when(esUtil.getDataByIdentifier(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(promise.future());
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
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
    boolean result = testScenario(req, ResponseCode.invalidUserId);
    assertTrue(result);
  }

  @Test
  public void testCreateNoteFailureWithInvalidUserId() {
    Request req = new Request();
    Map<String, Object> reqMap = new HashMap<>();
    req.setRequest(reqMap);
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(new HashMap<>());
    when(esUtil.getDataByIdentifier(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(promise.future());
    req.setOperation(ActorOperations.CREATE_NOTE.getValue());
    boolean result = testScenario(req, ResponseCode.invalidUserId);
    assertTrue(result);
  }

  @Test
  public void testUpdateNoteFailure() {
    Request req = new Request();
    req.getContext().put(JsonKey.USER_ID, userId);
    req.getContext().put(JsonKey.NOTE_ID, noteId);
    Map<String, Object> reqMap = new HashMap<>();
    req.setRequest(reqMap);
    req.setOperation(ActorOperations.UPDATE_NOTE.getValue());
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(new HashMap<>());
    when(esUtil.getDataByIdentifier(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(promise.future());
    boolean result = testScenario(req, ResponseCode.unAuthorized);
    assertTrue(result);
  }

  @Test
  public void testUpdateNoteFailurewithUserIdMismatch() {
    Request req = new Request();
    req.getContext().put(JsonKey.REQUESTED_BY, userId);
    req.getContext().put(JsonKey.NOTE_ID, noteId);
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.USER_ID, "misMatch");
    req.setRequest(reqMap);
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(reqMap);

    req.setOperation(ActorOperations.UPDATE_NOTE.getValue());
    when(esUtil.getDataByIdentifier(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(promise.future());
    boolean result = testScenario(req, ResponseCode.errorForbidden);
    assertTrue(result);
  }

  @Test
  public void testUpdateNoteFailurewithEmptyNote() {
    Request req = new Request();
    req.getContext().put(JsonKey.REQUESTED_BY, userId);
    req.getContext().put(JsonKey.NOTE_ID, noteId);
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.USER_ID, userId);
    req.setRequest(reqMap);
    req.setOperation(ActorOperations.UPDATE_NOTE.getValue());
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(reqMap);
    Promise<Map<String, Object>> promiseAny = Futures.promise();
    promiseAny.success(new HashMap<>());
    when(esUtil.getDataByIdentifier(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(promise.future())
        .thenReturn(promiseAny.future());
    boolean result = testScenario(req, ResponseCode.invalidNoteId);
    assertTrue(result);
  }

  @Test
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
    when(esUtil.getDataByIdentifier(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(promise.future())
        .thenReturn(promise.future());
    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
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
    when(esUtil.search(Mockito.any(), Mockito.anyString())).thenReturn(promise.future());
    boolean result = testScenario(req, null);
    assertTrue(result);
  }

  @Test
  public void testGetNoteFailurewithUserIdMismatch() {
    Request req = new Request();
    req.getContext().put(JsonKey.REQUESTED_BY, userId);
    req.getContext().put(JsonKey.NOTE_ID, noteId);
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.USER_ID, "misMatch");
    req.setRequest(reqMap);
    req.setOperation(ActorOperations.GET_NOTE.getValue());
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(reqMap);

    when(esUtil.getDataByIdentifier(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(promise.future());
    boolean result = testScenario(req, ResponseCode.errorForbidden);
    assertTrue(result);
  }

  @Test
  public void testGetNoteFailureWithInvalidUserId() {
    Request req = new Request();
    Map<String, Object> reqMap = new HashMap<>();
    req.setRequest(reqMap);
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(new HashMap<>());
    when(esUtil.getDataByIdentifier(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(promise.future());
    req.setOperation(ActorOperations.GET_NOTE.getValue());
    boolean result = testScenario(req, ResponseCode.invalidParameterValue);
    assertTrue(result);
  }

  @Test
  public void testGetNoteFailureWithInvalidNoteId() {
    Request req = new Request();
    Map<String, Object> reqMap = new HashMap<>();
    req.getContext().put(JsonKey.REQUESTED_BY, userId);
    req.getContext().put(JsonKey.NOTE_ID, noteId);
    reqMap.put(JsonKey.USER_ID, userId);
    reqMap.put(JsonKey.COUNT, 0L);
    req.setRequest(reqMap);
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(reqMap);
    when(esUtil.getDataByIdentifier(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(promise.future());
    when(esUtil.search(Mockito.any(), Mockito.anyString())).thenReturn(promise.future());
    req.setOperation(ActorOperations.GET_NOTE.getValue());
    boolean result = testScenario(req, ResponseCode.invalidNoteId);
    assertTrue(result);
  }

  @Test
  public void testGetNoteSuccess() {
    Request req = new Request();
    Map<String, Object> reqMap = new HashMap<>();
    req.getContext().put(JsonKey.REQUESTED_BY, userId);
    req.getContext().put(JsonKey.NOTE_ID, noteId);
    reqMap.put(JsonKey.USER_ID, userId);
    reqMap.put(JsonKey.COUNT, 1L);
    req.setRequest(reqMap);
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(reqMap);
    when(esUtil.getDataByIdentifier(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(promise.future());
    when(esUtil.search(Mockito.any(), Mockito.anyString())).thenReturn(promise.future());
    req.setOperation(ActorOperations.GET_NOTE.getValue());
    boolean result = testScenario(req, null);
    assertTrue(result);
  }

  @Test
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
    when(esUtil.getDataByIdentifier(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(promise.future())
        .thenReturn(promise.future());
    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(getSuccessResponse());
    boolean result = testScenario(req, null);
    assertTrue(result);
  }

  @Test
  public void testDeleteNoteFailurewithEmptyNote() {
    Request req = new Request();
    req.getContext().put(JsonKey.REQUESTED_BY, userId);
    req.getContext().put(JsonKey.NOTE_ID, noteId);
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.USER_ID, userId);
    req.setRequest(reqMap);
    req.setOperation(ActorOperations.DELETE_NOTE.getValue());
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(reqMap);
    Promise<Map<String, Object>> promise_any = Futures.promise();
    promise_any.success(new HashMap<>());
    when(esUtil.getDataByIdentifier(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(promise.future())
        .thenReturn(promise_any.future());
    boolean result = testScenario(req, ResponseCode.invalidNoteId);
    assertTrue(result);
  }

  @Test
  public void testDeleteNoteFailurewithUserIdMismatch() {
    Request req = new Request();
    req.getContext().put(JsonKey.REQUESTED_BY, userId);
    req.getContext().put(JsonKey.NOTE_ID, noteId);
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.USER_ID, "misMatch");
    req.setRequest(reqMap);
    req.setOperation(ActorOperations.DELETE_NOTE.getValue());
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(reqMap);
    when(esUtil.getDataByIdentifier(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(promise.future());
    boolean result = testScenario(req, ResponseCode.errorForbidden);
    assertTrue(result);
  }

  private boolean testScenario(Request reqObj, ResponseCode errorCode) {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    subject.tell(reqObj, probe.getRef());

    if (errorCode == null) {
      Response res = probe.expectMsgClass(duration("10 second"), Response.class);
      return null != res && res.getResponseCode() == ResponseCode.OK;
    } else {
      ProjectCommonException res =
          probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
      return res.getCode().equals(errorCode.getErrorCode())
          || res.getResponseCode() == errorCode.getResponseCode();
    }
  }

  private static Response getSuccessResponse() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    return response;
  }
}
