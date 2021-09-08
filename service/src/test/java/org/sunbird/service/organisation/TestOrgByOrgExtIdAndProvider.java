package org.sunbird.service.organisation;

import static org.powermock.api.mockito.PowerMockito.mock;
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
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.organisation.impl.OrgExternalServiceImpl;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CassandraOperationImpl.class, ServiceFactory.class})
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
public class TestOrgByOrgExtIdAndProvider {

  private CassandraOperation cassandraOperation;

  @Before
  public void setUp() {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    PowerMockito.when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
  }

  @Test
  public void getOrgByOrgExternalIdAndProviderTest() {
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
    Response response1 = new Response();
    List<Map<String, Object>> orgList1 = new ArrayList<>();
    Map<String, Object> org1 = new HashMap<>();
    org1.put(JsonKey.EXTERNAL_ID, "extId");
    org1.put(JsonKey.PROVIDER, "provider");
    org1.put(JsonKey.ORG_ID, "orgId");
    org1.put(JsonKey.ORG_NAME, "orgName");
    orgList1.add(org1);
    response1.getResult().put(JsonKey.RESPONSE, orgList1);
    when(cassandraOperation.getRecordById(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.any(RequestContext.class)))
        .thenReturn(response1);
    OrgExternalService orgExternalService = new OrgExternalServiceImpl();
    Map<String, Object> orgRes =
        orgExternalService.getOrgByOrgExternalIdAndProvider(
            "extId", "provider", new RequestContext());
    Assert.assertNotNull(orgRes);
  }
}
