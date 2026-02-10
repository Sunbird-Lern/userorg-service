package org.sunbird.util.contentstore;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpHeaders;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.common.ProjectUtil;
import org.sunbird.common.PropertiesCache;

public class ContentStoreUtil {
  private static final LoggerUtil logger = new LoggerUtil(ContentStoreUtil.class);

  private static Map<String, String> getHeaders() {
    Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeaders.AUTHORIZATION, JsonKey.BEARER + PropertiesCache.getInstance().readProperty(JsonKey.SUNBIRD_AUTHORIZATION));
    return headers;
  }

  public static Map<String, Object> readFramework(String frameworkId, RequestContext context) {
    logger.info(context, "ContentStoreUtil:readFramework: frameworkId = " + frameworkId);
    return handleReadRequest(frameworkId, JsonKey.SUNBIRD_FRAMEWORK_READ_API, context);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> handleReadRequest(String id, String urlPath, RequestContext context) {
    Map<String, String> headers = getHeaders();
    ProjectUtil.setTraceIdInHeader(headers, context);
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> resultMap = new HashMap<>();

    logger.info(context, "ContentStoreUtil:handleReadRequest: id = " + id);

    try {
      String requestUrl =
          ProjectUtil.getConfigValue(JsonKey.SUNBIRD_API_BASE_URL)
              + ProjectUtil.getConfigValue(urlPath)
              + "/"
              + id;
      String response = HttpClientUtil.get(requestUrl, headers, null);

      resultMap = mapper.readValue(response, Map.class);
      if (!((String) resultMap.get(JsonKey.RESPONSE_CODE)).equalsIgnoreCase(JsonKey.OK)) {
        logger.info(context, "ContentStoreUtil:handleReadRequest: Response code is not ok.");
        return null;
      }
    } catch (Exception e) {
      logger.error(
          context,
          "ContentStoreUtil:handleReadRequest: Exception occurred with error message = "
              + e.getMessage(),
          e);
    }
    return resultMap;
  }
}
