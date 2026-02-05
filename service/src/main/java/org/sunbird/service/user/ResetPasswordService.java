package org.sunbird.service.user;

import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.keycloak.KeycloakRequiredActionLinkUtil;
import org.sunbird.url.URLShortner;
import org.sunbird.url.URLShortnerImpl;
import org.sunbird.common.ProjectUtil;

public class ResetPasswordService {

  private final LoggerUtil logger = new LoggerUtil(ResetPasswordService.class);

  public String getUserRequiredActionLink(
      Map<String, Object> templateMap, boolean isUrlShortRequired, RequestContext context) {
    URLShortner urlShortner = new URLShortnerImpl();
    String redirectUri =
        StringUtils.isNotBlank((String) templateMap.get(JsonKey.REDIRECT_URI))
            ? ((String) templateMap.get(JsonKey.REDIRECT_URI))
            : null;
    logger.debug(context, "Util:getUserRequiredActionLink redirectURI = " + redirectUri);
    if (StringUtils.isBlank((String) templateMap.get(JsonKey.PASSWORD))) {
      String url =
          KeycloakRequiredActionLinkUtil.getLink(
              (String) templateMap.get(JsonKey.USERNAME),
              redirectUri,
              KeycloakRequiredActionLinkUtil.UPDATE_PASSWORD,
              context);

      templateMap.put(
          JsonKey.SET_PASSWORD_LINK, isUrlShortRequired ? urlShortner.shortUrl(url, context) : url);
      return isUrlShortRequired ? urlShortner.shortUrl(url, context) : url;

    } else {
      String url =
          KeycloakRequiredActionLinkUtil.getLink(
              (String) templateMap.get(JsonKey.USERNAME),
              redirectUri,
              KeycloakRequiredActionLinkUtil.VERIFY_EMAIL,
              context);
      templateMap.put(
          JsonKey.VERIFY_EMAIL_LINK, isUrlShortRequired ? urlShortner.shortUrl(url, context) : url);
      return isUrlShortRequired ? urlShortner.shortUrl(url, context) : url;
    }
  }

  /**
   * As per requirement this page need to be redirect to /resources always.
   *
   * @return url of login page
   */
  public String getSunbirdLoginUrl() {
    StringBuilder webUrl = new StringBuilder();
    webUrl.append(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_WEB_URL));
    // Default value for PASSWORD_RESET_LOGIN_PAGE_URL is /resources as configured in
    // externalresource.properties
    webUrl.append(ProjectUtil.getConfigValue(JsonKey.PASSWORD_RESET_LOGIN_PAGE_URL));
    return webUrl.toString();
  }
}
