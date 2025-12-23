package org.sunbird.util.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.powermock.api.mockito.PowerMockito.*;

import org.apache.pekko.dispatch.Futures;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.datasecurity.EncryptionService;
import org.sunbird.datasecurity.impl.DefaultEncryptionServiceImpl;
import org.sunbird.dto.SearchDTO;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.User;
import org.sunbird.model.user.UserDeclareEntity;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.util.DataCacheHandler;
import org.sunbird.util.ProjectUtil;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  CassandraOperationImpl.class,
  DataCacheHandler.class,
  EsClientFactory.class,
  ElasticSearchRestHighImpl.class,
  DefaultEncryptionServiceImpl.class,
  EncryptionService.class,
  org.sunbird.datasecurity.impl.ServiceFactory.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UserUtilTest {
  private static Response response;
  public static CassandraOperationImpl cassandraOperationImpl;
  private static ElasticSearchService esService;
  private static final String KEYSPACE_NAME = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE);

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
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.TYPE, JsonKey.EMAIL);
    reqMap.put(JsonKey.VALUE, "test@test.com");
    when(cassandraOperationImpl.getRecordsByCompositeKey(
            KEYSPACE_NAME, JsonKey.USER_LOOKUP, reqMap, null))
        .thenReturn(response);
    Map<String, Object> reqMapPhone = new HashMap<>();
    reqMap.put(JsonKey.TYPE, JsonKey.PHONE);
    reqMap.put(JsonKey.VALUE, "9663890400");
    when(cassandraOperationImpl.getRecordsByCompositeKey(
            KEYSPACE_NAME, JsonKey.USER_LOOKUP, reqMapPhone, null))
        .thenReturn(existResponse);
    when(DataCacheHandler.getConfigSettings()).thenReturn(settingMap);

    PowerMockito.mockStatic(EsClientFactory.class);
    esService = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
    PowerMockito.when(
            cassandraOperationImpl.deleteRecord(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(response);
    doNothing()
        .when(cassandraOperationImpl)
        .deleteRecord(Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any());
    Map<String, Object> req = new HashMap<>();
    req.put(JsonKey.MANAGED_BY, "ManagedBy");
    List managedUserList = new ArrayList<Map<String, Object>>();
    while (managedUserList.size() <= 31) {
      managedUserList.add(new User());
    }
    Map<String, Object> contentMap = new HashMap<>();
    contentMap.put(JsonKey.CONTENT, managedUserList);

    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(contentMap);
    when(esService.search(Mockito.any(SearchDTO.class), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());

    List<Map<String, Object>> userOrgMapList = new ArrayList<>();
    Map<String, Object> userOrgMap = new HashMap<String, Object>();
    userOrgMap.put(JsonKey.USER_ID, "userId");
    userOrgMap.put(JsonKey.ORGANISATION_ID, "orgId");
    userOrgMap.put(JsonKey.IS_DELETED, false);
    userOrgMapList.add(userOrgMap);
    Response userOrgResponse = new Response();
    userOrgResponse.put(JsonKey.RESPONSE, userOrgMapList);
    when(cassandraOperationImpl.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(userOrgResponse);
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
  public void testValidateManagedUserLimit() {
    beforeEachTest();
    try {
      UserUtil.validateManagedUserLimit("ManagedBy", null);
    } catch (ProjectCommonException e) {
      assertEquals(e.getErrorResponseCode(), 400);
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

  @Test(expected = Exception.class)
  public void testValidateExternalIdsAndReturnActiveUser() {
    beforeEachTest();
    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(JsonKey.EXTERNAL_ID, "extId");
    requestMap.put(JsonKey.EXTERNAL_ID_PROVIDER, "provider");
    requestMap.put(JsonKey.EXTERNAL_ID_TYPE, "idType");

    Response response = new Response();
    List<Map<String, Object>> resList = new ArrayList<>();
    Map<String, Object> res = new HashMap<>();
    resList.add(res);
    response.getResult().put(JsonKey.RESPONSE, resList);
    when(cassandraOperationImpl.getRecordsByCompositeKey(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.any(RequestContext.class)))
        .thenReturn(response);
    UserUtil.validateExternalIdsAndReturnActiveUser(requestMap, new RequestContext());
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
    when(esService.search(new SearchDTO(), ProjectUtil.EsType.organisation.getTypeName(), null))
        .thenReturn(promise.future());
    UserUtil.fetchOrgIdByProvider(providers, null);
    Assert.assertTrue(true);
  }

  @Test
  public void testEncryptDeclareFields() throws Exception {
    beforeEachTest();
    List<Map<String, Object>> declarations = new ArrayList<>();
    Map<String, Object> declareFieldMap = new HashMap<>();
    Map<String, Object> userInfo = new HashMap<>();
    userInfo.put(JsonKey.DECLARED_EMAIL, "a**.com");
    userInfo.put(JsonKey.DECLARED_PHONE, "9****90");
    userInfo.put(JsonKey.DECLARED_DISTRICT, "Karnataka");
    declareFieldMap.put(JsonKey.INFO, userInfo);
    declarations.add(declareFieldMap);
    Map<String, Object> dbRecords = new HashMap<>();
    RequestContext context = new RequestContext();
    try {
      UserUtil.encryptDeclarationFields(declarations, dbRecords, context);
    } catch (Exception ex) {
      Map<String, Object> orgMap = new HashMap<>();
      List<Map<String, Object>> orgList = new ArrayList<>();

      orgMap.put("id", "1234");
      orgMap.put("channel", "channel004");
      orgList.add(orgMap);
      Map<String, Object> contentMap = new HashMap<>();
      contentMap.put(JsonKey.CONTENT, orgList);

      Promise<Map<String, Object>> promise = Futures.promise();
      promise.success(contentMap);
      when(esService.search(new SearchDTO(), ProjectUtil.EsType.organisation.getTypeName(), null))
          .thenReturn(promise.future());
    }
    Assert.assertTrue(true);
  }

  @Test
  public void testCreateSelfDeclaredObject() {
    Map<String, Object> declareFieldMap = new HashMap<>();
    declareFieldMap.put(JsonKey.USER_ID, "1234");
    declareFieldMap.put(JsonKey.ORG_ID, "012345678");
    declareFieldMap.put(JsonKey.PERSONA, "teacher");
    declareFieldMap.put(JsonKey.OPERATION, "add");
    Map<String, Object> userInfo = new HashMap<>();
    userInfo.put(JsonKey.DECLARED_EMAIL, "a**.com");
    userInfo.put(JsonKey.DECLARED_PHONE, "9****90");
    userInfo.put(JsonKey.DECLARED_DISTRICT, "Karnataka");
    declareFieldMap.put(JsonKey.INFO, userInfo);

    UserDeclareEntity userDeclareEntity =
        UserUtil.createUserDeclaredObject(declareFieldMap, "01245444444");
    Assert.assertEquals("SUBMITTED", userDeclareEntity.getStatus());
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
    List<Map<String, Object>> res = UserUtil.getActiveUserOrgDetails("123-456-789", null);
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
    when(esService.search(new SearchDTO(), ProjectUtil.EsType.organisation.getTypeName(), null))
        .thenReturn(promise.future());
    Map<String, String> externalIds = new HashMap<>();
    externalIds.put(JsonKey.PROVIDER, "1234");
    externalIds.put(JsonKey.USER_ID, "w131-2323-323-232-3232");
    List<Map<String, String>> externalIdList = new ArrayList<>();
    externalIdList.add(externalIds);
    UserUtil.updateExternalIdsWithProvider(externalIdList, null);
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
    when(esService.search(new SearchDTO(), ProjectUtil.EsType.organisation.getTypeName(), null))
        .thenReturn(promise.future());
    Map<String, String> externalIds = new HashMap<>();
    externalIds.put(JsonKey.PROVIDER, "channel1004");
    externalIds.put(JsonKey.USER_ID, "w131-2323-323-232-3232");
    List<Map<String, String>> externalIdList = new ArrayList<>();
    externalIdList.add(externalIds);
    Map<String, Object> userMap = new HashMap<>();
    userMap.put(JsonKey.EXTERNAL_IDS, externalIdList);
    try {
      UserUtil.updateExternalIdsProviderWithOrgId(userMap, null);
    } catch (Exception ex) {
      Assert.assertTrue(true);
      Assert.assertEquals(
          "Invalid value provider for parameter channel1004. Please provide a valid value.",
          ex.getMessage());
    }
  }

  @Test
  public void testUpdateExternalIdsProviderWithOrgId() {
    beforeEachTest();
    List<Map<String, String>> externalIds = new ArrayList<>();
    Map<String, String> extId1 = new HashMap<>();
    extId1.put(JsonKey.ORIGINAL_ID_TYPE, JsonKey.DECLARED_EMAIL);
    extId1.put(JsonKey.ORIGINAL_PROVIDER, "0123");
    extId1.put(JsonKey.ORIGINAL_EXTERNAL_ID, "abc@diksha.com");
    extId1.put(JsonKey.ID_TYPE, JsonKey.DECLARED_EMAIL);
    extId1.put(JsonKey.PROVIDER, "0123");
    extId1.put(JsonKey.EXTERNAL_ID, "abc@diksha.com");
    extId1.put(JsonKey.OPERATION, "add");
    externalIds.add(extId1);

    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(JsonKey.USER_ID, "user1");
    requestMap.put(JsonKey.CHANNEL, "0123");
    requestMap.put(JsonKey.ROOT_ORG_ID, "012345678921");
    requestMap.put(JsonKey.EXTERNAL_IDS, externalIds);
    UserUtil.updateExternalIdsProviderWithOrgId(requestMap, null);
    Assert.assertTrue(true);
  }

  @Test
  public void testUpdateExternalIds2ProviderWithOrgId() {
    beforeEachTest();
    List<Map<String, String>> externalIds = new ArrayList<>();
    Map<String, String> extId1 = new HashMap<>();
    extId1.put(JsonKey.ORIGINAL_ID_TYPE, JsonKey.DECLARED_EMAIL);
    extId1.put(JsonKey.ORIGINAL_PROVIDER, "0123");
    extId1.put(JsonKey.ORIGINAL_EXTERNAL_ID, "abc@diksha.com");
    extId1.put(JsonKey.ID_TYPE, JsonKey.DECLARED_EMAIL);
    extId1.put(JsonKey.PROVIDER, "0123");
    extId1.put(JsonKey.EXTERNAL_ID, "abc@diksha.com");
    extId1.put(JsonKey.OPERATION, "add");
    Map<String, String> extId2 = new HashMap<>();
    extId2.put(JsonKey.ORIGINAL_ID_TYPE, JsonKey.DECLARED_EMAIL);
    extId2.put(JsonKey.ORIGINAL_PROVIDER, "01234");
    extId2.put(JsonKey.ORIGINAL_EXTERNAL_ID, "abc@diksha.com");
    extId2.put(JsonKey.ID_TYPE, JsonKey.DECLARED_EMAIL);
    extId2.put(JsonKey.PROVIDER, "01234");
    extId2.put(JsonKey.EXTERNAL_ID, "abc@diksha.com");
    extId2.put(JsonKey.OPERATION, "add");
    externalIds.add(extId1);
    externalIds.add(extId2);

    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(JsonKey.USER_ID, "user1");
    requestMap.put(JsonKey.CHANNEL, "0123");
    requestMap.put(JsonKey.ROOT_ORG_ID, "012345678921");
    requestMap.put(JsonKey.EXTERNAL_IDS, externalIds);
    try {
      UserUtil.updateExternalIdsProviderWithOrgId(requestMap, null);
    } catch (Exception ex) {
      Assert.assertTrue(true);
      Assert.assertNotNull(ex);
    }
  }

  @Test
  public void testUpdateExternalIds3ProviderWithOrgId() {
    beforeEachTest();
    List<Map<String, String>> externalIds = new ArrayList<>();
    Map<String, String> extId2 = new HashMap<>();
    extId2.put(JsonKey.ORIGINAL_ID_TYPE, JsonKey.DECLARED_EMAIL);
    extId2.put(JsonKey.ORIGINAL_PROVIDER, "01234");
    extId2.put(JsonKey.ORIGINAL_EXTERNAL_ID, "abc@diksha.com");
    extId2.put(JsonKey.ID_TYPE, JsonKey.DECLARED_EMAIL);
    extId2.put(JsonKey.PROVIDER, "01234");
    extId2.put(JsonKey.EXTERNAL_ID, "abc@diksha.com");
    extId2.put(JsonKey.OPERATION, "add");
    externalIds.add(extId2);

    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(JsonKey.USER_ID, "user1");
    requestMap.put(JsonKey.CHANNEL, "0123");
    requestMap.put(JsonKey.ROOT_ORG_ID, "012345678921");
    requestMap.put(JsonKey.EXTERNAL_IDS, externalIds);
    try {
      UserUtil.updateExternalIdsProviderWithOrgId(requestMap, null);
    } catch (Exception ex) {
      Assert.assertTrue(true);
      Assert.assertEquals(
          "Invalid value provider for parameter 01234. Please provide a valid value.",
          ex.getMessage());
    }
  }

  @Test
  public void testAddMaskEmailAndMaskPhone() {
    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(JsonKey.PHONE, "9999999999");
    requestMap.put(JsonKey.EMAIL, "sunbird@example.com");
    UserUtil.addMaskEmailAndMaskPhone(requestMap);
    Assert.assertTrue(true);
  }

  @Test
  public void testRemoveEntryFromUserLookUp() {
    beforeEachTest();
    Map<String, Object> mergeeMap = new HashMap<>();
    mergeeMap.put(JsonKey.EMAIL, "someEmail");
    mergeeMap.put(JsonKey.PHONE, "somePhone");
    mergeeMap.put(JsonKey.USERNAME, "someUsername");
    List<String> userLookUpIdentifiers =
        Stream.of(JsonKey.EMAIL, JsonKey.PHONE, JsonKey.USERNAME).collect(Collectors.toList());
    UserUtil.removeEntryFromUserLookUp(mergeeMap, userLookUpIdentifiers, null);
    Assert.assertTrue(true);
  }
}
