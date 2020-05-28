package org.sunbird.learner.actors.user.service;

import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.datasecurity.EncryptionService;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.learner.util.Util.DbInfo;

public class UserService {

  private DbInfo userDb = Util.dbInfoMap.get(JsonKey.USER_DB);

  public void checkKeyUniqueness(String key, String value, boolean isEncrypted) {
    if (StringUtils.isBlank(key) || StringUtils.isBlank(value)) {
      ProjectLogger.log(
          "UserService:checkKeyUniqueness: Key or value is null. key = "
              + key
              + " value= "
              + value,
          LoggerEnum.ERROR.name());
      return;
    }
    String val = value;
    if (isEncrypted) {
      try {
        val = getEncryptionService().encryptData(val);
      } catch (Exception e) {
        ProjectLogger.log(
            "UserService:checkKeyUniqueness: Exception occurred with error message = "
                + e.getMessage(),
            e);
      }
    }

    Response result = getCassandraOperation().getRecordsByIndexedProperty(userDb.getKeySpace(), userDb.getTableName(), key, val);

    List<Map<String, Object>> userMapList =
        (List<Map<String, Object>>) result.get(JsonKey.RESPONSE);

    if (!userMapList.isEmpty()) {
      ResponseCode responseCode = null;
      if (JsonKey.EMAIL.equals(key)) {
        responseCode = ResponseCode.emailInUse;
      } else if (JsonKey.PHONE.equals(key)) {
        responseCode = ResponseCode.PhoneNumberInUse;
      }
      ProjectCommonException.throwClientErrorException(responseCode, null);
    }
  }

  private CassandraOperation getCassandraOperation(){
    return ServiceFactory.getInstance();
  }

  private EncryptionService getEncryptionService(){
    return org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getEncryptionServiceInstance(null);
  }

}
