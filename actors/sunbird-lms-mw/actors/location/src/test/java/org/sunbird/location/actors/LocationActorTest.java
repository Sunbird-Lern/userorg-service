package org.sunbird.location.actors;

import static akka.testkit.JavaTestKit.duration;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.testkit.javadsl.TestKit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
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
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.GeoLocationJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LocationActorOperation;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.ServiceFactory;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  ElasticSearchRestHighImpl.class,
  EsClientFactory.class,
  ElasticSearchHelper.class
})
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
public class LocationActorTest {

  private static final ActorSystem system = ActorSystem.create("system");
  private static final Props props = Props.create(LocationActor.class);
  private static Map<String, Object> data;
  private static ElasticSearchRestHighImpl esSearch;

  @BeforeClass
  public static void init() {

    PowerMockito.mockStatic(EsClientFactory.class);
    PowerMockito.mockStatic(ServiceFactory.class);
    CassandraOperationImpl cassandraOperation = mock(CassandraOperationImpl.class);
    esSearch = mock(ElasticSearchRestHighImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esSearch);
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(getSuccessResponse());
    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(getSuccessResponse());
    when(cassandraOperation.deleteRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(getSuccessResponse());
  }

  @Before
  public void setUp() {

    Map<String, Object> esRespone = new HashMap<>();
    esRespone.put(JsonKey.CONTENT, new ArrayList<>());
    esRespone.put(GeoLocationJsonKey.LOCATION_TYPE, "STATE");
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(esRespone);

    when(esSearch.search(Mockito.any(SearchDTO.class), Mockito.anyString()))
        .thenReturn(promise.future());
    when(esSearch.getDataByIdentifier(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(promise.future());
    data = getDataMap();
  }

  @Test
  public void testCreateLocationSuccess() {
    Map<String, Object> res = new HashMap<>(data);
    res.remove(GeoLocationJsonKey.PARENT_CODE);
    res.remove(GeoLocationJsonKey.PARENT_ID);
    boolean result = testScenario(LocationActorOperation.CREATE_LOCATION, true, null, null);
    assertTrue(result);
  }

  @Test
  public void testUpdateLocationSuccess() {

    boolean result = testScenario(LocationActorOperation.UPDATE_LOCATION, true, data, null);
    assertTrue(result);
  }

  @Test
  public void testDeleteLocationSuccess() {

    boolean result = testScenario(LocationActorOperation.DELETE_LOCATION, true, data, null);
    assertTrue(result);
  }

  @Test
  public void testSearchLocationSuccess() {

    boolean result = testScenario(LocationActorOperation.SEARCH_LOCATION, true, data, null);
    assertTrue(result);
  }

  @Test
  public void testCreateLocationFailureWithInvalidValue() {

    data.put(GeoLocationJsonKey.LOCATION_TYPE, "anyLocationType");
    boolean result =
        testScenario(
            LocationActorOperation.CREATE_LOCATION, false, data, ResponseCode.invalidValue);
    assertTrue(result);
  }

  @Test
  public void testCreateLocationFailureWithoutMandatoryParams() {

    data.put(GeoLocationJsonKey.LOCATION_TYPE, "block");
    boolean result =
        testScenario(
            LocationActorOperation.CREATE_LOCATION,
            false,
            data,
            ResponseCode.mandatoryParamsMissing);
    assertTrue(result);
  }

  @Test
  public void testCreateLocationFailureWithParentLocationNotAllowed() {

    data.put(GeoLocationJsonKey.PARENT_CODE, "anyCode");
    boolean result =
        testScenario(
            LocationActorOperation.CREATE_LOCATION, false, data, ResponseCode.parentNotAllowed);
    assertTrue(result);
  }

  @Test
  public void testDeleteLocationFailureWithInvalidLocationDeleteRequest() {
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getContentMapFromES());
    when(esSearch.search(Mockito.any(SearchDTO.class), Mockito.anyString()))
        .thenReturn(promise.future());
    boolean result =
        testScenario(
            LocationActorOperation.DELETE_LOCATION,
            false,
            data,
            ResponseCode.invalidLocationDeleteRequest);
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
      LocationActorOperation actorOperation,
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
      Response res = probe.expectMsgClass(duration("10 second"), Response.class);
      return null != res;
    } else {
      ProjectCommonException res =
          probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
      return res.getCode().equals(errorCode.getErrorCode())
          || res.getResponseCode() == errorCode.getResponseCode();
    }
  }

  private static Map<String, Object> getDataMap() {

    data = new HashMap();
    data.put(GeoLocationJsonKey.LOCATION_TYPE, "STATE");
    data.put(GeoLocationJsonKey.CODE, "S01");
    data.put(JsonKey.NAME, "DUMMY_STATE");
    data.put(JsonKey.ID, "id_01");
    return data;
  }

  private static Response getSuccessResponse() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    return response;
  }
}
