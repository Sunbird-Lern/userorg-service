package org.sunbird.service.user.impl;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.BeforeClass;
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
import org.sunbird.service.user.UserOrgService;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CassandraOperation.class, ServiceFactory.class})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class UserOrgServiceImplTest {

  @BeforeClass
  public static void setUp() {
    PowerMockito.mockStatic(ServiceFactory.class);
    CassandraOperation cassandraOperationImpl = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);

    List<Map<String, Object>> userOrgMapList = new ArrayList<>();
    Map<String, Object> userOrgMap = new HashMap<String, Object>();
    userOrgMap.put(JsonKey.USER_ID, "userId");
    userOrgMap.put(JsonKey.ORGANISATION_ID, "orgId");
    userOrgMapList.add(userOrgMap);
    Response userOrgResponse = new Response();
    userOrgResponse.put(JsonKey.RESPONSE, userOrgMapList);

    when(cassandraOperationImpl.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(userOrgResponse);
    when(cassandraOperationImpl.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(userOrgResponse);
  }

  @Test
  public void getUserOrgListByUserIdTest() {
    UserOrgService userOrgService = UserOrgServiceImpl.getInstance();
    List<Map<String, Object>> userList =
        userOrgService.getUserOrgListByUserId("userId", new RequestContext());
    Assert.assertNotNull(userList);
  }

  @Test
  public void registerUserToOrgTest() {
    UserOrgService userOrgService = UserOrgServiceImpl.getInstance();
    Map userMap = new HashMap<String, Object>();
    userMap.put(JsonKey.ID, "id");
    userMap.put(JsonKey.ORGANISATION_ID, "orgId");
    userMap.put(JsonKey.ASSOCIATION_TYPE, "associateType");
    userMap.put(JsonKey.HASHTAGID, "hashId");
    userOrgService.registerUserToOrg(userMap, new RequestContext());
    Assert.assertNotNull(userMap);
  }

  @Test
  public void upsertUserToOrgTest() {
    UserOrgService userOrgService = UserOrgServiceImpl.getInstance();
    Map userMap = new HashMap<String, Object>();
    userMap.put(JsonKey.ID, "id");
    userMap.put(JsonKey.ORGANISATION_ID, "orgId");
    userMap.put(JsonKey.ASSOCIATION_TYPE, "associateType");
    userMap.put(JsonKey.HASHTAGID, "hashId");
    userMap.put(JsonKey.ROLES, "some Roles");
    userOrgService.upsertUserOrgData(userMap, new RequestContext());
    Assert.assertNotNull(userMap);
  }
}
