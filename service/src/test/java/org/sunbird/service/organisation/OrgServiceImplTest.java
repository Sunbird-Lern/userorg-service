package org.sunbird.service.organisation;

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
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.dao.organisation.OrgDao;
import org.sunbird.dao.organisation.impl.OrgDaoImpl;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.organisation.impl.OrgServiceImpl;
import org.sunbird.util.Util;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({OrgDao.class, OrgDaoImpl.class, Util.class,
        ServiceFactory.class})
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
public class OrgServiceImplTest {
  private OrgService orgService = null;
  private static CassandraOperationImpl cassandraOperation;

  @Before
  public void setUp() {
    PowerMockito.mockStatic(OrgDaoImpl.class);
    OrgDao orgDao = PowerMockito.mock(OrgDao.class);
    PowerMockito.when(OrgDaoImpl.getInstance()).thenReturn(orgDao);
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ORG_ID, "anyOrgId");
    /*PowerMockito.when(
            orgDao.getOrgByExternalId(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(map);*/
    PowerMockito.when(orgDao.getOrgById(Mockito.anyString(), Mockito.any())).thenReturn(map);
    orgService = OrgServiceImpl.getInstance();

    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    PowerMockito.when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
            .thenReturn(getRecordsByProperty(true));
  }

  @Test
  public void testGetOrgById() {
    Map<String, Object> map = orgService.getOrgById("id", new RequestContext());
    Assert.assertNotNull(map);
  }

  @Test
  public void testGetOrgByExternalIdAndProvider() {
    Map<String, Object> map =
        orgService.getOrgByExternalIdAndProvider("extId", "provider", new RequestContext());
    Assert.assertNotNull(map);
  }
  private Response getRecordsByProperty(boolean empty) {
    Response res = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    if (!empty) {
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.ID, "orgId");
      map.put(JsonKey.IS_DELETED, true);
      map.put(JsonKey.CHANNEL, "channel1");
      map.put(JsonKey.IS_TENANT, true);
      list.add(map);
    }
    res.put(JsonKey.RESPONSE, list);
    return res;
  }
}
