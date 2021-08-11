package org.sunbird.service.tenantpreference;

import org.apache.commons.collections.MapUtils;
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
import org.sunbird.exception.ProjectCommonException;
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
  ServiceFactory.class,
  CassandraOperationImpl.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class TenantPreferenceServiceTest {

  private CassandraOperation cassandraOperation = null;

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    PowerMockito.when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
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

  private Response createCassandraInsertSuccessResponse() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    return response;
  }

  //@Test
  public void validateAndGetTenantPreferencesByIdForCreateTest() {
    when(cassandraOperation.getRecordsByProperties(
      Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
      .thenReturn(cassandraGetRecordByPropertiesEmptyResponse());
    TenantPreferenceService preferenceService = new TenantPreferenceService();
    Map<String,Object> preference = preferenceService.validateAndGetTenantPreferencesById("45456464682","someKey",JsonKey.CREATE,new RequestContext());
    Assert.assertTrue(MapUtils.isEmpty(preference));
  }

  @Test(expected = ProjectCommonException.class)
  public void validateAndGetTenantPreferencesByIdForCreateFailureTest() {
    when(cassandraOperation.getRecordsByProperties(
      Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
      .thenReturn(cassandraGetRecordByProperty());
    TenantPreferenceService preferenceService = new TenantPreferenceService();
    preferenceService.validateAndGetTenantPreferencesById("45456464682","someKey",JsonKey.CREATE,new RequestContext());
  }

  @Test
  public void validateAndGetTenantPreferencesByIdByUpdateFailureTest() {
    when(cassandraOperation.getRecordsByProperties(
      Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
      .thenReturn(cassandraGetRecordByProperty());
    TenantPreferenceService preferenceService = new TenantPreferenceService();
    Map<String,Object> preference = preferenceService.validateAndGetTenantPreferencesById("45456464682","someKey",JsonKey.UPDATE,new RequestContext());
    Assert.assertTrue(MapUtils.isNotEmpty(preference));
  }

  @Test
  public void createPreferenceSuccess() {
    Map<String,Object> data = new HashMap<>();
    data.put("someKey","key");
    TenantPreferenceService preferenceService = new TenantPreferenceService();
    Response response = preferenceService.createPreference("87986546549","someKey",data, "1245-4654-8454",new RequestContext());
    Assert.assertNotNull(response);
  }

  @Test
  public void updatePreferenceFailure() {
    Map<String,Object> data = new HashMap<>();
    data.put("someKey","key");
    TenantPreferenceService preferenceService = new TenantPreferenceService();
    Response response = preferenceService.updatePreference("87986546549","someKey",data, "1245-4654-8454",new RequestContext());
    Assert.assertNotNull(response);
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

  private static Response cassandraGetRecordByPropertiesEmptyResponse() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList();
    response.put(JsonKey.RESPONSE, list);
    return response;
  }

}
