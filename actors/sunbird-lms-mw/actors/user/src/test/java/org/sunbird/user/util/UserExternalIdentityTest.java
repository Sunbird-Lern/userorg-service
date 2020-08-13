package org.sunbird.user.util;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class UserExternalIdentityTest {

  @Test
  public void testUserExternalIdentity() {
    User user = new User();
    user.setId("123-456-789");
    user.setUserId("123-456-789");
    Map<String, String> externalId = new HashMap<>();
    externalId.put(JsonKey.ID, "id");
    externalId.put(JsonKey.ID_TYPE, "idType");
    externalId.put(JsonKey.PROVIDER, "provider");
    externalId.put(JsonKey.OPERATION, JsonKey.ADD);
    List<Map<String, String>> extList = new ArrayList<>();
    extList.add(externalId);
    user.setExternalIds(extList);

    Response response1 = new Response();
    List<Map<String, Object>> responseList = new ArrayList<>();
    Map<String, Object> result = new HashMap<>();
    result.put(JsonKey.ID, "id");
    result.put(JsonKey.ID_TYPE, "idType");
    result.put(JsonKey.PROVIDER, "provider");
    result.put(JsonKey.USER_ID, "123-456-7890");
    responseList.add(result);
    response1.getResult().put(JsonKey.RESPONSE, responseList);

    PowerMockito.mockStatic(ServiceFactory.class);
    CassandraOperationImpl cassandraOperationImpl = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    when(cassandraOperationImpl.getRecordsByIndexedProperty(
            JsonKey.SUNBIRD, "user", JsonKey.EMAIL, "test@test.com"))
        .thenReturn(response1);
    when(cassandraOperationImpl.getRecordsByProperties(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(response1);
    try {
      UserUtil.checkExternalIdUniqueness(user, JsonKey.CREATE);
    } catch (ProjectCommonException ex) {
      Assert.assertNotNull(ex);
    }
  }

  @Test
  public void testUserExternalIdentityFrUpdate() {
    User user = new User();
    user.setId("123-456-789");
    user.setUserId("123-456-789");
    Map<String, String> externalId = new HashMap<>();
    externalId.put(JsonKey.ID, "id");
    externalId.put(JsonKey.ID_TYPE, "idType");
    externalId.put(JsonKey.PROVIDER, "provider");
    externalId.put(JsonKey.OPERATION, JsonKey.ADD);
    List<Map<String, String>> extList = new ArrayList<>();
    extList.add(externalId);
    user.setExternalIds(extList);

    Response response1 = new Response();
    List<Map<String, Object>> responseList = new ArrayList<>();
    Map<String, Object> result = new HashMap<>();
    result.put(JsonKey.ID, "id");
    result.put(JsonKey.ID_TYPE, "idType");
    result.put(JsonKey.PROVIDER, "provider");
    result.put(JsonKey.USER_ID, "123-456-7890");
    responseList.add(result);
    response1.getResult().put(JsonKey.RESPONSE, responseList);

    PowerMockito.mockStatic(ServiceFactory.class);
    CassandraOperationImpl cassandraOperationImpl = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    when(cassandraOperationImpl.getRecordsByIndexedProperty(
            JsonKey.SUNBIRD, "user", JsonKey.EMAIL, "test@test.com"))
        .thenReturn(response1);
    when(cassandraOperationImpl.getRecordsByProperties(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(response1);
    try {
      UserUtil.checkExternalIdUniqueness(user, JsonKey.UPDATE);
    } catch (ProjectCommonException ex) {
      Assert.assertNotNull(ex);
    }
  }

  @Test
  public void testUserExternalIdentityFrRemove() {
    User user = new User();
    user.setId("123-456-789");
    user.setUserId("123-456-789");
    Map<String, String> externalId = new HashMap<>();
    externalId.put(JsonKey.ID, "id");
    externalId.put(JsonKey.ID_TYPE, "idType");
    externalId.put(JsonKey.PROVIDER, "provider");
    externalId.put(JsonKey.OPERATION, JsonKey.REMOVE);
    List<Map<String, String>> extList = new ArrayList<>();
    extList.add(externalId);
    user.setExternalIds(extList);

    Response response1 = new Response();
    List<Map<String, Object>> responseList = new ArrayList<>();
    Map<String, Object> result = new HashMap<>();
    result.put(JsonKey.ID, "id");
    result.put(JsonKey.ID_TYPE, "idType");
    result.put(JsonKey.PROVIDER, "provider");
    result.put(JsonKey.USER_ID, "123-456-7890");
    responseList.add(result);
    response1.getResult().put(JsonKey.RESPONSE, responseList);

    PowerMockito.mockStatic(ServiceFactory.class);
    CassandraOperationImpl cassandraOperationImpl = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    when(cassandraOperationImpl.getRecordsByIndexedProperty(
            JsonKey.SUNBIRD, "user", JsonKey.EMAIL, "test@test.com"))
        .thenReturn(response1);
    when(cassandraOperationImpl.getRecordsByProperties(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(response1);
    try {
      UserUtil.checkExternalIdUniqueness(user, JsonKey.UPDATE);
    } catch (ProjectCommonException ex) {
      Assert.assertNotNull(ex);
    }
  }

  @Test
  public void testUserExternalIdentityFr() {
    User user = new User();
    user.setId("123-456-789");
    user.setUserId("123-456-789");
    Map<String, String> externalId = new HashMap<>();
    externalId.put(JsonKey.ID, "id");
    externalId.put(JsonKey.ID_TYPE, "idType");
    externalId.put(JsonKey.PROVIDER, "provider");
    externalId.put(JsonKey.OPERATION, JsonKey.REMOVE);
    List<Map<String, String>> extList = new ArrayList<>();
    extList.add(externalId);
    user.setExternalIds(extList);

    Response response1 = new Response();
    List<Map<String, Object>> responseList = new ArrayList<>();
    response1.getResult().put(JsonKey.RESPONSE, responseList);

    PowerMockito.mockStatic(ServiceFactory.class);
    CassandraOperationImpl cassandraOperationImpl = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    when(cassandraOperationImpl.getRecordsByIndexedProperty(
            JsonKey.SUNBIRD, "user", JsonKey.EMAIL, "test@test.com"))
        .thenReturn(response1);
    when(cassandraOperationImpl.getRecordsByProperties(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(response1);
    try {
      UserUtil.checkExternalIdUniqueness(user, JsonKey.UPDATE);
    } catch (ProjectCommonException ex) {
      Assert.assertNotNull(ex);
    }
  }
}
