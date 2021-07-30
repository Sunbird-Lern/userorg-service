package org.sunbird.service.organisation;

import java.util.HashMap;
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
import org.sunbird.keys.JsonKey;
import org.sunbird.learner.organisation.dao.OrgDao;
import org.sunbird.learner.organisation.dao.impl.OrgDaoImpl;
import org.sunbird.learner.organisation.service.OrgService;
import org.sunbird.learner.util.Util;
import org.sunbird.request.RequestContext;

@RunWith(PowerMockRunner.class)
@PrepareForTest({OrgDao.class, OrgDaoImpl.class, Util.class})
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

  @Before
  public void setUp() {
    PowerMockito.mockStatic(OrgDaoImpl.class);
    OrgDao orgDao = PowerMockito.mock(OrgDao.class);
    PowerMockito.when(OrgDaoImpl.getInstance()).thenReturn(orgDao);
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ORG_ID, "anyOrgId");
    PowerMockito.when(
            orgDao.getOrgByExternalId(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(map);
    PowerMockito.when(orgDao.getOrgById(Mockito.anyString(), Mockito.any())).thenReturn(map);
    orgService = OrgServiceImpl.getInstance();
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
}
