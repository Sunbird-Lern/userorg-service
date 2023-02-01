package org.sunbird.service.user;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.dao.user.UserDao;
import org.sunbird.dao.user.impl.UserDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.user.impl.UserServiceImpl;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  UserDao.class,
  UserDaoImpl.class,
  ServiceFactory.class,
  CassandraOperationImpl.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class UserServiceImplTest {

  private static CassandraOperation cassandraOperationImpl = null;
  @Before
  public void setUp() throws JsonProcessingException {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperationImpl = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    Response response = new Response();
    Map<String, Object> user = new HashMap<>();
    user.put(JsonKey.ID, "46545665465465");
    user.put(JsonKey.IS_DELETED, false);
    user.put(JsonKey.FLAGS_VALUE, 3);
    user.put(JsonKey.FIRST_NAME, "firstName");
    user.put(JsonKey.PROFILE_USERTYPE, "{\"useType\":45}");
    String profileLocation = "[{\"id\":\"4567891231\",\"type\":\"state\"}]";
    user.put(JsonKey.PROFILE_LOCATION, profileLocation);
    Map<String, String> tncMap = new HashMap<>();
    tncMap.put("tncAcceptedOn", "2021-01-04 19:45:29:725+0530");
    tncMap.put("version", "3.9.0");
    ObjectMapper mapper = new ObjectMapper();
    String tnc = mapper.writeValueAsString(tncMap);
    Map<String, String> groupTncMap = new HashMap<>();
    groupTncMap.put("groupsTnc", tnc);
    user.put(JsonKey.ALL_TNC_ACCEPTED, groupTncMap);
    List<Map<String, Object>> userList = new ArrayList<>();
    userList.add(user);
    response.getResult().put(JsonKey.RESPONSE, userList);
    when(cassandraOperationImpl.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(response);
    PowerMockito.when(
            cassandraOperationImpl.getPropertiesValueById(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyList(),
                Mockito.anyList(),
                Mockito.any(RequestContext.class)))
        .thenReturn(getRecordsByProperty(false));

    Response getRolesRes = new Response();
    Map<String, Object> roleMap = new HashMap<>();
    roleMap.put("role", "somerole");
    roleMap.put("userId", "46545665465465");
    roleMap.put("scope", "[{\"orgnaisationId\":\"46545665\"}]");
    List<Map> roleList = new ArrayList<>();
    roleList.add(roleMap);
    getRolesRes.put(JsonKey.RESPONSE, roleList);

    Response userOrgRes = new Response();
    Map<String, Object> userOrgMap = new HashMap<>();
    userOrgMap.put("userId", "46545665465465");
    userOrgMap.put("organisationId", "46545665");
    userOrgMap.put("isDeleted", false);
    List<Map> userOrgList = new ArrayList<>();
    userOrgList.add(userOrgMap);
    userOrgRes.put(JsonKey.RESPONSE, userOrgList);

    PowerMockito.when(
            cassandraOperationImpl.getRecordById(
                Mockito.any(), Mockito.any(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(userOrgRes)
        .thenReturn(getRolesRes);
  }

  private Response getRecordsByProperty(boolean empty) {
    Response res = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    if (!empty) {
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.ID, "46545665");
      map.put(JsonKey.IS_DELETED, true);
      map.put(JsonKey.CHANNEL, "channel1");
      map.put(JsonKey.IS_TENANT, true);
      list.add(map);
    }
    res.put(JsonKey.RESPONSE, list);
    return res;
  }

  // @Test
  public void getUserDetailsTest() {
    UserService userService = UserServiceImpl.getInstance();
    String phone =
        userService.getDecryptedEmailPhoneByUserId(
            "46545665465465", JsonKey.PHONE, new RequestContext());
    Assert.assertNotNull(phone);
  }

  @Test(expected = ProjectCommonException.class)
  public void getUserDetailsTest2() {
    UserService userService = UserServiceImpl.getInstance();
    userService.getDecryptedEmailPhoneByUserId(
        "2123-456-8997", JsonKey.RECOVERY_PHONE, new RequestContext());
  }

  @Test
  public void getUserDetailsByIdTest() {
    UserService userService = UserServiceImpl.getInstance();
    Map<String, Object> userDetails =
        userService.getUserDetailsById("userId", new RequestContext());
    Assert.assertNotNull(userDetails);
  }

  @Test
  public void getUserDetailsByIdForES() {
    UserService userService = UserServiceImpl.getInstance();
    Map<String, Object> userDetailsForEs =
        userService.getUserDetailsForES("3422-324-2342", new RequestContext());
    Assert.assertNotNull(userDetailsForEs);
  }

  @Test(expected = ProjectCommonException.class)
  public void testValidateUserIdFailure() {
    UserService userService = UserServiceImpl.getInstance();
    Request request = new Request();
    request.getContext().put(JsonKey.USER_ID, "userId");
    request.getContext().put(JsonKey.MANAGED_FOR, "managedFor");
    userService.validateUserId(request, "123456", new RequestContext());
  }
}
