package org.sunbird.dao.user.impl;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.MapUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.dao.user.UserSelfDeclarationDao;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.UserDeclareEntity;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.common.ProjectUtil;

public class UserSelfDeclarationDaoImpl implements UserSelfDeclarationDao {
  private final CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private static UserSelfDeclarationDao userSelfDeclarationDao = null;

  public static UserSelfDeclarationDao getInstance() {
    if (userSelfDeclarationDao == null) {
      userSelfDeclarationDao = new UserSelfDeclarationDaoImpl();
    }
    return userSelfDeclarationDao;
  }

  public void insertSelfDeclaredFields(Map<String, Object> extIdMap, RequestContext context) {

    cassandraOperation.insertRecord(
            ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE), JsonKey.USER_DECLARATION_DB, extIdMap, context);
  }

  public List<Map<String, Object>> getUserSelfDeclaredFields(
      UserDeclareEntity userDeclareEntity, RequestContext context) {
    List<Map<String, Object>> dbResExternalIds = new ArrayList<>();
    Map<String, Object> properties = new HashMap<>();
    properties.put(JsonKey.USER_ID, userDeclareEntity.getUserId());
    properties.put(JsonKey.ORG_ID, userDeclareEntity.getOrgId());
    properties.put(JsonKey.PERSONA, userDeclareEntity.getPersona());
    Response response =
        cassandraOperation.getRecordsByProperties(
                ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE), JsonKey.USER_DECLARATION_DB, properties, context);
    if (null != response && null != response.getResult()) {
      dbResExternalIds = (List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE);
    }
    return dbResExternalIds;
  }

  public UserDeclareEntity upsertUserSelfDeclaredFields(
      UserDeclareEntity userDeclareEntity, RequestContext context) {
    Map<String, Object> compositeKey = new HashMap<>();
    compositeKey.put(JsonKey.USER_ID, userDeclareEntity.getUserId());
    compositeKey.put(JsonKey.ORG_ID, userDeclareEntity.getOrgId());
    compositeKey.put(JsonKey.PERSONA, userDeclareEntity.getPersona());
    Map<String, Object> updateFieldsMap = new HashMap<>();
    if (MapUtils.isNotEmpty(userDeclareEntity.getUserInfo())) {
      updateFieldsMap.put(JsonKey.USER_INFO, userDeclareEntity.getUserInfo());
    }
    updateFieldsMap.put(JsonKey.STATUS, userDeclareEntity.getStatus());
    updateFieldsMap.put(JsonKey.ERROR_TYPE, userDeclareEntity.getErrorType());
    updateFieldsMap.put(JsonKey.UPDATED_BY, userDeclareEntity.getUpdatedBy());
    updateFieldsMap.put(
        JsonKey.UPDATED_ON, new Timestamp(Calendar.getInstance().getTime().getTime()));
    cassandraOperation.updateRecord(
            ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE), JsonKey.USER_DECLARATION_DB, updateFieldsMap, compositeKey, context);
    return userDeclareEntity;
  }

  public List<Map<String, Object>> getUserSelfDeclaredFields(
      String userId, RequestContext context) {
    List<Map<String, Object>> dbResExternalIds = new ArrayList<>();
    Map<String, Object> properties = new HashMap<>();
    properties.put(JsonKey.USER_ID, userId);
    Response response =
        cassandraOperation.getRecordById(
                ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE), JsonKey.USER_DECLARATION_DB, properties, context);
    if (null != response && null != response.getResult()) {
      dbResExternalIds = (List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE);
    }
    return dbResExternalIds;
  }

  public void deleteUserSelfDeclaredDetails(
      String userId, String orgId, String persona, RequestContext context) {
    Map<String, String> properties = new HashMap<>();
    properties.put(JsonKey.USER_ID, userId);
    properties.put(JsonKey.ORG_ID, orgId);
    properties.put(JsonKey.PERSONA, persona);
    cassandraOperation.deleteRecord(
            ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE), JsonKey.USER_DECLARATION_DB, properties, context);
  }

  public Response updateUserSelfDeclaredFields(
      Map<String, Object> updateFieldsMap,
      Map<String, Object> compositeKey,
      RequestContext context) {
    return cassandraOperation.updateRecord(
            ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE), JsonKey.USER_DECLARATION_DB, updateFieldsMap, compositeKey, context);
  }
}
