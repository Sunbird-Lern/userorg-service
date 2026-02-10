package org.sunbird.service.user;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.RequestContext;
import org.sunbird.keycloak.KeycloakUtil;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  KeycloakUtil.class,
  HttpClientUtil.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class ResetPasswordServiceTest {

  @Test
  public void getUserRequiredActionLinkTestForVerifyEmailLink() throws Exception {
    PowerMockito.mockStatic(HttpClientUtil.class);
    when(HttpClientUtil.post(Mockito.anyString(),Mockito.anyString(),Mockito.anyMap(),Mockito.any(RequestContext.class))).thenReturn("{\"link\":\"success\"}");

    PowerMockito.mockStatic(KeycloakUtil.class);
    when(KeycloakUtil.getAdminAccessToken(Mockito.any(RequestContext.class), Mockito.anyString()))
      .thenReturn("accessToken");

    Map<String,Object> map = new HashMap<>();
    map.put(JsonKey.REDIRECT_URI, "/resources");
    map.put(JsonKey.PASSWORD,"password");

    ResetPasswordService service = new ResetPasswordService();
    String link = service.getUserRequiredActionLink(map, false, new RequestContext());
    Assert.assertNotNull(link);
  }

  @Test
  public void getUserRequiredActionLinkTestForResetPasswordLink() throws Exception {
    PowerMockito.mockStatic(HttpClientUtil.class);
    when(HttpClientUtil.post(Mockito.anyString(),Mockito.anyString(),Mockito.anyMap(),Mockito.any(RequestContext.class))).thenReturn("{\"link\":\"success\"}");

    PowerMockito.mockStatic(KeycloakUtil.class);
    when(KeycloakUtil.getAdminAccessToken(Mockito.any(RequestContext.class), Mockito.anyString()))
      .thenReturn("accessToken");

    Map<String,Object> map = new HashMap<>();
    map.put(JsonKey.REDIRECT_URI, "/resources");

    ResetPasswordService service = new ResetPasswordService();
    String link = service.getUserRequiredActionLink(map, false, new RequestContext());
    Assert.assertNotNull(link);
  }

}
