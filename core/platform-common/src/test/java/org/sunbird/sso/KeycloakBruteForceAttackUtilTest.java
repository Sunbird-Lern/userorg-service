package org.sunbird.sso;

import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.when;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.util.ProjectUtil;

@PrepareForTest({ProjectUtil.class, HttpClientUtil.class, KeycloakUtil.class})
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*"
})
public class KeycloakBruteForceAttackUtilTest {

  @BeforeClass
  public static void setup() throws Exception {
    PowerMockito.mockStatic(HttpClientUtil.class);
    PowerMockito.mockStatic(ProjectUtil.class);
    PowerMockito.mockStatic(KeycloakUtil.class);
    when(ProjectUtil.getConfigValue(Mockito.anyString())).thenReturn("anyString");
    when(HttpClientUtil.postFormData(Mockito.anyString(), Mockito.anyMap(), Mockito.anyMap()))
        .thenReturn("{\"access_token\":\"accesstoken\"}");
    PowerMockito.when(KeycloakUtil.getAdminAccessToken(Mockito.any(RequestContext.class)))
        .thenReturn("accesstoken");
  }

  @Test
  public void testGetUserStatus() throws Exception {
    when(HttpClientUtil.get(Mockito.anyString(), Mockito.anyMap()))
        .thenReturn("{\"disabled\": true}");
    boolean isDisabled =
        KeycloakBruteForceAttackUtil.getUserStatus(
            "54646546-7899721321-4654", new RequestContext());
    assertTrue(isDisabled);
  }

  // @Test
  public void testUnlockTempDisabledUser() throws Exception {
    when(HttpClientUtil.delete(Mockito.anyString(), Mockito.anyMap())).thenReturn("");
    boolean isDisabled =
        KeycloakBruteForceAttackUtil.unlockTempDisabledUser(
            "54646546-7899721321-4654", new RequestContext());
    assertTrue(isDisabled);
  }
}
