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

@PrepareForTest({ProjectUtil.class, HttpClientUtil.class})
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*"
})
public class KeycloakUtilTest {

  @BeforeClass
  public static void setup() throws Exception {
    PowerMockito.mockStatic(HttpClientUtil.class);
    PowerMockito.mockStatic(ProjectUtil.class);
    when(HttpClientUtil.postFormData(Mockito.anyString(), Mockito.anyMap(), Mockito.anyMap()))
        .thenReturn("{\"access_token\":\"accesstoken\"}");
    when(ProjectUtil.getConfigValue(Mockito.anyString())).thenReturn("anyString");
  }

  @Test
  public void testGetAdminAccessToken() throws Exception {
    String token = KeycloakUtil.getAdminAccessToken(new RequestContext());
    assertTrue(token.equals("accesstoken"));
  }
}
