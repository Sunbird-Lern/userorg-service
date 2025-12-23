package org.sunbird.actor.location;

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
import org.sunbird.dto.SearchDTO;
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
public class LocationActorTest {

  private ActorSystem system;
  private Props props;
  private Map<String, Object> data;
  private ElasticSearchRestHighImpl esSearch;

  @Before
  public void setUp() {
    PowerMockito.mockStatic(EsClientFactory.class);
    esSearch = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esSearch);

    PowerMockito.mockStatic(ServiceFactory.class);
    CassandraOperationImpl cassandraOperation = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getSuccessResponse());
    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getSuccessResponse());
    when(cassandraOperation.deleteRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(getSuccessResponse());
    Map<String, Object> esRespone = new HashMap<>();
    esRespone.put(JsonKey.CONTENT, new ArrayList<>());
    esRespone.put(JsonKey.LOCATION_TYPE, "STATE");
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(esRespone);

    when(esSearch.search(Mockito.any(SearchDTO.class), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
    when(esSearch.getDataByIdentifier(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
    data = getDataMap();

    system = ActorSystem.create("system");
    props = Props.create(LocationActor.class);
  }

  @Test
  public void testDeleteLocationSuccess() {
    boolean result = testScenario(ActorOperations.DELETE_LOCATION, true, data, null);
    assertTrue(result);
  }

  @Test
  public void testCreateLocationSuccess() {
    Map<String, Object> res = new HashMap<>(data);
    res.remove(JsonKey.PARENT_CODE);
    res.remove(JsonKey.PARENT_ID);
    boolean result = testScenario(ActorOperations.CREATE_LOCATION, true, null, null);
    assertTrue(result);
  }

  @Test
  public void testUpdateLocationSuccess() {

    boolean result = testScenario(ActorOperations.UPDATE_LOCATION, true, data, null);
    assertTrue(result);
  }

  @Test
  public void testSearchLocationSuccess() {

    boolean result = testScenario(ActorOperations.SEARCH_LOCATION, true, data, null);
    assertTrue(result);
  }

  @Test
  public void testCreateLocationFailureWithInvalidValue() {

    data.put(JsonKey.LOCATION_TYPE, "anyLocationType");
    boolean result =
        testScenario(ActorOperations.CREATE_LOCATION, false, data, ResponseCode.invalidValue);
    assertTrue(result);
  }

  @Test
  public void testCreateLocationFailureWithoutMandatoryParams() {

    data.put(JsonKey.LOCATION_TYPE, "block");
    boolean result =
        testScenario(
            ActorOperations.CREATE_LOCATION, false, data, ResponseCode.mandatoryParamsMissing);
    assertTrue(result);
  }

  @Test
  public void testCreateLocationFailureWithParentLocationNotAllowed() {

    data.put(JsonKey.PARENT_CODE, "anyCode");
    boolean result =
        testScenario(ActorOperations.CREATE_LOCATION, false, data, ResponseCode.parentNotAllowed);
    assertTrue(result);
  }

  @Test
  public void testDeleteLocationFailureWithInvalidLocationDeleteRequest() {
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getContentMapFromES());
    when(esSearch.search(Mockito.any(SearchDTO.class), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
    Promise<Map<String, Object>> promise2 = Futures.promise();
    promise2.success(new HashMap<>());
    when(esSearch.getDataByIdentifier(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise2.future());
    boolean result =
        testScenario(ActorOperations.DELETE_LOCATION, false, data, ResponseCode.invalidParameter);
    assertTrue(result);
  }

  private Map<String, Object> getContentMapFromES() {

    List<Map<String, Object>> lst = new ArrayList<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put("any", "any");
    lst.add(innerMap);
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.CONTENT, lst);
    return map;
  }

  private boolean testScenario(
      ActorOperations actorOperation,
      boolean isSuccess,
      Map<String, Object> data,
      ResponseCode errorCode) {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = new Request();

    if (data != null) actorMessage.getRequest().putAll(data);
    actorMessage.setOperation(actorOperation.getValue());
    subject.tell(actorMessage, probe.getRef());

    if (isSuccess) {
      Response res = probe.expectMsgClass(Duration.ofSeconds(10), Response.class);
      return null != res;
    } else {
      ProjectCommonException res =
          probe.expectMsgClass(Duration.ofSeconds(10), ProjectCommonException.class);
      return res.getErrorCode().equals(errorCode.getErrorCode())
          || res.getErrorResponseCode() == errorCode.getResponseCode();
    }
  }

  private Map<String, Object> getDataMap() {
    data = new HashMap();
    data.put(JsonKey.LOCATION_TYPE, "STATE");
    data.put(JsonKey.CODE, "S01");
    data.put(JsonKey.NAME, "DUMMY_STATE");
    data.put(JsonKey.ID, "id_01");
    data.put(JsonKey.LOCATION_ID, "id_01");
    return data;
  }

  private static Response getSuccessResponse() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    return response;
  }
}
