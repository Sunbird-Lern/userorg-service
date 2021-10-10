package org.sunbird.dao.user.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.dao.user.UserLookupDao;
import org.sunbird.datasecurity.EncryptionService;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.util.DataCacheHandler;
import org.sunbird.util.Util;

public class UserLookupDaoImpl implements UserLookupDao {
  private final LoggerUtil logger = new LoggerUtil(UserLookupDaoImpl.class);
  private final Util.DbInfo userLookUp = Util.dbInfoMap.get(JsonKey.USER_LOOKUP);
  private final CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private final EncryptionService encryptionService =
      org.sunbird.datasecurity.impl.ServiceFactory.getEncryptionServiceInstance();
  private static UserLookupDao userLookUpDao = null;

  public static UserLookupDao getInstance() {
    if (userLookUpDao == null) {
      userLookUpDao = new UserLookupDaoImpl();
    }
    return userLookUpDao;
  }

  public Response insertRecords(List<Map<String, Object>> reqMap, RequestContext context) {
    Response result =
        cassandraOperation.batchInsert(
            userLookUp.getKeySpace(), userLookUp.getTableName(), reqMap, context);
    return result;
  }

  public void deleteRecords(List<Map<String, String>> reqMap, RequestContext context) {
    logger.info(
        context, "UserLookUp:deleteRecords removing " + reqMap.size() + " lookups from table");
    for (Map<String, String> dataMap : reqMap) {
      cassandraOperation.deleteRecord(
          userLookUp.getKeySpace(), userLookUp.getTableName(), dataMap, context);
    }
  }

  public Response insertExternalIdIntoUserLookup(
      List<Map<String, Object>> reqMap, String userId, RequestContext context) {
    Response result = null;
    if (CollectionUtils.isNotEmpty(reqMap)) {
      Map<String, Object> lookUp = new HashMap<>();
      Map<String, Object> externalId =
          reqMap
              .stream()
              .filter(
                  x -> ((String) x.get(JsonKey.ID_TYPE)).equals((String) x.get(JsonKey.PROVIDER)))
              .findFirst()
              .orElse(null);
      if (org.apache.commons.collections.MapUtils.isNotEmpty(externalId)) {
        lookUp.put(JsonKey.TYPE, JsonKey.USER_LOOKUP_FILED_EXTERNAL_ID);
        lookUp.put(JsonKey.USER_ID, userId);
        // provider is the orgId, not the channel
        lookUp.put(
            JsonKey.VALUE, externalId.get(JsonKey.ID) + "@" + externalId.get(JsonKey.PROVIDER));
      }
      result =
          cassandraOperation.insertRecord(
              userLookUp.getKeySpace(), userLookUp.getTableName(), lookUp, context);
    }
    return result;
  }

  public List<Map<String, Object>> getRecordByType(
      String type, String value, boolean encrypt, RequestContext context) {
    if (encrypt) {
      try {
        value = encryptionService.encryptData(value, context);
      } catch (Exception e) {
        logger.info(context, "Exception occurred while encrypting email/phone " + e);
      }
    }
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.TYPE, type);
    reqMap.put(JsonKey.VALUE, value);
    Response response =
        cassandraOperation.getRecordsByCompositeKey(
            userLookUp.getKeySpace(), userLookUp.getTableName(), reqMap, context);
    List<Map<String, Object>> userMapList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    return userMapList;
  }

  public List<Map<String, Object>> getEmailByType(String email, RequestContext context) {
    String emailSetting = DataCacheHandler.getConfigSettings().get(JsonKey.EMAIL_UNIQUE);
    List<Map<String, Object>> userMapList = null;
    if (StringUtils.isNotBlank(emailSetting) && Boolean.parseBoolean(emailSetting)) {
      userMapList = getRecordByType(JsonKey.EMAIL, email, true, context);
    }
    return userMapList;
  }

  public List<Map<String, Object>> getPhoneByType(String phone, RequestContext context) {
    String phoneSetting = DataCacheHandler.getConfigSettings().get(JsonKey.PHONE_UNIQUE);
    List<Map<String, Object>> userMapList = null;
    if (StringUtils.isNotBlank(phoneSetting) && Boolean.parseBoolean(phoneSetting)) {
      userMapList = getRecordByType(JsonKey.PHONE, phone, true, context);
    }
    return userMapList;
  }

  public List<Map<String, Object>> getUsersByUserNames(
      Map<String, Object> partitionKeyMap, RequestContext context) {
    Response response =
        cassandraOperation.getRecordsByCompositePartitionKey(
            userLookUp.getKeySpace(), userLookUp.getTableName(), partitionKeyMap, context);
    List<Map<String, Object>> userMapList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    return userMapList;
  }
}
