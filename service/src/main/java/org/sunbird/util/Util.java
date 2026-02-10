package org.sunbird.util;

import java.util.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.datasecurity.DataMaskingService;
import org.sunbird.datasecurity.DecryptionService;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.common.ProjectUtil;
import org.sunbird.request.Request;
import org.sunbird.response.Response;

/**
 * Utility class for actors
 *
 * @author arvind .
 */
public final class Util {

  public static final Map<String, DbInfo> dbInfoMap = new HashMap<>();
  private static final String KEY_SPACE_NAME = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE);
  private static final CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private static final DecryptionService decService =
      org.sunbird.datasecurity.impl.ServiceFactory.getDecryptionServiceInstance();
  private static final DataMaskingService maskingService =
      org.sunbird.datasecurity.impl.ServiceFactory.getMaskingServiceInstance();

  static {
    initializeDBProperty();
  }

  private Util() {}

  /** This method will initialize the cassandra data base property */
  private static void initializeDBProperty() {
    // setting db info (keyspace , table) into static map
    // this map will be used during cassandra data base interaction.
    // this map will have each DB name and it's corresponding keyspace and table
    // name.
    dbInfoMap.put(JsonKey.USER_DB, getDbInfoObject(KEY_SPACE_NAME, "user"));
    dbInfoMap.put(JsonKey.ORG_DB, getDbInfoObject(KEY_SPACE_NAME, "organisation"));
    dbInfoMap.put(JsonKey.ROLE, getDbInfoObject(KEY_SPACE_NAME, "role"));
    dbInfoMap.put(JsonKey.URL_ACTION, getDbInfoObject(KEY_SPACE_NAME, "url_action"));
    dbInfoMap.put(JsonKey.ACTION_GROUP, getDbInfoObject(KEY_SPACE_NAME, "action_group"));
    dbInfoMap.put(JsonKey.USER_ACTION_ROLE, getDbInfoObject(KEY_SPACE_NAME, "user_action_role"));
    dbInfoMap.put(JsonKey.ROLE_GROUP, getDbInfoObject(KEY_SPACE_NAME, "role_group"));
    dbInfoMap.put(JsonKey.USER_ORG_DB, getDbInfoObject(KEY_SPACE_NAME, "user_organisation"));
    dbInfoMap.put(JsonKey.BULK_OP_DB, getDbInfoObject(KEY_SPACE_NAME, "bulk_upload_process"));
    dbInfoMap.put(JsonKey.USER_NOTES_DB, getDbInfoObject(KEY_SPACE_NAME, "user_notes"));
    dbInfoMap.put(
        JsonKey.TENANT_PREFERENCE_DB, getDbInfoObject(KEY_SPACE_NAME, "tenant_preference"));
    dbInfoMap.put(JsonKey.SYSTEM_SETTINGS_DB, getDbInfoObject(KEY_SPACE_NAME, "system_settings"));
    dbInfoMap.put(JsonKey.USER_CERT, getDbInfoObject(KEY_SPACE_NAME, JsonKey.USER_CERT));
    dbInfoMap.put(JsonKey.USER_FEED_DB, getDbInfoObject(KEY_SPACE_NAME, JsonKey.USER_FEED_DB));
    dbInfoMap.put(
        JsonKey.USR_DECLARATION_TABLE,
        getDbInfoObject(KEY_SPACE_NAME, JsonKey.USR_DECLARATION_TABLE));
    dbInfoMap.put(
        JsonKey.TENANT_PREFERENCE_V2, getDbInfoObject(KEY_SPACE_NAME, "tenant_preference_v2"));

    dbInfoMap.put(JsonKey.USER_LOOKUP, getDbInfoObject(KEY_SPACE_NAME, "user_lookup"));
    dbInfoMap.put(JsonKey.LOCATION, getDbInfoObject(KEY_SPACE_NAME, JsonKey.LOCATION));
    dbInfoMap.put(JsonKey.USER_ROLES, getDbInfoObject(KEY_SPACE_NAME, JsonKey.USER_ROLES));
  }

  private static DbInfo getDbInfoObject(String keySpace, String table) {
    DbInfo dbInfo = new DbInfo();
    dbInfo.setKeySpace(keySpace);
    dbInfo.setTableName(table);
    return dbInfo;
  }

  /** class to hold cassandra db info. */
  public static class DbInfo {
    private String keySpace;
    private String tableName;

    /** No-arg constructor */
    DbInfo() {}

    public String getKeySpace() {
      return keySpace;
    }

    public void setKeySpace(String keySpace) {
      this.keySpace = keySpace;
    }

    public String getTableName() {
      return tableName;
    }

    public void setTableName(String tableName) {
      this.tableName = tableName;
    }
  }

  public static void initializeContext(Request request, String env) {
    Map<String, Object> requestContext = request.getContext();
    env = StringUtils.isNotBlank(env) ? env : "";
    requestContext.put(JsonKey.ENV, env);
    requestContext.put(JsonKey.REQUEST_TYPE, JsonKey.API_CALL);
    if (JsonKey.USER.equalsIgnoreCase((String) request.getContext().get(JsonKey.ACTOR_TYPE))) {
      String requestedByUserId = (String) request.getContext().get(JsonKey.REQUESTED_BY);
      if (StringUtils.isNotBlank(requestedByUserId)) {
        Util.DbInfo usrDbInfo = dbInfoMap.get(JsonKey.USER_DB);
        Response userResponse =
            cassandraOperation.getRecordById(
                usrDbInfo.getKeySpace(),
                usrDbInfo.getTableName(),
                (String) request.getContext().get(JsonKey.REQUESTED_BY),
                request.getRequestContext());
        List<Map<String, Object>> userList =
            (List<Map<String, Object>>) userResponse.get(JsonKey.RESPONSE);
        if (CollectionUtils.isNotEmpty(userList)) {
          Map<String, Object> result = userList.get(0);
          if (result != null) {
            String rootOrgId = (String) result.get(JsonKey.ROOT_ORG_ID);
            if (StringUtils.isNotBlank(rootOrgId)) {
              Map<String, String> rollup = new HashMap<>();

              rollup.put("l1", rootOrgId);
              requestContext.put(JsonKey.ROLLUP, rollup);
            }
          }
        }
      }
    }
  }

  public static void addMaskEmailAndPhone(Map<String, Object> userMap) {
    String phone = (String) userMap.get(JsonKey.PHONE);
    String email = (String) userMap.get(JsonKey.EMAIL);
    userMap.put(JsonKey.ENC_PHONE, phone);
    userMap.put(JsonKey.ENC_EMAIL, email);
    if (!StringUtils.isBlank(phone)) {
      userMap.put(JsonKey.PHONE, maskingService.maskPhone(decService.decryptData(phone, null)));
    }
    if (!StringUtils.isBlank(email)) {
      userMap.put(JsonKey.EMAIL, maskingService.maskEmail(decService.decryptData(email, null)));
    }
  }

  public static Map<String, Object> getUserDefaultValue() {
    Map<String, Object> user = new HashMap<>();
    user.put("avatar", null);
    user.put("gender", null);
    user.put("language", null);
    user.put("lastLoginTime", null);
    user.put("location", null);
    user.put("profileSummary", null);
    user.put("profileVisibility", null);
    user.put("tempPassword", null);
    user.put("thumbnail", null);
    user.put("registryId", null);
    user.put("accesscode", null);
    user.put("webPages", null);
    user.put("currentLoginTime", null);
    user.put("password", null);
    user.put("loginId", null);
    user.put(JsonKey.EMAIL_VERIFIED, true);
    user.put(JsonKey.PHONE_VERIFIED, true);
    return user;
  }

  public static Map<String, Object> getOrgDefaultValue() {
    Map<String, Object> org = new HashMap<>();
    org.put("dateTime", null);
    org.put("preferredLanguage", null);
    org.put("approvedBy", null);
    org.put("addressId", null);
    org.put("approvedDate", null);
    org.put("communityId", null);
    org.put("isApproved", null);
    org.put("locationId", null);
    org.put("noOfMembers", null);
    org.put("orgCode", null);
    org.put("theme", null);
    org.put("thumbnail", null);
    org.put("isDefault", null);
    org.put("parentOrgId", null);
    org.put("orgTypeId", null);
    org.put("orgType", null);
    return org;
  }
}
