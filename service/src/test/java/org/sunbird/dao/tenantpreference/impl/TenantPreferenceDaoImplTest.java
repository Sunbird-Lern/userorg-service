package org.sunbird.dao.tenantpreference.impl;

import org.junit.Assert;
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
import org.sunbird.dao.tenantpreference.TenantPreferenceDao;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  CassandraOperationImpl.class,
  ServiceFactory.class,
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class TenantPreferenceDaoImplTest {

  @Before
  public void setUp() {
    PowerMockito.mockStatic(ServiceFactory.class);
    CassandraOperation cassandraOperation = mock(CassandraOperationImpl.class);
    PowerMockito.when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(cassandraOperation.getRecordsByProperties(
      Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
      .thenReturn(cassandraGetRecordByProperty());
    when(cassandraOperation.insertRecord(
      Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
      .thenReturn(createCassandraInsertSuccessResponse());
    when(cassandraOperation.updateRecord(
      Mockito.anyString(),
      Mockito.anyString(),
      Mockito.anyMap(),
      Mockito.anyMap(),
      Mockito.any()))
      .thenReturn(createCassandraInsertSuccessResponse());
  }

  @Test
  public void getPreferenceByIdTest() {
    TenantPreferenceDao preferenceDao = TenantPreferenceDaoImpl.getInstance();
    List<Map<String,Object>> preference = preferenceDao.getTenantPreferenceById("545645132","key", new RequestContext());
    Assert.assertNotNull(preference);
  }

  @Test
  public void insertTenantPreferenceTest() {
    Map<String,Object> preferenceObj = new HashMap<>();
    preferenceObj.put(JsonKey.ORG_ID, "5454545");
    preferenceObj.put(JsonKey.KEY,"key");
    TenantPreferenceDao preferenceDao = TenantPreferenceDaoImpl.getInstance();
    Response response = preferenceDao.insertTenantPreference(preferenceObj, new RequestContext());
    Assert.assertNotNull(response);
  }

  @Test
  public void updateTenantPreferenceTest() {
    Map<String,Object> preferenceObj = new HashMap<>();
    preferenceObj.put(JsonKey.ORG_ID, "5454545");
    preferenceObj.put(JsonKey.KEY,"key");
    preferenceObj.put(JsonKey.DATA,"data");

    Map<String,Object> keys = new HashMap<>();
    keys.put(JsonKey.ORG_ID, "5454545");
    keys.put(JsonKey.KEY,"key");
    TenantPreferenceDao preferenceDao = TenantPreferenceDaoImpl.getInstance();
    Response response = preferenceDao.updateTenantPreference(preferenceObj,keys, new RequestContext());
    Assert.assertNotNull(response);
  }

  private Response createCassandraInsertSuccessResponse() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    return response;
  }

  private static Response cassandraGetRecordByProperty() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.KEY, "anyKey");
    map.put(JsonKey.ORG_ID, "45456464682");
    map.put(
      JsonKey.DATA,
      "{\"default\":{\"action\":\"volunteer\",\"templateName\":\"volunteer\",\"fields\":[[{\"title\":\"Please confirm that ALL the following items are verified (by ticking the check-boxes) before you can publish:\",\"contents\":[{\"name\":\"Appropriateness\",\"checkList\":[\"No Hate speech, Abuse, Violence, Profanity\",\"No Discrimination or Defamation\",\"Is suitable for children\"]}]}]]}}");
    list.add(map);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }


}
