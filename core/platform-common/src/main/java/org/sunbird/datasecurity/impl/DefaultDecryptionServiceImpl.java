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
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.util.ProjectUtil;

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

  @Override
  public String decryptData(String data, RequestContext context) {
    return decryptData(data, false, context);
  }

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
}
