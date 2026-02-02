package org.sunbird.datasecurity;

import java.util.List;
import java.util.Map;
import org.sunbird.request.RequestContext;

/**
 * Service interface for data encryption operations.
 * Implementations provide specific encryption logic.
 */
public interface EncryptionService {

  String ALGORITHM = "AES";
  int ITERATIONS = 3;
  byte[] keyValue =
      new byte[] {'T', 'h', 'i', 's', 'A', 's', 'I', 'S', 'e', 'r', 'c', 'e', 'K', 't', 'e', 'y'};

  /**
   * Encrypts the values in a map.
   *
   * @param data The map containing data to encrypt.
   * @param context The request context.
   * @return The map with encrypted values.
   * @throws Exception If an error occurs during encryption.
   */
  Map<String, Object> encryptData(Map<String, Object> data, RequestContext context);

  /**
   * Encrypts the values in a map without a request context.
   * Delegates to {@link #encryptData(Map, RequestContext)} with null context.
   *
   * @param data The map containing data to encrypt.
   * @return The map with encrypted values.
   * @throws Exception If an error occurs during encryption.
   */
  default Map<String, Object> encryptData(Map<String, Object> data) throws Exception {
    return encryptData(data, null);
  }

  /**
   * Encrypts the values in a list of maps.
   *
   * @param data The list of maps to encrypt.
   * @param context The request context.
   * @return The list of maps with encrypted values.
   * @throws Exception If an error occurs during encryption.
   */
  List<Map<String, Object>> encryptData(List<Map<String, Object>> data, RequestContext context);

  /**
   * Encrypts the values in a list of maps without a request context.
   * Delegates to {@link #encryptData(List, RequestContext)} with null context.
   *
   * @param data The list of maps to encrypt.
   * @return The list of maps with encrypted values.
   * @throws Exception If an error occurs during encryption.
   */
  default List<Map<String, Object>> encryptData(List<Map<String, Object>> data) throws Exception {
    return encryptData(data, null);
  }

  /**
   * Encrypts a single string value.
   *
   * @param data The string to encrypt.
   * @param context The request context.
   * @return The encrypted string.
   * @throws Exception If an error occurs during encryption.
   */
  String encryptData(String data, RequestContext context);

  /**
   * Encrypts a single string value without a request context.
   * Delegates to {@link #encryptData(String, RequestContext)} with null context.
   *
   * @param data The string to encrypt.
   * @return The encrypted string.
   * @throws Exception If an error occurs during encryption.
   */
  default String encryptData(String data) throws Exception {
    return encryptData(data, null);
  }
}
