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
 * Utility class to interact with Keycloak APIs, specifically for token retrieval.
 */
public class KeycloakUtil {
  private static final LoggerUtil logger = new LoggerUtil(KeycloakUtil.class);

  private KeycloakUtil() {}

  /**
   * Retrieves the Keycloak Admin Access Token using client credentials.
   *
   * @param context The request context for logging.
   * @param url The Keycloak token endpoint URL.
   * @return The access token string.
   * @throws Exception If an error occurs during the API call or response parsing.
   */
  public static String getAdminAccessToken(RequestContext context, String url) throws Exception {
    Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
    Map<String, String> fields = new HashMap<>();
    fields.put("client_id", ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_CLIENT_ID));
    fields.put("client_secret", ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_CLIENT_SECRET));
    fields.put("grant_type", "client_credentials");

    logger.debug(context, "KeycloakUtil:getAdminAccessToken: Fetching admin token from URL: " + url);
    String response = HttpClientUtil.postFormData(url, fields, headers, context);
    logger.debug(context, "KeycloakUtil:getAdminAccessToken: Response = " + response);
    
    Map<String, Object> responseMap = new ObjectMapper().readValue(response, Map.class);
    return (String) responseMap.get("access_token");
  }

  /**
   * Retrieves the Admin Access Token using the configured SSO URL (with domain).
   *
   * @param context The request context.
   * @return The access token string.
   * @throws Exception If an error occurs.
   */
  public static String getAdminAccessTokenWithDomain(RequestContext context) throws Exception {
    String url =
        ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_URL)
            + "realms/"
            + ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_RELAM)
            + "/protocol/openid-connect/token";
    return getAdminAccessToken(context, url);
  }

  /**
   * Retrieves the Admin Access Token using the configured Load Balancer IP (without domain),
   * typically used for internal calls or when avoiding DNS resolution issues.
   *
   * @param context The request context.
   * @return The access token string.
   * @throws Exception If an error occurs.
   */
  public static String getAdminAccessTokenWithoutDomain(RequestContext context) throws Exception {
    String url =
        ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_LB_IP)
            + "/auth/realms/"
            + ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_RELAM)
            + "/protocol/openid-connect/token";
    return getAdminAccessToken(context, url);
  }
}
