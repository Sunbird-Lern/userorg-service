package org.sunbird.content.store.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpHeaders;
import org.sunbird.common.models.util.HttpUtil;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;

public class ContentStoreUtil {

  private static Map<String, String> getHeaders() {
    Map<String, String> headers = new HashMap<>();
    headers.put(
        HttpHeaders.AUTHORIZATION,
        JsonKey.BEARER + ProjectUtil.getConfigValue(JsonKey.SUNBIRD_AUTHORIZATION));
    return headers;
  }

  public static Map<String, Object> readFramework(String frameworkId) {
    ProjectLogger.log(
        "ContentStoreUtil:readFramework: frameworkId = " + frameworkId, LoggerEnum.INFO.name());
    return handleReadRequest(frameworkId, JsonKey.SUNBIRD_FRAMEWORK_READ_API);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> handleReadRequest(String id, String urlPath) {
    Map<String, String> headers = getHeaders();
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> resultMap = new HashMap<>();

    ProjectLogger.log("ContentStoreUtil:handleReadRequest: id = " + id, LoggerEnum.INFO.name());

    try {
      String requestUrl =
          ProjectUtil.getConfigValue(JsonKey.SUNBIRD_API_BASE_URL)
              + ProjectUtil.getConfigValue(urlPath)
              + "/"
              + id;
      String response = HttpUtil.sendGetRequest(requestUrl, headers);

      resultMap = mapper.readValue(response, Map.class);
      if (!((String) resultMap.get(JsonKey.RESPONSE_CODE)).equalsIgnoreCase(JsonKey.OK)) {
        ProjectLogger.log(
            "ContentStoreUtil:handleReadRequest: Response code is not ok.",
            LoggerEnum.ERROR.name());
        return null;
      }
    } catch (Exception e) {
      ProjectLogger.log(
          "ContentStoreUtil:handleReadRequest: Exception occurred with error message = "
              + e.getMessage(),
          e);
    }
    return resultMap;
  }
}
