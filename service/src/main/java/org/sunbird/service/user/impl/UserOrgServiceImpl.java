package org.sunbird.service.user.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.dao.user.UserOrgDao;
import org.sunbird.dao.user.impl.UserOrgDaoImpl;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.user.UserOrgService;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.Util;

public class UserOrgServiceImpl implements UserOrgService {
  private static UserOrgServiceImpl userOrgService = null;
  private static LoggerUtil logger = new LoggerUtil(UserOrgServiceImpl.class);
  private UserOrgDao userOrgDao = UserOrgDaoImpl.getInstance();

  private static CassandraOperation cassandraOperation = ServiceFactory.getInstance();

  public static UserOrgService getInstance() {
    if (userOrgService == null) {
      userOrgService = new UserOrgServiceImpl();
    }
    return userOrgService;
  }

  @Override
  public void registerUserToOrg(Map<String, Object> userMap, RequestContext context) {
    Map<String, Object> reqMap = new WeakHashMap<>();
    reqMap.put(JsonKey.ID, ProjectUtil.getUniqueIdFromTimestamp(1));
    reqMap.put(JsonKey.USER_ID, userMap.get(JsonKey.ID));
    reqMap.put(JsonKey.ORGANISATION_ID, userMap.get(JsonKey.ORGANISATION_ID));
    reqMap.put(JsonKey.ORG_JOIN_DATE, ProjectUtil.getFormattedDate());
    reqMap.put(JsonKey.IS_DELETED, false);
    reqMap.put(JsonKey.ASSOCIATION_TYPE, userMap.get(JsonKey.ASSOCIATION_TYPE));
    if (StringUtils.isNotEmpty((String) userMap.get(JsonKey.HASHTAGID))) {
      reqMap.put(JsonKey.HASHTAGID, userMap.get(JsonKey.HASHTAGID));
    }
    Util.DbInfo usrOrgDb = Util.dbInfoMap.get(JsonKey.USER_ORG_DB);
    try {
      cassandraOperation.insertRecord(
          usrOrgDb.getKeySpace(), usrOrgDb.getTableName(), reqMap, context);
    } catch (Exception e) {
      logger.error(context, e.getMessage(), e);
    }
  }

  public List<Map<String, Object>> getUserOrgListByUserId(String userId, RequestContext context) {
    Response response = userOrgDao.getUserOrgListByUserId(userId, context);
    return (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
  }

  public void deleteUserOrgMapping(
          List<Map<String, Object>> userOrgList, RequestContext context){
    userOrgDao.deleteUserOrgMapping(userOrgList, context);
  }
}
