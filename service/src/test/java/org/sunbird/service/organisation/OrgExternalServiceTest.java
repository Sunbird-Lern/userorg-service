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
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.organisation.impl.OrgExternalServiceImpl;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  CassandraOperationImpl.class,
  ServiceFactory.class,
  CassandraOperation.class,
  CassandraUtil.class
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

  @Before
  public void setUp() throws Exception {
    cassandraOperation = PowerMockito.mock(CassandraOperation.class);
    PowerMockito.mockStatic(ServiceFactory.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
  }

  @Test
  public void getOrgIdFromOrgExternalIdAndProviderTest() {
    Response response = new Response();
    List<Map<String, Object>> orgList = new ArrayList<>();
    Map<String, Object> org = new HashMap<>();
    org.put(JsonKey.EXTERNAL_ID, "extId");
    org.put(JsonKey.PROVIDER, "provider");
    org.put(JsonKey.ORG_ID, "orgId");
    orgList.add(org);
    response.getResult().put(JsonKey.RESPONSE, orgList);
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.any(RequestContext.class)))
        .thenReturn(response);
    OrgExternalService orgExternalService = new OrgExternalServiceImpl();
    String orgId =
        orgExternalService.getOrgIdFromOrgExternalIdAndProvider(
            "extId", "provider", new RequestContext());
    Assert.assertEquals("orgId", orgId);
  }

  @Test
  public void addOrgExtIdTest() {
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(new Response());
    OrgExternalService orgExternalService = new OrgExternalServiceImpl();
    Map<String, Object> orgExtMap = new HashMap<>();
    orgExtMap.put(JsonKey.EXTERNAL_ID, "extId");
    orgExtMap.put(JsonKey.PROVIDER, "provider");
    orgExtMap.put(JsonKey.ORG_ID, "orgId");
    Response response = orgExternalService.addOrgExtId(orgExtMap, new RequestContext());
    Assert.assertNotNull(response);
  }

  @Test
  public void deleteOrgExtIdTest() {
    OrgExternalService orgExternalService = new OrgExternalServiceImpl();
    Map<String, String> orgExtMap = new HashMap<>();
    orgExtMap.put(JsonKey.EXTERNAL_ID, "extId");
    orgExtMap.put(JsonKey.PROVIDER, "provider");
    orgExtMap.put(JsonKey.ORG_ID, "orgId");
    orgExternalService.deleteOrgExtId(orgExtMap, new RequestContext());
    Assert.assertNotNull(orgExternalService);
  }
}
