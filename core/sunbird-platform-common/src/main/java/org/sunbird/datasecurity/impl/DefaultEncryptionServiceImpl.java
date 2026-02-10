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
import org.sunbird.datasecurity.EncryptionService;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.common.ProjectUtil;

/**
 * Default implementation of the {@link EncryptionService} interface.
 * Uses AES encryption algorithm to encrypt data.
 */
public class DefaultEncryptionServiceImpl implements EncryptionService {
  private static final LoggerUtil logger = new LoggerUtil(DefaultEncryptionServiceImpl.class);

  private static String encryption_key = "";

  private String sunbirdEncryption = "";

  private static Cipher c;

  static {
    try {
      encryption_key = getSalt();
      Key key = generateKey();
      c = Cipher.getInstance(ALGORITHM);
      c.init(Cipher.ENCRYPT_MODE, key);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
  }

  public DefaultEncryptionServiceImpl() {
    sunbirdEncryption = System.getenv(JsonKey.SUNBIRD_ENCRYPTION);
    if (StringUtils.isBlank(sunbirdEncryption)) {
      sunbirdEncryption = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_ENCRYPTION);
    }
  }

  /**
   * Encrypts the values in a map.
   *
   * @param data The map containing data to encrypt.
   * @param context The request context.
   * @return The map with encrypted values.
   */
  @Override
  public Map<String, Object> encryptData(Map<String, Object> data, RequestContext context) {
    if (JsonKey.ON.equalsIgnoreCase(sunbirdEncryption)) {
      if (data == null) {
        return data;
      }
      Iterator<Entry<String, Object>> itr = data.entrySet().iterator();
      while (itr.hasNext()) {
        Entry<String, Object> entry = itr.next();
        if (!(entry.getValue() instanceof Map || entry.getValue() instanceof List)
            && null != entry.getValue()) {
          data.put(entry.getKey(), encrypt(entry.getValue() + "", context));
        }
      }
    }
    return data;
  }

  /**
   * Encrypts the values in a list of maps.
   *
   * @param data The list of maps to encrypt.
   * @param context The request context.
   * @return The list of maps with encrypted values.
   */
  @Override
  public List<Map<String, Object>> encryptData(
      List<Map<String, Object>> data, RequestContext context) {
    if (JsonKey.ON.equalsIgnoreCase(sunbirdEncryption)) {
      if (data == null || data.isEmpty()) {
        return data;
      }
      for (Map<String, Object> map : data) {
        encryptData(map, context);
      }
    }
    return data;
  }

  /**
   * Encrypts a single string value.
   *
   * @param data The string to encrypt.
   * @param context The request context.
   * @return The encrypted string.
   */
  @Override
  public String encryptData(String data, RequestContext context) {
    if (JsonKey.ON.equalsIgnoreCase(sunbirdEncryption)) {
      if (StringUtils.isNotBlank(data)) {
        return encrypt(data, context);
      } else {
        return data;
      }
    } else {
      return data;
    }
  }

  /**
   * Encrypts the given value using the configured algorithm/key.
   *
   * @param value String password or data to encrypt.
   * @param context The request context.
   * @return encrypted string.
   */
  @SuppressWarnings("restriction")
  public static String encrypt(String value, RequestContext context) {
    String valueToEnc = null;
    String eValue = value;
    for (int i = 0; i < ITERATIONS; i++) {
      valueToEnc = encryption_key + eValue;
      byte[] encValue = new byte[0];
      try {
        encValue = c.doFinal(valueToEnc.getBytes(StandardCharsets.UTF_8));
      } catch (Exception e) {
        logger.error(
            context, "Exception while encrypting user data, with message : " + e.getMessage(), e);
        throw new ProjectCommonException(
            ResponseCode.serverError,
            ResponseCode.serverError.getErrorMessage(),
            ResponseCode.SERVER_ERROR.getResponseCode());
      }
      eValue = new BASE64Encoder().encode(encValue);
    }
    return eValue;
  }

  private static Key generateKey() {
    return new SecretKeySpec(keyValue, ALGORITHM);
  }

  /** 
   * Retrieves the encryption salt (key) from environment or config.
   * @return The encryption key.
   */
  public static String getSalt() {
    if (!StringUtils.isBlank(encryption_key)) {
      return encryption_key;
    } else {
      encryption_key = System.getenv(JsonKey.ENCRYPTION_KEY);
      if (StringUtils.isBlank(encryption_key)) {
        logger.info("Salt value is not provided by Env");
        encryption_key = ProjectUtil.getConfigValue(JsonKey.ENCRYPTION_KEY);
      }
    }
    if (StringUtils.isBlank(encryption_key)) {
      logger.info("throwing exception for invalid salt");
      throw new ProjectCommonException(
          ResponseCode.invalidParameterValue,
          String.format(
              ResponseCode.invalidParameterValue.getErrorMessage(), JsonKey.ENCRYPTION_KEY),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    return encryption_key;
  }

  /**
   * Encrypts the values in a map without a request context.
   * Delegates to {@link #encryptData(Map, RequestContext)} with null context.
   *
   * @param data The map containing data to encrypt.
   * @return The map with encrypted values.
   * @throws Exception If an error occurs during encryption.
   */
  @Override
  public Map<String, Object> encryptData(Map<String, Object> data) throws Exception {
    return encryptData(data, null);
  }

  /**
   * Encrypts the values in a list of maps without a request context.
   * Delegates to {@link #encryptData(List, RequestContext)} with null context.
   *
   * @param data The list of maps to encrypt.
   * @return The list of maps with encrypted values.
   * @throws Exception If an error occurs during encryption.
   */
  @Override
  public List<Map<String, Object>> encryptData(List<Map<String, Object>> data) throws Exception {
    return encryptData(data, null);
  }

  /**
   * Encrypts a single string value without a request context.
   * Delegates to {@link #encryptData(String, RequestContext)} with null context.
   *
   * @param data The string to encrypt.
   * @return The encrypted string.
   * @throws Exception If an error occurs during encryption.
   */
  @Override
  public String encryptData(String data) throws Exception {
    return encryptData(data, null);
  }
}


