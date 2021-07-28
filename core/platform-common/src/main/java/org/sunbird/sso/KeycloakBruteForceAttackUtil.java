package org.sunbird.sso;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import org.apache.http.HttpHeaders;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.util.ProjectUtil;

public class KeycloakBruteForceAttackUtil {
  private static LoggerUtil logger = new LoggerUtil(KeycloakBruteForceAttackUtil.class);

  private KeycloakBruteForceAttackUtil() {}
  /**
   * Get status of a user in brute force detection
   *
   * @param userId
   * @return
   */
  public static boolean isUserAccountDisabled(String userId, RequestContext context)
      throws Exception {
    String fedUserPrefix =
        "f:"
            + ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYCLOAK_USER_FEDERATION_PROVIDER_ID)
            + ":";
    String url =
        ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_URL)
            + "admin/realms/"
            + ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_RELAM)
            + "/attack-detection/brute-force/users/"
            + fedUserPrefix
            + userId;
    String response = HttpClientUtil.get(url, getHeaders(context));
    logger.info(context, "KeycloakBruteForceAttackUtil:getUserStatus: Response = " + response);
    Map<String, Object> responseMap = new ObjectMapper().readValue(response, Map.class);
    return ((boolean) responseMap.get("disabled"));
  }

  public static boolean unlockTempDisabledUser(String userId, RequestContext context)
      throws Exception {
    String fedUserPrefix =
        "f:"
            + ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYCLOAK_USER_FEDERATION_PROVIDER_ID)
            + ":";
    String url =
        ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_URL)
            + "admin/realms/"
            + ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_RELAM)
            + "/attack-detection/brute-force/users/"
            + fedUserPrefix
            + userId;
    String response = HttpClientUtil.delete(url, getHeaders(context));
    logger.info(
        context, "KeycloakBruteForceAttackUtil:unlockTempDisabledUser: Response = " + response);
    return true;
  }

  private static Map<String, String> getHeaders(RequestContext context) throws Exception {
    Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    headers.put(JsonKey.AUTHORIZATION, JsonKey.BEARER + KeycloakUtil.getAdminAccessToken(context));
    return headers;
  }
}
