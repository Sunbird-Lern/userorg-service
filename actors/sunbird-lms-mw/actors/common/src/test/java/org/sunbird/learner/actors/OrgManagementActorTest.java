package org.sunbird.learner.actors;

import static akka.testkit.JavaTestKit.duration;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
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
import org.sunbird.actor.service.BaseMWService;
import org.sunbird.actor.service.SunbirdMWService;
import org.sunbird.actorutil.location.LocationClient;
import org.sunbird.actorutil.location.impl.LocationClientImpl;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.organisation.external.identity.service.OrgExternalService;
import org.sunbird.learner.organisation.service.impl.OrgServiceImpl;
import org.sunbird.learner.util.Util;
import org.sunbird.models.location.Location;
import org.sunbird.validator.location.LocationRequestValidator;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  Util.class,
  ElasticSearchRestHighImpl.class,
  ProjectUtil.class,
  LocationRequestValidator.class,
  EsClientFactory.class,
  LocationClient.class,
  LocationClientImpl.class,
  RequestRouter.class,
  BaseMWService.class,
  SunbirdMWService.class,
  ActorSelection.class,
  OrgExternalService.class,
  OrgServiceImpl.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*",
  "javax.script.*",
  "javax.xml.*",
  "com.sun.org.apache.xerces.*",
  "org.xml.*"
})
public class OrgManagementActorTest {

  private ActorSystem system = ActorSystem.create("system");
  private static final Props props = Props.create(OrganisationManagementActor.class);
  private static CassandraOperationImpl cassandraOperation;
  private static Map<String, Object> basicRequestData;
  private static ElasticSearchService esService;

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(ServiceFactory.class);
    PowerMockito.mockStatic(Util.class);
    PowerMockito.mockStatic(ProjectUtil.class);
    PowerMockito.mockStatic(EsClientFactory.class);
    PowerMockito.mockStatic(BaseMWService.class);
    PowerMockito.mockStatic(SunbirdMWService.class);
    SunbirdMWService.tellToBGRouter(Mockito.any(), Mockito.any());
    ActorSelection selection = PowerMockito.mock(ActorSelection.class);
    when(BaseMWService.getRemoteRouter(Mockito.anyString())).thenReturn(selection);

    cassandraOperation = mock(CassandraOperationImpl.class);
    esService = mock(ElasticSearchRestHighImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
    basicRequestData = getBasicData();
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getEsResponse(false));
    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
    when(cassandraOperation.getAllRecords(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyList(), Mockito.any()))
        .thenReturn(getAllRecords());
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getUpsertRecords());
    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getUpsertRecords());
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getRecordsByProperty(false))
        .thenReturn(getRecordsByProperty(false));
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(getRecordsByProperty(false))
        .thenReturn(getRecordsByProperty(false));
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getRecordsByProperty(false));
    when(Util.encryptData(Mockito.anyString())).thenReturn("userExtId");
    when(Util.registerChannel(Mockito.anyMap(), Mockito.any())).thenReturn(true);
    when(ProjectUtil.getUniqueIdFromTimestamp(Mockito.anyInt())).thenReturn("time");
    when(ProjectUtil.getFormattedDate()).thenReturn("date");
    when(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_VALID_LOCATION_TYPES)).thenReturn("dummy");
    when(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_API_REQUEST_LOWER_CASE_FIELDS))
        .thenReturn("lowercase");
    when(ProjectUtil.getConfigValue("org_index_alias")).thenReturn("org_alias");
  }

  @Test
  public void testCreateOrgFailureWithMandatoryParamOrgTypeMissing() {
    Map<String, Object> req = getRequestDataForOrgCreate(basicRequestData);
    req.remove(JsonKey.ORG_TYPE);
    boolean result =
        testScenario(
            getRequest(req, ActorOperations.CREATE_ORG.getValue()),
            ResponseCode.mandatoryParamsMissing);
    assertTrue(result);
  }

  @Test
  public void testCreateOrgFailureWithInvalidEmailFormat() {
    Map<String, Object> req = getRequestDataForOrgCreate(basicRequestData);
    req.put(JsonKey.EMAIL, "invalid_email_format.com");
    boolean result =
        testScenario(
            getRequest(req, ActorOperations.CREATE_ORG.getValue()), ResponseCode.emailFormatError);
    assertTrue(result);
  }

  @Test
  public void testCreateOrgFailureWithInvalidOrgTypeValue() {
    Map<String, Object> req = getRequestDataForOrgCreate(basicRequestData);
    req.put(JsonKey.ORG_TYPE, "invalidValue");
    boolean result =
        testScenario(
            getRequest(req, ActorOperations.CREATE_ORG.getValue()), ResponseCode.invalidValue);
    assertTrue(result);
  }

  @Test
  public void testCreateOrgFailureWithDuplicateChannel() {
    Map<String, Object> req = getRequestDataForOrgCreate(basicRequestData);
    req.put(JsonKey.ORG_TYPE, "board");
    req.put(JsonKey.IS_TENANT, true);
    boolean result =
        testScenario(
            getRequest(req, ActorOperations.CREATE_ORG.getValue()),
            ResponseCode.channelUniquenessInvalid);
    assertTrue(result);
  }

  @Test
  public void testCreateOrgSuccessWithExternalIdAndProvider() {
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getRecordsByProperty(true));
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getSuccess());
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getValidateChannelEsResponse(true));

    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
    Request req =
        getRequest(
            getRequestDataForOrgCreate(basicRequestData), ActorOperations.CREATE_ORG.getValue());
    boolean result = testScenario(req, null);
    assertTrue(result);
  }

  @Test
  public void testGetOrgDetails() {
    Map<String, Object> req = new HashMap<>();
    req.put(JsonKey.ORGANISATION_ID, "54652139879");
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getByIdEsResponse(false));
    when(esService.getDataByIdentifier(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
    boolean result =
        testScenario(getRequest(req, ActorOperations.GET_ORG_DETAILS.getValue()), null);
    assertTrue(result);
  }

  @Test
  public void testGetOrgDetailsFailure() {
    Map<String, Object> req = new HashMap<>();
    req.put(JsonKey.ORGANISATION_ID, "54652139879");
    Promise<Map<String, Object>> promise = Futures.promise();
    Map<String, Object> esRes = getByIdEsResponse(true);
    promise.success(esRes);
    when(esService.getDataByIdentifier(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
    boolean result =
        testScenario(
            getRequest(req, ActorOperations.GET_ORG_DETAILS.getValue()),
            ResponseCode.orgDoesNotExist);
    assertTrue(result);
  }

  @Test
  public void testCreateOrgSuccess() {
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getRecordsByProperty(true));
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getSuccess());
    Promise<Map<String, Object>> promise = Futures.promise();
    Map<String, Object> esMap = getValidateChannelEsResponse(true);
    esMap.put(JsonKey.CONTENT, new ArrayList<>());
    promise.success(esMap);

    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());

    Map<String, Object> req = getRequestDataForOrgCreate(basicRequestData);
    req.put(JsonKey.HASHTAGID, "orgId");
    req.put(JsonKey.IS_TENANT, true);
    Request reqst = getRequest(req, ActorOperations.CREATE_ORG.getValue());
    reqst.getContext().put(JsonKey.CALLER_ID, JsonKey.BULK_ORG_UPLOAD);
    boolean result = testScenario(reqst, null);
    assertTrue(result);
  }

  @Test
  public void testCreateOrgFailure() {
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getRecordsByProperty(true));
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getSuccess());
    Promise<Map<String, Object>> promise = Futures.promise();
    Map<String, Object> esMap = getValidateChannelEsResponse(true);
    esMap.put(JsonKey.CONTENT, new ArrayList<>());
    promise.success(esMap);
    Promise<Map<String, Object>> promise2 = Futures.promise();
    promise2.success(getValidateChannelEsResponse(true));

    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future())
        .thenReturn(promise2.future());

    Map<String, Object> req = getRequestDataForOrgCreate(basicRequestData);
    req.put(JsonKey.IS_TENANT, true);
    boolean result =
        testScenario(
            getRequest(req, ActorOperations.CREATE_ORG.getValue()), ResponseCode.slugIsNotUnique);
    assertTrue(result);
  }

  @Test
  public void testCreateOrgSuccessWithoutExternalIdAndProvider() {
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getRecordsByProperty(true));
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getSuccess());

    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getValidateChannelEsResponse(true));

    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
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

  @Test
  public void testUpdateOrgSuccess() {
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getRecordsByProperty(true));
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getValidateChannelEsResponse(true));

    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
    when(Util.updateChannel(Mockito.anyMap(), Mockito.any())).thenReturn(true);
    Map<String, Object> req = getRequestDataForOrgUpdate();
    req.put(JsonKey.HASHTAGID, "orgId");
    Request reqst = getRequest(req, ActorOperations.UPDATE_ORG.getValue());
    reqst.getContext().put(JsonKey.CALLER_ID, JsonKey.BULK_ORG_UPLOAD);
    boolean result = testScenario(reqst, null);
    assertTrue(result);
  }

  @Test
  public void testUpdateOrgFailureWithInvalidReqData() {
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getRecordsByProperty(true));
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getValidateChannelEsResponse(true));

    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
    when(Util.updateChannel(Mockito.anyMap(), Mockito.any())).thenReturn(true);
    Map<String, Object> req = getRequestDataForOrgUpdate();
    req.remove(JsonKey.ORGANISATION_ID);
    boolean result =
        testScenario(
            getRequest(req, ActorOperations.UPDATE_ORG.getValue()),
            ResponseCode.invalidRequestData);
    assertTrue(result);
  }

  @Test
  public void testUpdateOrgFailureWithInvalidExternalAndProviderId() throws Exception {
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getRecordsByProperty(true));
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getValidateChannelEsResponse(true));

    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
    when(Util.updateChannel(Mockito.anyMap(), Mockito.any())).thenReturn(true);
    Map<String, Object> req = getRequestDataForOrgUpdate();
    req.remove(JsonKey.ORGANISATION_ID);
    req.put(JsonKey.EXTERNAL_ID, "extId");
    req.put(JsonKey.PROVIDER, "provider");
    OrgExternalService orgExternalService = PowerMockito.mock(OrgExternalService.class);
    whenNew(OrgExternalService.class).withNoArguments().thenReturn(orgExternalService);
    when(orgExternalService.getOrgIdFromOrgExternalIdAndProvider(
            Mockito.anyString(), Mockito.anyString(), Mockito.any(RequestContext.class)))
        .thenReturn("");
    boolean result =
        testScenario(
            getRequest(req, ActorOperations.UPDATE_ORG.getValue()),
            ResponseCode.invalidRequestData);
    assertTrue(result);
  }

  @Test
  public void testUpdateOrgSuccess2() {
    Response res = getRecordsByProperty(true);
    res.getResult().put(JsonKey.EXTERNAL_ID, "extId");
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(res);
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getValidateChannelEsResponse(true));

    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
    when(Util.updateChannel(Mockito.anyMap(), Mockito.any())).thenReturn(true);
    Map<String, Object> req = getRequestDataForOrgUpdate();
    req.remove(JsonKey.CHANNEL);
    req.put(JsonKey.EXTERNAL_ID, "extId");
    Request request =
        getRequest(
            getRequestDataForOrgCreate(basicRequestData), ActorOperations.CREATE_ORG.getValue());
    boolean result = testScenario(request, null);
    assertTrue(result);
  }

  @Test
  public void testUpdateOrgFailureWithInvalidEmailFormat() {
    Map<String, Object> map = getRequestDataForOrgUpdate();
    map.put(JsonKey.EMAIL, "invalid_email_format.com");
    boolean result =
        testScenario(
            getRequest(map, ActorOperations.UPDATE_ORG.getValue()), ResponseCode.emailFormatError);
    assertTrue(result);
  }

  @Test
  public void testUpdateOrgFailureWithDuplicateChannel() {
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getRecordsByProperty(true));
    Promise<Map<String, Object>> promise = Futures.promise();
    Map<String, Object> esMap = getValidateChannelEsResponse(true);
    List<Map<String, Object>> list = (List<Map<String, Object>>) esMap.get(JsonKey.CONTENT);
    Map<String, Object> data = list.get(0);
    data.put(JsonKey.ID, "id");
    promise.success(esMap);

    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
    when(Util.updateChannel(Mockito.anyMap(), Mockito.any())).thenReturn(true);
    Map<String, Object> map = getRequestDataForOrgUpdate();
    map.put(JsonKey.IS_TENANT, true);
    boolean result =
        testScenario(
            getRequest(map, ActorOperations.UPDATE_ORG.getValue()),
            ResponseCode.channelUniquenessInvalid);
    assertTrue(result);
  }

  @Test
  public void testCreateOrgFail2() throws Exception {
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getRecordsByProperty(true));
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getSuccess());
    Promise<Map<String, Object>> promise = Futures.promise();
    Map<String, Object> esMap = getValidateChannelEsResponse(true);
    esMap.put(JsonKey.CONTENT, new ArrayList<>());
    promise.success(esMap);
    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
    PowerMockito.mockStatic(LocationClientImpl.class);
    LocationClient locationClient = mock(LocationClientImpl.class);
    when(LocationClientImpl.getInstance()).thenReturn(locationClient);
    LocationClientImpl client = mock(LocationClientImpl.class);
    whenNew(LocationClientImpl.class).withNoArguments().thenReturn(client);
    List<String> locList = new ArrayList<>();
    locList.add("54646");
    LocationRequestValidator locationRequestValidator =
        PowerMockito.mock(LocationRequestValidator.class);
    whenNew(LocationRequestValidator.class).withNoArguments().thenReturn(locationRequestValidator);
    when(locationRequestValidator.getHierarchyLocationIds(Mockito.any(), Mockito.anyList()))
        .thenReturn(locList);
    when(locationClient.getLocationByIds(Mockito.any(), Mockito.anyList(), Mockito.any()))
        .thenReturn(getLocationLists());
    Map<String, Object> req = getRequestDataForOrgCreate(basicRequestData);
    req.put(JsonKey.HASHTAGID, "orgId");
    req.put(JsonKey.IS_TENANT, true);
    List<String> locCode = new ArrayList<>();
    locCode.add("state");
    req.put(JsonKey.LOCATION_CODE, locCode);
    Request reqst = getRequest(req, ActorOperations.CREATE_ORG.getValue());
    boolean result = testScenario(reqst, ResponseCode.invalidParameterValue);
    assertTrue(result);
  }

  @Test
  public void testCreateOrgFail1() throws Exception {
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getRecordsByProperty(true));
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getSuccess());
    Promise<Map<String, Object>> promise = Futures.promise();
    Map<String, Object> esMap = getValidateChannelEsResponse(true);
    esMap.put(JsonKey.CONTENT, new ArrayList<>());
    promise.success(esMap);
    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
    PowerMockito.mockStatic(LocationClientImpl.class);
    LocationClient locationClient = mock(LocationClientImpl.class);
    when(LocationClientImpl.getInstance()).thenReturn(locationClient);
    LocationClientImpl client = mock(LocationClientImpl.class);
    whenNew(LocationClientImpl.class).withNoArguments().thenReturn(client);
    List<String> locList = new ArrayList<>();
    locList.add("54646");
    LocationRequestValidator locationRequestValidator =
        PowerMockito.mock(LocationRequestValidator.class);
    whenNew(LocationRequestValidator.class).withNoArguments().thenReturn(locationRequestValidator);
    when(locationRequestValidator.getHierarchyLocationIds(Mockito.any(), Mockito.anyList()))
        .thenReturn(locList);
    when(locationClient.getLocationByIds(Mockito.any(), Mockito.anyList(), Mockito.any()))
        .thenReturn(getLocationLists());
    Map<String, Object> req = getRequestDataForOrgCreate(basicRequestData);
    req.put(JsonKey.HASHTAGID, "orgId");
    req.put(JsonKey.IS_TENANT, true);
    List<Map<String, String>> orgLocation = new ArrayList<>();
    Map<String, String> orgLoc1 = new HashMap<>();
    orgLoc1.put(JsonKey.ID, "54646");
    orgLoc1.put(JsonKey.TYPE, "state");
    orgLocation.add(orgLoc1);
    req.put(JsonKey.ORG_LOCATION, orgLocation);
    Request reqst = getRequest(req, ActorOperations.CREATE_ORG.getValue());
    boolean result = testScenario(reqst, ResponseCode.invalidParameterValue);
    assertTrue(result);
  }

  @Test
  public void testCreateOrgFail3() throws Exception {
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getRecordsByProperty(true));
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getSuccess());
    Promise<Map<String, Object>> promise = Futures.promise();
    Map<String, Object> esMap = getValidateChannelEsResponse(true);
    esMap.put(JsonKey.CONTENT, new ArrayList<>());
    promise.success(esMap);
    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
    PowerMockito.mockStatic(LocationClientImpl.class);
    LocationClient locationClient = mock(LocationClientImpl.class);
    when(LocationClientImpl.getInstance()).thenReturn(locationClient);
    LocationClientImpl client = mock(LocationClientImpl.class);
    whenNew(LocationClientImpl.class).withNoArguments().thenReturn(client);
    List<String> locList = new ArrayList<>();
    locList.add("54646");
    LocationRequestValidator locationRequestValidator =
        PowerMockito.mock(LocationRequestValidator.class);
    whenNew(LocationRequestValidator.class).withNoArguments().thenReturn(locationRequestValidator);
    when(locationRequestValidator.getHierarchyLocationIds(Mockito.any(), Mockito.anyList()))
        .thenReturn(locList);
    when(locationClient.getLocationByIds(Mockito.any(), Mockito.anyList(), Mockito.any()))
        .thenReturn(getLocationLists());
    Map<String, Object> req = getRequestDataForOrgCreate(basicRequestData);
    req.put(JsonKey.HASHTAGID, "orgId");
    req.put(JsonKey.IS_TENANT, true);
    List<Map<String, String>> orgLocation = new ArrayList<>();
    Map<String, String> orgLoc1 = new HashMap<>();
    orgLoc1.put(JsonKey.CODE, "ST001");
    orgLoc1.put(JsonKey.TYPE, "state");
    orgLocation.add(orgLoc1);
    req.put(JsonKey.ORG_LOCATION, orgLocation);
    Request reqst = getRequest(req, ActorOperations.CREATE_ORG.getValue());
    boolean result = testScenario(reqst, ResponseCode.invalidParameterValue);
    assertTrue(result);
  }

  public List<Location> getLocationLists() {
    List<Location> locations = new ArrayList<>();
    Location location = new Location();
    location.setType(JsonKey.STATE);
    location.setCode("locationCode");
    location.setId("54646");
    locations.add(location);
    return locations;
  }

  private Response getSuccess() {
    Response res = new Response();
    res.setResponseCode(ResponseCode.OK);
    return res;
  }

  private Map<String, Object> getRequestDataForOrgUpdate() {
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.CHANNEL, "channel");
    map.put(JsonKey.ORGANISATION_ID, "orgId");
    map.put(JsonKey.ORG_TYPE, "board");
    return map;
  }

  private Map<String, Object> getRequestDataForOrgCreate(Map<String, Object> map) {
    map.put(JsonKey.CHANNEL, "channel");
    map.put(JsonKey.IS_TENANT, false);
    map.put(JsonKey.EXTERNAL_ID, "externalId");
    map.put(JsonKey.ORG_TYPE, "board");

    return map;
  }

  private Response getRecordsByProperty(boolean empty) {
    Response res = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    if (!empty) {
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.ID, "orgId");
      map.put(JsonKey.IS_DELETED, true);
      map.put(JsonKey.CHANNEL, "channel1");
      map.put(JsonKey.IS_TENANT, true);
      list.add(map);
    }
    res.put(JsonKey.RESPONSE, list);
    return res;
  }

  private Response getUpsertRecords() {
    Response res = new Response();
    res.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    return res;
  }

  private Response getAllRecords() {
    Response res = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ID, "id");
    map.put(JsonKey.NAME, "orgType");
    list.add(map);
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
      content.put(JsonKey.ID, "id");
      content.put(JsonKey.ORGANISATION_TYPE, 2);
      contentList.add(content);
    }
    response.put(JsonKey.CONTENT, contentList);
    return response;
  }

  private Map<String, Object> getByIdEsResponse(boolean empty) {
    Map<String, Object> response = new HashMap<>();
    if (!empty) {
      response.put(JsonKey.ORGANISATION_ID, "orgId");
      response.put(JsonKey.HASHTAGID, "hashtagId");
      response.put(JsonKey.ID, "id");
      response.put(JsonKey.ORGANISATION_TYPE, 2);
    }
    return response;
  }

  private Map<String, Object> getValidateChannelEsResponse(boolean isValidChannel) {
    Map<String, Object> response = new HashMap<>();
    List<Map<String, Object>> contentList = new ArrayList<>();
    if (isValidChannel) {
      Map<String, Object> content = new HashMap<>();
      content.put(JsonKey.STATUS, 1);
      content.put(JsonKey.ID, "orgId");
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
      Response res = probe.expectMsgClass(duration("100 second"), Response.class);
      return null != res && res.getResponseCode() == ResponseCode.OK;
    } else {
      ProjectCommonException res =
          probe.expectMsgClass(duration("100 second"), ProjectCommonException.class);
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
