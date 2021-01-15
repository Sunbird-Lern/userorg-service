/** */
package org.sunbird.learner.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerUtil;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.RequestContext;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.role.service.RoleService;

/**
 * This class will handle the data cache.
 *
 * @author Amit Kumar
 */
public class DataCacheHandler implements Runnable {
  private static LoggerUtil logger = new LoggerUtil(DataCacheHandler.class);

  private static Map<String, Object> roleMap = new ConcurrentHashMap<>();
  private static Map<String, Object> telemetryPdata = new ConcurrentHashMap<>(3);
  private static Map<String, String> orgTypeMap = new ConcurrentHashMap<>();
  private static Map<String, String> configSettings = new ConcurrentHashMap<>();
  private static Map<String, Map<String, List<Map<String, String>>>> frameworkCategoriesMap =
      new ConcurrentHashMap<>();
  private static Map<String, List<String>> frameworkFieldsConfig = new ConcurrentHashMap<>();
  private static Map<String, List<String>> hashtagIdFrameworkIdMap = new ConcurrentHashMap<>();
  private static Map<String, Map<String, List<String>>> userTypeOrSubTypeConfigMap =
      new ConcurrentHashMap<>();
  private static Map<String, List<String>> stateLocationTypeConfigMap = new ConcurrentHashMap<>();
  private static Map<String, Map<String, Object>> formApiDataConfigMap = new ConcurrentHashMap<>();
  private static List<Map<String, String>> roleList = new CopyOnWriteArrayList<>();
  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private static final String KEY_SPACE_NAME = Util.KEY_SPACE_NAME;
  private static Response roleCacheResponse;
  private static Map<String, Integer> orderMap;
  public static String[] bulkUserAllowedFields = {
    JsonKey.FIRST_NAME,
    JsonKey.LAST_NAME,
    JsonKey.PHONE,
    JsonKey.COUNTRY_CODE,
    JsonKey.EMAIL,
    JsonKey.USERNAME,
    JsonKey.PHONE_VERIFIED,
    JsonKey.EMAIL_VERIFIED,
    JsonKey.ROLES,
    JsonKey.POSITION,
    JsonKey.GRADE,
    JsonKey.LOCATION,
    JsonKey.DOB,
    JsonKey.GENDER,
    JsonKey.LANGUAGE,
    JsonKey.PROFILE_SUMMARY,
    JsonKey.SUBJECT,
    JsonKey.WEB_PAGES,
    JsonKey.EXTERNAL_ID_PROVIDER,
    JsonKey.EXTERNAL_ID,
    JsonKey.EXTERNAL_ID_TYPE,
    JsonKey.EXTERNAL_IDS
  };
  public static String[] bulkOrgAllowedFields = {
    JsonKey.ORGANISATION_NAME,
    JsonKey.CHANNEL,
    JsonKey.IS_ROOT_ORG,
    JsonKey.PROVIDER,
    JsonKey.EXTERNAL_ID,
    JsonKey.DESCRIPTION,
    JsonKey.HOME_URL,
    JsonKey.ORG_CODE,
    JsonKey.ORG_TYPE,
    JsonKey.PREFERRED_LANGUAGE,
    JsonKey.THEME,
    JsonKey.CONTACT_DETAILS,
    JsonKey.LOC_ID,
    JsonKey.HASHTAGID,
    JsonKey.LOCATION_CODE
  };

  @Override
  public void run() {
    logger.info("DataCacheHandler:run: Cache refresh started.");
    roleCache(roleMap);
    orgTypeCache(orgTypeMap);
    cacheSystemConfig(configSettings);
    cacheRoleForRead();
    cacheTelemetryPdata(telemetryPdata);
    cacheFormApiDataConfig();
    cacheUserTypeOrSubTypeConfig();
    cacheLocationCodeTypeConfig();
    initLocationOrderMap();
    logger.info("DataCacheHandler:run: Cache refresh completed.");
  }

  // Get form data config
  private void cacheFormApiDataConfig() {
    for (Map.Entry<String, Map<String, Object>> itr : formApiDataConfigMap.entrySet()) {
      String stateCode = itr.getKey();
      RequestContext reqContext = new RequestContext();
      reqContext.setReqId(UUID.randomUUID().toString());
      reqContext.setDebugEnabled("false");
      Map<String, Object> formDataMap = FormApiUtilHandler.getFormApiConfig(stateCode, reqContext);
      formApiDataConfigMap.put(stateCode, formDataMap);
    }
  }

  // Update userType or SubType cache for the state which are fetched from form api
  private void cacheUserTypeOrSubTypeConfig() {
    if (MapUtils.isNotEmpty(formApiDataConfigMap)) {
      for (Map.Entry<String, Map<String, Object>> itr : formApiDataConfigMap.entrySet()) {
        String stateCode = itr.getKey();
        Map<String, Object> formData = itr.getValue();
        Map<String, List<String>> userTypeConfigMap = FormApiUtil.getUserTypeConfig(formData);
        userTypeOrSubTypeConfigMap.put(stateCode, userTypeConfigMap);
      }
    }
  }

  // Update Location Code Type cache for the state which are fetched from form api
  private void cacheLocationCodeTypeConfig() {
    if (MapUtils.isNotEmpty(formApiDataConfigMap)) {
      for (Map.Entry<String, Map<String, Object>> itr : formApiDataConfigMap.entrySet()) {
        String stateCode = itr.getKey();
        Map<String, Object> formData = itr.getValue();
        List<String> locationCodeLists = FormApiUtil.getLocationTypeConfigMap(formData);
        stateLocationTypeConfigMap.put(stateCode, locationCodeLists);
      }
    }
  }

  private void initLocationOrderMap() {
    if (orderMap == null) {
      orderMap = new HashMap<>();
      List<String> subTypeList =
          Arrays.asList(ProjectUtil.getConfigValue("sunbird_valid_location_types").split(";"));
      for (String str : subTypeList) {
        List<String> typeList =
            (((Arrays.asList(str.split(","))).stream().map(String::toLowerCase))
                .collect(Collectors.toList()));
        for (int i = 0; i < typeList.size(); i++) {
          orderMap.put(typeList.get(i), i);
        }
      }
    }
  }

  private void cacheTelemetryPdata(Map<String, Object> telemetryPdata) {
    telemetryPdata.put("telemetry_pdata_id", ProjectUtil.getConfigValue("telemetry_pdata_id"));
    telemetryPdata.put("telemetry_pdata_pid", ProjectUtil.getConfigValue("telemetry_pdata_pid"));
    telemetryPdata.put("telemetry_pdata_ver", ProjectUtil.getConfigValue("telemetry_pdata_ver"));
  }

  private void cacheRoleForRead() {
    roleCacheResponse = RoleService.getUserRoles();
  }

  public static Response getRoleResponse() {
    return roleCacheResponse;
  }

  public static Map<String, Object> getTelemetryPdata() {
    return telemetryPdata;
  }

  public static void setRoleResponse(Response response) {
    if (response != null) roleCacheResponse = response;
  }

  @SuppressWarnings("unchecked")
  private void cacheSystemConfig(Map<String, String> configSettings) {
    Response response =
        cassandraOperation.getAllRecords(KEY_SPACE_NAME, JsonKey.SYSTEM_SETTINGS_DB, null);
    logger.debug(
        "DataCacheHandler:cacheSystemConfig: Cache system setting fields" + response.getResult());
    List<Map<String, Object>> responseList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (null != responseList && !responseList.isEmpty()) {
      for (Map<String, Object> resultMap : responseList) {
        configSettings.put(
            ((String) resultMap.get(JsonKey.FIELD)), (String) resultMap.get(JsonKey.VALUE));
      }
    }
    configSettings.put(JsonKey.PHONE_UNIQUE, String.valueOf(true));
    configSettings.put(JsonKey.EMAIL_UNIQUE, String.valueOf(true));
  }

  @SuppressWarnings("unchecked")
  private void orgTypeCache(Map<String, String> orgTypeMap) {
    Response response = cassandraOperation.getAllRecords(KEY_SPACE_NAME, JsonKey.ORG_TYPE_DB, null);
    List<Map<String, Object>> responseList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (null != responseList && !responseList.isEmpty()) {
      for (Map<String, Object> resultMap : responseList) {
        orgTypeMap.put(
            ((String) resultMap.get(JsonKey.NAME)).toLowerCase(),
            (String) resultMap.get(JsonKey.ID));
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void roleCache(Map<String, Object> roleMap) {
    Response response = cassandraOperation.getAllRecords(KEY_SPACE_NAME, JsonKey.ROLE_GROUP, null);
    List<Map<String, Object>> responseList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    Set<String> roleSet = new HashSet<>();
    if (CollectionUtils.isNotEmpty(responseList)) {
      for (Map<String, Object> resultMap : responseList) {
        if (!roleSet.contains(((String) resultMap.get(JsonKey.ID)).trim())) {
          roleSet.add(((String) resultMap.get(JsonKey.ID)).trim());
          roleMap.put(
              ((String) resultMap.get(JsonKey.ID)).trim(),
              ((String) resultMap.get(JsonKey.NAME)).trim());
        }
      }
    }

    Response response2 = cassandraOperation.getAllRecords(KEY_SPACE_NAME, JsonKey.ROLE, null);
    List<Map<String, Object>> responseList2 =
        (List<Map<String, Object>>) response2.get(JsonKey.RESPONSE);
    if (CollectionUtils.isNotEmpty(responseList2)) {
      for (Map<String, Object> resultMap : responseList2) {
        if (!roleSet.contains(((String) resultMap.get(JsonKey.ID)).trim())) {
          roleSet.add(((String) resultMap.get(JsonKey.ID)).trim());
          roleMap.put(
              ((String) resultMap.get(JsonKey.ID)).trim(),
              ((String) resultMap.get(JsonKey.NAME)).trim());
        }
      }
    }

    roleMap
        .entrySet()
        .parallelStream()
        .forEach(
            (roleSetItem) -> {
              if (roleSet.contains(roleSetItem.getKey().trim())) {
                Map<String, String> role = new HashMap<>();
                role.put(JsonKey.ID, roleSetItem.getKey().trim());
                role.put(JsonKey.NAME, ((String) roleSetItem.getValue()).trim());
                roleList.add(role);
                roleSet.remove(roleSetItem.getKey().trim());
              }
            });
  }

  /** @return the roleMap */
  public static Map<String, Object> getRoleMap() {
    return roleMap;
  }

  /** @return the roleList */
  public static List<Map<String, String>> getUserReadRoleList() {
    return roleList;
  }

  /** @return the orgTypeMap */
  public static Map<String, String> getOrgTypeMap() {
    return orgTypeMap;
  }

  /** @return the configSettings */
  public static Map<String, String> getConfigSettings() {
    return configSettings;
  }

  public static Map<String, Map<String, List<String>>> getUserTypesConfig() {
    return userTypeOrSubTypeConfigMap;
  }

  public static Map<String, Map<String, List<Map<String, String>>>> getFrameworkCategoriesMap() {
    return frameworkCategoriesMap;
  }

  public static void setFrameworkFieldsConfig(Map<String, List<String>> frameworkFieldsConfig) {
    DataCacheHandler.frameworkFieldsConfig = frameworkFieldsConfig;
  }

  public static Map<String, List<String>> getFrameworkFieldsConfig() {
    return frameworkFieldsConfig;
  }

  public static void updateFrameworkCategoriesMap(
      String frameworkId, Map<String, List<Map<String, String>>> frameworkCacheMap) {
    DataCacheHandler.frameworkCategoriesMap.put(frameworkId, frameworkCacheMap);
  }

  public static Map<String, List<String>> getHashtagIdFrameworkIdMap() {
    return hashtagIdFrameworkIdMap;
  }

  public static Map<String, Integer> getLocationOrderMap() {
    return orderMap;
  }

  public static Map<String, List<String>> getLocationTypeConfig() {
    return stateLocationTypeConfigMap;
  }

  public static Map<String, Map<String, Object>> getFormApiDataConfigMap() {
    return formApiDataConfigMap;
  }
}
