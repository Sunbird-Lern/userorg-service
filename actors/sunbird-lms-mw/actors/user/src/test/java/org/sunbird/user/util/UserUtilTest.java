package org.sunbird.user.util;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.math.NumberUtils;
import org.junit.Before;
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
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.Util;
import org.sunbird.models.user.User;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  CassandraOperationImpl.class,
  DataCacheHandler.class,
  EsClientFactory.class,
  ElasticSearchRestHighImpl.class,
  Util.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*"
})
public class UserUtilTest {
  private static Response response;
  public static CassandraOperationImpl cassandraOperationImpl;
  private static ElasticSearchService esService;

  @Before
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

    PowerMockito.mockStatic(EsClientFactory.class);
    esService = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);

    PowerMockito.mockStatic(Util.class);
  }

  @Test
  public void generateUniqueStringSuccess() {
    String val = UserUtil.generateUniqueString(4);
    assertTrue(val.length() == 4);
  }

  @Test
  public void generateUniqueStringSecondCharCheck() {
    String val = UserUtil.generateUniqueString(5);
    assertTrue(val.length() == 5);
    assertTrue(
        NumberUtils.isNumber(val.substring(1, 2)) || NumberUtils.isNumber(val.substring(2, 3)));
  }

  @Test
  public void checkPhoneUniquenessExist() {
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
    Map<String, Object> userMap = new HashMap<String, Object>();
    userMap.put(JsonKey.FIRST_NAME, "Test User");
    UserUtil.setUserDefaultValueForV3(userMap);
    assertNotNull(userMap.get(JsonKey.USERNAME));
    assertNotNull(userMap.get(JsonKey.STATUS));
    assertNotNull(userMap.get(JsonKey.ROLES));
  }

  @Test
  public void testValidateManagedUserLimit() {

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
}
