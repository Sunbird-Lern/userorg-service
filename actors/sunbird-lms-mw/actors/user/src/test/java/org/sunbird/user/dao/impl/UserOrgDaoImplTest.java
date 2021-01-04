package org.sunbird.user.dao.impl;

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
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.request.RequestContext;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.models.user.org.UserOrg;
import org.sunbird.user.dao.UserOrgDao;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  CassandraOperationImpl.class,
})
@PowerMockIgnore({"javax.management.*"})
public class UserOrgDaoImplTest {

  private static CassandraOperationImpl cassandraOperationImpl;

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperationImpl = mock(CassandraOperationImpl.class);
  }

  @Test
  public void testupdateUserOrg() {
    Response response = new Response();
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    when(cassandraOperationImpl.updateRecord(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.anyMap(),
            Mockito.any()))
        .thenReturn(response);
    UserOrg userOrg = new UserOrg();
    userOrg.setUserId("123-456-789");
    userOrg.setOrganisationId("1234567890");
    userOrg.setDeleted(true);
    UserOrgDao userOrgDao = UserOrgDaoImpl.getInstance();
    Response res = userOrgDao.updateUserOrg(userOrg, null);
    Assert.assertNotNull(res);
  }

  @Test
  public void testGetUserOrg() {
    Response response = new Response();
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    when(cassandraOperationImpl.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(response);
    UserOrgDao userOrgDao = UserOrgDaoImpl.getInstance();
    Response res = userOrgDao.getUserOrgDetails("123-456-789", "1234567890", new RequestContext());
    Assert.assertNotNull(res);
  }
}
