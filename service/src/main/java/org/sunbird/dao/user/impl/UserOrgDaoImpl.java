package org.sunbird.dao.user.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.dao.user.UserOrgDao;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.UserOrg;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.common.ProjectUtil;

public final class UserOrgDaoImpl implements UserOrgDao {

  private static final String TABLE_NAME = JsonKey.USER_ORG;
  private final CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private final ObjectMapper mapper = new ObjectMapper();

  private static UserOrgDaoImpl instance;

  private UserOrgDaoImpl() {}

  public static UserOrgDao getInstance() {
    if (instance == null) {
      // To make thread safe
      synchronized (UserOrgDaoImpl.class) {
        // check again as multiple threads
        // can reach above step
        if (instance == null) instance = new UserOrgDaoImpl();
      }
    }
    return instance;
  }

  @Override
  public Response updateUserOrg(UserOrg userOrg, RequestContext context) {
    Map<String, Object> compositeKey = new LinkedHashMap<>(2);
    Map<String, Object> request = mapper.convertValue(userOrg, Map.class);
    compositeKey.put(JsonKey.USER_ID, request.remove(JsonKey.USER_ID));
    compositeKey.put(JsonKey.ORGANISATION_ID, request.remove(JsonKey.ORGANISATION_ID));
    return cassandraOperation.updateRecord(
            ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE), TABLE_NAME, request, compositeKey, context);
  }

  @Override
  public Response createUserOrg(UserOrg userOrg, RequestContext context) {
    return cassandraOperation.insertRecord(
            ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE), TABLE_NAME, mapper.convertValue(userOrg, Map.class), context);
  }

  @Override
  public Response getUserOrgListByUserId(String userId, RequestContext context) {
    Map<String, Object> compositeKey = new LinkedHashMap<>(2);
    compositeKey.put(JsonKey.USER_ID, userId);
    return cassandraOperation.getRecordById(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE), TABLE_NAME, compositeKey, context);
  }

  @Override
  public Response getUserOrgDetails(String userId, String organisationId, RequestContext context) {
    Map<String, Object> searchMap = new LinkedHashMap<>(2);
    searchMap.put(JsonKey.USER_ID, userId);
    if (StringUtils.isNotEmpty(organisationId)) {
      searchMap.put(JsonKey.ORGANISATION_ID, organisationId);
    }
    return cassandraOperation.getRecordsByCompositeKey(
            ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE), JsonKey.USER_ORG, searchMap, context);
  }

  @Override
  public Response insertRecord(Map reqMap, RequestContext context) {
    return cassandraOperation.insertRecord(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE), JsonKey.USER_ORG, reqMap, context);
  }

  public void deleteUserOrgMapping(List<Map<String, Object>> userOrgList, RequestContext context) {
    for (Map<String, Object> userOrg : userOrgList) {
      Map<String, String> compositeKey = new LinkedHashMap<>(2);
      compositeKey.put(JsonKey.USER_ID, (String) userOrg.get(JsonKey.USER_ID));
      compositeKey.put(JsonKey.ORGANISATION_ID, (String) userOrg.get(JsonKey.ORGANISATION_ID));
      cassandraOperation.deleteRecord(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE), JsonKey.USER_ORG, compositeKey, context);
    }
  }
}
