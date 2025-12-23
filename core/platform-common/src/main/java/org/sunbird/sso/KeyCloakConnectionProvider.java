/** */
package org.sunbird.sso;

import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.util.PropertiesCache;

/**
 * @author Manzarul This class will connect to key cloak server and provide the connection to do
 *     other operations.
 */
public class KeyCloakConnectionProvider {

  private static final LoggerUtil logger = new LoggerUtil(KeyCloakConnectionProvider.class);

  private static Keycloak keycloak;
  private static final PropertiesCache cache = PropertiesCache.getInstance();
  public static String SSO_URL = null;
  public static String SSO_REALM = null;
  public static String CLIENT_ID = null;

  static {
    try {
      initialiseConnection();
    } catch (Exception e) {
      logger.error(
          "Exception occurred while initializing keycloak connection: " + e.getMessage(), e);
    }
    registerShutDownHook();
  }

  /**
   * Method to initializate the Keycloak connection
   *
   * @return Keycloak connection
   */
  public static Keycloak initialiseConnection() throws Exception {
    keycloak = initialiseEnvConnection();
    if (keycloak != null) {
      return keycloak;
    }
    KeycloakBuilder keycloakBuilder =
        KeycloakBuilder.builder()
            .serverUrl(cache.getProperty(JsonKey.SSO_URL))
            .realm(cache.getProperty(JsonKey.SSO_REALM))
            .username(cache.getProperty(JsonKey.SSO_USERNAME))
            .password(cache.getProperty(JsonKey.SSO_PASSWORD))
            .clientId(cache.getProperty(JsonKey.SSO_CLIENT_ID))
            .resteasyClient(
                new ResteasyClientBuilderImpl()
                    .connectionPoolSize(Integer.parseInt(cache.getProperty(JsonKey.SSO_POOL_SIZE)))
                    .build());
    if (cache.getProperty(JsonKey.SSO_CLIENT_SECRET) != null
        && !(cache.getProperty(JsonKey.SSO_CLIENT_SECRET).equals(JsonKey.SSO_CLIENT_SECRET))) {
      keycloakBuilder.clientSecret(cache.getProperty(JsonKey.SSO_CLIENT_SECRET));
    }
    SSO_URL = cache.getProperty(JsonKey.SSO_URL);
    SSO_REALM = cache.getProperty(JsonKey.SSO_REALM);
    CLIENT_ID = cache.getProperty(JsonKey.SSO_CLIENT_ID);
    keycloak = keycloakBuilder.build();

    logger.info("key cloak instance is created successfully.");
    return keycloak;
  }

  /**
   * This method will provide the keycloak connection from environment variable. if environment
   * variable is not set then it will return null.
   *
   * @return Keycloak
   */
  private static Keycloak initialiseEnvConnection() throws Exception {
    String url = System.getenv(JsonKey.SUNBIRD_SSO_URL);
    String username = System.getenv(JsonKey.SUNBIRD_SSO_USERNAME);
    String password = System.getenv(JsonKey.SUNBIRD_SSO_PASSWORD);
    String cleintId = System.getenv(JsonKey.SUNBIRD_SSO_CLIENT_ID);
    String clientSecret = System.getenv(JsonKey.SUNBIRD_SSO_CLIENT_SECRET);
    String relam = System.getenv(JsonKey.SUNBIRD_SSO_RELAM);
    if (StringUtils.isBlank(url)
        || StringUtils.isBlank(username)
        || StringUtils.isBlank(password)
        || StringUtils.isBlank(cleintId)
        || StringUtils.isBlank(relam)) {
      logger.info("key cloak connection is not provided by Environment variable.");
      return null;
    }
    SSO_URL = url;
    SSO_REALM = relam;
    CLIENT_ID = cleintId;
    KeycloakBuilder keycloakBuilder =
        KeycloakBuilder.builder()
            .serverUrl(url)
            .realm(relam)
            .username(username)
            .password(password)
            .clientId(cleintId)
            .resteasyClient(
                new ResteasyClientBuilderImpl()
                    .connectionPoolSize(Integer.parseInt(cache.getProperty(JsonKey.SSO_POOL_SIZE)))
                    .build());

    if (StringUtils.isNotBlank(clientSecret)) {
      keycloakBuilder.clientSecret(clientSecret);
      logger.info("KeyCloakConnectionProvider:initialiseEnvConnection client sceret is provided.");
    }
    keycloakBuilder.grantType("client_credentials");
    keycloak = keycloakBuilder.build();
    logger.info("key cloak instance is created from Environment variable settings .");
    return keycloak;
  }

  /**
   * This method will provide key cloak connection instance.
   *
   * @return Keycloak
   */
  public static Keycloak getConnection() {
    if (keycloak != null) {
      return keycloak;
    } else {
      try {
        return initialiseConnection();
      } catch (Exception e) {
        logger.error("getConnection : " + e.getMessage(), e);
      }
    }
    return null;
  }

  /**
   * This class will be called by registerShutDownHook to register the call inside jvm , when jvm
   * terminate it will call the run method to clean up the resource.
   *
   * @author Manzarul
   */
  static class ResourceCleanUp extends Thread {
    public void run() {
      if (null != keycloak) {
        keycloak.close();
      }
    }
  }

  /** Register the hook for resource clean up. this will be called when jvm shut down. */
  public static void registerShutDownHook() {
    Runtime runtime = Runtime.getRuntime();
    runtime.addShutdownHook(new ResourceCleanUp());
  }
}
