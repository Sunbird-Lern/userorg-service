package org.sunbird.user.util;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.dispatch.Futures;
import java.util.*;
import org.apache.commons.lang3.math.NumberUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.datasecurity.EncryptionService;
import org.sunbird.common.models.util.datasecurity.impl.DefaultEncryptionServivceImpl;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.Util;
import org.sunbird.models.user.User;
import org.sunbird.models.user.UserDeclareEntity;
import scala.concurrent.Future;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  CassandraOperationImpl.class,
  DataCacheHandler.class,
  EsClientFactory.class,
  ElasticSearchRestHighImpl.class,
  DefaultEncryptionServivceImpl.class,
  Util.class,
  EncryptionService.class,
  org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class
})
@PowerMockIgnore({"javax.management.*"})
public class UserUtilTest {
  private static Response response;
  public static CassandraOperationImpl cassandraOperationImpl;
  private static ElasticSearchService esService;

  public void beforeEachTest() {
    PowerMockito.mockStatic(DataCacheHandler.class);
    response = new Response();
    List<Map<String, Object>> userMapList = new ArrayList<Map<String, Object>>();
    Map<String, Object> userMap = new HashMap<String, Object>();
    userMapList.add(userMap);
    Response existResponse = new Response();
    existResponse.put(JsonKey.RESPONSE, userMapList);
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperationImpl = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    Map<String, String> settingMap = new HashMap<String, String>();
    settingMap.put(JsonKey.PHONE_UNIQUE, "True");
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    when(cassandraOperationImpl.getRecordsByIndexedProperty(
            JsonKey.SUNBIRD, "user", JsonKey.EMAIL, "test@test.com"))
        .thenReturn(response);
    when(cassandraOperationImpl.getRecordsByIndexedProperty(
            JsonKey.SUNBIRD, "user", JsonKey.PHONE, "9663890400"))
        .thenReturn(existResponse);
    when(DataCacheHandler.getConfigSettings()).thenReturn(settingMap);
    when(DataCacheHandler.getTenantConfigMap()).thenReturn(getTenantConfig());
    PowerMockito.mockStatic(EsClientFactory.class);
    esService = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);

    PowerMockito.mockStatic(Util.class);
  }

  @Test
  public void generateUniqueStringSuccess() {
    beforeEachTest();
    String val = UserUtil.generateUniqueString(4);
    assertTrue(val.length() == 4);
  }

  @Test
  public void generateUniqueStringSecondCharCheck() {
    beforeEachTest();
    String val = UserUtil.generateUniqueString(5);
    assertTrue(val.length() == 5);
    assertTrue(
        NumberUtils.isNumber(val.substring(1, 2)) || NumberUtils.isNumber(val.substring(2, 3)));
  }

  @Test
  public void checkPhoneUniquenessExist() throws Exception {
    beforeEachTest();
    PowerMockito.mockStatic(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class);
    EncryptionService encryptionService = PowerMockito.mock(EncryptionService.class);
    when(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory
            .getEncryptionServiceInstance(null))
        .thenReturn(encryptionService);
    Map<String, String> settingMap = new HashMap<>();
    settingMap.put(JsonKey.PHONE_UNIQUE, "True");
    when(DataCacheHandler.getConfigSettings()).thenReturn(settingMap);

    when(encryptionService.encryptData(Mockito.anyString())).thenReturn("9663890400");
    Response response1 = new Response();
    List<Map<String, Object>> responseList = new ArrayList<>();
    Map<String, Object> result = new HashMap<>();
    result.put(JsonKey.IS_DELETED, false);
    result.put(JsonKey.USER_ID, "123-456-789");
    responseList.add(result);
    response1.getResult().put(JsonKey.RESPONSE, responseList);
    when(cassandraOperationImpl.getRecordsByIndexedProperty(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(response1);
    User user = new User();
    user.setPhone("9663890400");
    boolean response = false;
    try {
      UserUtil.checkPhoneUniqueness(user, "create");
      response = true;
    } catch (ProjectCommonException e) {
      assertEquals(e.getResponseCode(), 400);
    }
    assertFalse(response);
  }

  @Test
  public void checkPhoneExist() {
    PowerMockito.mockStatic(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class);
    EncryptionService encryptionService = PowerMockito.mock(EncryptionService.class);
    when(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory
            .getEncryptionServiceInstance(null))
        .thenReturn(encryptionService);
    beforeEachTest();
    boolean response = false;
    try {
      UserUtil.checkPhoneUniqueness("9663890400");
      response = true;
    } catch (ProjectCommonException e) {
      assertEquals(e.getResponseCode(), 400);
    }
    assertFalse(response);
  }

  @Test
  public void checkEmailExist() {
    beforeEachTest();
    boolean response = false;
    try {
      UserUtil.checkEmailUniqueness("test@test.com");
      response = true;
    } catch (ProjectCommonException e) {

    }
    assertTrue(response);
  }

  @Test
  public void copyAndConvertExternalIdsToLower() {
    beforeEachTest();
    List<Map<String, String>> externalIds = new ArrayList<Map<String, String>>();
    Map<String, String> userExternalIdMap = new HashMap<String, String>();
    userExternalIdMap.put(JsonKey.ID, "test123");
    userExternalIdMap.put(JsonKey.PROVIDER, "State");
    userExternalIdMap.put(JsonKey.ID_TYPE, "UserExtId");
    externalIds.add(userExternalIdMap);
    externalIds = UserUtil.copyAndConvertExternalIdsToLower(externalIds);
    userExternalIdMap = externalIds.get(0);
    assertNotNull(userExternalIdMap.get(JsonKey.ORIGINAL_EXTERNAL_ID));
    assertEquals(userExternalIdMap.get(JsonKey.PROVIDER), "state");
  }

  @Test
  public void setUserDefaultValueForV3() {
    beforeEachTest();
    Map<String, Object> userMap = new HashMap<String, Object>();
    userMap.put(JsonKey.FIRST_NAME, "Test User");
    UserUtil.setUserDefaultValueForV3(userMap);
    assertNotNull(userMap.get(JsonKey.USERNAME));
    assertNotNull(userMap.get(JsonKey.STATUS));
    assertNotNull(userMap.get(JsonKey.ROLES));
  }

  @Test
  public void testValidateManagedUserLimit() {
    beforeEachTest();
    Map<String, Object> req = new HashMap<>();
    req.put(JsonKey.MANAGED_BY, "ManagedBy");
    List managedUserList = new ArrayList<User>();
    while (managedUserList.size() <= 31) {
      managedUserList.add(new User());
    }
    when(Util.searchUser(req)).thenReturn(managedUserList);
    try {
      UserUtil.validateManagedUserLimit("ManagedBy");
    } catch (ProjectCommonException e) {
      assertEquals(e.getResponseCode(), 400);
      assertEquals(e.getMessage(), ResponseCode.managedUserLimitExceeded.getErrorMessage());
    }
  }

  @Test
  public void testTransformExternalIdsToSelfDeclaredRequest() {
    beforeEachTest();
    List<Map<String, String>> externalIds = getExternalIds();
    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(JsonKey.USER_ID, "user1");
    requestMap.put(JsonKey.CREATED_BY, "user1");
    List<UserDeclareEntity> userDeclareEntityList =
        UserUtil.transformExternalIdsToSelfDeclaredRequest(externalIds, requestMap);
    Assert.assertEquals("add", userDeclareEntityList.get(0).getOperation());
  }

  @Test
  public void testfetchOrgIdByProvider() {
    beforeEachTest();
    List<String> providers = new ArrayList<>();
    providers.add("channel004");

    Map<String, Object> orgMap = new HashMap<>();
    List<Map<String, Object>> orgList = new ArrayList<>();

    orgMap.put("id", "1234");
    orgMap.put("channel", "channel004");
    orgList.add(orgMap);
    Map<String, Object> contentMap = new HashMap<>();
    contentMap.put(JsonKey.CONTENT, orgList);

    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(contentMap);
    Future<Map<String, Object>> test = promise.future();
    SearchDTO searchDTO = new SearchDTO();
    when(Util.createSearchDto(Mockito.anyMap())).thenReturn(searchDTO);
    when(esService.search(searchDTO, ProjectUtil.EsType.organisation.getTypeName()))
        .thenReturn(promise.future());
    Map<String, String> providerMap = UserUtil.fetchOrgIdByProvider(providers);
    Assert.assertTrue(true);
  }

  @Test
  public void testEncryptDeclareFields() throws Exception {
    beforeEachTest();
    List<Map<String, Object>> declarations = new ArrayList<>();
    Map<String, Object> declareFieldMap = new HashMap<>();
    Map<String, Object> userInfo = new HashMap<>();
    userInfo.put(JsonKey.DECLARED_EMAIL, "abc@gmail.com");
    userInfo.put(JsonKey.DECLARED_DISTRICT, "Karnataka");
    declareFieldMap.put(JsonKey.INFO, userInfo);
    declarations.add(declareFieldMap);
    try {
      UserUtil.encryptDeclarationFields(declarations);
    } catch (Exception ex) {

    }
    Assert.assertTrue(true);
  }

  private List<Map<String, String>> getExternalIds() {
    beforeEachTest();
    List<Map<String, String>> externalIds = new ArrayList<>();
    Map<String, String> extId1 = new HashMap<>();
    extId1.put(JsonKey.ORIGINAL_ID_TYPE, JsonKey.DECLARED_EMAIL);
    extId1.put(JsonKey.ORIGINAL_PROVIDER, "0123");
    extId1.put(JsonKey.ORIGINAL_EXTERNAL_ID, "abc@diksha.com");
    extId1.put(JsonKey.OPERATION, "add");
    Map<String, String> extId2 = new HashMap<>();
    extId2.put(JsonKey.ORIGINAL_ID_TYPE, JsonKey.DECLARED_EMAIL);
    extId2.put(JsonKey.ORIGINAL_PROVIDER, "123");
    extId2.put(JsonKey.ORIGINAL_EXTERNAL_ID, "abc@diksha.com");
    extId2.put(JsonKey.OPERATION, "remove");

    externalIds.add(extId1);
    externalIds.add(extId2);

    return externalIds;
  }

  @Test
  public void testgetUserOrgDetailsDeActive() {
    beforeEachTest();
    Response response1 = new Response();
    List<Map<String, Object>> responseList = new ArrayList<>();
    Map<String, Object> result = new HashMap<>();
    result.put(JsonKey.IS_DELETED, true);
    result.put(JsonKey.USER_ID, "123-456-789");
    responseList.add(result);
    response1.getResult().put(JsonKey.RESPONSE, responseList);
    List<String> ids = new ArrayList<>();
    ids.add("123-456-789");
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    when(cassandraOperationImpl.getRecordsByPrimaryKeys(
            JsonKey.SUNBIRD, "user_organisation", ids, JsonKey.USER_ID))
        .thenReturn(response1);
    List<Map<String, Object>> res = UserUtil.getActiveUserOrgDetails("123-456-789");
    Assert.assertNotNull(res);
  }

  @Test
  public void testupdateExternalIdsWithProvider() {
    beforeEachTest();
    List<String> providers = new ArrayList<>();
    providers.add("channel004");

    Map<String, Object> orgMap = new HashMap<>();
    List<Map<String, Object>> orgList = new ArrayList<>();

    orgMap.put("id", "1234");
    orgMap.put("channel", "channel004");
    orgList.add(orgMap);
    Map<String, Object> contentMap = new HashMap<>();
    contentMap.put(JsonKey.CONTENT, orgList);

    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(contentMap);
    Future<Map<String, Object>> test = promise.future();
    SearchDTO searchDTO = new SearchDTO();
    when(Util.createSearchDto(Mockito.anyMap())).thenReturn(searchDTO);
    when(esService.search(searchDTO, ProjectUtil.EsType.organisation.getTypeName()))
        .thenReturn(promise.future());
    Map<String, String> externalIds = new HashMap<>();
    externalIds.put(JsonKey.PROVIDER, "1234");
    externalIds.put(JsonKey.USER_ID, "w131-2323-323-232-3232");
    List<Map<String, String>> externalIdList = new ArrayList<>();
    externalIdList.add(externalIds);
    UserUtil.updateExternalIdsWithProvider(externalIdList);
    Assert.assertTrue(true);
  }

  @Test
  public void testupdateExternalIdsProviderWithOrgId() {
    beforeEachTest();
    List<String> providers = new ArrayList<>();
    providers.add("channel004");

    Map<String, Object> orgMap = new HashMap<>();
    List<Map<String, Object>> orgList = new ArrayList<>();

    orgMap.put("id", "1234");
    orgMap.put("channel", "channel004");
    orgList.add(orgMap);
    Map<String, Object> contentMap = new HashMap<>();
    contentMap.put(JsonKey.CONTENT, orgList);

    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(contentMap);
    Future<Map<String, Object>> test = promise.future();
    SearchDTO searchDTO = new SearchDTO();
    when(Util.createSearchDto(Mockito.anyMap())).thenReturn(searchDTO);
    when(esService.search(searchDTO, ProjectUtil.EsType.organisation.getTypeName()))
        .thenReturn(promise.future());
    Map<String, String> externalIds = new HashMap<>();
    externalIds.put(JsonKey.PROVIDER, "channel1004");
    externalIds.put(JsonKey.USER_ID, "w131-2323-323-232-3232");
    List<Map<String, String>> externalIdList = new ArrayList<>();
    externalIdList.add(externalIds);
    Map<String, Object> userMap = new HashMap<>();
    userMap.put(JsonKey.EXTERNAL_IDS, externalIdList);
    try {
      UserUtil.updateExternalIdsProviderWithOrgId(userMap);
    } catch (Exception ex) {
      Assert.assertTrue(true);
      Assert.assertEquals(
          "Invalid value provider for parameter channel1004. Please provide a valid value.",
          ex.getMessage());
    }
  }

  @Test
  public void testGetPrivateFieldsWithOrgId() {
    beforeEachTest();
    Set<String> privateFieldsSet = UserUtil.getTenantPrivateFields(JsonKey.ALL);
    Assert.assertTrue(!privateFieldsSet.isEmpty());
  }

  private Map<String, Map<String, Object>> getTenantConfig() {
    Map<String, Map<String, Object>> tenantConfig = new HashMap<>();
    Map<String, Object> keyConfig = new HashMap<>();
    String data =
        "{\"templateName\":\"defaultTemplate\",\"action\":\"update\",\"private\":[\"declared-phone\",\"declared-email\"],"
            + "\"aliases\":{\"Diksha UUID\":\"userId\",\"Status\":\"input status\",\"State provided ext. ID\":\"userExternalId\",\"Channel\":\"channel\","
            + "\"Org ID\":\"orgId\",\"Persona\":\"persona\",\"Phone number\":\"phone\",\"Email ID\":\"email\",\"School Name\":\"schoolName\","
            + "\"School UDISE ID\":\"schoolUdiseId\",\"Diksha Sub-Org ID\":\"subOrgId\",\"Error Type\":\"errorType\"},\"mandatoryFields\":[\"Diksha UUID\","
            + "\"Status\",\"State provided ext. ID\",\"Channel\",\"Org ID\",\"Persona\"],\"optionalFields\":[\"School Name\",\"School UDISE ID\",\"Email ID\","
            + "\"Phone number\",\"Diksha Sub-Org ID\",\"Error Type\"]}";
    keyConfig.put(JsonKey.SELF_DECLARATIONS, data);
    tenantConfig.put(JsonKey.ALL, keyConfig);
    return tenantConfig;
  }
}
