package org.sunbird.notification.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;

public class JsonUtil {
  private static LoggerUtil logger = new LoggerUtil(JsonUtil.class);

  public static String toJson(Object object, RequestContext context) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.writeValueAsString(object);
    } catch (Exception e) {
      logger.error(context, "Exception occurred while converting object to string", e);
    }
    return null;
  }

  public static boolean isStringNullOREmpty(String value) {
    if (value == null || "".equals(value.trim())) {
      return true;
    }
    return false;
  }

  public static <T> T getAsObject(String res, Class<T> clazz, RequestContext context) {
    ObjectMapper mapper = new ObjectMapper();

    T result = null;
    try {
      JsonNode node = mapper.readTree(res);
      result = mapper.convertValue(node, clazz);
    } catch (IOException e) {
      logger.error(context, "Exception occurred while converting String to Object", e);
    }
    return result;
  }
}
