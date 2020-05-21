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
import org.sunbird.actor.router.RequestRouter;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.GeoLocationJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.validator.location.LocationRequestValidator;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  OrganisationManagementActor.class,
  Util.class,
  ElasticSearchRestHighImpl.class,
  RequestRouter.class,
  ProjectUtil.class,
  LocationRequestValidator.class,
  EsClientFactory.class
})
@PowerMockIgnore("javax.management.*")
public class OrgManagementActorTest {

  private ActorSystem system = ActorSystem.create("system");
  private static final Props props = Props.create(OrganisationManagementActor.class);
  private static CassandraOperationImpl cassandraOperation;
  private static Map<String, Object> basicRequestData;
  private static final String ADD_MEMBER_TO_ORG =
      ActorOperations.ADD_MEMBER_ORGANISATION.getValue();
  private static ElasticSearchService esService;

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(ServiceFactory.class);
    PowerMockito.mockStatic(Util.class);
    PowerMockito.mockStatic(ProjectUtil.class);
    PowerMockito.mockStatic(EsClientFactory.class);

    cassandraOperation = mock(CassandraOperationImpl.class);
    esService = mock(ElasticSearchRestHighImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
    basicRequestData = getBasicData();
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getEsResponse(false));
    when(esService.search(Mockito.any(), Mockito.anyString())).thenReturn(promise.future());
    when(cassandraOperation.getRecordsByProperty(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(getRecordsByProperty(false));
    when(cassandraOperation.getRecordsByProperties(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(getRecordsByProperty(false));
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(getRecordsByProperty(false));
    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(getRecordsByProperty(false));
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(getRecordsByProperty(false));
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(getRecordsByProperty(false));
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(getRecordsByProperty(false));

    when(Util.validateRoles(Mockito.anyList())).thenReturn("SUCCESS");
    when(Util.encryptData(Mockito.anyString())).thenReturn("userExtId");
    when(ProjectUtil.getUniqueIdFromTimestamp(Mockito.anyInt())).thenReturn("time");
    when(ProjectUtil.getFormattedDate()).thenReturn("date");
    when(ProjectUtil.getConfigValue(GeoLocationJsonKey.SUNBIRD_VALID_LOCATION_TYPES))
        .thenReturn("dummy");
    when(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_API_REQUEST_LOWER_CASE_FIELDS))
        .thenReturn("lowercase");
    PowerMockito.mockStatic(LocationRequestValidator.class);
  }

  // @Test
  public void testUpdateOrgStatus() {
    Request reqObj = new Request();
    Map<String, Object> requestData = new HashMap<>();
    requestData.put(JsonKey.REQUESTED_BY, "as23-12asd234-123");
    requestData.put(JsonKey.ORGANISATION_ID, "orgId");
    reqObj.setRequest(requestData);
    reqObj.setOperation(ActorOperations.UPDATE_ORG_STATUS.getValue());
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(getRecordsByProperty(false));
    boolean result = testScenario(reqObj, null);
    assertTrue(result);
  }

  @Test
  public void testAddUserToOrgSuccessWithUserIdAndOrgId() {

    boolean result =
        testScenario(
            getRequest(
                getRequestData(true, true, false, false, basicRequestData), ADD_MEMBER_TO_ORG),
            null);
    assertTrue(result);
  }

  @Test
  public void testAddUserToOrgSuccessWithUserExtIdAndOrgId() {

    boolean result =
        testScenario(
            getRequest(
                getRequestData(false, true, true, false, basicRequestData), ADD_MEMBER_TO_ORG),
            null);
    assertTrue(result);
  }

  @Test
  public void testAddUserToOrgSuccessWithUserIdAndOrgExtId() {

    boolean result =
        testScenario(
            getRequest(
                getRequestData(true, false, false, true, basicRequestData), ADD_MEMBER_TO_ORG),
            null);
    assertTrue(result);
  }

  @Test
  public void testAddUserToOrgSuccessWithUserExtIdAndOrgExtId() {

    boolean result =
        testScenario(
            getRequest(
                getRequestData(false, false, true, true, basicRequestData), ADD_MEMBER_TO_ORG),
            null);
    assertTrue(result);
  }

  @Test
  public void testAddUserToOrgFailureWithUserNotFoundWithUserId() {
    when(cassandraOperation.getRecordsByProperty(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(getRecordsByProperty(true));
    boolean result =
        testScenario(
            getRequest(
                getRequestData(true, false, true, true, basicRequestData), ADD_MEMBER_TO_ORG),
            ResponseCode.invalidUsrData);
    assertTrue(result);
  }

  @Test
  public void testAddUserToOrgFailureWithOrgNotFoundWithOrgId() {
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getEsResponse(true));
    when(esService.search(Mockito.any(), Mockito.anyString())).thenReturn(promise.future());
    boolean result =
        testScenario(
            getRequest(
                getRequestData(true, false, true, true, basicRequestData), ADD_MEMBER_TO_ORG),
            ResponseCode.invalidOrgData);
    assertTrue(result);
  }

  @Test
  public void testAddUserToOrgFailureWithUserNotFoundWithUserExtId() {
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(getRecordsByProperty(true));

    boolean result =
        testScenario(
            getRequest(
                getRequestData(false, false, true, true, basicRequestData), ADD_MEMBER_TO_ORG),
            ResponseCode.invalidUsrData);
    assertTrue(result);
  }

  @Test
  public void testAddUserToOrgFailureWithOrgNotFoundWithOrgExtId() {
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getEsResponse(true));
    when(esService.search(Mockito.any(), Mockito.anyString())).thenReturn(promise.future());
    boolean result =
        testScenario(
            getRequest(
                getRequestData(true, false, true, true, basicRequestData), ADD_MEMBER_TO_ORG),
            ResponseCode.invalidOrgData);
    assertTrue(result);
  }

  @Test
  public void testCreateOrgSuccessWithExternalIdAndProvider() {
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(getRecordsByProperty(true));
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(getSuccess());
    when(cassandraOperation.getRecordsByProperties(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(getRecordsByProperty(true));
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getValidateChannelEsResponse(true));

    when(esService.search(Mockito.any(), Mockito.anyString())).thenReturn(promise.future());
    boolean result =
        testScenario(
            getRequest(
                getRequestDataForOrgCreate(basicRequestData),
                ActorOperations.CREATE_ORG.getValue()),
            null);
    assertTrue(result);
  }

  @Test
  public void testCreateOrgSuccessWithoutExternalIdAndProvider() {
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(getRecordsByProperty(true));
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(getSuccess());
    when(cassandraOperation.getRecordsByProperties(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(getRecordsByProperty(true));
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getValidateChannelEsResponse(true));

    when(esService.search(Mockito.any(), Mockito.anyString())).thenReturn(promise.future());
    Map<String, Object> map = getRequestDataForOrgCreate(basicRequestData);
    map.remove(JsonKey.EXTERNAL_ID);
    boolean result = testScenario(getRequest(map, ActorOperations.CREATE_ORG.getValue()), null);
    assertTrue(result);
  }

  @Test
  public void testCreateOrgFailureWithoutChannel() {
    Map<String, Object> map = getRequestDataForOrgCreate(basicRequestData);
    map.remove(JsonKey.CHANNEL);
    boolean result =
        testScenario(
            getRequest(map, ActorOperations.CREATE_ORG.getValue()),
            ResponseCode.mandatoryParamsMissing);
    assertTrue(result);
  }

  private Response getOrgStatus() {
    Response res = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.STATUS, 1);
    map.put(JsonKey.ID, "id");
    list.add(map);
    res.put(JsonKey.RESPONSE, list);
    return res;
  }

  private Response getSuccess() {
    Response res = new Response();
    res.setResponseCode(ResponseCode.OK);
    return res;
  }

  private Map<String, Object> getRequestDataForOrgCreate(Map<String, Object> map) {
    map.put(JsonKey.CHANNEL, "channel");
    map.put(JsonKey.IS_ROOT_ORG, false);
    map.put(JsonKey.EXTERNAL_ID, "externalId");

    return map;
  }

  private Map<String, Object> getRequestData(
      boolean userId, boolean orgId, boolean userExtId, boolean OrgExtId, Map<String, Object> map) {
    List<String> rolesList = new ArrayList<>();
    rolesList.add("dummyRole");
    map.put(JsonKey.ROLES, rolesList);
    if (userId) {
      map.put(JsonKey.USER_ID, "userId");
    }
    if (orgId) {
      map.put(JsonKey.ORGANISATION_ID, "orgId");
    }
    if (userExtId) {
      map.put(JsonKey.USER_EXTERNAL_ID, "userExtId");
    }
    if (OrgExtId) {
      map.put(JsonKey.EXTERNAL_ID, "externalId");
    }
    return map;
  }

  private Response getRecordsByProperty(boolean empty) {
    Response res = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    if (!empty) {
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.ID, "userId");
      map.put(JsonKey.IS_DELETED, true);
      list.add(map);
    }
    res.put(JsonKey.RESPONSE, list);
    return res;
  }

  private Map<String, Object> getEsResponse(boolean empty) {
    Map<String, Object> response = new HashMap<>();
    List<Map<String, Object>> contentList = new ArrayList<>();
    if (!empty) {
      Map<String, Object> content = new HashMap<>();
      content.put(JsonKey.ORGANISATION_ID, "orgId");
      content.put(JsonKey.HASHTAGID, "hashtagId");
      contentList.add(content);
    }
    response.put(JsonKey.CONTENT, contentList);
    return response;
  }

  private Map<String, Object> getValidateChannelEsResponse(boolean isValidChannel) {
    Map<String, Object> response = new HashMap<>();
    List<Map<String, Object>> contentList = new ArrayList<>();
    if (isValidChannel) {
      Map<String, Object> content = new HashMap<>();
      content.put(JsonKey.STATUS, 1);
      content.put(JsonKey.ID, "id");
      contentList.add(content);
    }
    response.put(JsonKey.CONTENT, contentList);
    return response;
  }

  private boolean testScenario(Request request, ResponseCode errorCode) {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    subject.tell(request, probe.getRef());

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

  private Request getRequest(Map<String, Object> requestData, String actorOperation) {
    Request reqObj = new Request();
    reqObj.setRequest(requestData);
    reqObj.setOperation(actorOperation);
    return reqObj;
  }

  private Map<String, Object> getBasicData() {
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.PROVIDER, "provider");
    map.put(JsonKey.USER_PROVIDER, "userProvider");
    map.put(JsonKey.USER_ID_TYPE, "userIdType");
    return map;
  }
}
