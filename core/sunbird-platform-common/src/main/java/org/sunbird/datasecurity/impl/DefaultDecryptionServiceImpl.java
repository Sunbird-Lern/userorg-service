package org.sunbird.datasecurity.impl;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.datasecurity.DecryptionService;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.common.ProjectUtil;

/**
 * Default implementation of the {@link DecryptionService} interface.
 * Uses AES encryption algorithm to decrypt data.
 */
public class DefaultDecryptionServiceImpl implements DecryptionService {
  private static final LoggerUtil logger = new LoggerUtil(DefaultDecryptionServiceImpl.class);

  private static String sunbird_encryption = "";

  private String sunbirdEncryption = "";

  private static Cipher c;

  static {
    try {
      sunbird_encryption = DefaultEncryptionServiceImpl.getSalt();
      Key key = generateKey();
      c = Cipher.getInstance(ALGORITHM);
      c.init(Cipher.DECRYPT_MODE, key);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
  }

  public DefaultDecryptionServiceImpl() {
    sunbirdEncryption = System.getenv(JsonKey.SUNBIRD_ENCRYPTION);
    if (StringUtils.isBlank(sunbirdEncryption)) {
      sunbirdEncryption = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_ENCRYPTION);
    }
  }

  /**
   * Decrypts values in a map if encryption is enabled.
   * Modifies the map in-place.
   *
   * @param data The map containing data to decrypt.
   * @param context The request context.
   * @return The data map with decrypted values.
   */
  @Override
  public Map<String, Object> decryptData(Map<String, Object> data, RequestContext context) {
    if (JsonKey.ON.equalsIgnoreCase(sunbirdEncryption)) {
      if (data == null) {
        return data;
      }
      Iterator<Entry<String, Object>> itr = data.entrySet().iterator();
      while (itr.hasNext()) {
        Entry<String, Object> entry = itr.next();
        if (!(entry.getValue() instanceof Map || entry.getValue() instanceof List)
            && null != entry.getValue()) {
          data.put(entry.getKey(), decrypt(entry.getValue() + "", false, context));
        }
      }
    }
    return data;
  }

  /**
   * Decrypts values in a list of maps.
   *
   * @param data The list of maps to decrypt.
   * @param context The request context.
   * @return The list of maps with decrypted values.
   */
  @Override
  public List<Map<String, Object>> decryptData(
      List<Map<String, Object>> data, RequestContext context) {
    if (JsonKey.ON.equalsIgnoreCase(sunbirdEncryption)) {
      if (data == null || data.isEmpty()) {
        return data;
      }

      for (Map<String, Object> map : data) {
        decryptData(map, context);
      }
    }
    return data;
  }

  /**
   * Decrypts a single string value.
   *
   * @param data The string to decrypt.
   * @param context The request context.
   * @return The decrypted string, or the original string if encryption is disabled.
   */
  @Override
  public String decryptData(String data, RequestContext context) {
    return decryptData(data, false, context);
  }

  /**
   * Decrypts a single string value, optionally throwing an exception on failure.
   *
   * @param data The string to decrypt.
   * @param throwExceptionOnFailure Whether to throw an exception if decryption fails.
   * @param context The request context.
   * @return The decrypted string.
   */
  @Override
  public String decryptData(String data, boolean throwExceptionOnFailure, RequestContext context) {
    if (JsonKey.ON.equalsIgnoreCase(sunbirdEncryption)) {
      if (StringUtils.isBlank(data)) {
        return data;
      } else {
        return decrypt(data, throwExceptionOnFailure, context);
      }
    } else {
      return data;
    }
  }

  /**
   * Internal method to perform the decryption logic.
   *
   * @param value The value to decrypt.
   * @param throwExceptionOnFailure Whether to throw an exception on error.
   * @param context The request context.
   * @return The decrypted value.
   */
  public static String decrypt(
      String value, boolean throwExceptionOnFailure, RequestContext context) {
    try {
      String dValue = null;
      String valueToDecrypt = value.trim();
      for (int i = 0; i < ITERATIONS; i++) {
        byte[] decordedValue = new BASE64Decoder().decodeBuffer(valueToDecrypt);
        byte[] decValue = c.doFinal(decordedValue);
        dValue =
            new String(decValue, StandardCharsets.UTF_8).substring(sunbird_encryption.length());
        valueToDecrypt = dValue;
      }
      return dValue;
    } catch (Exception ex) {
      // This could happen with masked email and phone number. Not others.
      logger.error(context, "DefaultDecryptionServiceImpl:decrypt: ignorable errorMsg = ", ex);
      if (throwExceptionOnFailure) {
        logger.info(
            context, "Throwing exception error upon explicit ask by callers for value " + value);
        ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
      }
    }
    return value;
  }

  private static Key generateKey() {
    return new SecretKeySpec(keyValue, ALGORITHM);
  }

  /**
   * Decrypts values in a map without a request context.
   * Delegates to {@link #decryptData(Map, RequestContext)} with null context.
   *
   * @param data The map containing data to decrypt.
   * @return The data map with decrypted values.
   */
  @Override
  public Map<String, Object> decryptData(Map<String, Object> data) {
    return decryptData(data, null);
  }

  /**
   * Decrypts values in a list of maps without a request context.
   * Delegates to {@link #decryptData(List, RequestContext)} with null context.
   *
   * @param data The list of maps to decrypt.
   * @return The list of maps with decrypted values.
   */
  @Override
  public List<Map<String, Object>> decryptData(List<Map<String, Object>> data) {
    return decryptData(data, null);
  }

  /**
   * Decrypts a single string value without a request context.
   * Delegates to {@link #decryptData(String, RequestContext)} with null context.
   *
   * @param data The string to decrypt.
   * @return The decrypted string.
   */
  @Override
  public String decryptData(String data) {
    return decryptData(data, null);
  }

  /**
    * Decrypts a single string value without a request context, optionally throwing an exception on failure.
    * Delegates to {@link #decryptData(String, boolean, RequestContext)} with null context.
    *
    * @param data The string to decrypt.
    * @param throwExceptionOnFailure Whether to throw an exception if decryption fails.
    * @return The decrypted string.
    */
  @Override
  public String decryptData(String data, boolean throwExceptionOnFailure) {
    return decryptData(data, throwExceptionOnFailure, null);
  }
}
