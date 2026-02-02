package org.sunbird.keycloak;

import org.sunbird.keycloak.impl.KeyCloakServiceImpl;

/**
 * Factory class to provide instances of SSOManager.
 */
public class SSOServiceFactory {
  private static SSOManager ssoManager = null;

  private SSOServiceFactory() {}

  /**
   * Returns a singleton instance of the SSOManager.
   * <p>
   * If the instance is not already created, it initializes a new {@link KeyCloakServiceImpl}.
   * </p>
   *
   * @return The singleton {@link SSOManager} instance.
   */
  public static SSOManager getInstance() {
    if (null == ssoManager) {
      ssoManager = new KeyCloakServiceImpl();
    }
    return ssoManager;
  }
}
