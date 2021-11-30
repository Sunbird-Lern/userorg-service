package org.sunbird.service.user.impl;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;

import java.util.HashMap;
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
import org.sunbird.actor.user.validator.UserCreateRequestValidator;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.organisation.Organisation;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.organisation.OrgExternalService;
import org.sunbird.service.organisation.OrgService;
import org.sunbird.service.organisation.impl.OrgExternalServiceImpl;
import org.sunbird.service.organisation.impl.OrgServiceImpl;
import org.sunbird.service.user.SSOUserService;
import org.sunbird.service.user.UserLookupService;
import org.sunbird.service.user.UserService;
import org.sunbird.util.DataCacheHandler;
import org.sunbird.util.user.UserUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  CassandraOperationImpl.class,
  UserServiceImpl.class,
  UserService.class,
  OrgServiceImpl.class,
  OrgService.class,
  OrgExternalService.class,
  DataCacheHandler.class,
  OrgExternalServiceImpl.class,
  UserCreateRequestValidator.class,
  UserLookupService.class,
  UserLookUpServiceImpl.class,
  UserUtil.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class SSOUserServiceImplTest {

  @Before
  public void beforeEachTest() throws Exception {
    PowerMockito.mockStatic(UserCreateRequestValidator.class);
    PowerMockito.mockStatic(OrgServiceImpl.class);
    PowerMockito.mockStatic(OrgServiceImpl.class);
    PowerMockito.mockStatic(OrgExternalServiceImpl.class);
    PowerMockito.mockStatic(UserServiceImpl.class);
    OrgService orgService = mock(OrgServiceImpl.class);
    PowerMockito.when(OrgServiceImpl.getInstance()).thenReturn(orgService);
    OrgExternalServiceImpl orgExternalService = PowerMockito.mock(OrgExternalServiceImpl.class);

    UserService userService = mock(UserService.class);
    PowerMockito.when(UserServiceImpl.getInstance()).thenReturn(userService);

    when(orgService.getRootOrgIdFromChannel(Mockito.anyString(), nullable(RequestContext.class)))
        .thenReturn("OrgID");

    Organisation organisation = new Organisation();
    organisation.setId("rootOrgId");
    organisation.setChannel("anyChannel");
    organisation.setRootOrgId("rootOrgId");
    organisation.setTenant(false);
    PowerMockito.when(orgService.getOrgObjById(Mockito.anyString(), Mockito.any()))
        .thenReturn(organisation);

    PowerMockito.when(OrgExternalServiceImpl.getInstance()).thenReturn(orgExternalService);
    PowerMockito.when(
            orgExternalService.getOrgIdFromOrgExternalIdAndProvider(
                Mockito.anyString(), Mockito.anyString(), nullable(RequestContext.class)))
        .thenReturn("anyOrgId");
    PowerMockito.mockStatic(UserLookUpServiceImpl.class);
    UserLookupService userLookupService = mock(UserLookupService.class);
    PowerMockito.when(UserLookUpServiceImpl.getInstance()).thenReturn(userLookupService);
    when(userService.createUser(Mockito.anyMap(), nullable(RequestContext.class)))
        .thenReturn(new Response());
    PowerMockito.when(
            userLookupService.insertRecords(Mockito.anyMap(), nullable(RequestContext.class)))
        .thenReturn(getSuccessResponse());
  }

  @Test
  public void validateOrgIdAndPrimaryRecoveryKeys() {
    SSOUserService ssoUserService = SSOUserServiceImpl.getInstance();
    Map<String, Object> userMap = new HashMap();
    userMap.put(JsonKey.ORGANISATION_ID, "anyOrgId");
    userMap.put(JsonKey.CHANNEL, "anyChannel");
    userMap.put(JsonKey.ORG_EXTERNAL_ID, "anyOrgExtId");
    Assert.assertNotNull(
        ssoUserService.validateOrgIdAndPrimaryRecoveryKeys(userMap, new Request()));
  }

  @Test
  public void createUserAndPassword() {
    SSOUserService ssoUserService = SSOUserServiceImpl.getInstance();
    Map<String, Object> userMap = new HashMap();
    userMap.put(JsonKey.ID, "someId");
    PowerMockito.mockStatic(UserUtil.class);
    PowerMockito.when(UserUtil.updatePassword(Mockito.anyMap(), nullable(RequestContext.class)))
        .thenReturn(true);
    Assert.assertNotNull(
        ssoUserService.createUserAndPassword(new HashMap(), userMap, new Request()));
  }

  public static Response getSuccessResponse() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    return response;
  }
}
