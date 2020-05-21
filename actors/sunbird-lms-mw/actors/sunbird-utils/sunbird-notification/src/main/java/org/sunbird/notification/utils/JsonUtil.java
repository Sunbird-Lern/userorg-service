package org.sunbird.notification.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.jboss.logging.Logger;

public class JsonUtil {
  private static Logger logger = Logger.getLogger(JsonUtil.class);

  public static String toJson(Object object) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.writeValueAsString(object);
    } catch (Exception e) {
      // ProjectLogger.log("JsonUtil:getJsonString error occured : " + e, LoggerEnum.INFO);
    }
    return null;
  }

  public static boolean isStringNullOREmpty(String value) {
    if (value == null || "".equals(value.trim())) {
      return true;
    }
    return false;
  }

  public static <T> T getAsObject(String res, Class<T> clazz) {
    ObjectMapper mapper = new ObjectMapper();

    T result = null;
    try {
      JsonNode node = mapper.readTree(res);
      result = mapper.convertValue(node, clazz);
    } catch (IOException e) {
      // ProjectLogger.log("JsonUtil:getAsObject error occured : " + e, LoggerEnum.INFO);
      e.printStackTrace();
    }
    return result;
  }
}
