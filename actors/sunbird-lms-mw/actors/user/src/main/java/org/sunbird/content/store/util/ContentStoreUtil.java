package org.sunbird.content.store.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpHeaders;
import org.sunbird.common.models.util.HttpClientUtil;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerUtil;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.RequestContext;

public class ContentStoreUtil {
  private static LoggerUtil logger = new LoggerUtil(ContentStoreUtil.class);

  private static Map<String, String> getHeaders() {
    Map<String, String> headers = new HashMap<>();
    headers.put(
        HttpHeaders.AUTHORIZATION,
        JsonKey.BEARER + ProjectUtil.getConfigValue(JsonKey.SUNBIRD_AUTHORIZATION));
    return headers;
  }

  public static Map<String, Object> readFramework(String frameworkId, RequestContext context) {
    logger.info(context, "ContentStoreUtil:readFramework: frameworkId = " + frameworkId);
    return handleReadRequest(frameworkId, JsonKey.SUNBIRD_FRAMEWORK_READ_API, context);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> handleReadRequest(
      String id, String urlPath, RequestContext context) {
    Map<String, String> headers = getHeaders();
    headers.put(JsonKey.X_TRACE_ENABLED, context.getDebugEnabled());
    headers.put(JsonKey.X_REQUEST_ID, context.getReqId());
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> resultMap = new HashMap<>();

    logger.info(context, "ContentStoreUtil:handleReadRequest: id = " + id);

    try {
      String requestUrl =
          ProjectUtil.getConfigValue(JsonKey.SUNBIRD_API_BASE_URL)
              + ProjectUtil.getConfigValue(urlPath)
              + "/"
              + id;
      String response = HttpClientUtil.get(requestUrl, headers);

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
