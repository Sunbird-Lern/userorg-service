package org.sunbird.util;

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
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.response.Response;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  EsClientFactory.class,
  ElasticSearchRestHighImpl.class,
  ElasticSearchHelper.class,
  CassandraOperationImpl.class,
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class UtilGetUserRoleTest {

  private static CassandraOperationImpl cassandraOperationImpl;
  private static ElasticSearchService esService;

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperationImpl = mock(CassandraOperationImpl.class);
    PowerMockito.mockStatic(EsClientFactory.class);
    esService = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
  }

  @Test
  public void testGetUserRoles() {
    List<Map<String, Object>> userRoleDetails = new ArrayList<>();
    Map<String, Object> userRoleMap = new HashMap<>();
    userRoleMap.put("role", "CONTENT_CREATOR");
    userRoleMap.put("userid", "4a3ded8a-d731-4f58-a722-e63b00925cd0");
    userRoleMap.put("scope", "[{\"orgId\":\"4578963210\"}]");
    userRoleDetails.add(userRoleMap);
    Map<String, Object> userRoleMap1 = new HashMap<>();
    userRoleMap1.put("role", "COURSE_CREATOR");
    userRoleMap1.put("userid", "4a3ded8a-d731-4f58-a722-e63b00925cd0");
    userRoleMap1.put("scope", "[{\"orgId\":\"4578963210\"}]");
    userRoleDetails.add(userRoleMap1);
    List<String> userIds = new ArrayList<>();
    userIds.add("4a3ded8a-d731-4f58-a722-e63b00925cd0");
    Response response = new Response();
    response.getResult().put(JsonKey.RESPONSE, userRoleDetails);
    PowerMockito.mockStatic(ServiceFactory.class);
    CassandraOperationImpl cassandraOperationImpl = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    when(cassandraOperationImpl.getRecordsByPrimaryKeys(
            JsonKey.SUNBIRD, JsonKey.USER_ROLES, userIds, JsonKey.USER_ID, null))
        .thenReturn(response);
    List<Map<String, Object>> userRoles =
        Util.getUserRoles("4a3ded8a-d731-4f58-a722-e63b00925cd0", null);
    Assert.assertNotNull(userRoles);
  }

  @Test
  public void testGetUserRolesWithScopeNull() {
    List<Map<String, Object>> userRoleDetails = new ArrayList<>();
    Map<String, Object> userRoleMap = new HashMap<>();
    userRoleMap.put("role", "CONTENT_CREATOR");
    userRoleMap.put("userid", "4a3ded8a-d731-4f58-a722-e63b00925cd0");
    userRoleMap.put("scope", "[{\"orgId\":\"4578963210\"}]");
    userRoleDetails.add(userRoleMap);
    Map<String, Object> userRoleMap1 = new HashMap<>();
    userRoleMap1.put("role", "COURSE_CREATOR");
    userRoleMap1.put("userid", "4a3ded8a-d731-4f58-a722-e63b00925cd0");
    userRoleMap1.put("scope", "[{orgId]");
    userRoleDetails.add(userRoleMap1);
    List<String> userIds = new ArrayList<>();
    userIds.add("4a3ded8a-d731-4f58-a722-e63b00925cd0");
    Response response = new Response();
    response.getResult().put(JsonKey.RESPONSE, userRoleDetails);
    PowerMockito.mockStatic(ServiceFactory.class);
    CassandraOperationImpl cassandraOperationImpl = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    when(cassandraOperationImpl.getRecordsByPrimaryKeys(
            JsonKey.SUNBIRD, JsonKey.USER_ROLES, userIds, JsonKey.USER_ID, null))
        .thenReturn(response);
    List<Map<String, Object>> userRoles =
        Util.getUserRoles("4a3ded8a-d731-4f58-a722-e63b00925cd0", null);
    Assert.assertNotNull(userRoles);
  }
}
