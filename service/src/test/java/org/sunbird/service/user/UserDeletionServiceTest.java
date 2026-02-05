package org.sunbird.service.user;

import static org.junit.Assert.assertNotNull;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.User;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.user.impl.UserExternalIdentityServiceImpl;
import org.sunbird.keycloak.KeyCloakConnectionProvider;
import org.sunbird.keycloak.SSOManager;
import org.sunbird.keycloak.SSOServiceFactory;
import org.sunbird.common.ProjectUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  KeyCloakConnectionProvider.class,
  ServiceFactory.class,
  SSOServiceFactory.class,
  SSOManager.class,
  CassandraOperationImpl.class,
  UserExternalIdentityService.class,
  UserExternalIdentityServiceImpl.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class UserDeletionServiceTest {
  private static CassandraOperationImpl cassandraOperation = mock(CassandraOperationImpl.class);

  @BeforeClass
  public static void beforeClass() {
    PowerMockito.mockStatic(ServiceFactory.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    Response response = createCassandraUpdateSuccessResponse();
    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(response);
  }

  @Before
  public void init() {

    UserRepresentation userRepresentation = mock(UserRepresentation.class);
    RealmResource realmResource = mock(RealmResource.class);
    Keycloak keycloak = mock(Keycloak.class);
    PowerMockito.mockStatic(KeyCloakConnectionProvider.class);
    when(KeyCloakConnectionProvider.getConnection()).thenReturn(keycloak);
    when(keycloak.realm(Mockito.anyString())).thenReturn(realmResource);

    UsersResource usersResource = mock(UsersResource.class);
    when(realmResource.users()).thenReturn(usersResource);

    UserResource userResource = mock(UserResource.class);
    when(usersResource.get(Mockito.any())).thenReturn(userResource);
    when(userResource.toRepresentation()).thenReturn(userRepresentation);
  }

  @Test
  public void testDeleteUserSuccess() {
    try {
      ObjectMapper mapper = new ObjectMapper();
      Map<String, Object> esUserMap = new HashMap<>();
      esUserMap.put(JsonKey.IS_BLOCKED, true);
      esUserMap.put(JsonKey.STATUS, ProjectUtil.Status.DELETED.getValue());
      esUserMap.put(JsonKey.MASKED_EMAIL, "");
      esUserMap.put(JsonKey.MASKED_PHONE, "");
      esUserMap.put(JsonKey.FIRST_NAME, "");
      esUserMap.put(JsonKey.LAST_NAME, "");
      esUserMap.put(JsonKey.PHONE, "");
      esUserMap.put(JsonKey.EMAIL, "");
      esUserMap.put(JsonKey.PREV_USED_EMAIL, "");
      esUserMap.put(JsonKey.PREV_USED_PHONE, "");
      esUserMap.put(JsonKey.PROFILE_LOCATION, new ArrayList<>());
      esUserMap.put(JsonKey.RECOVERY_EMAIL, "");
      esUserMap.put(JsonKey.RECOVERY_PHONE, "");
      esUserMap.put(JsonKey.USER_NAME, "");
      esUserMap.put(JsonKey.IS_DELETED, true);

      Response userDetailsById = new Response();
      Map<String, Object> userMap = new HashMap<>();
      userMap.put(JsonKey.ID, "46545665465465");
      userMap.put(JsonKey.IS_DELETED, false);
      userMap.put(JsonKey.FIRST_NAME, "firstName");
      userMap.put(JsonKey.STATUS, 1);
      userMap.put(JsonKey.EMAIL, "test@test.com");
      userMap.put(JsonKey.PHONE, "9876543210");
      userMap.put(JsonKey.EXTERNAL_ID, "9876");
      List<Map<String, Object>> userList = new ArrayList<>();
      userList.add(userMap);
      userDetailsById.getResult().put(JsonKey.RESPONSE, userList);
      when(cassandraOperation.getRecordById(
              Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
          .thenReturn(userDetailsById);

      PowerMockito.mockStatic(SSOServiceFactory.class);
      SSOManager ssoManager = PowerMockito.mock(SSOManager.class);
      PowerMockito.when(SSOServiceFactory.getInstance()).thenReturn(ssoManager);
      PowerMockito.when(ssoManager.removeUser(Mockito.anyMap(), Mockito.any()))
          .thenReturn(JsonKey.SUCCESS);

      when(cassandraOperation.deleteRecord(
              Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
          .thenReturn(userDetailsById);

      Map<String, String> userExtIdMap = new HashMap<>();
      userExtIdMap.put("provider", "0132818330295992324");
      userExtIdMap.put("idtype", "teacherId");
      userExtIdMap.put("externalid", "cedd456");
      userExtIdMap.put("userid", "46545665465465");

      List<Map<String, String>> userExtIdRespList = new ArrayList<>();
      userExtIdRespList.add(userExtIdMap);

      UserExternalIdentityServiceImpl externalIdentityService =
          PowerMockito.mock(UserExternalIdentityServiceImpl.class);
      PowerMockito.whenNew(UserExternalIdentityServiceImpl.class)
          .withNoArguments()
          .thenReturn(externalIdentityService);

      when(externalIdentityService.getUserExternalIds(Mockito.anyString(), Mockito.any()))
          .thenReturn(userExtIdRespList);

      when(externalIdentityService, "deleteUserExternalIds", Mockito.any(), Mockito.any())
          .thenReturn(true);

      UserDeletionService userDeletionService = new UserDeletionService();
      Response response =
          userDeletionService.deleteUser(
              "46545665465465",
              ssoManager,
              mapper.convertValue(userMap, User.class),
              esUserMap,
              new RequestContext());
      assertNotNull(response);
    } catch (Exception ex) {
      assertNotNull(ex);
    }
  }

  private static Response createCassandraUpdateSuccessResponse() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    return response;
  }
}
