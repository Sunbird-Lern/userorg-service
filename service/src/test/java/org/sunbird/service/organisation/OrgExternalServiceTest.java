package org.sunbird.service.organisation;

import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
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
import org.sunbird.common.CassandraUtil;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.response.Response;
import org.sunbird.service.organisation.impl.OrgExternalServiceImpl;
import org.sunbird.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        CassandraOperationImpl.class,
        ServiceFactory.class,
        CassandraOperation.class,
        CassandraUtil.class,
        Util.class
})
@PowerMockIgnore({
        "javax.management.*",
        "javax.net.ssl.*",
        "javax.security.*",
        "jdk.internal.reflect.*",
        "javax.crypto.*",
        "javax.script.*",
        "javax.xml.*",
        "com.sun.org.apache.xerces.*",
        "org.xml.*"
})
public class OrgExternalServiceTest {

  private CassandraOperation cassandraOperation;
  private final String ORG_EXTERNAL_IDENTITY = "org_external_identity";
  private OrgExternalServiceImpl orgExternalService;

  @Before
  public void setUp() {
    orgExternalService = new OrgExternalServiceImpl();
    cassandraOperation = PowerMockito.mock(CassandraOperation.class);
    PowerMockito.mockStatic(ServiceFactory.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    PowerMockito.mockStatic(Util.class);
  }

  @Test
  public void testGetOrgIdFromOrgExtIdFailure() {
    try {

      Map<String, Object> dbRequestMap = new HashMap<>();
      dbRequestMap.put(JsonKey.EXTERNAL_ID, "anyorgextid");
      dbRequestMap.put(JsonKey.PROVIDER, "anyprovider");
      Response response = new Response();
      List<Map<String, Object>> orgList = new ArrayList<>();
      Map<String, Object> map = new HashMap<>();
      orgList.add(map);
      response.put(JsonKey.RESPONSE, orgList);
      when(cassandraOperation.getRecordsByCompositeKey(
              Util.KEY_SPACE_NAME, ORG_EXTERNAL_IDENTITY, dbRequestMap, null))
              .thenReturn(response);
      String resp =
              orgExternalService.getOrgIdFromOrgExternalIdAndProvider(
                      "anyOrgExtid", "anyprovider", null);
      Assert.assertEquals(null, resp);

    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }

  @Test
  public void testGetOrgIdFromOrgExtIdSuccess() {
    try {
      Map<String, Object> dbRequestMap = new HashMap<>();
      dbRequestMap.put(JsonKey.EXTERNAL_ID, "orgextid");
      dbRequestMap.put(JsonKey.PROVIDER, "provider");
      Response response = new Response();
      List<Map<String, Object>> orgList = new ArrayList<>();
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.ORG_ID, "anyOrgId");
      orgList.add(map);
      response.put(JsonKey.RESPONSE, orgList);
      when(cassandraOperation.getRecordsByCompositeKey(
              Util.KEY_SPACE_NAME, ORG_EXTERNAL_IDENTITY, dbRequestMap, null))
              .thenReturn(response);
      String resp =
              orgExternalService.getOrgIdFromOrgExternalIdAndProvider("OrgExtid", "provider", null);
      Assert.assertEquals("anyOrgId", resp);

    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }

  @Test
  public void testGetOrgByOrgExtIdSuccess() {
    try {
      Map<String, Object> dbRequestMap = new HashMap<>();
      dbRequestMap.put(JsonKey.EXTERNAL_ID, "orgextid");
      dbRequestMap.put(JsonKey.PROVIDER, "provider");
      Response response = new Response();
      List<Map<String, Object>> orgList = new ArrayList<>();
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.ORG_ID, "anyOrgId");
      orgList.add(map);
      response.put(JsonKey.RESPONSE, orgList);
      when(cassandraOperation.getRecordsByCompositeKey(
              Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
              .thenReturn(response);
      when(cassandraOperation.getRecordById(
              Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
              .thenReturn(response);
      Map<String, Object> resp =
              orgExternalService.getOrgByOrgExternalIdAndProvider("OrgExtid", "provider", null);
      Assert.assertNotNull(resp);

    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }

  @Test
  public void testGetEmptyOrgByOrgExtIdSuccess() {
    try {
      Map<String, Object> dbRequestMap = new HashMap<>();
      dbRequestMap.put(JsonKey.EXTERNAL_ID, "orgextid");
      dbRequestMap.put(JsonKey.PROVIDER, "provider");
      Response response = new Response();
      List<Map<String, Object>> orgList = new ArrayList<>();
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.ORG_ID, "anyOrgId");
      orgList.add(map);
      response.put(JsonKey.RESPONSE, orgList);
      when(cassandraOperation.getRecordsByCompositeKey(
              Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
              .thenReturn(response);
      Response response1 = new Response();
      response1.put(JsonKey.RESPONSE, null);
      when(cassandraOperation.getRecordById(
              Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
              .thenReturn(response1);
      Map<String, Object> resp =
              orgExternalService.getOrgByOrgExternalIdAndProvider("OrgExtid", "provider", null);
      Assert.assertNotNull(resp);

    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }
}
