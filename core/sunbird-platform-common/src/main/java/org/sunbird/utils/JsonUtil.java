package org.sunbird.utils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;

/**
 * Utility class for JSON operations using Jackson.
 * Provides static methods for serialization, deserialization, and conversion.
 */
public class JsonUtil {

  private static final LoggerUtil logger = new LoggerUtil(JsonUtil.class);
  private static ObjectMapper mapper = new ObjectMapper();
  private static ObjectMapper mapperWithDateFormat = new ObjectMapper();

  static {
    // Configure default mapper
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    mapper.setSerializationInclusion(Include.NON_NULL);

    // Configure mapper with date format support base settings
    mapperWithDateFormat.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapperWithDateFormat.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
  }

  /**
   * Serializes an object to a JSON string.
   *
   * @param obj The object to serialize.
   * @return The JSON string representation.
   * @throws Exception If serialization fails.
   */
  public static String serialize(Object obj) throws Exception {
    return mapper.writeValueAsString(obj);
  }

  /**
   * Deserializes a JSON string to a POJO.
   *
   * @param value The JSON string.
   * @param clazz The target class.
   * @param <T> The type of the target class.
   * @return The deserialized object.
   * @throws Exception If deserialization fails.
   */
  public static <T> T deserialize(String value, Class<T> clazz) throws Exception {
    return mapper.readValue(value, clazz);
  }

  /**
   * Deserializes an InputStream to a POJO.
   *
   * @param value The InputStream containing JSON.
   * @param clazz The target class.
   * @param <T> The type of the target class.
   * @return The deserialized object.
   * @throws Exception If deserialization fails.
   */
  public static <T> T deserialize(InputStream value, Class<T> clazz) throws Exception {
    return mapper.readValue(value, clazz);
  }

  /**
   * Converts an object to the target class type using Jackson conversion.
   *
   * @param value The source object.
   * @param clazz The target class.
   * @param <T> The type of the target class.
   * @return The converted object.
   * @throws Exception If conversion fails.
   */
  public static <T> T convert(Object value, Class<T> clazz) throws Exception {
    return mapper.convertValue(value, clazz);
  }

  /**
   * Converts an object to the target class using a specific date format.
   *
   * @param value The source object.
   * @param clazz The target class.
   * @param dateFormat The SimpleDateFormat to use.
   * @param <T> The type of the target class.
   * @return The converted object.
   * @throws Exception If conversion fails.
   */
  public static <T> T convertWithDateFormat(
      Object value, Class<T> clazz, SimpleDateFormat dateFormat) throws Exception {
    mapperWithDateFormat.setDateFormat(dateFormat);
    return mapperWithDateFormat.convertValue(value, clazz);
  }

  /**
   * Serializes an object to a JSON string, logging any errors instead of throwing.
   *
   * @param object The object to serialize.
   * @param context The request context for logging.
   * @return The JSON string, or null if an error occurs.
   */
  public static String toJson(Object object, RequestContext context) {
    try {
      return mapper.writeValueAsString(object);
    } catch (Exception e) {
      logger.error(context, "JsonUtil:toJson: Error occurred while serializing object to JSON string.", e);
    }
    return null;
  }

  /**
   * Checks if a string is null or empty (after trimming).
   *
   * @param value The string to check.
   * @return True if null or empty/whitespace, false otherwise.
   */
  public static boolean isStringNullOREmpty(String value) {
    return value == null || "".equals(value.trim());
  }

  /**
   * Deserializes a JSON string to a POJO, logging any errors instead of throwing.
   *
   * @param res The JSON string.
   * @param clazz The target class.
   * @param context The request context for logging.
   * @param <T> The type of the target class.
   * @return The deserialized object, or null if an error occurs.
   */
  public static <T> T getAsObject(String res, Class<T> clazz, RequestContext context) {
    T result = null;
    try {
      JsonNode node = mapper.readTree(res);
      result = mapper.convertValue(node, clazz);
    } catch (Exception e) {
      logger.error(context, "JsonUtil:getAsObject: Error occurred while deserializing JSON string to Object.", e);
    }
    return result;
  }
}
