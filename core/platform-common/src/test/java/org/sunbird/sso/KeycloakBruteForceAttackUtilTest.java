package org.sunbird.sso;

import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.when;

import org.junit.Before;
import org.junit.Ignore;
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
@Ignore
public class KeycloakBruteForceAttackUtilTest {
  @Before
  public void setup() throws Exception {
    PowerMockito.mockStatic(ProjectUtil.class);
    PowerMockito.mockStatic(HttpClientUtil.class);
    PowerMockito.mockStatic(KeycloakUtil.class);
    when(ProjectUtil.getConfigValue(Mockito.anyString())).thenReturn("anyString");
    when(KeycloakUtil.getAdminAccessTokenWithoutDomain(Mockito.any(RequestContext.class)))
      .thenReturn("accessToken");
    when(HttpClientUtil.get(Mockito.anyString(), Mockito.anyMap()))
      .thenReturn("{\"disabled\":true}");
    when(HttpClientUtil.delete(Mockito.anyString(), Mockito.anyMap())).thenReturn("");
  }

  @Test
  public void testIsUserAccountDisabled() throws Exception {
    boolean bool =
      KeycloakBruteForceAttackUtil.isUserAccountDisabled(
        "4564654-789797-121", new RequestContext());
    assertTrue(bool);
  }

  @Test
  public void testUnlockTempDisabledUser() throws Exception {
    boolean bool =
      KeycloakBruteForceAttackUtil.unlockTempDisabledUser(
        "4564654-789797-121", new RequestContext());
    assertTrue(bool);
  }
}