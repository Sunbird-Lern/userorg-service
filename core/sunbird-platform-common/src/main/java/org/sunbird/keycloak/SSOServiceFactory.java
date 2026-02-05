package org.sunbird.keycloak;

import org.sunbird.keycloak.impl.KeyCloakServiceImpl;

/**
 * Factory class for obtaining SSO service instances.
 * Implements the Singleton pattern to provide a single instance of SSOManager.
 */
public class SSOServiceFactory {
  private static SSOManager ssoManager = null;

  private SSOServiceFactory() {}

  /**
   * Returns the singleton instance of SSOManager.
   * If the instance does not exist, it creates a new KeyCloakServiceImpl.
   *
   * @return The singleton SSOManager instance.
   */
  public static SSOManager getInstance() {
    if (null == ssoManager) {
      ssoManager = new KeyCloakServiceImpl();
    }
    return ssoManager;
  }
}
