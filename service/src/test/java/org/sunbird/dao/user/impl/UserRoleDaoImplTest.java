package org.sunbird.dao.user.impl;

import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.*;

import org.apache.pekko.dispatch.Futures;
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
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.dao.user.UserRoleDao;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  CassandraOperation.class,
  ServiceFactory.class,
  ElasticSearchRestHighImpl.class,
  ElasticSearchService.class,
  EsClientFactory.class
})
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
    PowerMockito.mockStatic(EsClientFactory.class);
    ElasticSearchService esService = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
    Promise<Boolean> promise2 = Futures.promise();
    promise2.success(true);
    when(esService.update(
      Mockito.anyString(),
      Mockito.anyString(),
      Mockito.anyMap(),
      Mockito.any(RequestContext.class)))
      .thenReturn(promise2.future());
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperationImpl = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    Response response = new Response();
    when(cassandraOperationImpl.batchInsert(
      Mockito.anyString(), Mockito.anyString(), Mockito.anyList(), Mockito.any()))
      .thenReturn(response);
    when(cassandraOperationImpl.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(response);
    when(cassandraOperationImpl.updateRecord(
      Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.anyMap(), Mockito.any()))
      .thenReturn(response);
    doNothing().when(cassandraOperationImpl).deleteRecord(
      Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any());
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
    Assert.assertNotNull(response);
  }

  @Test
  public void testGetUserRole() {
    UserRoleDao userRoleDao = UserRoleDaoImpl.getInstance();
    List<Map<String, Object>> res =
        userRoleDao.getUserRoles("someUserId", "someRole", new RequestContext());
    Assert.assertNotNull(res);
  }

  @Test
  public void testUpdateRoleScope() {
    List<Map<String, Object>> userRoleList = new ArrayList<>();
    Map<String, Object> userRole = new HashMap<>();
    userRole.put(JsonKey.USER_ID,"userId");
    userRole.put(JsonKey.ROLE,"role");
    userRole.put(JsonKey.SCOPE,"scope");
    userRole.put(JsonKey.ORGANISATION_ID, "randomOrgID");
    userRoleList.add(userRole);
    UserRoleDao userRoleDao = UserRoleDaoImpl.getInstance();
    Response response = userRoleDao.updateRoleScope(userRoleList, new RequestContext());
    Assert.assertEquals(ResponseCode.OK,response.getResponseCode());
  }

  @Test
  public void testDeleteUserRole() {
    List<Map<String, String>> userRoleList = new ArrayList<>();
    Map<String, String> userRole = new HashMap<>();
    userRole.put(JsonKey.USER_ID,"userId");
    userRole.put(JsonKey.ORGANISATION_ID, "randomOrgID");
    userRoleList.add(userRole);
    UserRoleDao userRoleDao = UserRoleDaoImpl.getInstance();
    userRoleDao.deleteUserRole(userRoleList, new RequestContext());
    Assert.assertNotNull(userRoleList);
  }

  @Test
  public void testUpdateUserRoleToES() {
    Map<String, Object> userRole = new HashMap<>();
    userRole.put(JsonKey.USER_ID,"userId");
    userRole.put(JsonKey.ORGANISATION_ID, "randomOrgID");
    UserRoleDao userRoleDao = UserRoleDaoImpl.getInstance();
    boolean bool = userRoleDao.updateUserRoleToES("userId", userRole, new RequestContext());
    Assert.assertTrue(bool);
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
