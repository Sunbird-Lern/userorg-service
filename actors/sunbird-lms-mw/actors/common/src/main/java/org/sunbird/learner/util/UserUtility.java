package org.sunbird.learner.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.models.util.datasecurity.DataMaskingService;
import org.sunbird.common.models.util.datasecurity.DecryptionService;
import org.sunbird.common.models.util.datasecurity.EncryptionService;
import org.sunbird.common.models.util.datasecurity.impl.ServiceFactory;

/**
 * This class is for utility methods for encrypting user data.
 *
 * @author Amit Kumar
 */
public final class UserUtility {

  private static List<String> userKeyToEncrypt;
  private static List<String> addressKeyToEncrypt;
  private static List<String> userKeyToDecrypt;
  private static List<String>userKeysToMasked;
  private static DecryptionService decryptionService;
  private static DataMaskingService maskingService;
  private static List<String>phoneMaskedAttributes;
  private static List<String>emailMaskedAttributes;

  static {
    init();
  }

  private UserUtility() {}

  public static Map<String, Object> encryptUserData(Map<String, Object> userMap) throws Exception {
    return encryptSpecificUserData(userMap, userKeyToEncrypt);
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> encryptSpecificUserData(
      Map<String, Object> userMap, List<String> fieldsToEncrypt) throws Exception {
    EncryptionService service = ServiceFactory.getEncryptionServiceInstance(null);
    // Encrypt user basic info
    for (String key : fieldsToEncrypt) {
      if (userMap.containsKey(key)) {
        userMap.put(key, service.encryptData((String) userMap.get(key)));
      }
    }
    // Encrypt user address Info
    if (userMap.containsKey(JsonKey.ADDRESS)) {
      List<Map<String, Object>> addressList =
          (List<Map<String, Object>>) userMap.get(JsonKey.ADDRESS);
      for (Map<String, Object> map : addressList) {
        for (String key : addressKeyToEncrypt) {
          if (map.containsKey(key)) {
            map.put(key, service.encryptData((String) map.get(key)));
          }
        }
      }
    }
    return userMap;
  }

  public static List<Map<String, Object>> encryptUserAddressData(
      List<Map<String, Object>> addressList) throws Exception {
    EncryptionService service = ServiceFactory.getEncryptionServiceInstance(null);
    // Encrypt user address Info
    for (Map<String, Object> map : addressList) {
      for (String key : addressKeyToEncrypt) {
        if (map.containsKey(key)) {
          map.put(key, service.encryptData((String) map.get(key)));
        }
      }
    }
    return addressList;
  }

  public static Map<String, Object> decryptUserData(Map<String, Object> userMap) {
    return decryptSpecificUserData(userMap, userKeyToEncrypt);
  }

  public static Map<String, Object> decryptSpecificUserData(
      Map<String, Object> userMap, List<String> fieldsToDecrypt) {
    DecryptionService service = ServiceFactory.getDecryptionServiceInstance(null);
    // Decrypt user basic info
    for (String key : fieldsToDecrypt) {
      if (userMap.containsKey(key)) {
        userMap.put(key, service.decryptData((String) userMap.get(key)));
      }
    }

    // Decrypt user address Info
    if (userMap.containsKey(JsonKey.ADDRESS)) {
      List<Map<String, Object>> addressList =
          (List<Map<String, Object>>) userMap.get(JsonKey.ADDRESS);
      for (Map<String, Object> map : addressList) {
        for (String key : addressKeyToEncrypt) {
          if (map.containsKey(key)) {
            map.put(key, service.decryptData((String) map.get(key)));
          }
        }
      }
    }
    return userMap;
  }

  public static Map<String, Object> decryptUserDataFrmES(Map<String, Object> userMap) {
    DecryptionService service = ServiceFactory.getDecryptionServiceInstance(null);
    // Decrypt user basic info
    for (String key : userKeyToDecrypt) {
      if (userMap.containsKey(key)) {
        if (userKeysToMasked.contains(key)) {
          userMap.put(key, maskEmailOrPhone((String) userMap.get(key), key));
        } else {
          userMap.put(key, service.decryptData((String) userMap.get(key)));
        }
      }
    }
    // Decrypt user address Info
    if (userMap.containsKey(JsonKey.ADDRESS)) {
      List<Map<String, Object>> addressList =
          (List<Map<String, Object>>) userMap.get(JsonKey.ADDRESS);
      for (Map<String, Object> map : addressList) {
        for (String key : addressKeyToEncrypt) {
          if (map.containsKey(key)) {
            map.put(key, service.decryptData((String) map.get(key)));
          }
        }
      }
    }
    return userMap;
  }

  public static List<Map<String, Object>> decryptUserAddressData(
      List<Map<String, Object>> addressList) {
    DecryptionService service = ServiceFactory.getDecryptionServiceInstance(null);
    // Decrypt user address info
    for (Map<String, Object> map : addressList) {
      for (String key : addressKeyToEncrypt) {
        if (map.containsKey(key)) {
          map.put(key, service.decryptData((String) map.get(key)));
        }
      }
    }
    return addressList;
  }

  public static Map<String, Object> encryptUserSearchFilterQueryData(Map<String, Object> map)
      throws Exception {
    Map<String, Object> filterMap = (Map<String, Object>) map.get(JsonKey.FILTERS);
    EncryptionService service = ServiceFactory.getEncryptionServiceInstance(null);
    // Encrypt user basic info
    for (String key : userKeyToEncrypt) {
      if (filterMap.containsKey(key)) {
        filterMap.put(key, service.encryptData((String) filterMap.get(key)));
      }
    }
    // Encrypt user address Info
    for (String key : addressKeyToEncrypt) {
      if ((filterMap).containsKey((JsonKey.ADDRESS + "." + key))) {
        filterMap.put(
            (JsonKey.ADDRESS + "." + key),
            service.encryptData((String) filterMap.get(JsonKey.ADDRESS + "." + key)));
      }
    }
    return filterMap;
  }

  public static String encryptData(String data) throws Exception {
    EncryptionService service = ServiceFactory.getEncryptionServiceInstance(null);
    return service.encryptData(data);
  }

  public static String maskEmailOrPhone(String encryptedEmailOrPhone, String type) {
    if (StringUtils.isEmpty(encryptedEmailOrPhone)) {
      return StringUtils.EMPTY;
    }
    if (phoneMaskedAttributes.contains(type)) {
      return maskingService.maskPhone(decryptionService.decryptData(encryptedEmailOrPhone));
    } else if (emailMaskedAttributes.contains(type)) {
      return maskingService.maskEmail(decryptionService.decryptData(encryptedEmailOrPhone));
    }
    return StringUtils.EMPTY;
  }

  private static void init(){
    decryptionService =
            org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getDecryptionServiceInstance(
                    null);
    maskingService =
            org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getMaskingServiceInstance(
                    null);
    String userKey = PropertiesCache.getInstance().getProperty("userkey.encryption");
    userKeyToEncrypt = new ArrayList<>(Arrays.asList(userKey.split(",")));
    ProjectLogger.log("UserUtility:init:user encrypt  attributes got".concat(userKey+""), LoggerEnum.INFO.name());
    String addressKey = PropertiesCache.getInstance().getProperty("addresskey.encryption");
    ProjectLogger.log("UserUtility:init:user address encrypt  attributes got".concat(addressKey+""), LoggerEnum.INFO.name());
    addressKeyToEncrypt = new ArrayList<>(Arrays.asList(addressKey.split(",")));
    String userKeyDecrypt = PropertiesCache.getInstance().getProperty("userkey.decryption");
    String userKeyToMasked=PropertiesCache.getInstance().getProperty("userkey.masked");
    userKeyToDecrypt = new ArrayList<>(Arrays.asList(userKeyDecrypt.split(",")));
    userKeysToMasked=new ArrayList<>(Arrays.asList(userKeyToMasked.split(",")));
    String emailTypeAttributeKey=PropertiesCache.getInstance().getProperty("userkey.emailtypeattributes");
    String phoneTypeAttributeKey=PropertiesCache.getInstance().getProperty("userkey.phonetypeattributes");
    emailMaskedAttributes=new ArrayList<>(Arrays.asList(emailTypeAttributeKey.split(",")));
    ProjectLogger.log("UserUtility:init:email masked attributes got".concat(emailTypeAttributeKey+""), LoggerEnum.INFO.name());
    phoneMaskedAttributes=new ArrayList<>(Arrays.asList(phoneTypeAttributeKey.split(",")));
    ProjectLogger.log("UserUtility:init:phone masked attributes got".concat(phoneTypeAttributeKey+""),LoggerEnum.INFO.name());

  }
}
