package org.sunbird.sso;

import java.util.Map;
import org.keycloak.admin.client.Keycloak;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.util.ProjectUtil;

public class KeycloakBruteForceAttackUtil {
  private static LoggerUtil logger = new LoggerUtil(KeycloakBruteForceAttackUtil.class);

  private KeycloakBruteForceAttackUtil() {}

  private static Keycloak keycloak = KeyCloakConnectionProvider.getConnection();
  private static String fedUserPrefix =
      "f:" + ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYCLOAK_USER_FEDERATION_PROVIDER_ID) + ":";
  /**
   * Get status of a user in brute force detection
   *
   * @param userId
   * @return
   */
  public static boolean isUserAccountDisabled(String userId, RequestContext context) {
    Map<String, Object> attackStatus =
        keycloak
            .realm(KeyCloakConnectionProvider.SSO_REALM)
            .attackDetection()
            .bruteForceUserStatus(fedUserPrefix + userId);
    logger.info(context, "check attack detection for userId : " + userId + ", " + attackStatus);
    return ((boolean) attackStatus.get("disabled"));
  }

  /**
   * @param userId
   * @param context
   * @return
   */
  public static boolean unlockTempDisabledUser(String userId, RequestContext context) {
    keycloak
        .realm(KeyCloakConnectionProvider.SSO_REALM)
        .attackDetection()
        .clearBruteForceForUser(fedUserPrefix + userId);
    logger.info(context, "clear Brute Force For User for userId : " + userId);
    return true;
  }
}
