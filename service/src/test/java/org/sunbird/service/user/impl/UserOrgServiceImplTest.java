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
import org.sunbird.dao.user.UserOrgDao;
import org.sunbird.dao.user.impl.UserOrgDaoImpl;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.user.UserOrgService;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  CassandraOperation.class,
  ServiceFactory.class,
  UserOrgDaoImpl.class,
  UserOrgDao.class
})
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
    Response response = new Response();
    response.getResult().put(JsonKey.RESPONSE, new ArrayList<>());
    when(cassandraOperationImpl.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(response);
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
    PowerMockito.mockStatic(UserOrgDaoImpl.class);
    UserOrgDao userOrgDao = mock(UserOrgDaoImpl.class);
    when(UserOrgDaoImpl.getInstance()).thenReturn(userOrgDao);
    UserOrgService userOrgService = UserOrgServiceImpl.getInstance();
    Map userMap = new HashMap<String, Object>();
    userMap.put(JsonKey.ID, "id");
    userMap.put(JsonKey.ORGANISATION_ID, "orgId");
    userMap.put(JsonKey.ASSOCIATION_TYPE, "associateType");
    userMap.put(JsonKey.HASHTAGID, "hashId");
    when(userOrgDao.insertRecord(Mockito.anyMap(), Mockito.any())).thenReturn(new Response());
    userOrgService.registerUserToOrg(userMap, new RequestContext());
  }
}
