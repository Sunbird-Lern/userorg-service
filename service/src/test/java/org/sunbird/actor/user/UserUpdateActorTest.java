package org.sunbird.actor.user;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Props;
import org.apache.pekko.dispatch.Futures;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.apache.pekko.util.Timeout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.location.Location;
import org.sunbird.model.organisation.Organisation;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.service.user.ExtendedUserProfileService;
import org.sunbird.service.user.impl.ExtendedUserProfileServiceImpl;
import org.sunbird.util.DataCacheHandler;
import org.sunbird.util.user.UserUtil;
import scala.concurrent.Future;

@PrepareForTest({ExtendedUserProfileServiceImpl.class, ExtendedUserProfileService.class})
public class UserUpdateActorTest extends UserManagementActorTestBase {

  public final Props props = Props.create(UserUpdateActor.class);
  private String extendedProfileConfig =
      "{\"profileDetails.json\":{\"$schema\":\"http://json-schema.org/draft-07/schema\",\"type\":\"object\",\"definitions\":{\"@type\":{\"type\":\"string\"},\"MaritalStatus\":{\"type\":\"string\",\"enum\":[\"Married\",\"Single\"]},\"NonEmptyStr\":{\"type\":\"string\",\"pattern\":\"\\\\A(?!\\\\s*\\\\Z).+\"}},\"properties\":{\"personalDetails\":{\"type\":\"object\",\"title\":\"The Personal Details Schema\",\"required\":[\"firstname\",\"surname\",\"maritalStatus\"],\"properties\":{\"firstname\":{\"$id\":\"#/properties/firstname\",\"$ref\":\"#/definitions/NonEmptyStr\",\"$comment\":\"User first name\"},\"surname\":{\"$id\":\"#/properties/surname\",\"$ref\":\"#/definitions/NonEmptyStr\",\"$comment\":\"User surname\"},\"maritalStatus\":{\"$id\":\"#/properties/maritalStatus\",\"$ref\":\"#/definitions/MaritalStatus\",\"$comment\":\"Marital Status\"}}}},\"title\":\"profileDetails\",\"required\":[\"personalDetails\"]}}";

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
    Map<String, String> configMap = new HashMap<>();
    configMap.put(JsonKey.CUSTODIAN_ORG_CHANNEL, "channel");
    configMap.put(JsonKey.CUSTODIAN_ORG_ID, "custodianRootOrgId");
    configMap.put("extendedProfileSchemaConfig", extendedProfileConfig);
    when(DataCacheHandler.getConfigSettings()).thenReturn(configMap);

    Map<String, Map<String, List<String>>> userTypeConfigMap = new HashMap<>();
    when(DataCacheHandler.getUserTypesConfig()).thenReturn(userTypeConfigMap);
  }

  @Test
  public void testUpdateUserFailureWithInvalidLocationCodes() {

    reqMap.put(JsonKey.USER_TYPE, "userType");
    reqMap.put(JsonKey.USER_SUB_TYPE, "userSubType");
    reqMap.put(JsonKey.LOCATION_CODES, Arrays.asList("anyLocationCodes"));
    reqMap.put(JsonKey.ASSOCIATION_TYPE, "1");
    boolean result =
        testScenario(
            getRequest(false, false, false, reqMap, ActorOperations.UPDATE_USER),
            ResponseCode.invalidRequestParameter,
            props);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserFailureWithInvalidOrganisationId() {
    reqMap.put(JsonKey.ORGANISATION_ID, "invalidOrgId");
    reqMap.remove(JsonKey.USERNAME);
    reqMap.put(JsonKey.USER_ID, "1235-45659751-21566");
    boolean result =
        testScenario(
            getRequest(false, false, false, reqMap, ActorOperations.UPDATE_USER),
            ResponseCode.invalidParameterValue,
            props);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserSuccess() {
    Map<String, Object> req = getExternalIdMap();
    getUpdateRequestWithDefaultFlags(req);
    Map<String, Object> user = new HashMap<>();
    user.put(JsonKey.IS_DELETED, false);
    user.put(JsonKey.ROOT_ORG_ID, "custodianRootOrgId");
    reqMap.put(JsonKey.ASSOCIATION_TYPE, "1");
    user.putAll(getMapObject());
    when(UserUtil.validateExternalIdsAndReturnActiveUser(Mockito.anyMap(), Mockito.any()))
        .thenReturn(user);
    boolean result =
        testScenario(getRequest(true, true, true, req, ActorOperations.UPDATE_USER), null, props);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserSuccessv2() {
    Map<String, Object> req = getExternalIdMap();
    getUpdateRequestWithDefaultFlags(req);
    req.put(JsonKey.ORG_EXTERNAL_ID, "orgExtId");
    req.put(JsonKey.STATE_ID, "statelocid");
    Map<String, Object> user = new HashMap<>();
    user.put(JsonKey.IS_DELETED, false);
    user.put(JsonKey.ROOT_ORG_ID, "custodianRootOrgId");
    reqMap.put(JsonKey.ASSOCIATION_TYPE, "1");

    user.putAll(getMapObject());
    when(UserUtil.validateExternalIdsAndReturnActiveUser(Mockito.anyMap(), Mockito.any()))
        .thenReturn(user);
    boolean result =
        testScenario(getRequest(true, true, true, req, ActorOperations.UPDATE_USER), null, props);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserSuccessv3() {
    Map<String, Object> req = getExternalIdMap();
    getUpdateRequestWithDefaultFlags(req);
    req.put(JsonKey.ORG_EXTERNAL_ID, "orgExtId");
    req.put(JsonKey.USER_ID, "userId");
    req.put(JsonKey.STATE_ID, "statelocid");
    Map<String, Object> user = new HashMap<>();
    user.put(JsonKey.IS_DELETED, false);
    user.put(JsonKey.ROOT_ORG_ID, "custodianRootOrgId");
    reqMap.put(JsonKey.ASSOCIATION_TYPE, "1");
    Organisation org = new Organisation();
    org.setId("id");
    org.setRootOrgId("rootOrgId");
    org.setChannel("channel");
    org.setOrgName("orgName");
    List<Organisation> orgList = new ArrayList<>();
    orgList.add(org);
    user.putAll(getMapObject());
    when(UserUtil.validateExternalIdsAndReturnActiveUser(Mockito.anyMap(), Mockito.any()))
        .thenReturn(user);

    Map<String, Object> userOrg = new HashMap<>();
    userOrg.put(JsonKey.USER_ID, "userId");
    userOrg.put(JsonKey.USER_ID, "id");
    List<Map<String, Object>> userOrgListDb = new ArrayList<>();
    userOrgListDb.add(userOrg);
    when(UserUtil.getUserOrgDetails(Mockito.anyBoolean(), Mockito.anyString(), Mockito.any()))
        .thenReturn(userOrgListDb);

    boolean result =
        testScenario(getRequest(true, true, true, req, ActorOperations.UPDATE_USER), null, props);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserSuccessNewVersion() {
    Map<String, Object> req = getExternalIdMap();
    getUpdateRequestWithDefaultFlags(req);
    Map<String, Object> user = new HashMap<>();
    user.put(JsonKey.IS_DELETED, false);
    user.put(JsonKey.ROOT_ORG_ID, "custodianRootOrgId");
    user.putAll(getMapObject());
    when(UserUtil.validateExternalIdsAndReturnActiveUser(Mockito.anyMap(), Mockito.any()))
        .thenReturn(user);
    boolean result =
        testScenario(
            getRequest(true, true, true, req, ActorOperations.UPDATE_USER_V2), null, props);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserFailure() {
    Map<String, Object> req = new HashMap<>();
    req.put(JsonKey.LOCATION_CODES, Arrays.asList("locationCodes"));
    boolean result =
        testScenario(
            getRequest(true, false, true, req, ActorOperations.UPDATE_USER),
            ResponseCode.mandatoryParamsMissing,
            props);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserFailureNewVersion() {
    Map<String, Object> req = new HashMap<>();
    req.put(JsonKey.LOCATION_CODES, Arrays.asList("locationCodes"));
    boolean result =
        testScenario(
            getRequest(true, false, true, req, ActorOperations.UPDATE_USER_V2),
            ResponseCode.mandatoryParamsMissing,
            props);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserFailureWithInvalidLocationCodesNewVersion() {

    reqMap.put(JsonKey.USER_TYPE, "userType");
    reqMap.put(JsonKey.USER_SUB_TYPE, "userSubType");
    reqMap.put(JsonKey.LOCATION_CODES, Arrays.asList("anyLocationCodes"));
    boolean result =
        testScenario(
            getRequest(false, false, false, reqMap, ActorOperations.UPDATE_USER_V2),
            ResponseCode.invalidRequestParameter,
            props);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserUpdateEmailSuccess() {
    Map<String, Object> user = new HashMap<>();
    user.put(JsonKey.PHONE, "4346345377");
    user.put(JsonKey.EMAIL, "username@gmail.com");
    user.put(JsonKey.USERNAME, "username");
    user.put(JsonKey.ROOT_ORG_ID, "rootOrgId");
    user.put(JsonKey.USER_TYPE, "rootOrgId");
    when(UserUtil.isEmailOrPhoneDiff(Mockito.anyMap(), Mockito.anyMap(), Mockito.anyString()))
        .thenReturn(true);
    when(UserUtil.validateExternalIdsAndReturnActiveUser(Mockito.anyMap(), Mockito.any()))
        .thenReturn(user);
    Map<String, Object> req = getExternalIdMap();
    getUpdateRequestWithDefaultFlags(req);
    boolean result =
        testScenario(getRequest(true, true, true, req, ActorOperations.UPDATE_USER), null, props);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserUpdateEmailSuccessNewVersion() {
    Map<String, Object> user = new HashMap<>();
    user.put(JsonKey.PHONE, "4346345377");
    user.put(JsonKey.EMAIL, "username@gmail.com");
    user.put(JsonKey.USERNAME, "username");
    user.put(JsonKey.ROOT_ORG_ID, "rootOrgId");
    user.put(JsonKey.USER_TYPE, "teacher");
    user.put(JsonKey.USER_SUB_TYPE, "crc");
    user.put(JsonKey.PROFILE_LOCATION, Arrays.asList("anyLocationCodes"));
    when(UserUtil.isEmailOrPhoneDiff(Mockito.anyMap(), Mockito.anyMap(), Mockito.anyString()))
        .thenReturn(true);
    when(UserUtil.validateExternalIdsAndReturnActiveUser(Mockito.anyMap(), Mockito.any()))
        .thenReturn(user);
    Map<String, Object> req = getExternalIdMap();
    getUpdateRequestWithDefaultFlags(req);
    boolean result =
        testScenario(
            getRequest(true, true, true, req, ActorOperations.UPDATE_USER_V2), null, props);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserUpdateV3() {
    Map<String, Object> user = new HashMap<>();
    user.put(JsonKey.PHONE, "4346345377");
    user.put(JsonKey.EMAIL, "username@gmail.com");
    user.put(JsonKey.USERNAME, "username");
    user.put(JsonKey.ROOT_ORG_ID, "rootOrgId");
    Map<String, Object> usertypes = new HashMap<>();
    usertypes.put("type", "teacher");
    usertypes.put("subType", null);
    List<Map<String, Object>> userTypeList = new ArrayList<>();
    userTypeList.add(usertypes);
    user.put(JsonKey.PROFILE_USERTYPES, userTypeList);
    when(UserUtil.isEmailOrPhoneDiff(Mockito.anyMap(), Mockito.anyMap(), Mockito.anyString()))
        .thenReturn(true);
    when(UserUtil.validateExternalIdsAndReturnActiveUser(Mockito.anyMap(), Mockito.any()))
        .thenReturn(user);
    Map<String, Object> req = getExternalIdMap();
    getUpdateRequestWithDefaultFlags(req);
    Request request = getRequest(true, true, true, req, ActorOperations.UPDATE_USER_V3);
    request.getRequest().put(JsonKey.PROFILE_USERTYPES, userTypeList);
    boolean result = testScenario(request, null, props);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserUpdateEmailSuccessNewVersion2() {
    Map<String, Object> user = new HashMap<>();
    user.put(JsonKey.PHONE, "4346345377");
    user.put(JsonKey.EMAIL, "username@gmail.com");
    user.put(JsonKey.USERNAME, "username");
    user.put(JsonKey.ROOT_ORG_ID, "rootOrgId");
    user.put(JsonKey.USER_TYPE, "teacher");
    user.put(JsonKey.USER_SUB_TYPE, "crc");
    user.put(JsonKey.PROFILE_LOCATION, Arrays.asList("anyLocationCodes"));
    when(UserUtil.isEmailOrPhoneDiff(Mockito.anyMap(), Mockito.anyMap(), Mockito.anyString()))
        .thenReturn(true);
    when(UserUtil.validateExternalIdsAndReturnActiveUser(Mockito.anyMap(), Mockito.any()))
        .thenReturn(user);
    Map<String, Object> req = getExternalIdMap();
    getUpdateRequestWithDefaultFlags(req);
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    subject.tell(getRequest(true, true, true, req, ActorOperations.UPDATE_USER_V2), probe.getRef());
    assertTrue(true);
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
            null,
            props);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserWithLocationCodesFailure() {
    Future<Object> future = Futures.future(() -> getEsResponse(), system.dispatcher());
    when(Patterns.ask(
            Mockito.any(ActorRef.class), Mockito.any(Request.class), Mockito.any(Timeout.class)))
        .thenReturn(future);
    Request req =
        getRequest(
            true, true, true, getUpdateRequestWithLocationCodes(), ActorOperations.UPDATE_USER);
    req.getRequest().put(JsonKey.LOCATION_CODES, "locationCode");
    boolean result = testScenario(req, ResponseCode.dataTypeError, props);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserSuccessWithLocationCodesNewVersion2() {
    Future<Object> future = Futures.future(() -> getEsResponse(), system.dispatcher());
    when(Patterns.ask(
            Mockito.any(ActorRef.class), Mockito.any(Request.class), Mockito.any(Timeout.class)))
        .thenReturn(future);
    boolean result =
        testScenario(
            getRequest(
                true, true, true, getUpdateRequestWithLocationCodes(), ActorOperations.UPDATE_USER),
            null,
            props);
    assertTrue(result);
  }

  public List<Location> getLocationLists2() {
    List<Location> locations = new ArrayList<>();
    Location location = new Location();
    location.setType("cluster");
    location.setCode("locationCode");
    locations.add(location);
    return locations;
  }

  @Test
  public void testUpdateUserFailureWithLocationSchool() {
    Map<String, Object> user = new HashMap<>();
    user.putAll(getMapObject());
    List<Map<String, String>> profileLoc = new ArrayList<>();
    Map<String, String> profileLoc1 = new HashMap<>();
    profileLoc1.put(JsonKey.TYPE, "state");
    profileLoc1.put(JsonKey.ID, "1231231-2312-12312");
    profileLoc.add(profileLoc1);
    user.put(JsonKey.PROFILE_LOCATION, profileLoc);
    when(UserUtil.validateExternalIdsAndReturnActiveUser(Mockito.anyMap(), Mockito.any()))
        .thenReturn(user);
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
            null,
            props);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserFailureWithLocationSchoolNewVersion() {
    Map<String, Object> user = new HashMap<>();
    user.putAll(getMapObject());
    List<Map<String, String>> profileLoc = new ArrayList<>();
    Map<String, String> profileLoc1 = new HashMap<>();
    profileLoc1.put(JsonKey.TYPE, "state");
    profileLoc1.put(JsonKey.ID, "1231231-2312-12312");
    profileLoc.add(profileLoc1);
    user.put(JsonKey.PROFILE_LOCATION, profileLoc);
    when(UserUtil.validateExternalIdsAndReturnActiveUser(Mockito.anyMap(), Mockito.any()))
        .thenReturn(user);
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
                ActorOperations.UPDATE_USER_V2),
            null,
            props);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserSuccessWithoutUserCallerId() {
    Map<String, Object> req = getExternalIdMap();
    getUpdateRequestWithDefaultFlags(req);
    Request reqObj = getRequest(false, true, true, req, ActorOperations.UPDATE_USER);
    reqObj.getRequest().put(JsonKey.PROFILE_LOCATION, null);
    reqObj.getRequest().put(JsonKey.PROFILE_USERTYPE, null);
    reqObj.getRequest().put(JsonKey.TNC_ACCEPTED_ON, null);
    reqObj.getRequest().put(JsonKey.USER_TYPE, null);
    boolean result =
        testScenario(getRequest(false, true, true, req, ActorOperations.UPDATE_USER), null, props);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserSuccessWithoutUserCallerIdNewVersion() {
    Map<String, Object> req = getExternalIdMap();
    getUpdateRequestWithDefaultFlags(req);
    boolean result =
        testScenario(
            getRequest(false, true, true, req, ActorOperations.UPDATE_USER_V2), null, props);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserSuccessWithUserTypeTeacher() {
    Map<String, Object> req = getExternalIdMap();
    getUpdateRequestWithDefaultFlags(req);
    req.put(JsonKey.USER_TYPE, "teacher");
    req.put(JsonKey.USER_SUB_TYPE, null);
    List<String> locCodes = new ArrayList<>();
    locCodes.add("locationCode");
    req.put(JsonKey.LOCATION_CODES, locCodes);
    List<Map<String, String>> externalIds = new ArrayList<>();
    Map<String, String> externalId = new HashMap<>();
    externalId.put(JsonKey.DECLARED_STATE, "state");
    externalId.put(JsonKey.ID, "locationCode");
    externalId.put(JsonKey.PROVIDER, "provider");
    externalId.put(JsonKey.ID_TYPE, "provider");
    externalIds.add(externalId);
    req.put(JsonKey.EXTERNAL_IDS, externalIds);
    Map<String, Object> user = new HashMap<>();
    user.put(JsonKey.PHONE, "4346345377");
    user.put(JsonKey.EMAIL, "username@gmail.com");
    user.put(JsonKey.USERNAME, "username");
    user.put(JsonKey.ROOT_ORG_ID, "rootOrgId");
    user.put(JsonKey.LOCATION_IDS, Arrays.asList("id"));
    when(UserUtil.validateExternalIdsAndReturnActiveUser(Mockito.anyMap(), Mockito.any()))
        .thenReturn(user);
    boolean result =
        testScenario(getRequest(false, true, true, req, ActorOperations.UPDATE_USER), null, props);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserSuccessWithUserTypeTeacherNewVersion() {
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
        testScenario(
            getRequest(false, true, true, req, ActorOperations.UPDATE_USER_V2), null, props);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserOrgFailureWithPublicApi() {
    Map<String, Object> req = getUserOrgUpdateRequest(false);
    req.remove(JsonKey.USER_ID);
    Request request = getRequest(false, false, true, req, ActorOperations.UPDATE_USER);
    boolean result = testScenario(request, ResponseCode.mandatoryParamsMissing, props);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserOrgFailureWithPublicApiNewVersion() {
    Map<String, Object> req = getUserOrgUpdateRequest(false);
    req.remove(JsonKey.USER_ID);
    Request request = getRequest(false, false, true, req, ActorOperations.UPDATE_USER_V2);
    boolean result = testScenario(request, ResponseCode.mandatoryParamsMissing, props);
    assertTrue(result);
  }

  @Test
  public void testUpdateExtendedUserProfileV3() {
    PowerMockito.mockStatic(ExtendedUserProfileServiceImpl.class);
    ExtendedUserProfileService extendedUserProfileService = mock(ExtendedUserProfileService.class);
    when(ExtendedUserProfileServiceImpl.getInstance()).thenReturn(extendedUserProfileService);

    Map<String, Object> req = getExternalIdMap();
    getUpdateRequestWithDefaultFlags(req);
    req.put(JsonKey.USER_ID, "userId");
    Map<String, Object> profileDetails = new HashMap<>();
    Map<String, Object> personalDetails = new HashMap<>();
    personalDetails.put("firstname", "Demo Name");
    personalDetails.put("surname", "Surname");
    personalDetails.put("maritalStatus", "Married");
    profileDetails.put("personalDetails", personalDetails);
    req.put(JsonKey.PROFILE_DETAILS, profileDetails);
    Map<String, Object> user = new HashMap<>();
    user.put(JsonKey.PHONE, "4346345377");
    user.put(JsonKey.EMAIL, "username@gmail.com");
    user.put(JsonKey.USERNAME, "username");
    user.put(JsonKey.ROOT_ORG_ID, "rootOrgId");
    when(UserUtil.isEmailOrPhoneDiff(Mockito.anyMap(), Mockito.anyMap(), Mockito.anyString()))
        .thenReturn(true);
    when(UserUtil.validateExternalIdsAndReturnActiveUser(Mockito.anyMap(), Mockito.any()))
        .thenReturn(user);
    Request request = getRequest(true, true, true, req, ActorOperations.UPDATE_USER_V3);
    boolean result = testScenario(request, null, props);
    assertTrue(result);
  }

  @Test
  public void testUpdateExtendedUserProfileFailureV3() {
    // Remove schemaConfig from DataCacheHandler
    Map<String, String> configMap = new HashMap<>();
    configMap.put(JsonKey.CUSTODIAN_ORG_CHANNEL, "channel");
    configMap.put(JsonKey.CUSTODIAN_ORG_ID, "custodianRootOrgId");
    when(DataCacheHandler.getConfigSettings()).thenReturn(configMap);

    Map<String, Object> req = getExternalIdMap();
    getUpdateRequestWithDefaultFlags(req);
    req.put(JsonKey.USER_ID, "userId");
    Map<String, Object> profileDetails = new HashMap<>();
    Map<String, Object> personalDetails = new HashMap<>();
    personalDetails.put("martialStatus", "Married");
    profileDetails.put("unknownObject", personalDetails);
    req.put(JsonKey.PROFILE_DETAILS, profileDetails);
    Map<String, Object> user = new HashMap<>();
    user.put(JsonKey.PHONE, "4346345377");
    user.put(JsonKey.EMAIL, "username@gmail.com");
    user.put(JsonKey.USERNAME, "username");
    user.put(JsonKey.ROOT_ORG_ID, "rootOrgId");
    when(UserUtil.isEmailOrPhoneDiff(Mockito.anyMap(), Mockito.anyMap(), Mockito.anyString()))
        .thenReturn(true);
    when(UserUtil.validateExternalIdsAndReturnActiveUser(Mockito.anyMap(), Mockito.any()))
        .thenReturn(user);
    Request request = getRequest(true, true, true, req, ActorOperations.UPDATE_USER_V3);
    boolean result = testScenario(request, ResponseCode.extendUserProfileNotLoaded, props);
    assertTrue(result);
  }

  @Test
  public void testUpdateInvalidExtendedUserProfileFailureV3() {
    PowerMockito.mockStatic(ExtendedUserProfileServiceImpl.class);
    ExtendedUserProfileService extendedUserProfileService = mock(ExtendedUserProfileService.class);
    when(ExtendedUserProfileServiceImpl.getInstance()).thenReturn(extendedUserProfileService);

    Map<String, Object> req = getExternalIdMap();
    getUpdateRequestWithDefaultFlags(req);
    req.put(JsonKey.USER_ID, "userId");
    req.put(JsonKey.PROFILE_DETAILS, "this is my profile");
    Map<String, Object> user = new HashMap<>();
    user.put(JsonKey.PHONE, "4346345377");
    user.put(JsonKey.EMAIL, "username@gmail.com");
    user.put(JsonKey.USERNAME, "username");
    user.put(JsonKey.ROOT_ORG_ID, "rootOrgId");
    when(UserUtil.isEmailOrPhoneDiff(Mockito.anyMap(), Mockito.anyMap(), Mockito.anyString()))
        .thenReturn(true);
    when(UserUtil.validateExternalIdsAndReturnActiveUser(Mockito.anyMap(), Mockito.any()))
        .thenReturn(user);
    Request request = getRequest(true, true, true, req, ActorOperations.UPDATE_USER_V3);
    boolean result = testScenario(request, ResponseCode.invalidValue, props);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserProfileWithInvalidOperation() {
    Map<String, Object> req = getExternalIdMap();
    getUpdateRequestWithDefaultFlags(req);
    req.put(JsonKey.USER_ID, "userId");
    Map<String, Object> user = new HashMap<>();
    user.put(JsonKey.PHONE, "4346345377");
    Request request = getRequest(true, true, true, req, ActorOperations.CREATE_USER);
    boolean result = testScenario(request, ResponseCode.extendUserProfileNotLoaded, props);
    assertFalse(result);
  }
}
