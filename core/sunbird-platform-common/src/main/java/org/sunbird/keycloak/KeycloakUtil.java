package org.sunbird.keycloak;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import org.apache.http.HttpHeaders;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.common.ProjectUtil;

/**
 * Utility class for Keycloak operations, specifically for retrieving admin access tokens.
 */
public class KeycloakUtil {
  private static final LoggerUtil logger = new LoggerUtil(KeycloakUtil.class);

  private KeycloakUtil() {}

  /**
   * Retrieves an admin access token from a specified URL.
   *
   * @param context The request context for logging.
   * @param url     The URL to request the token from.
   * @return The admin access token.
   * @throws Exception If an error occurs during the HTTP request or response parsing.
   */
  public static String getAdminAccessToken(RequestContext context, String url) throws Exception {
    Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
    Map<String, String> fields = new HashMap<>();
    fields.put("client_id", ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_CLIENT_ID));
    fields.put("client_secret", ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_CLIENT_SECRET));
    fields.put("grant_type", "client_credentials");

    String response = HttpClientUtil.postFormData(url, fields, headers, context);
    logger.debug(context, "KeycloakUtil:getAdminAccessToken: Response = " + response);
    Map<String, Object> responseMap = new ObjectMapper().readValue(response, Map.class);
    return (String) responseMap.get("access_token");
  }

  /**
   * Retrieves an admin access token using the configured SSO URL (with domain).
   *
   * @param context The request context.
   * @return The admin access token.
   * @throws Exception If an error occurs during token retrieval.
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
   * Retrieves an admin access token using the load balancer IP (without domain).
   *
   * @param context The request context.
   * @return The admin access token.
   * @throws Exception If an error occurs during token retrieval.
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
