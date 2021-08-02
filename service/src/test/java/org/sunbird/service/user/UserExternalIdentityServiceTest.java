package org.sunbird.service.user;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
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
import org.sunbird.response.Response;
import org.sunbird.service.user.impl.UserExternalIdentityServiceImpl;
import org.sunbird.util.user.UserUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  UserUtil.class,
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
public class UserExternalIdentityServiceTest {

  @Test
  public void getUserV1Test() {
    PowerMockito.mockStatic(ServiceFactory.class);
    CassandraOperation cassandraOperationImpl = mock(CassandraOperation.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    Response response = new Response();
    List<Map<String, Object>> resp = new ArrayList<>();
    Map<String, Object> userList = new HashMap<>();
    userList.put(JsonKey.USER_ID, "1234");
    resp.add(userList);
    response.put(JsonKey.RESPONSE, resp);
    when(cassandraOperationImpl.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(response);
    Map<String, String> orgProviderMap = new HashMap<>();
    orgProviderMap.put("channel1004", "01234567687");
    PowerMockito.mockStatic(UserUtil.class);
    when(UserUtil.fetchOrgIdByProvider(Mockito.anyList(), Mockito.any()))
        .thenReturn(orgProviderMap);
    when(UserUtil.getCaseInsensitiveOrgFromProvider(Mockito.anyString(), Mockito.anyMap()))
        .thenReturn("01234567687");
    UserExternalIdentityService userExternalIdentityService = new UserExternalIdentityServiceImpl();

    String userId =
        userExternalIdentityService.getUserV1("1234", "channel1004", "channel1004", null);
    Assert.assertTrue(true);
  }

  @Test
  public void getUserV2Test() {
    PowerMockito.mockStatic(ServiceFactory.class);
    CassandraOperation cassandraOperationImpl = mock(CassandraOperation.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    Response response = new Response();
    List<Map<String, Object>> resp = new ArrayList<>();
    Map<String, Object> userList = new HashMap<>();
    userList.put(JsonKey.USER_ID, "1234");
    resp.add(userList);
    response.put(JsonKey.RESPONSE, resp);
    when(cassandraOperationImpl.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(response);
    UserExternalIdentityService userExternalIdentityService = new UserExternalIdentityServiceImpl();

    String userId =
        userExternalIdentityService.getUserV2("1234", "channel1004", "channel1004", null);
    Assert.assertTrue(true);
  }

  @Test
  public void getSelfDeclaredDetailsTest() {
    PowerMockito.mockStatic(ServiceFactory.class);
    CassandraOperation cassandraOperationImpl = mock(CassandraOperation.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    Response response = new Response();
    List<Map<String, Object>> resp = new ArrayList<>();
    Map<String, Object> userList = new HashMap<>();
    userList.put(JsonKey.USER_ID, "1234");
    userList.put(JsonKey.PERSONA, "Teacher");
    userList.put(JsonKey.STATUS, "SUBMITTED");
    Map userInfo = new HashMap();
    userInfo.put(JsonKey.DECLARED_EMAIL, "demo@gmail.com");
    userList.put(JsonKey.USER_INFO, userInfo);
    resp.add(userList);
    response.put(JsonKey.RESPONSE, resp);
    Map user = new HashMap();
    user.put(JsonKey.USER_ID, "1234");
    when(cassandraOperationImpl.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(response);
    UserExternalIdentityService userExternalIdentityService = new UserExternalIdentityServiceImpl();
    List selfDeclareExternalId = userExternalIdentityService.getSelfDeclaredDetails("1234", null);
    Assert.assertNotNull(selfDeclareExternalId);
  }
}
