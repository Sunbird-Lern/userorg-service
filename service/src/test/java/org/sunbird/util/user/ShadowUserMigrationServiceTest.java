package org.sunbird.util.user;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.model.ClaimStatus;
import org.sunbird.model.ShadowUser;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.response.Response;
import org.sunbird.service.user.ShadowUserMigrationService;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.common.ProjectUtil;

@RunWith(PowerMockRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({UserServiceImpl.class, ServiceFactory.class, CassandraOperationImpl.class})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class ShadowUserMigrationServiceTest {

  private static Response response;
  public static CassandraOperationImpl cassandraOperationImpl;
  private static final String KEYSPACE_NAME = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE);

  @Before
  public void beforeEachTest() {
    response = new Response();
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperationImpl = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    when(cassandraOperationImpl.searchValueInList(
            KEYSPACE_NAME, JsonKey.SHADOW_USER, JsonKey.USER_IDs, "EFG", null))
        .thenReturn(getRecordsById(false));
    when(cassandraOperationImpl.searchValueInList(
            KEYSPACE_NAME, JsonKey.SHADOW_USER, JsonKey.USER_IDs, "DEF", null))
        .thenReturn(getRecordsById(true));
    when(cassandraOperationImpl.searchValueInList(
            KEYSPACE_NAME, JsonKey.SHADOW_USER, JsonKey.USER_IDs, "ABC", new HashMap<>(), null))
        .thenReturn(getRecordsById(false));
    when(cassandraOperationImpl.searchValueInList(
            KEYSPACE_NAME, JsonKey.SHADOW_USER, JsonKey.USER_IDs, "XYZ", new HashMap<>(), null))
        .thenReturn(getRecordsById(true));
  }

  @Test
  public void testGetRecordByUserIdSuccess() {
    ShadowUser shadowUser = ShadowUserMigrationService.getRecordByUserId("EFG", null);
    Assert.assertEquals("TN", shadowUser.getChannel());
  }

  @Test
  public void testGetRecordByUserIdFailure() {
    ShadowUser shadowUser = ShadowUserMigrationService.getRecordByUserId("DEF", null);
    Assert.assertEquals(null, shadowUser);
  }

  @Test
  public void testUpdateRecord() {
    Map<String, Object> compositeKeysMap = new HashMap<>();
    compositeKeysMap.put(JsonKey.USER_EXT_ID, "anyUserExtId");
    compositeKeysMap.put(JsonKey.CHANNEL, "anyChannel");
    when(cassandraOperationImpl.updateRecord(
            KEYSPACE_NAME, JsonKey.SHADOW_USER, new HashMap<>(), compositeKeysMap, null))
        .thenReturn(response);
    boolean isRecordUpdated =
        ShadowUserMigrationService.updateRecord(new HashMap<>(), "anyChannel", "anyUserExtId", null);
    Assert.assertEquals(true, isRecordUpdated);
  }

  @Test
  public void testmarkUserAsRejected() {
    ShadowUser shadowUser =
        new ShadowUser.ShadowUserBuilder()
            .setChannel("anyChannel")
            .setUserExtId("anyUserExtId")
            .build();
    Map<String, Object> compositeKeysMap = new HashMap<>();
    compositeKeysMap.put(JsonKey.USER_EXT_ID, "anyUserExtId");
    compositeKeysMap.put(JsonKey.CHANNEL, "anyChannel");
    when(cassandraOperationImpl.updateRecord(
            KEYSPACE_NAME, JsonKey.SHADOW_USER, new HashMap<>(), compositeKeysMap, null))
        .thenReturn(response);
    boolean isRecordUpdated = ShadowUserMigrationService.markUserAsRejected(shadowUser, null);
    Assert.assertEquals(true, isRecordUpdated);
  }

  @Test
  public void testUpdateClaimStatus() {
    ShadowUser shadowUser =
        new ShadowUser.ShadowUserBuilder()
            .setChannel("anyChannel")
            .setUserExtId("anyUserExtId")
            .build();
    Map<String, Object> compositeKeysMap = new HashMap<>();
    compositeKeysMap.put(JsonKey.USER_EXT_ID, "anyUserExtId");
    compositeKeysMap.put(JsonKey.CHANNEL, "anyChannel");
    when(cassandraOperationImpl.updateRecord(
            KEYSPACE_NAME, JsonKey.SHADOW_USER, new HashMap<>(), compositeKeysMap, null))
        .thenReturn(response);
    boolean isRecordUpdated =
        ShadowUserMigrationService.updateClaimStatus(shadowUser, ClaimStatus.ELIGIBLE.getValue(), null);
    Assert.assertEquals(true, isRecordUpdated);
  }

  @Test
  public void testGetEligibleUserByIdsSuccess() {
    List<ShadowUser> shadowUserList =
        ShadowUserMigrationService.getEligibleUsersById("ABC", new HashMap<>(), null);
    Assert.assertEquals(1, shadowUserList.size());
  }

  @Test
  public void testGetEligibleUserByIdsFailure() {
    List<ShadowUser> shadowUserList =
        ShadowUserMigrationService.getEligibleUsersById("XYZ", new HashMap<>(), null);
    Assert.assertEquals(0, shadowUserList.size());
  }

  @Test
  public void testGetEligibleUserByIdsWithoutProps() {
    List<ShadowUser> shadowUserList = ShadowUserMigrationService.getEligibleUsersById("EFG", null);
    Assert.assertEquals(1, shadowUserList.size());
  }

  private Response getRecordsById(boolean empty) {
    Response res = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    if (!empty) {
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.CHANNEL, "TN");
      map.put(JsonKey.USER_ID, "ABC");
      map.put(JsonKey.CLAIM_STATUS, ClaimStatus.ELIGIBLE.getValue());
      list.add(map);
    }
    res.put(JsonKey.RESPONSE, list);
    return res;
  }
}
