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
 * Implementation of the URLShortner interface.
 */
public class URLShortnerImpl implements URLShortner {
  private static final LoggerUtil logger = new LoggerUtil(URLShortnerImpl.class);

  private static String resUrl = null;
  private static final String SUNBIRD_WEB_URL = "sunbird_web_url";

  /**
   * Shortens the provided URL using the configured URL shortening service.
   * If the service is disabled or fails, the original URL is returned.
   *
   * @param url     The URL to shorten.
   * @param context The request context.
   * @return The shortened URL string.
   */
  @SuppressWarnings("unchecked")
  @Override
  public String shortUrl(String url, RequestContext context) {
    boolean isShortenerEnabled = false;
    try {
      isShortenerEnabled = Boolean.parseBoolean(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_URL_SHORTNER_ENABLE));
    } catch (Exception ex) {
      logger.error(context, "URLShortnerImpl:shortUrl: Exception occurred while parsing sunbird_url_shortner_enable key: " + ex.getMessage(), ex);
    }

    if (isShortenerEnabled) {
      String baseUrl = PropertiesCache.getInstance().getProperty("sunbird_url_shortner_base_url");
      String accessToken = System.getenv("url_shortner_access_token");
      if (StringUtils.isBlank(accessToken)) {
        accessToken = PropertiesCache.getInstance().getProperty("sunbird_url_shortner_access_token");
      }
      String requestURL = baseUrl + accessToken + "&longUrl=" + url;
      String response = HttpClientUtil.get(requestURL, null, context);
      
      if (StringUtils.isNotBlank(response)) {
        ObjectMapper mapper = new ObjectMapper();
        try {
          Map<String, Object> map = mapper.readValue(response, HashMap.class);
          Map<String, String> dataMap = (Map<String, String>) map.get("data");
          return dataMap.get("url");
        } catch (IOException | ClassCastException e) {
          logger.error(context, "URLShortnerImpl:shortUrl: Exception occurred while parsing response: " + e.getMessage(), e);
        }
      }
    }
    return url;
  }

  /**
   * Retrieves the shortened URL for the configured Sunbird web URL.
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
