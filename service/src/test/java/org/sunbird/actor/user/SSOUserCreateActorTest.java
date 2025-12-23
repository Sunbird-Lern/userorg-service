package org.sunbird.actor.user;

import java.time.Duration;

import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.when;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.dispatch.Futures;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.apache.pekko.util.Timeout;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.organisation.Organisation;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import org.sunbird.util.DataCacheHandler;
import org.sunbird.util.Util;
import scala.concurrent.Future;

public class SSOUserCreateActorTest extends UserManagementActorTestBase {

  @Before
  public void before() {
    PowerMockito.mockStatic(DataCacheHandler.class);
    List<String> subTypeList = Arrays.asList("state,district,block,cluster,school;".split(";"));
    Map<String, Integer> orderMap = new HashMap<>();
    for (String str : subTypeList) {
      List<String> typeList =
          (((Arrays.asList(str.split(","))).stream().map(String::toLowerCase))
              .collect(Collectors.toList()));
      for (int i = 0; i < typeList.size(); i++) {
        orderMap.put(typeList.get(i), i);
      }
    }
    when(DataCacheHandler.getLocationOrderMap()).thenReturn(orderMap);
    Map<String, String> config = new HashMap<>();
    config.put(JsonKey.CUSTODIAN_ORG_ID, "custOrgId");
    when(DataCacheHandler.getConfigSettings()).thenReturn(config);
  }

  @Test
  public void testCreateUserSuccessWithUserCallerId() {
    reqMap.put(JsonKey.PROFILE_LOCATION, Arrays.asList("anyLocationCodes"));
    boolean result =
        testScenario(
            getRequest(true, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_USER),
            null);
    assertTrue(result);
  }

  @Test
  public void testCreateUserSuccessWithIsTenantAsFalse() {
    Organisation organisation = new Organisation();
    organisation.setId("rootOrgId");
    organisation.setChannel("anyChannel");
    organisation.setRootOrgId("rootOrgId");
    organisation.setTenant(false);
    when(orgService.getOrgObjById(Mockito.anyString(), Mockito.any()))
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
    when(orgService.getOrgObjById(Mockito.anyString(), Mockito.any()))
        .thenReturn(organisation);
    Request request =
        getRequest(true, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_USER);
    request.getRequest().remove(JsonKey.CHANNEL);
    boolean result = testScenario(request, null);
    assertTrue(result);
  }

  @Test
  public void testCreateUserSuccessWithoutUserCallerId() {
    Organisation organisation = new Organisation();
    organisation.setId("rootOrgId");
    organisation.setChannel("anyChannel");
    organisation.setRootOrgId("rootOrgId");
    organisation.setTenant(true);
    when(orgService.getOrgObjById(Mockito.anyString(), Mockito.any()))
        .thenReturn(organisation);
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
  public void testCreateUserSuccessWithOrgExternalIdNewVersion() {
    boolean result =
        testScenario(
            getRequest(
                false, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_SSO_USER),
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
  public void testCreateUserSuccessWithoutUserCallerIdChannelAndRootOrgIdNewVersion() {

    boolean result =
        testScenario(getRequest(false, false, true, reqMap, ActorOperations.CREATE_SSO_USER), null);
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
  public void testCreateUserFailureWithInvalidUserTypeAndSubtype() {

    reqMap.put(JsonKey.USER_TYPE, "anyUserType");
    reqMap.put(JsonKey.USER_SUB_TYPE, "anyUserSubType");
    reqMap.put(JsonKey.LOCATION_CODES, Arrays.asList("LocationCodes"));
    reqMap.put(JsonKey.PROFILE_USERTYPE, Arrays.asList("userType"));
    boolean result =
        testScenario(
            getRequest(false, false, false, reqMap, ActorOperations.CREATE_USER),
            ResponseCode.invalidParameterValue);
    assertTrue(result);
  }

  @Test
  public void testCreateUserFailureWithInvalidUserTypeAndSubtypeNewVersion() {

    reqMap.put(JsonKey.USER_TYPE, "anyUserType");
    reqMap.put(JsonKey.USER_SUB_TYPE, "anyUserSubType");
    reqMap.put(JsonKey.LOCATION_CODES, Arrays.asList("LocationCodes"));
    reqMap.put(JsonKey.PROFILE_USERTYPE, Arrays.asList("userType"));
    boolean result =
        testScenario(
            getRequest(false, false, false, reqMap, ActorOperations.CREATE_SSO_USER),
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

  @Test
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
  public void testCreateUserSuccessWithLocationCodesNewVersion() {
    Future<Object> future = Futures.future(() -> getEsResponse(), system.dispatcher());
    when(Patterns.ask(
            Mockito.any(ActorRef.class), Mockito.any(Request.class), Mockito.any(Timeout.class)))
        .thenReturn(future);
    reqMap.put(JsonKey.LOCATION_CODES, Arrays.asList("locationCode"));
    boolean result =
        testScenario(getRequest(true, true, true, reqMap, ActorOperations.CREATE_SSO_USER), null);
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
  public void testCreateUserFailureWithInvalidExternalIdsNewVersion() {

    reqMap.put(JsonKey.EXTERNAL_IDS, "anyExternalId");
    boolean result =
        testScenario(
            getRequest(false, false, false, reqMap, ActorOperations.CREATE_SSO_USER),
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
  public void testCreateUserFailureWithInvalidRolesNewVersion() {

    reqMap.put(JsonKey.ROLES, "anyRoles");
    boolean result =
        testScenario(
            getRequest(false, false, false, reqMap, ActorOperations.CREATE_SSO_USER),
            ResponseCode.dataTypeError);
    assertTrue(result);
  }

  @Test
  public void testCreateUserFailureWithInvalidCountryCode() {

    reqMap.put(JsonKey.COUNTRY_CODE, "anyCode");
    boolean result =
        testScenario(
            getRequest(false, false, false, reqMap, ActorOperations.CREATE_USER),
            ResponseCode.invalidParameter);
    assertTrue(result);
  }

  @Test
  public void testCreateUserFailureWithInvalidCountryCodeNewVersion() {

    reqMap.put(JsonKey.COUNTRY_CODE, "anyCode");
    boolean result =
        testScenario(
            getRequest(false, false, false, reqMap, ActorOperations.CREATE_SSO_USER),
            ResponseCode.invalidParameter);
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
  public void testCreateUserSuccessWithUserTypeAsTeacher() {
    reqMap.put(JsonKey.USER_TYPE, "teacher");

    boolean result =
        testScenario(
            getRequest(true, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_USER),
            null);
    assertTrue(result);
  }

  @Test
  public void testCreateUserSuccessWithUserTypeAsTeacherNewVersion() {
    reqMap.put(JsonKey.USER_TYPE, "teacher");

    boolean result =
        testScenario(
            getRequest(
                true, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_SSO_USER),
            null);
    assertTrue(result);
  }

  @Test
  public void testCreateUserSuccessWithUserSync() {
    reqMap.put("sync", true);
    PowerMockito.mockStatic(Util.class);
    Map<String, Object> user = getEsResponseMap();
    user.put(JsonKey.USER_ID, "123456789");
    when(userService.getUserDetailsForES(Mockito.anyString(), Mockito.any())).thenReturn(user);
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    subject.tell(
        getRequest(true, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_USER),
        probe.getRef());
    probe.expectMsgClass(Duration.ofSeconds(10), Response.class);
  }
}
