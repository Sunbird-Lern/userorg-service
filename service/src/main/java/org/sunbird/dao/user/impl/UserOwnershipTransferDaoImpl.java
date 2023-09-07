package org.sunbird.dao.user.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.dao.user.UserOwnershipTransferDao;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.util.ProjectUtil;

/**
 * Implementation class of UserDao interface.
 *
 * @author Amit Kumar
 */
public class UserOwnershipTransferDaoImpl implements UserOwnershipTransferDao {

  private final LoggerUtil logger = new LoggerUtil(UserOwnershipTransferDaoImpl.class);
  private static final String TABLE_NAME = JsonKey.USER_OWNERSHIP_TRANSFER;
  private static final String KEY_SPACE_NAME = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE);
  private final CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private static UserOwnershipTransferDao userOwnershipTransferDao = null;

  public static UserOwnershipTransferDao getInstance() {
    if (userOwnershipTransferDao == null) {
      userOwnershipTransferDao = new UserOwnershipTransferDaoImpl();
    }
    return userOwnershipTransferDao;
  }

  @Override
  public Response createUserOwnershipTransfer(
      Map<String, Object> ownershipMap, RequestContext context) {
    return cassandraOperation.insertRecord(KEY_SPACE_NAME, TABLE_NAME, ownershipMap, context);
  }

  @Override
  public Response updateUserOwnershipTransfer(
      Map<String, Object> ownershipMap, RequestContext context) {
    return cassandraOperation.updateRecord(KEY_SPACE_NAME, TABLE_NAME, ownershipMap, context);
  }

  @Override
  public Map<String, Object> getUserOwnershipTransferDetailsById(
      String userId, String organisationId, RequestContext context) {
    Map<String, Object> dbRequestMap = new LinkedHashMap<>(3);
    dbRequestMap.put(JsonKey.ORGANISATION_ID, organisationId);
    dbRequestMap.put(JsonKey.USER_ID, userId);
    Response response =
        cassandraOperation.getRecordsByCompositeKey(
            KEY_SPACE_NAME, TABLE_NAME, dbRequestMap, context);
    List<Map<String, Object>> responseList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (CollectionUtils.isNotEmpty(responseList)) {
      return responseList.get(0);
    }
    return null;
  }
}
