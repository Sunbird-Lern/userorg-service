package org.sunbird.user;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
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
import org.sunbird.learner.util.Util;
import org.sunbird.models.user.User;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(PowerMockRunner.class)
@PrepareForTest({
  CassandraOperationImpl.class,
  ServiceFactory.class,
  EncryptionService.class,
  org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "javax.crypto.*",
  "javax.script.*"
})
public class SupportMultipleExternalIdsTest {

  private static User user;

  @Before
  public void beforeEach() {

    PowerMockito.mockStatic(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class);
    EncryptionService encryptionService = Mockito.mock(EncryptionService.class);
    Mockito.when(
            org.sunbird.common.models.util.datasecurity.impl.ServiceFactory
                .getEncryptionServiceInstance(null))
        .thenReturn(encryptionService);
    try {
      Mockito.when(encryptionService.encryptData(Mockito.anyString())).thenReturn("abc123");
    } catch (Exception e) { // TODO Auto-generated catch block
      Assert.fail("Initialization failed");
    }
  }

  @BeforeClass
  public static void setUp() {

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

    PowerMockito.mockStatic(ServiceFactory.class);
    CassandraOperation cassandraOperation = PowerMockito.mock(CassandraOperationImpl.class);
    PowerMockito.when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    Response response1 = new Response();

    List<Map<String, String>> resMapList = new ArrayList<>();
    resMapList.add(externalIdResMap);
    response1.put(JsonKey.RESPONSE, resMapList);
    PowerMockito.when(
            cassandraOperation.getRecordsByCompositeKey(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(response1);
  }

  @Test
  public void testCheckExternalIdUniquenessSuccessForCreate() {

    try {
      Util.checkExternalIdUniqueness(user, JsonKey.CREATE);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.userAlreadyExists.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testCheckExternalIdUniquenessSuccessWithUpdateOperation() {

    try {
      user.setUserId("someUserId2");
      user.getExternalIds().get(0).put(JsonKey.OPERATION, JsonKey.UPDATE);
      Util.checkExternalIdUniqueness(user, JsonKey.UPDATE);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.externalIdNotFound.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testCheckExternalIdUniquenessSuccessForUpdate() {

    try {
      user.setUserId("someUserId2");
      user.getExternalIds().get(0).remove(JsonKey.OPERATION);
      Util.checkExternalIdUniqueness(user, JsonKey.UPDATE);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.externalIdAssignedToOtherUser.getErrorCode(), e.getCode());
    }
  }

  @Test
  public void testCheckExternalIdUniquenessSuccessWithRemoveOperation() {

    try {
      user.setUserId("someUserId2");
      user.getExternalIds().get(0).put(JsonKey.OPERATION, JsonKey.REMOVE);
      Util.checkExternalIdUniqueness(user, JsonKey.UPDATE);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.externalIdNotFound.getErrorCode(), e.getCode());
    }
  }
}
