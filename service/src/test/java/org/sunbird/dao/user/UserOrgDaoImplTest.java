package org.sunbird.dao.user;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

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
import org.sunbird.dao.user.impl.UserOrgDaoImpl;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.model.user.UserOrg;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  CassandraOperationImpl.class,
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class UserOrgDaoImplTest {

  private static CassandraOperation cassandraOperationImpl = null;

  @Before
  public void setUp() {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperationImpl = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    Response response = new Response();
    when(cassandraOperationImpl.updateRecord(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.anyMap(),
            Mockito.any()))
        .thenReturn(response);
    when(cassandraOperationImpl.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(response);
  }

  @Test
  public void testUpdateUserOrg() {
    UserOrg userOrg = new UserOrg();
    userOrg.setUserId("123-456-789");
    userOrg.setOrganisationId("1234567890");
    userOrg.setDeleted(true);
    UserOrgDao userOrgDao = UserOrgDaoImpl.getInstance();
    Response res = userOrgDao.updateUserOrg(userOrg, new RequestContext());
    Assert.assertNotNull(res);
  }

  @Test
  public void testGetUserOrg() {
    UserOrgDao userOrgDao = UserOrgDaoImpl.getInstance();
    Response res = userOrgDao.getUserOrgDetails("123-456-789", "1234567890", new RequestContext());
    Assert.assertNotNull(res);
  }
}
