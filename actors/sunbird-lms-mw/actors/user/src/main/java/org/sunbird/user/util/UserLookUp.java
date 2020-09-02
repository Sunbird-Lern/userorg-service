package org.sunbird.user.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.datasecurity.EncryptionService;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.Util;

public class UserLookUp {

  private static CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private static EncryptionService encryptionService =
      org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getEncryptionServiceInstance(
          null);
  private static Util.DbInfo userLookUp = Util.dbInfoMap.get(JsonKey.USER_LOOK_UP);

  public Response add(String type, String value, String userId, boolean isEncrypted) {
    try {
      if (isEncrypted) {
        value = encryptionService.encryptData(value);
      }
    } catch (Exception e) {
      ProjectLogger.log("Exception occurred while encrypting email/phone", e);
    }
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.TYPE, type);
    reqMap.put(JsonKey.VALUE, value);
    reqMap.put(JsonKey.USER_ID, userId);
    Response result =
        cassandraOperation.insertRecord(
            userLookUp.getKeySpace(), userLookUp.getTableName(), reqMap);
    return result;
  }

  public Response getRecordByType(String type, String value, boolean isEncrypted) {
    try {
      if (isEncrypted) {
        value = encryptionService.encryptData(value);
      }
    } catch (Exception e) {
      ProjectLogger.log("Exception occurred while encrypting email/phone", e);
    }
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.TYPE, type);
    reqMap.put(JsonKey.VALUE, value);
    Response response =
        cassandraOperation.getRecordsByCompositeKey(
            userLookUp.getKeySpace(), userLookUp.getTableName(), reqMap);
    return response;
  }

  public void getRecordByEmail(String email) {
    // Get Phone configuration if not found , by default phone will be unique across
    // the application
    String emailSetting = DataCacheHandler.getConfigSettings().get(JsonKey.EMAIL_UNIQUE);
    if (StringUtils.isNotBlank(emailSetting) && Boolean.parseBoolean(emailSetting)) {
      if (StringUtils.isNotBlank(email)) {
        Response result = getRecordByType(JsonKey.EMAIL, email, true);
        List<Map<String, Object>> userMapList =
            (List<Map<String, Object>>) result.get(JsonKey.RESPONSE);
        if (!userMapList.isEmpty()) {
          ProjectCommonException.throwClientErrorException(
              ResponseCode.emailAlreadyExistError, null);
        }
      }
    }
  }

  public void getRecordByPhone(String phone) {
    // Get Phone configuration if not found , by default phone will be unique across
    // the application
    String phoneSetting = DataCacheHandler.getConfigSettings().get(JsonKey.PHONE_UNIQUE);
    if (StringUtils.isNotBlank(phoneSetting) && Boolean.parseBoolean(phoneSetting)) {
      if (StringUtils.isNotBlank(phone)) {
        Response result = getRecordByType(JsonKey.PHONE, phone, true);
        List<Map<String, Object>> userMapList =
            (List<Map<String, Object>>) result.get(JsonKey.RESPONSE);
        if (!userMapList.isEmpty()) {
          ProjectCommonException.throwClientErrorException(ResponseCode.PhoneNumberInUse, null);
        }
      }
    }
  }
}
