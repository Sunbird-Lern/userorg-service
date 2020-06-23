package org.sunbird.systemsettings.dao.impl;

import java.util.*;
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
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.models.systemsetting.SystemSetting;
import org.sunbird.systemsettings.dao.SystemSettingDao;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CassandraOperationImpl.class})
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
public class SystemSettingDaoImplTest {
  private CassandraOperation cassandraOperation;
  private SystemSettingDao systemSettingDaoImpl;
  private static String ROOT_ORG_ID = "defaultRootOrgId";
  private static String FIELD = "someField";
  private static String VALUE = "someValue";

  @Before
  public void setUp() {
    cassandraOperation = PowerMockito.mock(CassandraOperationImpl.class);
    systemSettingDaoImpl = new SystemSettingDaoImpl(cassandraOperation);
  }

  @Test
  public void testSetSystemSettingSuccess() {
    PowerMockito.when(
            cassandraOperation.upsertRecord(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(new Response());
    boolean thrown = false;
    try {
      SystemSetting systemSetting = new SystemSetting(ROOT_ORG_ID, FIELD, VALUE);
      Response upsertResp = systemSettingDaoImpl.write(systemSetting);
      Assert.assertNotEquals(null, upsertResp);
    } catch (Exception e) {
      thrown = true;
    }
    Assert.assertEquals(false, thrown);
  }

  @Test
  public void testReadSystemSettingSuccess() {
    PowerMockito.when(
            cassandraOperation.getRecordsByIndexedProperty(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(getSystemSettingSuccessResponse(false));
    SystemSetting systemSetting = systemSettingDaoImpl.readByField(ROOT_ORG_ID);
    Assert.assertTrue(null != systemSetting);
  }

  @Test
  public void testReadSystemSettingEmpty() {
    PowerMockito.when(
            cassandraOperation.getRecordsByIndexedProperty(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(getSystemSettingSuccessResponse(true));
    SystemSetting systemSetting = systemSettingDaoImpl.readByField(FIELD);
    Assert.assertTrue(null == systemSetting);
  }

  @Test
  public void testReadAllSystemSettingsSuccess() {
    PowerMockito.when(cassandraOperation.getAllRecords(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(getSystemSettingSuccessResponse(false));
    List<SystemSetting> result = systemSettingDaoImpl.readAll();
    Assert.assertTrue(null != result);
  }

  @Test
  public void testReadAllSystemSettingsEmpty() {
    PowerMockito.when(cassandraOperation.getAllRecords(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(getSystemSettingSuccessResponse(true));
    List<SystemSetting> result = systemSettingDaoImpl.readAll();
    Assert.assertTrue(null != result);
  }

  private Response getSystemSettingSuccessResponse(boolean isEmpty) {
    Response response = new Response();
    if (!isEmpty)
      response.put(
          JsonKey.RESPONSE,
          new ArrayList<Map<String, Object>>(Arrays.asList(new HashMap<String, Object>())));
    else {
      response.put(JsonKey.RESPONSE, new ArrayList<Map<String, Object>>());
    }
    return response;
  }
}
