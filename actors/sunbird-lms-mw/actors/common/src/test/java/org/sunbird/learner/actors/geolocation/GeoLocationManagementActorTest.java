package org.sunbird.learner.actors.geolocation;

import static akka.testkit.JavaTestKit.duration;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
import org.junit.BeforeClass;
import org.junit.Ignore;
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
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ServiceFactory.class, Util.class, DataCacheHandler.class})
@PowerMockIgnore({"javax.management.*"})
@Ignore // Will depricate these api with SC-2169
public class GeoLocationManagementActorTest {

  private ActorSystem system = ActorSystem.create("system");
  private static final Props props = Props.create(GeoLocationManagementActor.class);
  private static CassandraOperationImpl cassandraOperation;
  private static final String orgId = "hhjcjr79fw4p89";
  private static final String type = "husvej";
  private static final String userId = "vcurc633r8911";
  private static List<Map<String, Object>> createResponse;
  private static String id = "anyId";

  @BeforeClass
  public static void setUp() {

    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    Map<String, Object> orgMap = new HashMap<String, Object>();
    orgMap.put(JsonKey.ID, orgId);
  }

  @Before
  public void beforeTest() {
    PowerMockito.mockStatic(ServiceFactory.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getSuccessResponse());
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(cassandraGetRecordById());
    /*when(cassandraOperation.getRecordsByIndexedProperty(
        Mockito.anyString(),
        Mockito.anyString(),
        Mockito.anyString(),
        Mockito.anyString(),
        Mockito.any()))
    .thenReturn(cassandraGetRecordById());*/
    when(cassandraOperation.getRecordsByProperty(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyList(),
            Mockito.any()))
        .thenReturn(cassandraGetRecordById());
  }

  private static Response cassandraGetRecordById() {
    Response response = new Response();
    List list = new ArrayList();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.NAME, "anyName");
    map.put(JsonKey.ID, "anyId");
    map.put(JsonKey.USER_COUNT, 0);
    list.add(map);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }

  private static Response getSuccessResponse() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    return response;
  }

  @Test
  public void createGeoLocationSuccess() {

    List<Map<String, Object>> dataList = new ArrayList<>();
    Map<String, Object> dataMap = new HashMap<>();
    dataMap.put(JsonKey.LOCATION, "location");
    dataMap.put(JsonKey.TYPE, type);

    dataList.add(dataMap);

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = new Request();

    actorMessage.getRequest().put(JsonKey.REQUESTED_BY, userId);
    actorMessage.setOperation(ActorOperations.CREATE_GEO_LOCATION.getValue());

    actorMessage.getRequest().put(JsonKey.DATA, dataList);
    actorMessage.getRequest().put(JsonKey.ROOT_ORG_ID, orgId);

    subject.tell(actorMessage, probe.getRef());
    Response res = probe.expectMsgClass(duration("100 second"), Response.class);
    createResponse = (List<Map<String, Object>>) res.getResult().get(JsonKey.RESPONSE);
    if (createResponse != null && createResponse.size() > 0) {
      id = (String) createResponse.get(0).get(JsonKey.ID);
      createResponse.remove(createResponse.get(0));
    }
    Assert.assertTrue(null != id);
  }

  @Test
  public void createGeoLocationFailureWithNullOrgId() {

    List<Map<String, Object>> dataList = new ArrayList<>();

    Map<String, Object> dataMap = new HashMap<>();
    dataMap.put(JsonKey.LOCATION, "location");
    dataMap.put(JsonKey.TYPE, type);

    dataList.add(dataMap);

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = new Request();

    actorMessage.getRequest().put(JsonKey.REQUESTED_BY, userId);
    actorMessage.setOperation(ActorOperations.CREATE_GEO_LOCATION.getValue());

    actorMessage.getRequest().put(JsonKey.DATA, dataList);
    actorMessage.getRequest().put(JsonKey.ROOT_ORG_ID, null);

    subject.tell(actorMessage, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    assertTrue(exc.getCode().equals(ResponseCode.invalidOrgId.getErrorCode()));
  }

  @Test
  public void createGeoLocationTestWithInvalidOrgId() {

    List<Map<String, Object>> dataList = new ArrayList<>();

    Map<String, Object> dataMap = new HashMap<>();
    dataMap.put(JsonKey.LOCATION, "location");
    dataMap.put(JsonKey.TYPE, type);

    dataList.add(dataMap);

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = new Request();

    actorMessage.getRequest().put(JsonKey.REQUESTED_BY, userId);
    actorMessage.setOperation(ActorOperations.CREATE_GEO_LOCATION.getValue());

    actorMessage.getRequest().put(JsonKey.DATA, dataList);
    actorMessage.getRequest().put(JsonKey.ROOT_ORG_ID, "invalidOrgId");

    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(getFailureResponse());
    subject.tell(actorMessage, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    assertTrue(exc.getCode().equals(ResponseCode.invalidOrgId.getErrorCode()));
  }

  private Response getFailureResponse() {
    Response response = new Response();
    List<Map<String, Object>> objList = new ArrayList<>();
    response.put(JsonKey.RESPONSE, objList);
    return response;
  }

  @Test
  public void createGeoLocationFailureWithInvalidData() {

    List<Map<String, Object>> dataList = new ArrayList<>();

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = new Request();

    actorMessage.getRequest().put(JsonKey.REQUESTED_BY, userId);
    actorMessage.setOperation(ActorOperations.CREATE_GEO_LOCATION.getValue());

    actorMessage.getRequest().put(JsonKey.DATA, dataList);
    actorMessage.getRequest().put(JsonKey.ROOT_ORG_ID, orgId);

    subject.tell(actorMessage, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    assertTrue(exc.getCode().equals(ResponseCode.invalidRequestData.getErrorCode()));
  }

  @Test
  public void getGeoLocationSuccessOrgId() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = new Request();

    actorMessage.getRequest().put(JsonKey.REQUESTED_BY, userId);
    actorMessage.getRequest().put(JsonKey.TYPE, JsonKey.ORGANISATION);
    actorMessage.getRequest().put(JsonKey.ID, orgId);
    actorMessage.setOperation(ActorOperations.GET_GEO_LOCATION.getValue());

    actorMessage.getRequest().put(JsonKey.ROOT_ORG_ID, orgId);

    subject.tell(actorMessage, probe.getRef());
    Response res = probe.expectMsgClass(duration("100 second"), Response.class);
    Assert.assertTrue(null != res.get(JsonKey.RESPONSE));
  }

  @Test
  public void getGeoLocationSuccessWithLocationId() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = new Request();

    actorMessage.getRequest().put(JsonKey.REQUESTED_BY, userId);
    actorMessage.getRequest().put(JsonKey.TYPE, JsonKey.LOCATION);
    actorMessage.getRequest().put(JsonKey.ID, id);
    actorMessage.setOperation(ActorOperations.GET_GEO_LOCATION.getValue());

    actorMessage.getRequest().put(JsonKey.ROOT_ORG_ID, orgId);

    subject.tell(actorMessage, probe.getRef());
    Response res = probe.expectMsgClass(duration("100 second"), Response.class);
    Assert.assertTrue(null != res.get(JsonKey.RESPONSE));
  }

  @Test
  public void getGeoLocationFailureWithNullType() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = new Request();

    actorMessage.getRequest().put(JsonKey.REQUESTED_BY, userId);
    actorMessage.getRequest().put(JsonKey.TYPE, null);
    actorMessage.getRequest().put(JsonKey.ID, orgId);
    actorMessage.setOperation(ActorOperations.GET_GEO_LOCATION.getValue());

    actorMessage.getRequest().put(JsonKey.ROOT_ORG_ID, orgId);
    subject.tell(actorMessage, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    assertTrue(exc.getCode().equals(ResponseCode.invalidRequestData.getErrorCode()));
  }

  @Test
  public void getGeoLocationFailureWithInvalidType() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = new Request();

    actorMessage.getRequest().put(JsonKey.REQUESTED_BY, userId);
    actorMessage.getRequest().put(JsonKey.TYPE, "Invalid type");
    actorMessage.getRequest().put(JsonKey.ID, orgId);
    actorMessage.setOperation(ActorOperations.GET_GEO_LOCATION.getValue());

    actorMessage.getRequest().put(JsonKey.ROOT_ORG_ID, orgId);

    subject.tell(actorMessage, probe.getRef());

    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    assertTrue(exc.getCode().equals(ResponseCode.invalidTypeValue.getErrorCode()));
  }

  @Test
  public void updateGeoLocationSuccess() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = new Request();

    actorMessage.getRequest().put(JsonKey.REQUESTED_BY, userId);
    actorMessage.getRequest().put(JsonKey.LOCATION, "updated location");
    actorMessage.getRequest().put(JsonKey.TYPE, type);
    actorMessage.getRequest().put(JsonKey.LOCATION_ID, id);
    actorMessage.setOperation(ActorOperations.UPDATE_GEO_LOCATION.getValue());

    subject.tell(actorMessage, probe.getRef());
    Response res = probe.expectMsgClass(duration("100 second"), Response.class);
    Assert.assertTrue(null != res.get(JsonKey.RESPONSE));
  }

  @Test
  public void updateGeoLocationFailureWithNullId() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = new Request();

    actorMessage.getRequest().put(JsonKey.REQUESTED_BY, userId);
    actorMessage.getRequest().put(JsonKey.LOCATION, "updated location");
    actorMessage.getRequest().put(JsonKey.TYPE, type);
    actorMessage.getRequest().put(JsonKey.LOCATION_ID, null);
    actorMessage.setOperation(ActorOperations.UPDATE_GEO_LOCATION.getValue());

    subject.tell(actorMessage, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    assertTrue(exc.getCode().equals(ResponseCode.invalidRequestData.getErrorCode()));
  }

  @Test
  public void updateGeoLocationFailureWithInvalidId() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = new Request();

    actorMessage.getRequest().put(JsonKey.REQUESTED_BY, userId);
    actorMessage.getRequest().put(JsonKey.LOCATION, "updated location");
    actorMessage.getRequest().put(JsonKey.TYPE, type);
    actorMessage.getRequest().put(JsonKey.LOCATION_ID, "invalId");
    actorMessage.setOperation(ActorOperations.UPDATE_GEO_LOCATION.getValue());

    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(getFailureResponse());

    subject.tell(actorMessage, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    assertTrue(exc.getCode().equals(ResponseCode.invalidLocationId.getErrorCode()));
  }

  @Test
  public void deleteGeoLocationSuccess() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = new Request();

    actorMessage.getRequest().put(JsonKey.LOCATION_ID, id);
    actorMessage.setOperation(ActorOperations.DELETE_GEO_LOCATION.getValue());

    subject.tell(actorMessage, probe.getRef());
    Response res = probe.expectMsgClass(duration("100 second"), Response.class);
    Assert.assertTrue(null != res.get(JsonKey.RESPONSE));
  }

  @Test
  public void deleteGeoLocationFailureWithNullId() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = new Request();

    actorMessage.getRequest().put(JsonKey.LOCATION_ID, null);
    actorMessage.setOperation(ActorOperations.DELETE_GEO_LOCATION.getValue());

    subject.tell(actorMessage, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    assertTrue(exc.getCode().equals(ResponseCode.invalidRequestData.getErrorCode()));
  }

  @Test
  public void geoLocationFailureWithInvalidOperation() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = new Request();

    actorMessage.getRequest().put(JsonKey.LOCATION_ID, null);
    actorMessage.setOperation("invalid operation");

    subject.tell(actorMessage, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    assertTrue(exc.getCode().equals(ResponseCode.invalidRequestData.getErrorCode()));
  }

  @Test
  public void getUserCountSuccess() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = new Request();
    List<Object> list = new ArrayList<>();
    list.add("locId1");
    actorMessage.getRequest().put(JsonKey.LOCATION_IDS, list);
    actorMessage.setOperation(ActorOperations.GET_USER_COUNT.getValue());

    subject.tell(actorMessage, probe.getRef());
    Response res = probe.expectMsgClass(duration("100 second"), Response.class);

    List<Map<String, Object>> result =
        (List<Map<String, Object>>) res.getResult().get(JsonKey.LOCATIONS);
    Map<String, Object> map = result.get(0);
    int count = (int) map.get(JsonKey.USER_COUNT);
    assertEquals(0, count);
  }

  @Test
  public void sendNotificationGeoLocationSuccess() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = new Request();

    actorMessage.getRequest().put(JsonKey.REQUESTED_BY, userId);
    actorMessage.getRequest().put(JsonKey.LOCATION, "updated location");
    actorMessage.getRequest().put(JsonKey.TYPE, type);
    actorMessage.getRequest().put(JsonKey.LOCATION_ID, id);
    actorMessage.setOperation(ActorOperations.SEND_NOTIFICATION.getValue());

    subject.tell(actorMessage, probe.getRef());
    Response res = probe.expectMsgClass(duration("100 second"), Response.class);
    Assert.assertTrue(null != res.get(JsonKey.RESPONSE));
  }

  @Test
  public void sendNotificationGeoLocationFailure() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request actorMessage = new Request();

    actorMessage.getRequest().put(JsonKey.REQUESTED_BY, userId);
    actorMessage.getRequest().put(JsonKey.LOCATION, "updated location");
    actorMessage.getRequest().put(JsonKey.TYPE, type);
    actorMessage.getRequest().put(JsonKey.LOCATION_ID, id);
    actorMessage.setOperation(ActorOperations.SEND_NOTIFICATION.getValue());

    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(getFailureResponse());

    subject.tell(actorMessage, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("100 second"), ProjectCommonException.class);
    assertTrue(exc.getCode().equals(ResponseCode.invalidTopic.getErrorCode()));
  }
}
