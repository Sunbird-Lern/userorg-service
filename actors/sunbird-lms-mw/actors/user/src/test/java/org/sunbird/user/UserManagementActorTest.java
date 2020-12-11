package org.sunbird.user;

import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.dispatch.Futures;
import akka.pattern.Patterns;
import akka.util.Timeout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.Util;
import org.sunbird.user.util.UserUtil;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.Promise;

public class UserManagementActorTest extends UserManagementActorTestBase {

  @Test
  public void testCreateUserSuccessWithUserCallerId() {

    boolean result =
        testScenario(
            getRequest(true, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_USER),
            null);
    assertTrue(result);
  }

  @Test
  public void testCreateUserSuccessWithoutUserCallerId() {

    boolean result =
        testScenario(
            getRequest(
                false, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_USER),
            null);
    assertTrue(result);
  }

  @Test
  public void testCreateUserSuccessWithOrgExternalId() {
    reqMap.put(JsonKey.ORG_EXTERNAL_ID, "any");
    boolean result =
        testScenario(
            getRequest(
                false, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_USER),
            null);
    assertTrue(result);
  }

  @Test
  public void testCreateUserSuccessWithoutUserCallerIdChannelAndRootOrgId() {

    boolean result =
        testScenario(getRequest(false, false, true, reqMap, ActorOperations.CREATE_USER), null);
    assertTrue(result);
  }

  @Test
  public void testCreateUserFailureWithInvalidChannelAndOrgId() {

    reqMap.put(JsonKey.CHANNEL, "anyReqChannel");
    reqMap.put(JsonKey.ORGANISATION_ID, "anyOrgId");
    boolean result =
        testScenario(
            getRequest(false, false, false, reqMap, ActorOperations.CREATE_USER),
            ResponseCode.parameterMismatch);
    assertTrue(result);
  }

  @Test
  public void testCreateUserFailureWithInvalidLocationCodes() {
    Future<Object> future = Futures.future(() -> null, system.dispatcher());
    when(Patterns.ask(
            Mockito.any(ActorRef.class), Mockito.any(Request.class), Mockito.any(Timeout.class)))
        .thenReturn(future);

    reqMap.put(JsonKey.LOCATION_CODES, Arrays.asList("invalidLocationCode"));
    boolean result =
        testScenario(
            getRequest(false, false, false, reqMap, ActorOperations.CREATE_USER),
            ResponseCode.invalidParameterValue);
    assertTrue(result);
  }

  @Test
  public void testCreateUserSuccessWithoutVersion() {

    boolean result =
        testScenario(getRequest(false, false, false, reqMap, ActorOperations.CREATE_USER), null);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserSelfDeclarations() {
    Map<String, Object> reqMap = createUpdateUserDeclrationRequests();
    boolean result =
        testScenario(
            getRequest(false, false, false, reqMap, ActorOperations.UPDATE_USER_DECLARATIONS),
            null);
    assertTrue(result);
  }

  @Test
  public void testCreateUserSuccessWithLocationCodes() {
    Future<Object> future1 = Futures.future(() -> getEsResponseForLocation(), system.dispatcher());
    Future<Object> future2 = Futures.future(() -> getEsResponse(), system.dispatcher());
    when(Patterns.ask(
            Mockito.any(ActorRef.class), Mockito.any(Request.class), Mockito.any(Timeout.class)))
        .thenReturn(future1)
        .thenReturn(future2);

    reqMap.put(JsonKey.LOCATION_CODES, Arrays.asList("locationCode"));
    boolean result =
        testScenario(getRequest(true, true, true, reqMap, ActorOperations.CREATE_USER), null);
    assertTrue(result);
  }

  @Test
  public void testCreateUserFailureWithInvalidExternalIds() {

    reqMap.put(JsonKey.EXTERNAL_IDS, "anyExternalId");
    boolean result =
        testScenario(
            getRequest(false, false, false, reqMap, ActorOperations.CREATE_USER),
            ResponseCode.dataTypeError);
    assertTrue(result);
  }

  @Test
  public void testCreateUserFailureWithInvalidRoles() {

    reqMap.put(JsonKey.ROLES, "anyRoles");
    boolean result =
        testScenario(
            getRequest(false, false, false, reqMap, ActorOperations.CREATE_USER),
            ResponseCode.dataTypeError);
    assertTrue(result);
  }

  @Test
  public void testCreateUserFailureWithInvalidCountryCode() {

    reqMap.put(JsonKey.COUNTRY_CODE, "anyCode");
    boolean result =
        testScenario(
            getRequest(false, false, false, reqMap, ActorOperations.CREATE_USER),
            ResponseCode.invalidCountryCode);
    assertTrue(result);
  }

  @Test
  public void testCreateUserFailureWithInvalidOrg() {
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(null);
    when(esService.getDataByIdentifier(
            Mockito.anyString(), Mockito.anyString(), Mockito.any(RequestContext.class)))
        .thenReturn(promise.future());
    boolean result =
        testScenario(
            getRequest(
                false, false, false, getAdditionalMapData(reqMap), ActorOperations.CREATE_USER),
            ResponseCode.invalidOrgData);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserFailureWithLocationCodes() {
    Future<Object> future2 = Futures.future(() -> null, system.dispatcher());
    when(Patterns.ask(
            Mockito.any(ActorRef.class), Mockito.any(Request.class), Mockito.any(Timeout.class)))
        .thenReturn(future2);

    when(userService.getUserById(Mockito.anyString(), Mockito.any())).thenReturn(getUser(false));
    boolean result =
        testScenario(
            getRequest(
                true, true, true, getUpdateRequestWithLocationCodes(), ActorOperations.UPDATE_USER),
            ResponseCode.invalidParameterValue);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserSuccess() {
    when(userService.getUserById(Mockito.anyString(), Mockito.any())).thenReturn(getUser(false));
    Map<String, Object> req = getExternalIdMap();
    getUpdateRequestWithDefaultFlags(req);
    boolean result =
        testScenario(getRequest(true, true, true, req, ActorOperations.UPDATE_USER), null);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserUpdateEmailSuccess() {
    when(userService.getUserById(Mockito.anyString(), Mockito.any())).thenReturn(getUser(false));
    Map<String, Object> user = new HashMap<>();
    user.put(JsonKey.PHONE, "4346345377");
    user.put(JsonKey.EMAIL, "username@gmail.com");
    user.put(JsonKey.USERNAME, "username");
    user.put(JsonKey.ROOT_ORG_ID, "rootOrgId");
    when(UserUtil.isEmailOrPhoneDiff(Mockito.anyMap(), Mockito.anyMap(), Mockito.anyString()))
        .thenReturn(true);
    when(UserUtil.validateExternalIdsAndReturnActiveUser(
            Mockito.anyMap(), Mockito.any(RequestContext.class)))
        .thenReturn(user);
    Map<String, Object> req = getExternalIdMap();
    getUpdateRequestWithDefaultFlags(req);
    boolean result =
        testScenario(getRequest(true, true, true, req, ActorOperations.UPDATE_USER), null);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserSuccessWithLocationCodes() {
    Future<Object> future1 = Futures.future(() -> getEsResponseForLocation(), system.dispatcher());
    Future<Object> future2 = Futures.future(() -> getEsResponse(), system.dispatcher());
    when(Patterns.ask(
            Mockito.any(ActorRef.class), Mockito.any(Request.class), Mockito.any(Timeout.class)))
        .thenReturn(future1)
        .thenReturn(future2);

    when(userService.getUserById(Mockito.anyString(), Mockito.any())).thenReturn(getUser(false));
    boolean result =
        testScenario(
            getRequest(
                true, true, true, getUpdateRequestWithLocationCodes(), ActorOperations.UPDATE_USER),
            null);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserSuccessWithoutUserCallerId() {
    when(userService.getUserById(Mockito.anyString(), Mockito.any())).thenReturn(getUser(false));
    Map<String, Object> req = getExternalIdMap();
    getUpdateRequestWithDefaultFlags(req);
    boolean result =
        testScenario(getRequest(false, true, true, req, ActorOperations.UPDATE_USER), null);
    assertTrue(result);
  }

  @Test
  public void testCreateUserSuccessWithUserTypeAsTeacher() {
    reqMap.put(JsonKey.USER_TYPE, JsonKey.TEACHER);

    when(userService.getRootOrgIdFromChannel(Mockito.anyString(), Mockito.any()))
        .thenReturn("rootOrgId")
        .thenReturn("");

    boolean result =
        testScenario(
            getRequest(true, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_USER),
            null);
    assertTrue(result);
  }

  @Test
  public void testCreateUserSuccessWithUserTypeAsOther() {
    reqMap.put(JsonKey.USER_TYPE, JsonKey.STUDENT);

    boolean result =
        testScenario(
            getRequest(true, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_USER),
            null);
    assertTrue(result);
  }

  @Test
  public void testCreateUserFailureWithUserTypeAsTeacherAndCustodianOrg() {
    reqMap.put(JsonKey.USER_TYPE, JsonKey.TEACHER);

    boolean result =
        testScenario(
            getRequest(false, false, true, reqMap, ActorOperations.CREATE_USER),
            ResponseCode.errorTeacherCannotBelongToCustodianOrg);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserSuccessWithUserTypeTeacher() {
    Map<String, Object> req = getExternalIdMap();
    getUpdateRequestWithDefaultFlags(req);
    req.put(JsonKey.USER_TYPE, JsonKey.TEACHER);
    when(userService.getUserById(Mockito.anyString(), Mockito.any())).thenReturn(getUser(false));
    Map<String, String> configMap = new HashMap<>();
    configMap.put(JsonKey.CUSTODIAN_ORG_CHANNEL, "channel");
    configMap.put(JsonKey.CUSTODIAN_ORG_ID, "custodianRootOrgId");
    when(DataCacheHandler.getConfigSettings()).thenReturn(configMap);
    boolean result =
        testScenario(getRequest(false, true, true, req, ActorOperations.UPDATE_USER), null);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserFailureWithUserTypeTeacher() {
    Map<String, Object> req = getExternalIdMap();
    req.put(JsonKey.USER_TYPE, JsonKey.TEACHER);
    when(userService.getUserById(Mockito.anyString(), Mockito.any())).thenReturn(getUser(false));
    Map<String, String> configMap = new HashMap<>();
    configMap.put(JsonKey.CUSTODIAN_ORG_CHANNEL, "channel");
    configMap.put(JsonKey.CUSTODIAN_ORG_ID, "rootOrgId");
    when(DataCacheHandler.getConfigSettings()).thenReturn(configMap);
    boolean result =
        testScenario(
            getRequest(false, true, true, req, ActorOperations.UPDATE_USER),
            ResponseCode.errorTeacherCannotBelongToCustodianOrg);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserOrgFailureWithoutUserIdPrivateApi() {
    Map<String, Object> req = getUserOrgUpdateRequest(false);
    Request request = getRequest(false, false, true, req, ActorOperations.UPDATE_USER);
    boolean result = testScenario(request, ResponseCode.errorUnsupportedField);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserOrgFailureWithPublicApi() {
    Map<String, Object> req = getUserOrgUpdateRequest(false);
    req.remove(JsonKey.USER_ID);
    Request request = getRequest(false, false, true, req, ActorOperations.UPDATE_USER);
    boolean result = testScenario(request, ResponseCode.mandatoryParamsMissing);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserOrgFailureWithOrganisationsPrivateApi() {
    Map<String, Object> req = getUserOrgUpdateRequest(false);
    req.put(JsonKey.ORGANISATIONS, new HashMap<>());
    Request request = getRequest(false, false, true, req, ActorOperations.UPDATE_USER);
    request.getContext().put(JsonKey.PRIVATE, true);
    boolean result = testScenario(request, ResponseCode.dataTypeError);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserOrgFailureWithInvalidOrganisationsPrivateApi() {
    Map<String, Object> req = getUserOrgUpdateRequest(true);
    req.put(JsonKey.ORGANISATIONS, Arrays.asList("a", "b"));
    Request request = getRequest(false, false, true, req, ActorOperations.UPDATE_USER);
    request.getContext().put(JsonKey.PRIVATE, true);
    boolean result = testScenario(request, ResponseCode.dataTypeError);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserOrgFailureWithoutOrganisationsPrivateApi() {
    Map<String, Object> req = getUserOrgUpdateRequest(true);
    ((Map) ((List) req.get(JsonKey.ORGANISATIONS)).get(0)).put(JsonKey.ORGANISATION_ID, "");
    Request request = getRequest(false, false, true, req, ActorOperations.UPDATE_USER);
    request.getContext().put(JsonKey.PRIVATE, true);
    boolean result = testScenario(request, ResponseCode.mandatoryParamsMissing);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserOrgFailureWithInvalidRolesDataTypePrivateApi() {
    Map<String, Object> req = getUserOrgUpdateRequest(true);
    ((Map) ((List) req.get(JsonKey.ORGANISATIONS)).get(0)).put(JsonKey.ROLES, "String");
    Request request = getRequest(false, false, true, req, ActorOperations.UPDATE_USER);
    request.getContext().put(JsonKey.PRIVATE, true);
    boolean result = testScenario(request, ResponseCode.dataTypeError);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserOrgFailureWithEmptyRolesReqPrivateApi() {
    Map<String, Object> req = getUserOrgUpdateRequest(true);
    ((Map) ((List) req.get(JsonKey.ORGANISATIONS)).get(0)).put(JsonKey.ROLES, new ArrayList<>());
    Request request = getRequest(false, false, true, req, ActorOperations.UPDATE_USER);
    request.getContext().put(JsonKey.PRIVATE, true);
    boolean result = testScenario(request, ResponseCode.emptyRolesProvided);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserOrgSuccessPrivateApi() {
    Map<String, Object> req = getUserOrgUpdateRequest(true);
    getUpdateRequestWithDefaultFlags(req);
    Request request = getRequest(false, false, true, req, ActorOperations.UPDATE_USER);
    request.getContext().put(JsonKey.PRIVATE, true);
    mockForUserOrgUpdate();
    boolean result = testScenario(request, null);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserOrgSuccessWithoutRolesPrivateApi() {
    Map<String, Object> req = getUserOrgUpdateRequest(true);
    getUpdateRequestWithDefaultFlags(req);
    Request request = getRequest(false, false, true, req, ActorOperations.UPDATE_USER);
    request.getContext().put(JsonKey.PRIVATE, true);
    mockForUserOrgUpdate();
    boolean result = testScenario(request, null);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserOrgFailureWithInvalidRolesPrivateApi() {
    Map<String, Object> req = getUserOrgUpdateRequest(true);
    Request request = getRequest(false, false, true, req, ActorOperations.UPDATE_USER);
    request.getContext().put(JsonKey.PRIVATE, true);
    mockForUserOrgUpdate();
    when(DataCacheHandler.getRoleMap()).thenReturn(roleMap(false));
    boolean result = testScenario(request, ResponseCode.invalidRole);
    assertTrue(result);
  }

  // @Test
  public void testCreateUserSuccessWithUserSync() {
    reqMap.put("sync", true);
    PowerMockito.mockStatic(Util.class);
    Map<String, Object> user = getEsResponseMap();
    user.put(JsonKey.USER_ID, "123456789");
    when(Util.getUserDetails(Mockito.anyString(), Mockito.any())).thenReturn(user);
    /*PipeToSupport.PipeableFuture pipe = PowerMockito.mock(PipeToSupport.PipeableFuture.class);
    Future<Map<String,Object>> future1 =
      Futures.future(() -> reqMap, system.dispatcher());
    when(pipe.to(Mockito.any(ActorRef.class))).thenReturn(future1);
    when(Patterns.pipe(Mockito.any(Future.class), Mockito.any())).thenReturn(pipe);*/

    boolean result =
        testScenario(
            getRequest(true, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_USER),
            null);
    assertTrue(true);
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
            null);
    assertTrue(true);
  }

  @Test
  @Ignore
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
            getRequest(false, false, false, reqMap, ActorOperations.GET_MANAGED_USERS), null);
    assertTrue(result);
  }

  private Map<String, Object> getSearchResults() {
    Map<String, Object> searchRequestMap = new HashMap<>();

    return searchRequestMap;
  }
}
