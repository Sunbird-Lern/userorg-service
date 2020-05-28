package org.sunbird.user;

import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.dispatch.Futures;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;
import org.sunbird.actorutil.InterServiceCommunicationFactory;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.learner.util.DataCacheHandler;
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
    when(InterServiceCommunicationFactory.getInstance())
        .thenReturn(interServiceCommunication)
        .thenReturn(interServiceCommunication);
    when(interServiceCommunication.getResponse(
            Mockito.any(ActorRef.class), Mockito.any(Request.class)))
        .thenReturn(null);
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
  public void testCreateUserSuccessWithLocationCodes() {
    when(InterServiceCommunicationFactory.getInstance())
        .thenReturn(interServiceCommunication)
        .thenReturn(interServiceCommunication);
    when(interServiceCommunication.getResponse(
            Mockito.any(ActorRef.class), Mockito.any(Request.class)))
        .thenReturn(getEsResponseForLocation())
        .thenReturn(getEsResponse());
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
    when(esService.getDataByIdentifier(Mockito.anyString(), Mockito.anyString()))
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
    when(interServiceCommunication.getResponse(
            Mockito.any(ActorRef.class), Mockito.any(Request.class)))
        .thenReturn(null);
    boolean result =
        testScenario(
            getRequest(
                true, true, true, getUpdateRequestWithLocationCodes(), ActorOperations.UPDATE_USER),
            ResponseCode.invalidParameterValue);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserSuccess() {
    Map<String, Object> req = getExternalIdMap();
    getUpdateRequestWithDefaultFlags(req);
    boolean result =
        testScenario(
            getRequest(true, true, true, req, ActorOperations.UPDATE_USER), null);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserSuccessWithLocationCodes() {
    when(InterServiceCommunicationFactory.getInstance())
        .thenReturn(interServiceCommunication)
        .thenReturn(interServiceCommunication);
    when(interServiceCommunication.getResponse(
            Mockito.any(ActorRef.class), Mockito.any(Request.class)))
        .thenReturn(getEsResponseForLocation())
        .thenReturn(getEsResponse());
    boolean result =
        testScenario(
            getRequest(
                true, true, true, getUpdateRequestWithLocationCodes(), ActorOperations.UPDATE_USER),
            null);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserSuccessWithoutUserCallerId() {
    Map<String, Object> req = getExternalIdMap();
    getUpdateRequestWithDefaultFlags(req);
    boolean result =
        testScenario(
            getRequest(false, true, true, req, ActorOperations.UPDATE_USER), null);
    assertTrue(result);
  }

  @Test
  public void testCreateUserSuccessWithUserTypeAsTeacher() {
    reqMap.put(JsonKey.USER_TYPE, JsonKey.TEACHER);

    when(userService.getRootOrgIdFromChannel(Mockito.anyString()))
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
    reqMap.put(JsonKey.USER_TYPE, JsonKey.OTHER);

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
    when(userService.getUserById(Mockito.anyString())).thenReturn(getUser(false));
    when(userService.getRootOrgIdFromChannel(Mockito.anyString())).thenReturn("rootOrgId1");
    boolean result =
        testScenario(getRequest(false, true, true, req, ActorOperations.UPDATE_USER), null);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserFailureWithUserTypeTeacher() {
    Map<String, Object> req = getExternalIdMap();
    req.put(JsonKey.USER_TYPE, JsonKey.TEACHER);
    when(userService.getUserById(Mockito.anyString())).thenReturn(getUser(false));
    when(userService.getRootOrgIdFromChannel(Mockito.anyString())).thenReturn("rootOrgId");
    boolean result =
        testScenario(
            getRequest(false, true, true, req, ActorOperations.UPDATE_USER),
            ResponseCode.errorTeacherCannotBelongToCustodianOrg);
    assertTrue(result);
  }

  //@Test
  public void testUpdateUserOrgFailureWithoutUserIdPrivateApi() {
    Map<String, Object> req = getUserOrgUpdateRequest(false);
    Request request = getRequest(false, false, true, req, ActorOperations.UPDATE_USER);
    boolean result = testScenario(request, ResponseCode.errorUnsupportedField);
    assertTrue(result);
  }

  //@Test
  public void testUpdateUserOrgFailureWithPublicApi() {
    Map<String, Object> req = getUserOrgUpdateRequest(false);
    req.remove(JsonKey.USER_ID);
    Request request = getRequest(false, false, true, req, ActorOperations.UPDATE_USER);
    boolean result = testScenario(request, ResponseCode.mandatoryParamsMissing);
    assertTrue(result);
  }

 // @Test
  public void testUpdateUserOrgFailureWithOrganisationsPrivateApi() {
    Map<String, Object> req = getUserOrgUpdateRequest(false);
    req.put(JsonKey.ORGANISATIONS, new HashMap<>());
    Request request = getRequest(false, false, true, req, ActorOperations.UPDATE_USER);
    request.getContext().put(JsonKey.PRIVATE, true);
    boolean result = testScenario(request, ResponseCode.dataTypeError);
    assertTrue(result);
  }

  //@Test
  public void testUpdateUserOrgFailureWithInvalidOrganisationsPrivateApi() {
    Map<String, Object> req = getUserOrgUpdateRequest(true);
    req.put(JsonKey.ORGANISATIONS, Arrays.asList("a", "b"));
    Request request = getRequest(false, false, true, req, ActorOperations.UPDATE_USER);
    request.getContext().put(JsonKey.PRIVATE, true);
    boolean result = testScenario(request, ResponseCode.dataTypeError);
    assertTrue(result);
  }

  //@Test
  public void testUpdateUserOrgFailureWithoutOrganisationsPrivateApi() {
    Map<String, Object> req = getUserOrgUpdateRequest(true);
    ((Map) ((List) req.get(JsonKey.ORGANISATIONS)).get(0)).put(JsonKey.ORGANISATION_ID, "");
    Request request = getRequest(false, false, true, req, ActorOperations.UPDATE_USER);
    request.getContext().put(JsonKey.PRIVATE, true);
    boolean result = testScenario(request, ResponseCode.mandatoryParamsMissing);
    assertTrue(result);
  }

 // @Test
  public void testUpdateUserOrgFailureWithInvalidRolesDataTypePrivateApi() {
    Map<String, Object> req = getUserOrgUpdateRequest(true);
    ((Map) ((List) req.get(JsonKey.ORGANISATIONS)).get(0)).put(JsonKey.ROLES, "String");
    Request request = getRequest(false, false, true, req, ActorOperations.UPDATE_USER);
    request.getContext().put(JsonKey.PRIVATE, true);
    boolean result = testScenario(request, ResponseCode.dataTypeError);
    assertTrue(result);
  }

  //@Test
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
  @Test
  public void testCreateUserSuccessWithUserSync() {
    reqMap.put("sync",true);
    boolean result =
      testScenario(
        getRequest(true, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_USER),
        null);
    assertTrue(result);
  }
}