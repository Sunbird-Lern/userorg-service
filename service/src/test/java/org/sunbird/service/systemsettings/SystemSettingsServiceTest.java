package org.sunbird.service.systemsettings;

import com.fasterxml.jackson.core.type.TypeReference;
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
import org.sunbird.common.Constants;
import org.sunbird.dao.systemsettings.SystemSettingDao;
import org.sunbird.dao.systemsettings.impl.SystemSettingDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.systemsettings.SystemSetting;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.util.DataCacheHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.powermock.api.mockito.PowerMockito.mock;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  CassandraOperationImpl.class,
  SystemSettingDao.class,
  SystemSettingDaoImpl.class,
  DataCacheHandler.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class SystemSettingsServiceTest {
  private CassandraOperation cassandraOperation = null;

  @Before
  public void setUp() throws Exception {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    PowerMockito.when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    SystemSettingDaoImpl systemSettingDao = PowerMockito.mock(SystemSettingDaoImpl.class);
    PowerMockito.whenNew(SystemSettingDaoImpl.class).withNoArguments().thenReturn(systemSettingDao);
  }

  @Test
  public void getSystemSettingByKeyTestSuccess() {
    initDataCacheMock();
    SystemSettingsService service = new SystemSettingsService();
    SystemSetting setting1 = service.getSystemSettingByKey("setting1", new RequestContext());
    Assert.assertNotNull(setting1);
  }

  @Test
  public void getSystemSettingByKeyTestSuccess2() {
    initDataCacheMock();
    getRecordByIdNonEmptyResponse();
    SystemSettingsService service = new SystemSettingsService();
    SystemSetting setting31 = service.getSystemSettingByKey("setting31", new RequestContext());
    Assert.assertNotNull(setting31);
  }

  @Test
  public void getSystemSettingByFieldAndKeyTestSuccess() {
    initDataCacheMock();
    getRecordByIdNonEmptyResponse();
    SystemSettingsService service = new SystemSettingsService();
    Map<String, Object> setting1 = service.getSystemSettingByFieldAndKey(
            "setting3",
            "valueKey1",
            new TypeReference<Map>() {},
            new RequestContext());
    Assert.assertNotNull(setting1);
  }

  @Test(expected = ProjectCommonException.class)
  public void getSystemSettingByKeyTestFailure() {
    initDataCacheMock();
    getRecordByIdEmptyResponse();
    SystemSettingsService service = new SystemSettingsService();
    service.getSystemSettingByKey("setting21", new RequestContext());
  }

  @Test
  public void getAllSystemSettingsTestSuccess() {
    initDataCacheMock();
    SystemSettingsService service = new SystemSettingsService();
    List<SystemSetting> settings = service.getAllSystemSettings(new RequestContext());
    Assert.assertNotNull(settings);
  }

  @Test
  public void getAllSystemSettingsTestSuccess2() {
    PowerMockito.mockStatic(DataCacheHandler.class);
    Map<String, String> systemSettings = new HashMap<>();
    PowerMockito.when(DataCacheHandler.getConfigSettings()).thenReturn(systemSettings);
    PowerMockito.when(
      cassandraOperation.getAllRecords(
        Mockito.anyString(), Mockito.anyString(), Mockito.any()))
      .thenReturn(getSystemSettingSuccessResponse(true));
    SystemSettingsService service = new SystemSettingsService();
    List<SystemSetting> settings = service.getAllSystemSettings(new RequestContext());
    Assert.assertNotNull(settings);
  }

  @Test
  public void setSystemSettings() {
    PowerMockito.when(
      cassandraOperation.upsertRecord(
        Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
      .thenReturn(new Response());
    SystemSettingsService service = new SystemSettingsService();
    Map<String,Object> setting = new HashMap<>();
    setting.put("someNewKey","someNewValue");
    Response res = service.setSystemSettings(setting, new RequestContext());
    Assert.assertNotNull(res);
  }

  private void initDataCacheMock() {
    PowerMockito.mockStatic(DataCacheHandler.class);
    Map<String, String> systemSettings = new HashMap<>();
    systemSettings.put("setting1","value1");
    systemSettings.put("setting2","value2");
    systemSettings.put("setting3","{\"valueKey1\":{\"url\":\"http://dev/terms.html\"}}");
    PowerMockito.when(DataCacheHandler.getConfigSettings()).thenReturn(systemSettings);
  }

  private void getRecordByIdEmptyResponse() {
    Response response = new Response();
    Map<String, Object> responseMap = new HashMap<>();
    responseMap.put(Constants.RESPONSE, new ArrayList<Map<String, Object>>());
    response.getResult().putAll(responseMap);
    PowerMockito.when(
      cassandraOperation.getRecordById(
        Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any(RequestContext.class)))
      .thenReturn(response);
  }

  private void getRecordByIdNonEmptyResponse() {
    Response response = new Response();
    Map<String, Object> responseMap = new HashMap<>();
    List<Map<String, Object>> resList = new ArrayList<>();
    Map<String, Object> res = new HashMap<>();
    res.put("key","someValue");
    resList.add(res);
    responseMap.put(Constants.RESPONSE, resList);
    response.getResult().putAll(responseMap);
    PowerMockito.when(
      cassandraOperation.getRecordById(
        Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any(RequestContext.class)))
      .thenReturn(response);
  }

  private Response getSystemSettingSuccessResponse(boolean isEmpty) {
    Response response = new Response();
    if (!isEmpty)
      response.put(
        JsonKey.RESPONSE,
        new ArrayList<Map<String, Object>>(Arrays.asList(new HashMap<>())));
    else {
      response.put(JsonKey.RESPONSE, new ArrayList<Map<String, Object>>());
    }
    return response;
  }
}
