package org.sunbird.dao.systemsettings.impl;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.sunbird.dao.systemsettings.SystemSettingDao;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.systemsettings.SystemSetting;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

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
public class SystemSettingDaoImplTest {
  private CassandraOperation cassandraOperation;
  private SystemSettingDao systemSettingDaoImpl;
  private static String ROOT_ORG_ID = "defaultRootOrgId";
  private static String FIELD = "someField";
  private static String VALUE = "someValue";

  @Before
  public void setUp() {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    systemSettingDaoImpl = new SystemSettingDaoImpl();
  }

  @Test
  public void testSetSystemSettingSuccess() {
    PowerMockito.when(
            cassandraOperation.upsertRecord(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(new Response());
    boolean thrown = false;
    try {
      SystemSetting systemSetting = new SystemSetting(ROOT_ORG_ID, FIELD, VALUE);
      Response upsertResp = systemSettingDaoImpl.write(systemSetting, new RequestContext());
      Assert.assertNotEquals(null, upsertResp);
    } catch (Exception e) {
      thrown = true;
    }
    Assert.assertEquals(false, thrown);
  }

  @Test
  public void testReadSystemSettingSuccess() {
    PowerMockito.when(
            cassandraOperation.getRecordById(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(getSystemSettingSuccessResponse(false));
    SystemSetting systemSetting =
        systemSettingDaoImpl.readByField(ROOT_ORG_ID, new RequestContext());
    Assert.assertNotNull(systemSetting);
  }

  @Test
  public void testReadSystemSettingEmpty() {
    PowerMockito.when(
            cassandraOperation.getRecordById(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(getSystemSettingSuccessResponse(true));
    SystemSetting systemSetting = systemSettingDaoImpl.readByField(FIELD, new RequestContext());
    Assert.assertNull(systemSetting);
  }

  @Test
  public void testReadAllSystemSettingsSuccess() {
    PowerMockito.when(
            cassandraOperation.getAllRecords(
                Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(getSystemSettingSuccessResponse(false));
    List<SystemSetting> result = systemSettingDaoImpl.readAll(new RequestContext());
    Assert.assertNotNull(result);
  }

  @Test
  public void testReadAllSystemSettingsEmpty() {
    PowerMockito.when(
            cassandraOperation.getAllRecords(
                Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(getSystemSettingSuccessResponse(true));
    List<SystemSetting> result = systemSettingDaoImpl.readAll(new RequestContext());
    Assert.assertNotNull(result);
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
