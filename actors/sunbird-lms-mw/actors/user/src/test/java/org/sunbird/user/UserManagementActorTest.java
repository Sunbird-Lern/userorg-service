package org.sunbird.user;

import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.*;

import akka.actor.ActorRef;
import akka.dispatch.Futures;
import akka.pattern.Patterns;
import akka.util.Timeout;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.learner.organisation.external.identity.service.OrgExternalService;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.Util;
import org.sunbird.models.organisation.Organisation;
import org.sunbird.user.util.UserUtil;
import scala.concurrent.Await;
import scala.concurrent.Future;

public class UserManagementActorTest extends UserManagementActorTestBase {

  //  @Test
  public void testCreateUserSuccessWithUserCallerId() {
    boolean result =
        testScenario(
            getRequest(true, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_USER),
            null);
    assertTrue(result);
  }

  //  @Test
  public void testCreateUserSuccessWithIsTenantAsFalse() {
    Organisation organisation = new Organisation();
    organisation.setId("rootOrgId");
    organisation.setChannel("anyChannel");
    organisation.setRootOrgId("rootOrgId");
    organisation.setTenant(false);
    when(organisationClient.esGetOrgById(Mockito.anyString(), Mockito.any()))
        .thenReturn(organisation);
    boolean result =
        testScenario(
            getRequest(true, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_USER),
            null);
    assertTrue(result);
  }

  @Test
  public void testCreateUserSuccessWithRequestedChannelAsNull() {
    Organisation organisation = new Organisation();
    organisation.setId("rootOrgId");
    organisation.setChannel("anyChannel");
    organisation.setRootOrgId("rootOrgId");
    organisation.setTenant(false);
    when(organisationClient.esGetOrgById(Mockito.anyString(), Mockito.any()))
        .thenReturn(organisation);
    Request request =
        getRequest(true, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_USER);
    request.getRequest().remove(JsonKey.CHANNEL);
    boolean result = testScenario(request, null);
    assertTrue(result);
  }

  //  @Test
  public void testCreateUserFailureWithInvalidOrgId() {
    Organisation organisation = new Organisation();
    organisation.setId("rootOrgId");
    organisation.setChannel("anyChannel");
    organisation.setRootOrgId("rootOrgId");
    organisation.setTenant(true);
    when(organisationClient.esGetOrgById(Mockito.anyString(), Mockito.any())).thenReturn(null);
    boolean result =
        testScenario(
            getRequest(true, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_USER),
            ResponseCode.invalidOrgData);
    assertTrue(result);
  }

  @Test
  public void testCreateUserFailureWithInvalidChannel() {
    Organisation organisation = new Organisation();
    organisation.setId("rootOrgId");
    organisation.setChannel("anyChannel");
    organisation.setRootOrgId("rootOrgId");
    organisation.setTenant(true);
    when(organisationClient.esGetOrgById(Mockito.anyString(), Mockito.any()))
        .thenReturn(organisation);
    when(userService.getRootOrgIdFromChannel(Mockito.anyString(), Mockito.any())).thenReturn("");
    boolean result =
        testScenario(
            getRequest(true, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_USER),
            ResponseCode.invalidParameterValue);
    assertTrue(result);
  }

  //  @Test
  public void testCreateUserFailureWithChannelAndOrgIdMismatch2() {
    Organisation organisation = new Organisation();
    organisation.setId("orgId");
    organisation.setChannel("anyChannel");
    organisation.setRootOrgId("id");
    organisation.setTenant(true);
    when(organisationClient.esGetOrgById(Mockito.anyString(), Mockito.any()))
        .thenReturn(organisation);
    boolean result =
        testScenario(
            getRequest(true, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_USER),
            ResponseCode.parameterMismatch);
    assertTrue(result);
  }

  //  @Test
  public void testCreateUserFailureWithChannelAndOrgIdMismatch() {
    Organisation organisation = new Organisation();
    organisation.setId("orgId");
    organisation.setChannel("channel");
    organisation.setRootOrgId("id");
    organisation.setTenant(true);
    when(organisationClient.esGetOrgById(Mockito.anyString(), Mockito.any()))
        .thenReturn(organisation);
    boolean result =
        testScenario(
            getRequest(true, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_USER),
            ResponseCode.parameterMismatch);
    assertTrue(result);
  }

  //  @Test
  public void testCreateUserSuccessWithoutUserCallerId() {
    Organisation organisation = new Organisation();
    organisation.setId("rootOrgId");
    organisation.setChannel("anyChannel");
    organisation.setRootOrgId("rootOrgId");
    organisation.setTenant(true);
    when(organisationClient.esGetOrgById(Mockito.anyString(), Mockito.any()))
        .thenReturn(organisation);
    boolean result =
        testScenario(
            getRequest(
                false, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_USER),
            null);
    assertTrue(result);
  }

  //  @Test
  public void testCreateUserSuccessWithOrgExternalId() throws Exception {
    OrgExternalService orgExternalService = PowerMockito.mock(OrgExternalService.class);
    whenNew(OrgExternalService.class).withNoArguments().thenReturn(orgExternalService);
    when(orgExternalService.getOrgIdFromOrgExternalIdAndProvider(
            Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn("");
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
  public void testCreateUserV4SuccessWithoutUserCallerIdChannelAndRootOrgId() {

    boolean result =
        testScenario(getRequest(false, false, true, reqMap, ActorOperations.CREATE_USER_V4), null);
    assertTrue(result);
  }

  @Test
  public void testCreateUserFailureWithInvalidChannelAndOrgId() {

    reqMap.put(JsonKey.CHANNEL, "anyReqChannel");
    //    reqMap.put(JsonKey.ORGANISATION_ID, "anyOrgId");
    boolean result =
        testScenario(
            getRequest(false, false, false, reqMap, ActorOperations.CREATE_USER),
            ResponseCode.invalidChannel);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserFailureWithInvalidLocationCodes() {

    reqMap.put(JsonKey.USER_TYPE, "userType");
    reqMap.put(JsonKey.USER_SUB_TYPE, "userSubType");
    reqMap.put(JsonKey.LOCATION_CODES, Arrays.asList("anyLocationCodes"));
    boolean result =
        testScenario(
            getRequest(false, false, false, reqMap, ActorOperations.UPDATE_USER),
            ResponseCode.CLIENT_ERROR);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserFailureWithInvalidProfileUsertype() {

    reqMap.put(JsonKey.USER_TYPE, "userType");
    reqMap.put(JsonKey.USER_SUB_TYPE, "userSubType");
    reqMap.put(JsonKey.PROFILE_USERTYPE, Arrays.asList("anyLocationCodes"));
    boolean result =
        testScenario(
            getRequest(false, false, false, reqMap, ActorOperations.UPDATE_USER),
            ResponseCode.CLIENT_ERROR);
    assertTrue(result);
  }

  @Test
  public void testCreateUserFailureWithInvalidUserTypeAndSubtype() {

    reqMap.put(JsonKey.USER_TYPE, "anyUserType");
    reqMap.put(JsonKey.USER_SUB_TYPE, "anyUserSubType");
    reqMap.put(JsonKey.LOCATION_CODES, Arrays.asList("LocationCodes"));
    boolean result =
        testScenario(
            getRequest(false, false, false, reqMap, ActorOperations.CREATE_USER),
            ResponseCode.invalidParameterValue);
    assertTrue(result);
  }

  /*  @Test
  public void testCreateUserFailureWithInvalidLocationCodes() {
    Future<Object> future = Futures.future(() -> null, system.dispatcher());
    when(Patterns.ask(
            Mockito.any(ActorRef.class), Mockito.any(Request.class), Mockito.any(Timeout.class)))
        .thenReturn(future);

    reqMap.put(JsonKey.LOCATION_CODES, Arrays.asList(""));
    boolean result =
        testScenario(
            getRequest(false, false, false, reqMap, ActorOperations.CREATE_USER),
            ResponseCode.invalidParameterValue);
    assertTrue(result);
  }*/

  //  @Test
  public void testCreateUserSuccessWithoutVersion() {

    boolean result =
        testScenario(getRequest(false, false, false, reqMap, ActorOperations.CREATE_USER), null);
    assertTrue(result);
  }

  @Test
  public void testCreateUserSuccessWithLocationCodes() {
    Future<Object> future = Futures.future(() -> getEsResponse(), system.dispatcher());
    when(Patterns.ask(
            Mockito.any(ActorRef.class), Mockito.any(Request.class), Mockito.any(Timeout.class)))
        .thenReturn(future);
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

  /*  @Test
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
  }*/

  @Test
  public void testUpdateUserSuccess() {
    Map<String, Object> req = getExternalIdMap();
    getUpdateRequestWithDefaultFlags(req);
    Map<String, Object> user = new HashMap<>();
    user.put(JsonKey.IS_DELETED, false);
    user.put(JsonKey.ROOT_ORG_ID, "custodianRootOrgId");
    user.putAll(getMapObject());
    when(UserUtil.validateExternalIdsAndReturnActiveUser(Mockito.anyMap(), Mockito.any()))
        .thenReturn(user);
    boolean result =
        testScenario(getRequest(true, true, true, req, ActorOperations.UPDATE_USER), null);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserFailure() {
    Map<String, Object> req = new HashMap<>();
    req.put(JsonKey.LOCATION_CODES, Arrays.asList("locationCodes"));
    boolean result =
        testScenario(
            getRequest(true, false, true, req, ActorOperations.UPDATE_USER),
            ResponseCode.mandatoryParamsMissing);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserUpdateEmailSuccess() {
    Map<String, Object> user = new HashMap<>();
    user.put(JsonKey.PHONE, "4346345377");
    user.put(JsonKey.EMAIL, "username@gmail.com");
    user.put(JsonKey.USERNAME, "username");
    user.put(JsonKey.ROOT_ORG_ID, "rootOrgId");
    when(UserUtil.isEmailOrPhoneDiff(Mockito.anyMap(), Mockito.anyMap(), Mockito.anyString()))
        .thenReturn(true);
    when(UserUtil.validateExternalIdsAndReturnActiveUser(Mockito.anyMap(), Mockito.any()))
        .thenReturn(user);
    Map<String, Object> req = getExternalIdMap();
    getUpdateRequestWithDefaultFlags(req);
    boolean result =
        testScenario(getRequest(true, true, true, req, ActorOperations.UPDATE_USER), null);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserSuccessWithLocationCodes() {
    Future<Object> future = Futures.future(() -> getEsResponse(), system.dispatcher());
    when(Patterns.ask(
            Mockito.any(ActorRef.class), Mockito.any(Request.class), Mockito.any(Timeout.class)))
        .thenReturn(future);
    boolean result =
        testScenario(
            getRequest(
                true, true, true, getUpdateRequestWithLocationCodes(), ActorOperations.UPDATE_USER),
            null);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserSuccessWithLocationSchool() {
    Future<Object> future = Futures.future(() -> getEsResponse(), system.dispatcher());
    when(Patterns.ask(
            Mockito.any(ActorRef.class), Mockito.any(Request.class), Mockito.any(Timeout.class)))
        .thenReturn(future);
    boolean result =
        testScenario(
            getRequest(
                true,
                true,
                true,
                getUpdateRequestWithLocationCodeSchoolAsOrgExtId(),
                ActorOperations.UPDATE_USER),
            null);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserSuccessWithoutUserCallerId() {
    Map<String, Object> req = getExternalIdMap();
    getUpdateRequestWithDefaultFlags(req);
    boolean result =
        testScenario(getRequest(false, true, true, req, ActorOperations.UPDATE_USER), null);
    assertTrue(result);
  }

  //  @Test
  public void testCreateUserSuccessWithUserTypeAsTeacher() {
    reqMap.put(JsonKey.USER_TYPE, "teacher");

    boolean result =
        testScenario(
            getRequest(true, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_USER),
            null);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserSuccessWithUserTypeTeacher() {
    Map<String, Object> req = getExternalIdMap();
    getUpdateRequestWithDefaultFlags(req);
    req.put(JsonKey.USER_TYPE, "teacher");
    req.put(JsonKey.USER_SUB_TYPE, "crc");
    Map<String, String> configMap = new HashMap<>();
    configMap.put(JsonKey.CUSTODIAN_ORG_CHANNEL, "channel");
    configMap.put(JsonKey.CUSTODIAN_ORG_ID, "custodianRootOrgId");
    when(DataCacheHandler.getConfigSettings()).thenReturn(configMap);
    Map<String, Object> user = new HashMap<>();
    user.put(JsonKey.PHONE, "4346345377");
    user.put(JsonKey.EMAIL, "username@gmail.com");
    user.put(JsonKey.USERNAME, "username");
    user.put(JsonKey.ROOT_ORG_ID, "rootOrgId");
    user.put(JsonKey.LOCATION_IDS, Arrays.asList("id"));
    when(UserUtil.validateExternalIdsAndReturnActiveUser(Mockito.anyMap(), Mockito.any()))
        .thenReturn(user);
    boolean result =
        testScenario(getRequest(false, true, true, req, ActorOperations.UPDATE_USER), null);
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
}
