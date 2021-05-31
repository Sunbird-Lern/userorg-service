package org.sunbird.user.dao.impl;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

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
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.RequestContext;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.user.dao.UserRoleDao;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CassandraOperation.class, ServiceFactory.class})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class UserRoleDaoImplTest {

  private static CassandraOperation cassandraOperationImpl = null;

  @Before
  public void setUp() {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperationImpl = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    Response response = new Response();
    when(cassandraOperationImpl.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(response);
  }

  @Test
  public void createUserRole() {
    UserRoleDao userRoleDao = UserRoleDaoImpl.getInstance();
    List<Map<String, Object>> res =
        userRoleDao.createUserRole(createUserRoleRequest(), new RequestContext());
    Assert.assertNotNull(res);
  }

  Map createUserRoleRequest() {
    Map<String, Object> userRoleReq = new HashMap<>();
    userRoleReq.put(JsonKey.USER_ID, "ramdomUserId");
    userRoleReq.put(JsonKey.ORGANISATION_ID, "randomOrgID");
    userRoleReq.put(JsonKey.ROLES, Arrays.asList("Admin", "Editor"));
    return userRoleReq;
  }
}
