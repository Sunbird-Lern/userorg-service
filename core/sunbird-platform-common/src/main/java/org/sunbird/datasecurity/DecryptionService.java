package org.sunbird.datasecurity;

import java.util.List;
import java.util.Map;
import org.sunbird.request.RequestContext;

/**
 * This service will have data decryption methods. Encryption logic will differ based on implementation classes.
 */
public interface DecryptionService {

  String ALGORITHM = "AES";
  int ITERATIONS = 3;
  byte[] keyValue =
      new byte[] {'T', 'h', 'i', 's', 'A', 's', 'I', 'S', 'e', 'r', 'c', 'e', 'K', 't', 'e', 'y'};

  /**
   * Decrypts the given data map. Values can be primitives, Strings, or nested Maps.
   *
   * @param data The map containing data to decrypt.
   * @param context The request context.
   * @return The map with decrypted values.
   */
  Map<String, Object> decryptData(Map<String, Object> data, RequestContext context);

  /**
   * Decrypts the given data map. Values can be primitives, Strings, or nested Maps.
   * Default implementation calls decryptData(data, null).
   *
   * @param data The map containing data to decrypt.
   * @return The map with decrypted values.
   */
  default Map<String, Object> decryptData(Map<String, Object> data) {
    return decryptData(data, null);
  }

  /**
   * Decrypts a list of data maps.
   *
   * @param data The list of maps to decrypt.
   * @param context The request context.
   * @return The list of maps with decrypted values.
   */
  List<Map<String, Object>> decryptData(List<Map<String, Object>> data, RequestContext context);

  /**
   * Decrypts a list of data maps.
   * Default implementation calls decryptData(data, null).
   *
   * @param data The list of maps to decrypt.
   * @return The list of maps with decrypted values.
   */
  default List<Map<String, Object>> decryptData(List<Map<String, Object>> data) {
    return decryptData(data, null);
  }

  /**
   * Decrypts the given string data.
   *
   * @param data The string to decrypt.
   * @param context The request context.
   * @return The decrypted string.
   */
  String decryptData(String data, RequestContext context);

  /**
   * Decrypts the given string data.
   * Default implementation calls decryptData(data, null).
   *
   * @param data The string to decrypt.
   * @return The decrypted string.
   */
  default String decryptData(String data) {
    return decryptData(data, null);
  }

  /**
   * Decrypts the given string data with an option to throw an exception on failure.
   *
   * @param data The string to decrypt.
   * @param throwExceptionOnFailure Whether to throw an exception if decryption fails.
   * @param context The request context.
   * @return The decrypted string.
   */
  String decryptData(String data, boolean throwExceptionOnFailure, RequestContext context);

  /**
   * Decrypts the given string data with an option to throw an exception on failure.
   * Default implementation calls decryptData(data, throwExceptionOnFailure, null).
   *
   * @param data The string to decrypt.
   * @param throwExceptionOnFailure Whether to throw an exception if decryption fails.
   * @return The decrypted string.
   */
  default String decryptData(String data, boolean throwExceptionOnFailure) {
    return decryptData(data, throwExceptionOnFailure, null);
  }
}
