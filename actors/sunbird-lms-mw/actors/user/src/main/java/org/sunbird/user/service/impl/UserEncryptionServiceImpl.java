package org.sunbird.user.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.PhoneValidator;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.datasecurity.DecryptionService;
import org.sunbird.common.models.util.datasecurity.EncryptionService;
import org.sunbird.common.models.util.datasecurity.impl.ServiceFactory;
import org.sunbird.user.service.UserEncryptionService;

public class UserEncryptionServiceImpl implements UserEncryptionService {

  private DecryptionService decryptionService = ServiceFactory.getDecryptionServiceInstance(null);
  private EncryptionService encryptionService = ServiceFactory.getEncryptionServiceInstance(null);
  private List<String> userEncryptedFieldList =
      Arrays.asList(JsonKey.USERNAME, JsonKey.LOGIN_ID, JsonKey.LOCATION);

  private UserEncryptionServiceImpl() {};

  public static class LazyInitialisation {
    private static UserEncryptionService INSTANCE = new UserEncryptionServiceImpl();
  }

  public static UserEncryptionService getInstance() {
    return LazyInitialisation.INSTANCE;
  }

  @Override
  public List<String> getDecryptedFields(Map<String, Object> userMap) {
    List<String> decryptedFields = new ArrayList<>();
    if (ProjectUtil.isEmailvalid((String) userMap.get(JsonKey.EMAIL))) {
      decryptedFields.add(JsonKey.EMAIL);
    }

    if (PhoneValidator.validatePhoneNumber((String) userMap.get(JsonKey.PHONE))) {
      decryptedFields.add(JsonKey.PHONE);
    }
    List<String> otherDecryptedFields = getOtherDecryptedFields(userMap);
    decryptedFields.addAll(otherDecryptedFields);
    return decryptedFields;
  }

  @Override
  public List<String> getEncryptedFields(Map<String, Object> userMap) {
    List<String> encryptedFields = new ArrayList<>();
    if (StringUtils.isNotEmpty((String) userMap.get(JsonKey.EMAIL))
        && !ProjectUtil.isEmailvalid((String) userMap.get(JsonKey.EMAIL))) {
      encryptedFields.add(JsonKey.EMAIL);
    }

    if (StringUtils.isNotEmpty((String) userMap.get(JsonKey.PHONE))
        && !PhoneValidator.validatePhoneNumber((String) userMap.get(JsonKey.PHONE))) {
      encryptedFields.add(JsonKey.PHONE);
    }

    List<String> otherEncryptedFields = getOtherEncryptedFields(userMap);
    encryptedFields.addAll(otherEncryptedFields);
    return encryptedFields;
  }

  private List<String> getOtherEncryptedFields(Map<String, Object> userMap) {
    List<String> decryptedFields = new ArrayList<>();
    for (String field : userEncryptedFieldList) {
      try {
        if (StringUtils.isNotBlank((String) userMap.get(field))) {
          decryptionService.decryptData((String) userMap.get(field), true);
          decryptedFields.add(field);
        }
      } catch (ProjectCommonException e) {
        ProjectLogger.log(
            "UserEncryptionServiceImpl:getOtherEncryptedFields: Field is not encrypted " + field,
            LoggerEnum.INFO.name());
      }
    }
    return decryptedFields;
  }

  private List<String> getOtherDecryptedFields(Map<String, Object> userMap) {
    List<String> decryptedFields = new ArrayList<>();
    for (String field : userEncryptedFieldList) {
      try {
        if (StringUtils.isNotBlank((String) userMap.get(field))
            && ((String) userMap.get(field))
                .equals(decryptionService.decryptData((String) userMap.get(field)))) {
          decryptedFields.add(field);
        }
      } catch (Exception e) {
        decryptedFields.add(field);
      }
    }
    return decryptedFields;
  }
}
