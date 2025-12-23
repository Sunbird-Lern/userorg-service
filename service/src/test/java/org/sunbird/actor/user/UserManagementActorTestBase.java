package org.sunbird.actor.user;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.eq;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.dispatch.Futures;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.pattern.PipeToSupport;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.apache.pekko.util.Timeout;
import java.util.*;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.Constants;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.dto.SearchDTO;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.location.Location;
import org.sunbird.model.organisation.Organisation;
import org.sunbird.model.user.User;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.organisation.OrgService;
import org.sunbird.service.organisation.impl.OrgExternalServiceImpl;
import org.sunbird.service.organisation.impl.OrgServiceImpl;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.UserLookUpServiceImpl;
import org.sunbird.service.user.impl.UserRoleServiceImpl;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.util.DataCacheHandler;
import org.sunbird.util.FormApiUtilHandler;
import org.sunbird.util.Util;
import org.sunbird.util.user.UserUtil;
import scala.concurrent.Future;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  EsClientFactory.class,
  Util.class,
  UserService.class,
  UserServiceImpl.class,
  OrgServiceImpl.class,
  OrgService.class,
  UserUtil.class,
  Patterns.class,
  DataCacheHandler.class,
  ElasticSearchRestHighImpl.class,
  PipeToSupport.PipeableFuture.class,
  FormApiUtilHandler.class,
  UserLookUpServiceImpl.class,
  ActorSelection.class,
  OrgExternalServiceImpl.class,
  UserRoleServiceImpl.class
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
public abstract class UserManagementActorTestBase {

  public ActorSystem system = ActorSystem.create("system");
  public final Props props = Props.create(SSOUserCreateActor.class);
  public static Map<String, Object> reqMap;
  public static UserServiceImpl userService;
  public static OrgServiceImpl orgService;
  public static CassandraOperationImpl cassandraOperation;
  public static ElasticSearchService esService;
  public static UserLookUpServiceImpl userLookupService;
  public static UserRoleServiceImpl userRoleService;

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(ServiceFactory.class);
    PowerMockito.mockStatic(EsClientFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    esService = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getSuccessResponse());
    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getSuccessResponse());
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getOrgFromCassandra());
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(getOrgandLocationFromCassandra());

    PowerMockito.mockStatic(Patterns.class);
    Future<Object> future = Futures.future(() -> getEsResponse(), system.dispatcher());
    when(Patterns.ask(
            Mockito.any(ActorRef.class), Mockito.any(Request.class), Mockito.any(Timeout.class)))
        .thenReturn(future);

    PowerMockito.mockStatic(FormApiUtilHandler.class);
    PowerMockito.when(FormApiUtilHandler.getFormApiConfig(eq("locationCode1"), Mockito.any()))
        .thenReturn(getFormApiConfig());
    PowerMockito.when(FormApiUtilHandler.getFormApiConfig(eq("default"), Mockito.any()))
        .thenReturn(getFormApiConfig());

    Map<String, Object> locDetails = new HashMap<>();
    locDetails.put(JsonKey.CODE, "locationCode");
    locDetails.put(JsonKey.TYPE, JsonKey.STATE);
    List<Map<String, Object>> locList = new ArrayList<>();
    locList.add(locDetails);
    Map<String, Object> esSearchRes = new HashMap<>();
    esSearchRes.put(JsonKey.CONTENT, locList);
    Promise<Map<String, Object>> esResF = Futures.promise();
    esResF.success(esSearchRes);
    when(esService.search(
            Mockito.any(SearchDTO.class), Mockito.anyString(), Mockito.any(RequestContext.class)))
        .thenReturn(esResF.future());

    PowerMockito.mockStatic(OrgServiceImpl.class);
    orgService = mock(OrgServiceImpl.class);
    when(OrgServiceImpl.getInstance()).thenReturn(orgService);
    when(orgService.getRootOrgIdFromChannel(Mockito.anyString(), Mockito.any()))
        .thenReturn("anyId");
    when(orgService.getRootOrgIdFromChannel(Mockito.anyString(), Mockito.any()))
        .thenReturn("rootOrgId");

    PowerMockito.mockStatic(UserServiceImpl.class);
    userService = mock(UserServiceImpl.class);
    when(UserServiceImpl.getInstance()).thenReturn(userService);
    when(userService.getUserById(Mockito.any(), Mockito.any())).thenReturn(getUser(false));
    when(userService.saveUserAttributes(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(getSaveResponse());
    when(userService.createUser(Mockito.anyMap(), Mockito.any())).thenReturn(getSuccessResponse());
    when(userService.updateUser(Mockito.anyMap(), Mockito.any())).thenReturn(getSuccessResponse());
    PowerMockito.mockStatic(UserLookUpServiceImpl.class);
    userLookupService = mock(UserLookUpServiceImpl.class);
    when(UserLookUpServiceImpl.getInstance()).thenReturn(userLookupService);
    when(userLookupService.insertRecords(Mockito.anyMap(), Mockito.any()))
        .thenReturn(getSuccessResponse());
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getEsResponseMap());
    when(esService.getDataByIdentifier(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
    Promise<String> esPromise = Futures.promise();
    esPromise.success("success");
    when(esService.save(Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(esPromise.future());

    PowerMockito.mockStatic(UserUtil.class);
    UserUtil.setUserDefaultValue(Mockito.anyMap(), Mockito.any());
    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(JsonKey.ROOT_ORG_ID, "rootOrgId");
    requestMap.put(JsonKey.TNC_ACCEPTED_ON, 12345678L);
    requestMap.put(JsonKey.USERNAME, "username");
    requestMap.put(JsonKey.EMAIL, "username@gmail.com");
    requestMap.put(JsonKey.PHONE, "4346345377");
    List externalIds = new ArrayList();
    Map externalId = new HashMap();
    externalId.put(JsonKey.ID, "extid1e2d");
    externalId.put(JsonKey.ID_TYPE, "channel1003");
    externalId.put(JsonKey.PROVIDER, "channel1003");
    externalId.put(JsonKey.OPERATION, "add");
    externalIds.add(externalId);
    requestMap.put(JsonKey.EXTERNAL_IDS, externalIds);
    PowerMockito.mockStatic(Util.class);
    when(userService.getUserDetailsForES(Mockito.any(), Mockito.any())).thenReturn(getMapObject());
    when(UserUtil.encryptUserData(Mockito.anyMap())).thenReturn(requestMap);
    PowerMockito.mockStatic(DataCacheHandler.class);
    when(DataCacheHandler.getRoleMap()).thenReturn(roleMap(true));
    when(DataCacheHandler.getUserTypesConfig()).thenReturn(getUserTypes());
    when(DataCacheHandler.getLocationTypeConfig()).thenReturn(getLocationTypeConfig());
    when(UserUtil.getActiveUserOrgDetails(Mockito.anyString(), Mockito.any()))
        .thenReturn(getUserOrgDetails());
    Map<String, String> configMap = new HashMap<>();
    configMap.put(JsonKey.CUSTODIAN_ORG_CHANNEL, "channel");
    configMap.put(JsonKey.CUSTODIAN_ORG_ID, "custodianRootOrgId");
    when(DataCacheHandler.getConfigSettings()).thenReturn(configMap);
    reqMap = getMapObject();
    Organisation organisation = new Organisation();
    organisation.setId("rootOrgId");
    organisation.setChannel("anyChannel");
    organisation.setRootOrgId("rootOrgId");
    organisation.setTenant(true);
    when(orgService.getOrgObjById(Mockito.anyString(), Mockito.any())).thenReturn(organisation);
    Map<String, Object> user = new HashMap<>();
    user.put(JsonKey.IS_DELETED, false);
    user.put(JsonKey.ROOT_ORG_ID, "custodianRootOrgId");
    user.putAll(getMapObject());
    when(UserUtil.validateExternalIdsAndReturnActiveUser(Mockito.anyMap(), Mockito.any()))
        .thenReturn(user);

    List<Map<String, Object>> userRoleListResponse = new ArrayList<>();
    List<Map<String, Object>> scopeList = new ArrayList<>();
    Map<String, Object> scopeMap = new HashMap();
    scopeMap.put(JsonKey.ORGANISATION_ID, "someOrg");
    scopeList.add(scopeMap);
    Map<String, Object> userRoleMap = new HashMap();
    userRoleMap.put(JsonKey.ROLE, "role");
    userRoleMap.put(JsonKey.SCOPE, scopeList);
    userRoleListResponse.add(userRoleMap);

    PowerMockito.mockStatic(UserRoleServiceImpl.class);
    userRoleService = mock(UserRoleServiceImpl.class);
    when(UserRoleServiceImpl.getInstance()).thenReturn(userRoleService);
    when(userRoleService.updateUserRole(Mockito.anyMap(), Mockito.any()))
        .thenReturn(userRoleListResponse);
  }

  public List<Map<String, String>> getLocationIdTypeList() {
    Map<String, String> idType = new HashMap<>();
    idType.put(JsonKey.ID, "locationId");
    idType.put(JsonKey.TYPE, "locationType");
    List<Map<String, String>> idTypeList = new ArrayList<>();
    idTypeList.add(idType);
    return idTypeList;
  }

  public List<Location> getLocationLists() {
    List<Location> locations = new ArrayList<>();
    Location location = new Location();
    location.setType(JsonKey.STATE);
    location.setCode("locationCode");
    locations.add(location);
    return locations;
  }

  public List<String> getLocationIdLists() {
    return Arrays.asList("id");
  }

  public Map<String, Object> getFormApiConfig() {
    Map<String, Object> formData = new HashMap<>();
    Map<String, Object> formMap = new HashMap<>();
    Map<String, Object> dataMap = new HashMap<>();
    List<Map<String, Object>> fieldsList = new ArrayList<>();
    Map<String, Object> field = new HashMap<>();
    Map<String, Object> children = new HashMap<>();
    List<Map<String, Object>> userTypeConfigList = new ArrayList<>();

    Map<String, Object> subPersonConfig = new HashMap<>();
    Map<String, Object> templateOptionsMap = new HashMap<>();
    List<Map<String, String>> options = new ArrayList<>();
    Map<String, String> option = new HashMap<>();
    option.put(JsonKey.VALUE, "crc");
    options.add(option);
    templateOptionsMap.put(JsonKey.OPTIONS, options);
    subPersonConfig.put(JsonKey.CODE, JsonKey.SUB_PERSONA);
    subPersonConfig.put(JsonKey.TEMPLATE_OPTIONS, templateOptionsMap);
    userTypeConfigList.add(subPersonConfig); // For subpersona config

    Map<String, Object> stateConfig = new HashMap<>();
    stateConfig.put(JsonKey.CODE, JsonKey.STATE);
    userTypeConfigList.add(stateConfig); // For state config

    children.put("teacher", userTypeConfigList);
    field.put(JsonKey.CODE, JsonKey.PERSONA);
    field.put(JsonKey.CHILDREN, children);
    fieldsList.add(field);
    dataMap.put(JsonKey.FIELDS, fieldsList);
    formMap.put(JsonKey.DATA, dataMap);
    formData.put(JsonKey.FORM, formMap);
    return formData;
  }

  public Response getOrgFromCassandra() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ORG_ID, "anyOrgId");
    list.add(map);
    response.put(Constants.RESPONSE, list);
    return response;
  }

  public Response getOrgandLocationFromCassandra() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ORG_ID, "anyOrgId");
    map.put(JsonKey.LOCATION_IDS, new ArrayList<String>(Arrays.asList("anyLocationId")));
    list.add(map);
    response.put(Constants.RESPONSE, list);
    return response;
  }

  public void mockForUserOrgUpdate() {
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(getListOrgResponse());
    when(esService.search(Mockito.any(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
    when(userService.getUserById(Mockito.anyString(), Mockito.any())).thenReturn(getUser(false));
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(null);
  }

  public Map<String, Object> getListOrgResponse() {
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ID, "org1");
    map.put(JsonKey.HASHTAGID, "hashtagId");
    map.put(JsonKey.CONTACT_DETAILS, "any");
    Map<String, Object> response = new HashMap<>();
    List<Map<String, Object>> content = new ArrayList<>();
    content.add(map);
    response.put(JsonKey.CONTENT, content);
    return response;
  }

  public Map<String, Object> getUserOrgUpdateRequest(boolean validOrgReq) {
    Map<String, Object> req = new HashMap<>();
    req.put(JsonKey.USER_ID, "userId");
    req.put(JsonKey.ORGANISATIONS, "any");
    if (validOrgReq) {
      Map<String, Object> map = new HashMap<>();
      List<Map<String, Object>> list = new ArrayList<>();
      map.put(JsonKey.ORGANISATION_ID, "org1");
      map.put(JsonKey.ROLES, Arrays.asList("PUBLIC"));
      list.add(map);
      req.put(JsonKey.ORGANISATIONS, list);
    }
    return req;
  }

  public List<Map<String, Object>> getUserOrgDetails() {
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ORGANISATION_ID, "any");
    list.add(map);
    return list;
  }

  public Map<String, Object> roleMap(boolean validRole) {
    Map<String, Object> map = new HashMap<>();
    if (validRole) {
      map.put("PUBLIC", "PUBLIC");
    }
    map.put("Invalid", "Invalid");
    return map;
  }

  public Map<String, Object> getAdditionalMapData(Map<String, Object> reqMap) {
    reqMap.put(JsonKey.ORGANISATION_ID, "anyOrgId");
    reqMap.put(JsonKey.CHANNEL, "anyChannel");
    return reqMap;
  }

  public Map<String, Map<String, List<String>>> getUserTypes() {
    Map<String, Map<String, List<String>>> userTypeOrSubTypeConfigMap = new HashMap<>();
    Map<String, List<String>> userTypeConfigMap = new HashMap<>();
    userTypeConfigMap.put("default", Arrays.asList());
    userTypeConfigMap.put("STUDENT", Arrays.asList());
    userTypeConfigMap.put("ADMINISTRATOR", Arrays.asList("BRC", "DAO"));
    userTypeConfigMap.put("TEACHER", Arrays.asList());
    userTypeConfigMap.put("GUARDIAN", Arrays.asList());
    return userTypeOrSubTypeConfigMap;
  }

  public Map<String, List<String>> getLocationTypeConfig() {
    Map<String, List<String>> locationTypeConfig = new HashMap<>();
    locationTypeConfig.put("locationCode1", Arrays.asList("school", "state"));
    // locationTypeConfig.put("default", Arrays.asList("school", "state"));
    return locationTypeConfig;
  }

  public Map<String, Object> getExternalIdMap() {

    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.EXTERNAL_ID, "externalId");
    reqMap.put(JsonKey.EXTERNAL_ID_PROVIDER, "externalIdProvider");
    reqMap.put(JsonKey.EXTERNAL_ID_TYPE, "externalIdType");
    return reqMap;
  }

  public HashMap<String, Object> getMapObject() {

    HashMap<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.FIRST_NAME, "firstname");
    reqMap.put(JsonKey.USERNAME, "userName");
    reqMap.put(JsonKey.EMAIL, "email@email.com");
    reqMap.put(JsonKey.LANGUAGE, new ArrayList<>());
    reqMap.put(JsonKey.DOB, "1992");
    return reqMap;
  }

  public Request getRequest(
      boolean isCallerIdReq,
      boolean isRootOrgIdReq,
      boolean isVersionReq,
      Map<String, Object> reqMap,
      ActorOperations actorOperation) {

    Request reqObj = new Request();
    HashMap<String, Object> innerMap = new HashMap<>();
    if (isCallerIdReq) innerMap.put(JsonKey.CALLER_ID, "anyCallerId");
    if (isVersionReq) innerMap.put(JsonKey.VERSION, "v2");
    if (isRootOrgIdReq) innerMap.put(JsonKey.ROOT_ORG_ID, "MY_ROOT_ORG_ID");
    innerMap.put(JsonKey.REQUESTED_BY, "requestedBy");
    reqObj.setRequest(reqMap);
    reqObj.setContext(innerMap);
    reqObj.setOperation(actorOperation.getValue());
    reqObj.setRequestContext(new RequestContext());
    return reqObj;
  }

  public Map<String, Object> getUpdateRequestWithLocationCodes() {
    Map<String, Object> reqObj = new HashMap();
    reqObj.put(JsonKey.LOCATION_CODES, Arrays.asList("locationCode"));
    reqObj.put(JsonKey.USER_ID, "userId");
    getUpdateRequestWithDefaultFlags(reqObj);
    return reqObj;
  }

  public Map<String, Object> getUpdateRequestWithLocationCodeSchoolAsOrgExtId() {
    Map<String, Object> reqObj = new HashMap();
    reqObj.put(JsonKey.ORG_EXTERNAL_ID, "orgExtId");
    reqObj.put(JsonKey.USER_ID, "userId");
    reqObj.put("updateUserSchoolOrg", true);
    getUpdateRequestWithDefaultFlags(reqObj);
    return reqObj;
  }

  public Map<String, Object> getUpdateRequestWithDefaultFlags(Map<String, Object> reqObj) {
    reqObj.put(JsonKey.STATE_VALIDATED, false);
    return reqObj;
  }

  public static Response getEsResponse() {

    Response response = new Response();
    Map<String, Object> map = new HashMap<>();
    map.put("anyString", new Object());
    response.put(JsonKey.RESPONSE, map);
    return response;
  }

  public static Response getSuccessResponse() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    return response;
  }

  public static Map<String, Object> getEsResponseMap() {
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.IS_TENANT, true);
    map.put(JsonKey.ID, "rootOrgId");
    map.put(JsonKey.CHANNEL, "anyChannel");
    return map;
  }

  public Object getEsResponseForLocation() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, Arrays.asList("id"));
    return response;
  }

  public User getUser(boolean isCustodian) {
    User user = new User();
    user.setRootOrgId("rootOrgId");
    user.setLocationIds(Arrays.asList("id"));
    if (isCustodian) {
      user.setRootOrgId("custodianOrgId");
    }
    return user;
  }

  public Response getSaveResponse() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, new HashMap<String, Object>());
    return response;
  }

  public List<Map<String, String>> getLocationIdType() {
    List<Map<String, String>> locationIdType = new ArrayList<>();
    Map<String, String> idType = new HashMap<>();
    idType.put(JsonKey.ID, "id");
    idType.put(JsonKey.TYPE, "type");
    locationIdType.add(idType);
    return locationIdType;
  }

  public boolean testScenario(Request reqObj, ResponseCode errorCode, Props props) {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    subject.tell(reqObj, probe.getRef());

    if (errorCode == null) {
      Response res = probe.expectMsgClass(Duration.ofSeconds(1000), Response.class);
      return null != res && res.getResponseCode() == ResponseCode.OK;
    } else {
      String errString = ActorOperations.getOperationCodeByActorOperation(reqObj.getOperation());
      ProjectCommonException res =
          probe.expectMsgClass(Duration.ofSeconds(1000), ProjectCommonException.class);
      return res.getErrorCode().equals("UOS_" + errString + errorCode.getErrorCode())
          || res.getErrorResponseCode() == errorCode.getResponseCode();
    }
  }

  public boolean testScenario(Request reqObj, ResponseCode errorCode) {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    subject.tell(reqObj, probe.getRef());

    if (errorCode == null) {
      Response res = probe.expectMsgClass(Duration.ofSeconds(1000), Response.class);
      return null != res && res.getResponseCode() == ResponseCode.OK;
    } else {
      ProjectCommonException res =
          probe.expectMsgClass(Duration.ofSeconds(1000), ProjectCommonException.class);
      return res.getResponseCode().name().equals(errorCode.name())
          || res.getErrorResponseCode() == errorCode.getResponseCode();
    }
  }

  public boolean testScenario(
      Request reqObj, ResponseCode errorCode, ResponseCode responseCode, Props props) {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    subject.tell(reqObj, probe.getRef());

    if (responseCode != null) {
      Response res = probe.expectMsgClass(Duration.ofSeconds(10), Response.class);
      return null != res && res.getResponseCode() == responseCode;
    }
    if (errorCode != null) {
      ProjectCommonException res =
          probe.expectMsgClass(Duration.ofSeconds(10), ProjectCommonException.class);
      return res.getErrorCode().equals(errorCode.getErrorCode())
          || res.getErrorResponseCode() == errorCode.getResponseCode();
    }
    return true;
  }
}
