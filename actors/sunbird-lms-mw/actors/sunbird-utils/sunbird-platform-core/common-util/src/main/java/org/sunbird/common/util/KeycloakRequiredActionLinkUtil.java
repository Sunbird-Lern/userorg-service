package org.sunbird.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.BaseRequest;
import com.mashape.unirest.request.body.RequestBodyEntity;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;

/**
 * Keycloak utility to create required action links.
 *
 * @author Amit Kumar
 */
public class KeycloakRequiredActionLinkUtil {

  public static final String VERIFY_EMAIL = "VERIFY_EMAIL";
  public static final String UPDATE_PASSWORD = "UPDATE_PASSWORD";
  private static final String CLIENT_ID = "clientId";
  private static final String REQUIRED_ACTION = "requiredAction";
  private static final String USERNAME = "userName";
  private static final String EXPIRATION_IN_SEC = "expirationInSecs";
  private static final String REDIRECT_URI = "redirectUri";
  private static final String ACCESS_TOKEN = "access_token";
  private static final String SUNBIRD_KEYCLOAK_LINK_EXPIRATION_TIME =
      "sunbird_keycloak_required_action_link_expiration_seconds";
  private static final String SUNBIRD_KEYCLOAK_REQD_ACTION_LINK = "/get-required-action-link";
  private static final String LINK = "link";

  private static ObjectMapper mapper = new ObjectMapper();

  /**
   * Get generated link for specified type and user from Keycloak service.
   *
   * @param userName User name
   * @param requiredAction Type of link to be generated. Supported types are UPDATE_PASSWORD and
   *     VERIFY_EMAIL.
   * @return Generated link from Keycloak service
   */
  public static String getLink(String userName, String redirectUri, String requiredAction) {
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
      return generateLink(request);
    } catch (Exception ex) {
      ProjectLogger.log(
          "KeycloakRequiredActionLinkUtil:getLink: Exception occurred with error message = "
              + ex.getMessage(),
          ex);
    }
    return null;
  }

  private static String generateLink(Map<String, String> request) throws Exception {
    Map<String, String> headers = new HashMap<>();

    headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    headers.put(JsonKey.AUTHORIZATION, JsonKey.BEARER + getAdminAccessToken());

    ProjectLogger.log(
        "KeycloakRequiredActionLinkUtil:generateLink: complete URL "
            + ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_URL)
            + "realms/"
            + ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_RELAM)
            + SUNBIRD_KEYCLOAK_REQD_ACTION_LINK,
        LoggerEnum.INFO.name());
    ProjectLogger.log(
        "KeycloakRequiredActionLinkUtil:generateLink: request body "
            + mapper.writeValueAsString(request),
        LoggerEnum.INFO.name());
    RequestBodyEntity baseRequest =
        Unirest.post(
                ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_URL)
                    + "realms/"
                    + ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_RELAM)
                    + SUNBIRD_KEYCLOAK_REQD_ACTION_LINK)
            .headers(headers)
            .body(mapper.writeValueAsString(request));
    HttpResponse<JsonNode> response = baseRequest.asJson();

    ProjectLogger.log(
        "KeycloakRequiredActionLinkUtil:generateLink: Response status = "
            + response.getStatus()
            + " body "
            + response.getBody(),
        LoggerEnum.INFO.name());

    return response.getBody().getObject().getString(LINK);
  }

  public static String getAdminAccessToken() throws Exception {
    Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
    BaseRequest request =
        Unirest.post(
                ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_URL)
                    + "realms/"
                    + ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_RELAM)
                    + "/protocol/openid-connect/token")
            .headers(headers)
            .field("client_id", ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_CLIENT_ID))
            .field("client_secret", ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_CLIENT_SECRET))
            .field("grant_type", "client_credentials");

    HttpResponse<JsonNode> response = request.asJson();
    ProjectLogger.log(
        "KeycloakRequiredActionLinkUtil:getAdminAccessToken: Response status = "
            + response.getStatus(),
        LoggerEnum.INFO.name());

    return response.getBody().getObject().getString(ACCESS_TOKEN);
  }
}
