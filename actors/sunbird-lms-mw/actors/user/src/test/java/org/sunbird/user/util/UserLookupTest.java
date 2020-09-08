package org.sunbird.user.util;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

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
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.datasecurity.EncryptionService;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.models.user.User;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  CassandraOperationImpl.class,
  DataCacheHandler.class,
  org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class
})
@PowerMockIgnore({"javax.management.*"})
public class UserLookupTest {
  public static CassandraOperation cassandraOperation;
  private static User user;
  private static EncryptionService encryptionService;

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(DataCacheHandler.class);
    Map<String, String> settingMap = new HashMap<String, String>();
    settingMap.put(JsonKey.PHONE_UNIQUE, "True");
    settingMap.put(JsonKey.EMAIL_UNIQUE, "True");
    when(DataCacheHandler.getConfigSettings()).thenReturn(settingMap);
    encryptionService = PowerMockito.mock(EncryptionService.class);
    PowerMockito.mockStatic(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class);
    when(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory
            .getEncryptionServiceInstance(null))
        .thenReturn(encryptionService);
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(new Response());

    List<Map<String, String>> externalIds = new ArrayList<>();
    Map<String, String> externalIdReqMap = new HashMap<>();
    externalIdReqMap.put(JsonKey.ID, "userId");
    externalIdReqMap.put(JsonKey.PROVIDER, "someProvider");
    externalIdReqMap.put(JsonKey.ID_TYPE, "someIdType");
    externalIdReqMap.put(JsonKey.USER_ID, "reqUserId");
    externalIdReqMap.put(JsonKey.EXTERNAL_ID, "someExternalId");

    externalIds.add(externalIdReqMap);
    user = new User();
    user.setExternalIds(externalIds);

    Map<String, String> externalIdResMap = new HashMap<>();
    externalIdResMap.put(JsonKey.PROVIDER, "someProvider");
    externalIdResMap.put(JsonKey.ID_TYPE, "someIdType");
    externalIdResMap.put(JsonKey.USER_ID, "someUserId");
    externalIdResMap.put(JsonKey.EXTERNAL_ID, "someExternalId");
  }

  @Test
  public void checkUsernameUniqueness() throws Exception {
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyObject(), Mockito.anyObject()))
        .thenReturn(getRecordsByCompositeKeyResponse());

    boolean response = new UserLookUp().checkUsernameUniqueness("userName", true, null);
    assertFalse(response);
  }

  @Test
  public void checkPhoneUniquenessExist() throws Exception {
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyObject(), Mockito.anyObject()))
        .thenReturn(getRecordsByCompositeKeyResponse());

    User user = new User();
    user.setPhone("9663890400");
    boolean response = false;
    try {
      new UserLookUp().checkPhoneUniqueness(user, "create", null);
      response = true;
    } catch (ProjectCommonException e) {
      assertEquals(e.getResponseCode(), 400);
    }
    assertFalse(response);
  }

  @Test
  public void checkPhoneUniqueness() throws Exception {
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyObject(), Mockito.anyObject()))
        .thenReturn(getRecordsByCompositeKeyResponse());

    User user = new User();
    user.setPhone("9663890400");
    boolean response = false;
    try {
      new UserLookUp().checkPhoneUniqueness(user, "update", null);
      response = true;
    } catch (ProjectCommonException e) {
      assertEquals(e.getResponseCode(), 400);
    }
    assertFalse(response);
  }

  @Test
  public void checkPhoneExist() {
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyObject(), Mockito.anyObject()))
        .thenReturn(getRecordsByCompositeKeyResponse());

    boolean response = false;
    try {
      new UserLookUp().checkPhoneUniqueness("9663890400", null);
      response = true;
    } catch (ProjectCommonException e) {
      assertEquals(e.getResponseCode(), 400);
    }
    assertFalse(response);
  }

  @Test
  public void checkEmailUniqueness() throws Exception {
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyObject(), Mockito.anyObject()))
        .thenReturn(getRecordsByCompositeKeyResponse());
    boolean response = false;
    try {
      new UserLookUp().checkEmailUniqueness("test@test.com", null);
      response = true;
    } catch (ProjectCommonException e) {
      assertEquals(e.getResponseCode(), 400);
    }
    assertFalse(response);
  }

  @Test
  public void checkEmailUniquenessWithOpType() throws Exception {
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyObject(), Mockito.anyObject()))
        .thenReturn(getRecordsByCompositeKeyResponse());
    User user = new User();
    user.setEmail("test@test.com");
    boolean response = false;
    try {
      new UserLookUp().checkEmailUniqueness(user, "create", null);
    } catch (ProjectCommonException e) {
      assertEquals(e.getResponseCode(), 400);
    }
    assertFalse(response);
  }

  public Response getRecordsByCompositeKeyResponse() {
    Response response1 = new Response();
    List<Map<String, Object>> responseList = new ArrayList<>();
    Map<String, Object> result = new HashMap<>();
    result.put(JsonKey.ID, "123-456-789");
    responseList.add(result);
    response1.getResult().put(JsonKey.RESPONSE, responseList);

    return response1;
  }

  @Test
  public void testCheckExternalIdUniquenessSuccessForCreate() {
    try {
      new UserLookUp().checkExternalIdUniqueness(user, JsonKey.CREATE, null);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.userAlreadyExists.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testCheckExternalIdUniquenessSuccessWithUpdateOperation() {
    try {
      user.setUserId("someUserId2");
      user.getExternalIds().get(0).put(JsonKey.OPERATION, JsonKey.UPDATE);
      new UserLookUp().checkExternalIdUniqueness(user, JsonKey.UPDATE, null);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.externalIdNotFound.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testCheckExternalIdUniquenessSuccessForUpdate() {

    try {
      user.setUserId("someUserId2");
      user.getExternalIds().get(0).remove(JsonKey.OPERATION);
      new UserLookUp().checkExternalIdUniqueness(user, JsonKey.UPDATE, null);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.externalIdAssignedToOtherUser.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testCheckExternalIdUniquenessSuccessWithRemoveOperation() {
    try {
      user.setUserId("someUserId2");
      user.getExternalIds().get(0).put(JsonKey.OPERATION, JsonKey.REMOVE);
      new UserLookUp().checkExternalIdUniqueness(user, JsonKey.UPDATE, null);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.externalIdNotFound.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void insertExternalIdIntoUserLookup() {
    Response response = new Response();
    response.put(JsonKey.SUCCESS, "success");
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> req = new HashMap<>();
    req.put(JsonKey.PROVIDER, "channel1003");
    req.put(JsonKey.ID_TYPE, "channel1003");
    req.put(JsonKey.ID, "extid1e2d");
    list.add(req);
    Response response1 = new UserLookUp().insertExternalIdIntoUserLookup(list, "someUserId2", null);
    assertNotNull(response1);
  }
}
