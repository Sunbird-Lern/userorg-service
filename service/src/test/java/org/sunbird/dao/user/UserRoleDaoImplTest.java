package org.sunbird.dao.user;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

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
import org.sunbird.dao.user.impl.UserRoleDaoImpl;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

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
    Response getRolesRes = new Response();
    Map<String, Object> roleMap = new HashMap<>();
    roleMap.put("role", "somerole");
    roleMap.put("userId", "someuserId");
    roleMap.put("scope", "[{\"orgnaisationId\":\"someOrgId\"}]");
    List<Map> roleList = new ArrayList<>();
    roleList.add(roleMap);
    getRolesRes.put(JsonKey.RESPONSE, roleList);
    PowerMockito.when(
            cassandraOperationImpl.getRecordById(
                Mockito.any(), Mockito.any(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getRolesRes);
  }

  @Test
  public void testCreateUserRole() {
    UserRoleDao userRoleDao = UserRoleDaoImpl.getInstance();
    Response response = userRoleDao.assignUserRole(createUserRoleRequest(), new RequestContext());
    Assert.assertNotNull(null != response && response.getResponseCode() == ResponseCode.OK);
  }

  @Test
  public void testGetUserRole() {
    UserRoleDao userRoleDao = UserRoleDaoImpl.getInstance();
    List<Map<String, Object>> res =
        userRoleDao.getUserRoles("someUserId", "someRole", new RequestContext());
    Assert.assertNotNull(res);
  }

  List<Map<String, Object>> createUserRoleRequest() {
    List<Map<String, Object>> res = new ArrayList<>();
    Map<String, Object> userRoleReq = new HashMap<>();
    userRoleReq.put(JsonKey.USER_ID, "ramdomUserId");
    userRoleReq.put(JsonKey.ORGANISATION_ID, "randomOrgID");
    userRoleReq.put(JsonKey.ROLES, "Admin");
    res.add(userRoleReq);
    return res;
  }
}
