package org.sunbird.notification.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;

/** Utility class for JSON operations and string validations. */
public class JsonUtil {
  private static final LoggerUtil logger = new LoggerUtil(JsonUtil.class);

  /**
   * Converts an object to its JSON string representation.
   *
   * @param object The object to convert.
   * @param context Request context for logging.
   * @return JSON string or null if conversion fails.
   */
  public static String toJson(Object object, RequestContext context) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.writeValueAsString(object);
    } catch (Exception e) {
      logger.error(context, "Exception occurred while converting object to string", e);
    }
    return null;
  }

  /**
   * Converts an object to its JSON string representation. (Overload without RequestContext)
   *
   * @param object The object to convert.
   * @return JSON string or null if conversion fails.
   */
  public static String toJson(Object object) {
    return toJson(object, null);
  }

  /**
   * Checks if a string is null or empty (including whitespace).
   *
   * @param value The string to check.
   * @return true if string is null or empty.
   */
  public static boolean isStringNullOREmpty(String value) {
    return value == null || value.trim().isEmpty();
  }

  /**
   * Converts a JSON string to an object of the specified class.
   *
   * @param res The JSON string.
   * @param clazz The target class.
   * @param context Request context for logging.
   * @param <T> Target type.
   * @return The converted object or null if conversion fails.
   */
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

  /**
   * Converts a JSON string to an object of the specified class. (Overload without RequestContext)
   *
   * @param res The JSON string.
   * @param clazz The target class.
   * @param <T> Target type.
   * @return The converted object or null if conversion fails.
   */
  public static <T> T getAsObject(String res, Class<T> clazz) {
    return getAsObject(res, clazz, null);
  }
}
