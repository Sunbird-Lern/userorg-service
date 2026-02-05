package org.sunbird.url;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.ProjectUtil;
import org.sunbird.common.PropertiesCache;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;

/**
 * Implementation of URLShortner interface.
 * Uses an external service to shorten URLs if enabled in configuration.
 */
public class URLShortnerImpl implements URLShortner {
  private static final LoggerUtil logger = new LoggerUtil(URLShortnerImpl.class);

  private static String resUrl = null;
  private static final String SUNBIRD_WEB_URL = "sunbird_web_url";

  /**
   * Shortens the provided long URL using an external service.
   *
   * @param url The long URL to shorten.
   * @param context The request context for logging.
   * @return The shortened URL, or the original URL if shortening fails or is disabled.
   */
  @Override
  public String shortUrl(String url, RequestContext context) {
    boolean isEnabled = false;
    try {
      isEnabled = Boolean.parseBoolean(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_URL_SHORTNER_ENABLE));
    } catch (Exception ex) {
      logger.error(context, "URLShortnerImpl:shortUrl: Exception occurred while parsing " + JsonKey.SUNBIRD_URL_SHORTNER_ENABLE, ex);
    }

    if (isEnabled) {
      String baseUrl = PropertiesCache.getInstance().getProperty("sunbird_url_shortner_base_url");
      String accessToken = System.getenv("url_shortner_access_token");
      if (StringUtils.isBlank(accessToken)) {
        accessToken = PropertiesCache.getInstance().getProperty("sunbird_url_shortner_access_token");
      }

      String requestURL = baseUrl + accessToken + "&longUrl=" + url;
      logger.debug(context, "URLShortnerImpl:shortUrl: Making request to URL: " + requestURL);
      String response = HttpClientUtil.get(requestURL, null, context);

      if (StringUtils.isNotBlank(response)) {
        try {
          ObjectMapper mapper = new ObjectMapper();
          Map<String, Object> map = mapper.readValue(response, HashMap.class);
          Map<String, String> dataMap = (Map<String, String>) map.get("data");
          if (dataMap != null && dataMap.containsKey("url")) {
            return dataMap.get("url");
          }
        } catch (IOException | ClassCastException e) {
          logger.error(context, "URLShortnerImpl:shortUrl: Exception occurred while parsing response: " + e.getMessage(), e);
        }
      } else {
        logger.warn(context, "URLShortnerImpl:shortUrl: Received empty response from URL shortener service", null);
      }
    }
    return url;
  }

  /**
   * Retrieves the shortened version of the configured SUNBIRD_WEB_URL.
   *
   * @param context The request context.
   * @return The shortened URL.
   */
  public String getUrl(RequestContext context) {
    if (StringUtils.isBlank(resUrl)) {
      String webUrl = System.getenv(SUNBIRD_WEB_URL);
      if (StringUtils.isBlank(webUrl)) {
        webUrl = PropertiesCache.getInstance().getProperty(SUNBIRD_WEB_URL);
      }
      return shortUrl(webUrl, context);
    } else {
      return resUrl;
    }
  }
}
