package org.sunbird.keycloak;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.common.ProjectUtil;

/**
 * Utility class for handling Keycloak brute force attack detection and user account management.
 * This class interacts with Keycloak's attack detection API to check user account status
 * and unlock temporarily disabled accounts.
 */
public class KeycloakBruteForceAttackUtil {
  private static final LoggerUtil logger = new LoggerUtil(KeycloakBruteForceAttackUtil.class);

  private KeycloakBruteForceAttackUtil() {}

  private static String fedUserPrefix =
      "f:" + ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYCLOAK_USER_FEDERATION_PROVIDER_ID) + ":";

  /**
   * Checks if a user account is disabled due to brute force attack detection.
   *
   * @param userId The ID of the user to check.
   * @param context The request context for logging.
   * @return True if the user account is disabled, false otherwise.
   * @throws Exception If an error occurs while communicating with Keycloak.
   */
  public static boolean isUserAccountDisabled(String userId, RequestContext context)
      throws Exception {
    String url =
        ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_LB_IP)
            + "/auth/admin/realms/"
            + ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_RELAM)
            + "/attack-detection/brute-force/users/"
            + fedUserPrefix
            + userId;
    String response = HttpClientUtil.get(url, getHeaders(context), context);
    logger.info(context, "KeycloakBruteForceAttackUtil:isUserAccountDisabled: Response = " + response);
    Map<String, Object> attackStatus = new ObjectMapper().readValue(response, Map.class);
    boolean isDisabled = ((boolean) attackStatus.get("disabled"));
    if (isDisabled) {
      logger.info(
          context,
          "KeycloakBruteForceAttackUtil:isUserAccountDisabled: User account is disabled for userId: "
              + userId
              + ", Status: "
              + attackStatus);
    }
    return isDisabled;
  }

  /**
   * Unlocks a temporarily disabled user account.
   *
   * @param userId The ID of the user to unlock.
   * @param context The request context for logging.
   * @return True if the operation was triggered successfully (response not validated).
   * @throws Exception If an error occurs while communicating with Keycloak.
   */
  public static boolean unlockTempDisabledUser(String userId, RequestContext context)
      throws Exception {
    String url =
        ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_LB_IP)
            + "/auth/admin/realms/"
            + ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_RELAM)
            + "/attack-detection/brute-force/users/"
            + fedUserPrefix
            + userId;
    HttpClientUtil.delete(url, getHeaders(context), context);
    logger.info(
        context,
        "KeycloakBruteForceAttackUtil:unlockTempDisabledUser: Unlocked brute force disabled user for userId: "
            + userId);
    return true;
  }

  /**
   * Generates HTTP headers including the authorization token.
   *
   * @param context The request context.
   * @return A map of HTTP headers.
   * @throws Exception If token generation fails.
   */
  private static Map<String, String> getHeaders(RequestContext context) throws Exception {
    Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    headers.put(HttpHeaders.AUTHORIZATION, JsonKey.BEARER + KeycloakUtil.getAdminAccessTokenWithoutDomain(context));
    return headers;
  }
}
