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

public class KeycloakUtil {
  private static LoggerUtil logger = new LoggerUtil(KeycloakUtil.class);

  private KeycloakUtil() {}

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

  public static String getAdminAccessTokenWithDomain(RequestContext context) throws Exception {
    String url =
        ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_URL)
            + "realms/"
            + ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_RELAM)
            + "/protocol/openid-connect/token";
    String token = getAdminAccessToken(context, url);
    return token;
  }

  public static String getAdminAccessTokenWithoutDomain(RequestContext context) throws Exception {
    String url =
        ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_LB_IP)
            + "/auth/realms/"
            + ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SSO_RELAM)
            + "/protocol/openid-connect/token";
    return getAdminAccessToken(context, url);
  }
}
