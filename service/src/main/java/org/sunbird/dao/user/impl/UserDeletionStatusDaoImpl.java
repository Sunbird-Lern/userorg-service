package org.sunbird.dao.user.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.dao.user.UserDeletionStatusDao;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.util.ProjectUtil;

public class UserDeletionStatusDaoImpl implements UserDeletionStatusDao {
  private final LoggerUtil logger = new LoggerUtil(UserDeletionStatusDaoImpl.class);
  private static final String TABLE_NAME = JsonKey.USER_DELETION_STATUS;
  private static final String KEY_SPACE_NAME = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE);
  private final CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private static UserDeletionStatusDao userDeletionStatusDao = null;

  public static UserDeletionStatusDao getInstance() {
    if (userDeletionStatusDao == null) {
      userDeletionStatusDao = new UserDeletionStatusDaoImpl();
    }
    return userDeletionStatusDao;
  }

  @Override
  public Response insertRecord(Map<String, Object> userDeletionStatus, RequestContext context) {
    return cassandraOperation.insertRecord(KEY_SPACE_NAME, TABLE_NAME, userDeletionStatus, context);
  }

  public Response insertRecords(List<Map<String, Object>> reqMap, RequestContext context) {
    Response result = cassandraOperation.batchInsert(KEY_SPACE_NAME, TABLE_NAME, reqMap, context);
    return result;
  }

  @Override
  public List<Map<String, Object>> getRecordByType(
      String type, String value, RequestContext context) {
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.TYPE, type);
    reqMap.put(JsonKey.VALUE, value);
    Response response =
        cassandraOperation.getRecordsByCompositeKey(KEY_SPACE_NAME, TABLE_NAME, reqMap, context);
    List<Map<String, Object>> userMapList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    return userMapList;
  }
}
