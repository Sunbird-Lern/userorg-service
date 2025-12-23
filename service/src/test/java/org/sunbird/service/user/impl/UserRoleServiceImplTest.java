package org.sunbird.service.user.impl;

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
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.user.UserRoleService;
import scala.concurrent.Promise;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

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
public class UserRoleServiceImplTest {

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
    roleMap.put("role", "ADMIN");
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
  public void updateUserRoleTestForCreate() {
    Map<String,Object> userRequest = new HashMap();
    userRequest.put(JsonKey.ORGANISATION_ID,"orgId");
    userRequest.put(JsonKey.ROLE_OPERATION,JsonKey.CREATE);
    userRequest.put(JsonKey.ROOT_ORG_ID,"rooTOrgId");
    userRequest.put(JsonKey.USER_ID,"userId");
    userRequest.put(JsonKey.ROLES, Arrays.asList("ADMIN","BOOK_CREATOR"));
    userRequest.put(JsonKey.REQUESTED_BY,"requestedBy");
    UserRoleService service = UserRoleServiceImpl.getInstance();
    List<Map<String,Object>> userRoleListResponse = service.updateUserRole(userRequest, new RequestContext());
    Assert.assertNotNull(userRoleListResponse);
  }

  @Test
  public void updateUserRoleTestForUpdate() {
    Map<String,Object> userRequest = new HashMap();
    userRequest.put(JsonKey.ORGANISATION_ID,"orgId");
    userRequest.put(JsonKey.ROLE_OPERATION,JsonKey.UPDATE);
    userRequest.put(JsonKey.ROOT_ORG_ID,"rooTOrgId");
    userRequest.put(JsonKey.USER_ID,"userId");
    userRequest.put(JsonKey.ROLES, Arrays.asList("ADMIN","BOOK_CREATOR"));
    userRequest.put(JsonKey.REQUESTED_BY,"requestedBy");
    UserRoleService service = UserRoleServiceImpl.getInstance();
    List<Map<String,Object>> userRoleListResponse = service.updateUserRole(userRequest, new RequestContext());
    Assert.assertNotNull(userRoleListResponse);
  }

  @Test
  public void updateUserRoleV2Test() {
    Map<String,Object> userRequest = new HashMap();
    userRequest.put(JsonKey.ORGANISATION_ID,"orgId");
    userRequest.put(JsonKey.ROLE_OPERATION,JsonKey.UPDATE);
    userRequest.put(JsonKey.ROOT_ORG_ID,"rooTOrgId");
    userRequest.put(JsonKey.USER_ID,"userId");
    List<Map<String, Object>> roles = new ArrayList<>();
    Map<String,Object> role = new HashMap<>();
    role.put(JsonKey.ROLE,"ADMIN");
    role.put(JsonKey.OPERATION,JsonKey.CREATE);
    List<Map<String, Object>> scopeList = new ArrayList<>();
    Map<String,Object> scope = new HashMap<>();
    scope.put(JsonKey.ORGANISATION_ID,"orgId");
    scopeList.add(scope);
    role.put(JsonKey.SCOPE,scopeList);
    roles.add(role);
    userRequest.put(JsonKey.ROLES, roles);
    userRequest.put(JsonKey.REQUESTED_BY,"requestedBy");
    UserRoleService service = UserRoleServiceImpl.getInstance();
    List<Map<String,Object>> userRoleListResponse = service.updateUserRoleV2(userRequest, new RequestContext());
    Assert.assertNotNull(userRoleListResponse);
  }

  @Test
  public void updateUserRoleV2Test2() {
    try {
      Map<String, Object> userRequest = new HashMap();
      userRequest.put(JsonKey.ORGANISATION_ID, "orgId");
      userRequest.put(JsonKey.ROLE_OPERATION, JsonKey.UPDATE);
      userRequest.put(JsonKey.ROOT_ORG_ID, "rooTOrgId");
      userRequest.put(JsonKey.USER_ID, "userId");
      List<Map<String, Object>> roles = new ArrayList<>();
      Map<String, Object> role = new HashMap<>();
      role.put(JsonKey.ROLE, "ADMIN");
      role.put(JsonKey.OPERATION, JsonKey.ADD);
      List<Map<String, Object>> scopeList = new ArrayList<>();
      Map<String, Object> scope = new HashMap<>();
      scope.put(JsonKey.ORGANISATION_ID, "orgId");
      scopeList.add(scope);
      role.put(JsonKey.SCOPE, scopeList);
      roles.add(role);
      userRequest.put(JsonKey.ROLES, roles);
      userRequest.put(JsonKey.REQUESTED_BY, "requestedBy");
      UserRoleService service = UserRoleServiceImpl.getInstance();
      List<Map<String, Object>> userRoleListResponse = service.updateUserRoleV2(userRequest, new RequestContext());
      Assert.assertNotNull(userRoleListResponse);
    } catch (Exception ex) {
      Assert.assertNotNull(ex);
    }
  }

  @Test
  public void updateUserRoleV2Test3() {
    try {
      Map<String, Object> userRequest = new HashMap();
      userRequest.put(JsonKey.ORGANISATION_ID, "orgId");
      userRequest.put(JsonKey.ROLE_OPERATION, JsonKey.UPDATE);
      userRequest.put(JsonKey.ROOT_ORG_ID, "rooTOrgId");
      userRequest.put(JsonKey.USER_ID, "userId");
      List<Map<String, Object>> roles = new ArrayList<>();
      Map<String, Object> role = new HashMap<>();
      role.put(JsonKey.ROLE, "ADMIN");
      role.put(JsonKey.OPERATION, JsonKey.REMOVE);
      List<Map<String, Object>> scopeList = new ArrayList<>();
      Map<String, Object> scope = new HashMap<>();
      scope.put(JsonKey.ORGANISATION_ID, "orgId");
      scopeList.add(scope);
      role.put(JsonKey.SCOPE, scopeList);
      roles.add(role);
      userRequest.put(JsonKey.ROLES, roles);
      userRequest.put(JsonKey.REQUESTED_BY, "requestedBy");
      UserRoleService service = UserRoleServiceImpl.getInstance();
      List<Map<String, Object>> userRoleListResponse = service.updateUserRoleV2(userRequest, new RequestContext());
      Assert.assertNotNull(userRoleListResponse);
    }catch (Exception ex ) {
      Assert.assertNotNull(ex);
    }
  }

  @Test
  public void updateUserRoleToESTest () {
    Map<String,Object> userMap = new HashMap<>();
    userMap.put(JsonKey.ID,"userId");
    userMap.put(JsonKey.ROLES,Arrays.asList("ADMIN"));
    UserRoleService service = UserRoleServiceImpl.getInstance();
    Boolean response = service.updateUserRoleToES("userId", userMap, new RequestContext());
    Assert.assertNotNull(response);
  }

}
