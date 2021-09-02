package org.sunbird.dao.user.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.dao.user.UserExternalIdentityDao;
import org.sunbird.dao.user.UserLookupDao;
import org.sunbird.datasecurity.EncryptionService;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

public class UserExternalIdentityDaoImpl implements UserExternalIdentityDao {

  private static LoggerUtil logger = new LoggerUtil(UserExternalIdentityDaoImpl.class);
  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private EncryptionService encryptionService =
      org.sunbird.datasecurity.impl.ServiceFactory.getEncryptionServiceInstance(null);

  @Override
  public String getUserIdByExternalId(String extId, String provider, RequestContext context) {
    if (StringUtils.isNotEmpty((provider))) {
      UserLookupDao userLookupDao = new UserLookupDaoImpl();
      List<Map<String, Object>> userRecordList =
          userLookupDao.getRecordByType(
              JsonKey.USER_LOOKUP_FILED_EXTERNAL_ID, extId + "@" + provider, false, context);
      if (CollectionUtils.isNotEmpty(userRecordList)) {
        logger.info(
            context,
            "getUserIdByExternalId: got userId from user_lookup for extId "
                + extId
                + " "
                + userRecordList.get(0).get(JsonKey.USER_ID));
        return (String) userRecordList.get(0).get(JsonKey.USER_ID);
      }
    }
    logger.info(
        context,
        "getUserIdByExternalId: got userId from user_lookup for extId " + extId + " is null");
    return null;
  }

  @Override
  public List<Map<String, String>> getUserExternalIds(String userId, RequestContext context) {
    List<Map<String, String>> dbResExternalIds = new ArrayList<>();
    Map<String, Object> req = new HashMap<>();
    req.put(JsonKey.USER_ID, userId);
    Response response =
        cassandraOperation.getRecordById(JsonKey.SUNBIRD, JsonKey.USR_EXT_IDNT_TABLE, req, context);
    if (null != response && null != response.getResult()) {
      dbResExternalIds = (List<Map<String, String>>) response.getResult().get(JsonKey.RESPONSE);
    }
    return dbResExternalIds;
  }

  @Override
  public List<Map<String, Object>> getUserSelfDeclaredDetails(
      String userId, RequestContext context) {
    List<Map<String, Object>> dbResExternalIds = new ArrayList<>();
    Map<String, Object> req = new HashMap<>();
    req.put(JsonKey.USER_ID, userId);
    Response response =
        cassandraOperation.getRecordById(
            JsonKey.SUNBIRD, JsonKey.USER_DECLARATION_DB, req, context);
    if (null != response && null != response.getResult()) {
      dbResExternalIds = (List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE);
    }
    return dbResExternalIds;
  }
}
