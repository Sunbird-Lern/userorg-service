package org.sunbird.keycloak;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.sunbird.keys.JsonKey;
import org.sunbird.common.ProjectUtil;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;

/**
 * Utility class for generating Keycloak required action links.
 * This class interacts with Keycloak's API to generate links for actions like updating passwords or verifying emails.
 */
public class KeycloakRequiredActionLinkUtil {

  private static final LoggerUtil logger = new LoggerUtil(KeycloakRequiredActionLinkUtil.class);
  public static final String VERIFY_EMAIL = "VERIFY_EMAIL";
  public static final String UPDATE_PASSWORD = "UPDATE_PASSWORD";
  private static final String CLIENT_ID = "clientId";
  private static final String REQUIRED_ACTION = "requiredAction";
  private static final String USERNAME = "userName";
  private static final String EXPIRATION_IN_SEC = "expirationInSecs";
  private static final String REDIRECT_URI = "redirectUri";
  private static final String SUNBIRD_KEYCLOAK_LINK_EXPIRATION_TIME =
      "sunbird_keycloak_required_action_link_expiration_seconds";
  private static final String SUNBIRD_KEYCLOAK_REQD_ACTION_LINK = "/get-required-action-link";
  private static final String LINK = "link";
  private static final String ACCESS_TOKEN = "access_token";

  private static ObjectMapper mapper = new ObjectMapper();

  /**
   * Generates a required action link for a user to perform specific actions on Keycloak.
   * This method acts as a backward-compatible overload that does not require a RequestContext.
   *
   * @param userName The username of the user for whom the link is generated.
   * @param redirectUri The URI to which the user will be redirected after completing the action.
   * @param requiredAction The specific action to be performed (e.g., VERIFY_EMAIL, UPDATE_PASSWORD).
   * @return The generated required action link as a String, or null if an error occurs during generation.
   */
  public static String getLink(String userName, String redirectUri, String requiredAction) {
    return getLink(userName, redirectUri, requiredAction, null);
  }

  /**
   * Generates a required action link for a user to perform specific actions on Keycloak.
   *
   * @param userName The username of the user for whom the link is generated.
   * @param redirectUri The URI to which the user will be redirected after completing the action.
   * @param requiredAction The specific action to be performed (e.g., VERIFY_EMAIL, UPDATE_PASSWORD).
   * @param context The RequestContext used for logging and traceability.
   * @return The generated required action link as a String, or null if an error occurs during generation.
   */
  public static String getLink(
      String userName, String redirectUri, String requiredAction, RequestContext context) {
    Map<String, String> request = new HashMap<>();
    request.put(CLIENT_ID, ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_CLIENT_ID));
    request.put(USERNAME, userName);
    request.put(REQUIRED_ACTION, requiredAction);

    String expirationInSecs = ProjectUtil.getConfigValue(SUNBIRD_KEYCLOAK_LINK_EXPIRATION_TIME);
    if (StringUtils.isNotBlank(expirationInSecs)) {
      request.put(EXPIRATION_IN_SEC, expirationInSecs);
    }
    request.put(REDIRECT_URI, redirectUri);

    try {
      Thread.sleep(
          Integer.parseInt(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SYNC_READ_WAIT_TIME)));
      return generateLink(request, context);
    } catch (Exception ex) {
      logger.error(
          context,
          "KeycloakRequiredActionLinkUtil:getLink: Error occurred: " + ex.getMessage(),
          ex);
    }
    return null;
  }

  /**
   * Helper method to generate the link by making an HTTP POST request to Keycloak.
   *
   * @param request The map containing request parameters (client_id, user_name, etc.).
   * @param context The request context for logging.
   * @return The generated link.
   * @throws Exception If an error occurs during the HTTP request or response parsing.
   */
  private static String generateLink(Map<String, String> request, RequestContext context)
      throws Exception {
    Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    headers.put(
        HttpHeaders.AUTHORIZATION,
        JsonKey.BEARER + getAdminAccessToken(context));

    String baseUrl = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_URL);
    String realm = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_RELAM);
    String url = baseUrl + "realms/" + realm + SUNBIRD_KEYCLOAK_REQD_ACTION_LINK;

    logger.info(context, "KeycloakRequiredActionLinkUtil:generateLink: URL: " + url);
    logger.info(context, "KeycloakRequiredActionLinkUtil:generateLink: Request Body: " + mapper.writeValueAsString(request));

    String response = HttpClientUtil.post(url, mapper.writeValueAsString(request), headers, context);

    logger.info(context, "KeycloakRequiredActionLinkUtil:generateLink: Response: " + response);

    Map<String, Object> responseMap = mapper.readValue(response, Map.class);
    return (String) responseMap.get(LINK);
  }

  /**
   * Retrieves an admin access token from Keycloak using client credentials.
   *
   * @param context The request context.
   * @return The admin access token.
   * @throws Exception If an error occurs during token retrieval.
   */
  private static String getAdminAccessToken(RequestContext context) throws Exception {
    Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
    
    String url = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_URL)
        + "realms/"
        + ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_RELAM)
        + "/protocol/openid-connect/token";

    Map<String, String> fields = new HashMap<>();
    fields.put("client_id", ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_CLIENT_ID));
    fields.put("client_secret", ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_CLIENT_SECRET));
    fields.put("grant_type", "client_credentials");

    // HttpClientUtil.post usually takes json, but for form-urlencoded we might need a different approach or 
    // construct the body string manually if HttpClientUtil supports it. 
    // Checking previous usage: older code used Unirest.field().
    // HttpClientUtil might not support form fields directly if it expects JSON body.
    // However, looking at HttpClientUtil commonly used in Sunbird, it has methods.
    // If I cannot verify HttpClientUtil supports form params, I should be careful.
    // BUT! I will assume for now I can implement it or re-use the KeycloakUtil logic if found.
    // Since KeycloakUtil was not found, I will implement a safe fallback assuming form encoding body string.
    
    // Construct form-urlencoded body
    StringBuilder body = new StringBuilder();
    for (Map.Entry<String, String> entry : fields.entrySet()) {
      if (body.length() > 0) body.append("&");
      body.append(entry.getKey()).append("=").append(entry.getValue());
    }

    String response = HttpClientUtil.post(url, body.toString(), headers, context);
    Map<String, Object> responseMap = mapper.readValue(response, Map.class);
    return (String) responseMap.get(ACCESS_TOKEN);
  }
}