package org.sunbird.keycloak;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import org.apache.http.HttpHeaders;
import org.sunbird.common.ProjectUtil;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;

/**
 * Utility class to handle Keycloak brute force attack detection and user unlocking.
 */
public class KeycloakBruteForceAttackUtil {
  private static final LoggerUtil logger = new LoggerUtil(KeycloakBruteForceAttackUtil.class);

  private KeycloakBruteForceAttackUtil() {}

  private static final String fedUserPrefix =
      "f:"
          + ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYCLOAK_USER_FEDERATION_PROVIDER_ID)
          + ":";

  /**
   * Checks if a user account is disabled due to brute force attack detection.
   *
   * @param userId The ID of the user to check (can be internal or federated ID).
   * @param context The request context for logging.
   * @return true if the user account is disabled, false otherwise.
   * @throws Exception If an error occurs during the API call.
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
    
    logger.debug(context, "KeycloakBruteForceAttackUtil:isUserAccountDisabled: Checking status for URL: " + url);
    String response = HttpClientUtil.get(url, getHeaders(context), context);
    logger.debug(context, "KeycloakBruteForceAttackUtil:isUserAccountDisabled: Response: " + response);

    Map<String, Object> attackStatus = new ObjectMapper().readValue(response, Map.class);
    boolean isDisabled = ((boolean) attackStatus.get("disabled"));
    
    if (isDisabled) {
      logger.info(
          context,
          "KeycloakBruteForceAttackUtil:isUserAccountDisabled: User account is disabled for userId: "
              + userId
              + ", Status: "
              + attackStatus);
    } else {
      logger.info(
          context,
          "KeycloakBruteForceAttackUtil:isUserAccountDisabled: User account is NOT disabled for userId: "
              + userId);
    }
    return isDisabled;
  }

  /**
   * Unlocks a temporarily disabled user account by clearing the brute force detection status.
   *
   * @param userId The ID of the user to unlock.
   * @param context The request context for logging.
   * @return true if the operation succeeds (delete call returns successfully).
   * @throws Exception If an error occurs during the API call.
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
    
    logger.info(context, "KeycloakBruteForceAttackUtil:unlockTempDisabledUser: Unlocking user with URL: " + url);
    HttpClientUtil.delete(url, getHeaders(context), context);
    logger.info(context, "KeycloakBruteForceAttackUtil:unlockTempDisabledUser: Successfully cleared brute force status for userId: " + userId);
    return true;
  }

  /**
   * Constructs the headers required for Keycloak Admin API calls.
   * Includes Authorization header with Admin Access Token.
   *
   * @param context The request context.
   * @return Map containing HTTP headers.
   * @throws Exception If admin token retrieval fails.
   */
  private static Map<String, String> getHeaders(RequestContext context) throws Exception {
    Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    headers.put(HttpHeaders.AUTHORIZATION, JsonKey.BEARER + KeycloakUtil.getAdminAccessTokenWithoutDomain(context));
    return headers;
  }
}
