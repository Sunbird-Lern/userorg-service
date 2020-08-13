package org.sunbird.user.util;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.sunbird.common.models.util.datasecurity.impl.DefaultEncryptionServivceImpl;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  CassandraOperationImpl.class,
  DataCacheHandler.class,
  DefaultEncryptionServivceImpl.class,
  Util.class,
  EncryptionService.class,
  org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class
})
@PowerMockIgnore({"javax.management.*"})
public class UserUtilityTest {
  @Test
  public void checkEmailUniqueness() throws Exception {
    PowerMockito.mockStatic(DataCacheHandler.class);
    Map<String, String> settingMap = new HashMap<>();
    settingMap.put(JsonKey.EMAIL_UNIQUE, "True");
    when(DataCacheHandler.getConfigSettings()).thenReturn(settingMap);
    PowerMockito.mockStatic(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class);
    EncryptionService encryptionService = PowerMockito.mock(DefaultEncryptionServivceImpl.class);

    when(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory
            .getEncryptionServiceInstance(null))
        .thenReturn(encryptionService);
    when(encryptionService.encryptData(Mockito.anyString())).thenReturn("test@test.com");

    Response response1 = new Response();
    List<Map<String, Object>> responseList = new ArrayList<>();
    Map<String, Object> result = new HashMap<>();
    result.put(JsonKey.IS_DELETED, false);
    result.put(JsonKey.USER_ID, "123-456-789");
    responseList.add(result);
    response1.getResult().put(JsonKey.RESPONSE, responseList);
    PowerMockito.mockStatic(ServiceFactory.class);
    CassandraOperation cassandraOperation = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(cassandraOperation.getRecordsByIndexedProperty(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(response1);

    boolean response = false;
    try {
      UserUtil.checkEmailUniqueness("test@test.com");
      response = true;
    } catch (ProjectCommonException e) {
      assertEquals(e.getResponseCode(), 400);
    }
    assertFalse(response);
  }

  @Test
  public void identifierExists() throws Exception {
    PowerMockito.mockStatic(DataCacheHandler.class);
    Map<String, String> settingMap = new HashMap<String, String>();
    settingMap.put(JsonKey.EMAIL_UNIQUE, "True");
    when(DataCacheHandler.getConfigSettings()).thenReturn(settingMap);
    PowerMockito.mockStatic(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class);
    EncryptionService encryptionService = PowerMockito.mock(DefaultEncryptionServivceImpl.class);
    when(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory
            .getEncryptionServiceInstance(null))
        .thenReturn(encryptionService);
    when(encryptionService.encryptData(Mockito.anyString())).thenReturn("test@test.com");
    // beforeEachTest();
    Response response1 = new Response();
    List<Map<String, Object>> responseList = new ArrayList<>();
    Map<String, Object> result = new HashMap<>();
    result.put(JsonKey.IS_DELETED, false);
    result.put(JsonKey.USER_ID, "123-456-789");
    responseList.add(result);
    response1.getResult().put(JsonKey.RESPONSE, responseList);
    PowerMockito.mockStatic(ServiceFactory.class);
    CassandraOperation cassandraOperation = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(cassandraOperation.getRecordsByIndexedProperty(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(response1);
    boolean bool = UserUtil.identifierExists("email", "test@test.com");
    assertTrue(bool);
  }
}
