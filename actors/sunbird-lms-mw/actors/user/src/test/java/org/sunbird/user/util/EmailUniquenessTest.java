package org.sunbird.user.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.datasecurity.EncryptionService;
import org.sunbird.common.models.util.datasecurity.impl.DefaultEncryptionServivceImpl;
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
  DefaultEncryptionServivceImpl.class,
  Util.class,
  EncryptionService.class,
  org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class
})
@PowerMockIgnore({"javax.management.*"})
public class EmailUniquenessTest {
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

    PowerMockito.mockStatic(EsClientFactory.class);
    esService = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);

    PowerMockito.mockStatic(Util.class);
  }

  @Test
  public void checkEmailUniquenessExist() throws Exception {
    beforeEachTest();
    PowerMockito.mockStatic(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class);
    EncryptionService encryptionService = PowerMockito.mock(EncryptionService.class);
    when(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory
            .getEncryptionServiceInstance(null))
        .thenReturn(encryptionService);
    Map<String, String> settingMap = new HashMap<>();
    settingMap.put(JsonKey.EMAIL_UNIQUE, "True");
    when(DataCacheHandler.getConfigSettings()).thenReturn(settingMap);

    when(encryptionService.encryptData(Mockito.anyString())).thenReturn("test@test.com");
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
    user.setEmail("test@test.com");
    boolean response = false;
    try {
      UserUtil.checkEmailUniqueness(user, "create");
      response = true;
    } catch (ProjectCommonException e) {
      assertEquals(e.getResponseCode(), 400);
    }
    assertFalse(response);
  }
}
