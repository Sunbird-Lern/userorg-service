package org.sunbird.sso;

import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.AttackDetectionResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.request.RequestContext;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.PropertiesCache;

@PrepareForTest({
  ProjectUtil.class,
  KeyCloakConnectionProvider.class,
  Keycloak.class,
  RealmResource.class,
  AttackDetectionResource.class,
  PropertiesCache.class
})
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
  public void setup() {
    PowerMockito.mockStatic(ProjectUtil.class);
    PowerMockito.mockStatic(PropertiesCache.class);
    PropertiesCache propertiesCache = mock(PropertiesCache.class);
    when(PropertiesCache.getInstance()).thenReturn(propertiesCache);
    PowerMockito.when(propertiesCache.getProperty(Mockito.anyString())).thenReturn("anyString");
    when(ProjectUtil.getConfigValue(Mockito.anyString())).thenReturn("anyString");
    PowerMockito.mockStatic(KeyCloakConnectionProvider.class);
    Keycloak keycloak = PowerMockito.mock(Keycloak.class);
    when(KeyCloakConnectionProvider.getConnection()).thenReturn(keycloak);
    RealmResource realmRes = mock(RealmResource.class);
    when(keycloak.realm(null)).thenReturn(realmRes);
    AttackDetectionResource attackDetectionResource = mock(AttackDetectionResource.class);
    when(realmRes.attackDetection()).thenReturn(attackDetectionResource);
    Map<String, Object> resp = new HashMap<>();
    resp.put("disabled", true);
    when(attackDetectionResource.bruteForceUserStatus(Mockito.anyString())).thenReturn(resp);
  }

  @Test
  public void testIsUserAccountDisabled() {
    boolean bool =
        KeycloakBruteForceAttackUtil.isUserAccountDisabled(
            "4564654-789797-121", new RequestContext());
    assertTrue(bool);
  }

  @Test
  public void testUnlockTempDisabledUser() {
    boolean bool =
        KeycloakBruteForceAttackUtil.unlockTempDisabledUser(
            "4564654-789797-121", new RequestContext());
    assertTrue(bool);
  }
}
