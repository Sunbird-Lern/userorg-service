package org.sunbird.service.organisation;

import static org.powermock.api.mockito.PowerMockito.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.organisation.impl.OrgServiceImpl;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ElasticSearchRestHighImpl.class,
  ElasticSearchHelper.class,
  EsClientFactory.class,
  CassandraOperationImpl.class,
  ServiceFactory.class
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
public class TestOrgByOrgExternalId {

  @BeforeClass
  public static void setUp() {
    PowerMockito.mockStatic(ServiceFactory.class);
    CassandraOperation cassandraOperation = mock(CassandraOperationImpl.class);
    PowerMockito.when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    PowerMockito.when(
            cassandraOperation.getRecordsByCompositeKey(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getRecordsByProperty(false));
    PowerMockito.when(
            cassandraOperation.getRecordById(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(getRecordsByProperty(false))
        .thenReturn(getRecordsByProperty(false));

    PowerMockito.when(
            cassandraOperation.getRecordsByPrimaryKeys(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyList(),
                Mockito.anyString(),
                Mockito.any(RequestContext.class)))
        .thenReturn(getRecordsByProperty(false));
    PowerMockito.when(
            cassandraOperation.getPropertiesValueById(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyList(),
                Mockito.anyList(),
                Mockito.any(RequestContext.class)))
        .thenReturn(getRecordsByProperty(false));
  }

  @Test
  public void testGetOrgByExternalIdAndProvider() {
    OrgService orgService = OrgServiceImpl.getInstance();
    Map<String, Object> map =
        orgService.getOrgByExternalIdAndProvider("extId", "provider", new RequestContext());
    Assert.assertNotNull(map);
  }

  @Test
  public void getChannelTest() {
    OrgService orgService = OrgServiceImpl.getInstance();
    String channel = orgService.getChannel("rootOrgId", new RequestContext());
    Assert.assertNotNull(channel);
  }

  private static Response getRecordsByProperty(boolean empty) {
    Response res = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    if (!empty) {
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.ID, "orgId");
      map.put(JsonKey.IS_DELETED, true);
      map.put(JsonKey.CHANNEL, "channel1");
      map.put(JsonKey.IS_TENANT, true);
      map.put(JsonKey.ORG_ID, "orgId");
      list.add(map);
    }
    res.put(JsonKey.RESPONSE, list);
    return res;
  }
}
