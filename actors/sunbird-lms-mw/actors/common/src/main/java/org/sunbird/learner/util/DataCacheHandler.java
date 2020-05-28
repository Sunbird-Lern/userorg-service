/** */
package org.sunbird.learner.util;

import org.apache.commons.lang3.StringUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.helper.CassandraConnectionManager;
import org.sunbird.helper.CassandraConnectionMngrFactory;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.role.service.RoleService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * This class will handle the data cache.
 *
 * @author Amit Kumar
 */
public class DataCacheHandler implements Runnable {

  private static Map<String, Object> roleMap = new ConcurrentHashMap<>();
  private static Map<String, Object> telemetryPdata = new ConcurrentHashMap<>(3);
  private static Map<String, String> orgTypeMap = new ConcurrentHashMap<>();
  private static Map<String, String> configSettings = new ConcurrentHashMap<>();
  private static Map<String, Map<String, List<Map<String, String>>>> frameworkCategoriesMap =
      new ConcurrentHashMap<>();
  private static Map<String, List<String>> frameworkFieldsConfig = new ConcurrentHashMap<>();
  private static Map<String, List<String>> hashtagIdFrameworkIdMap = new HashMap<>();
  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private static final String KEY_SPACE_NAME = Util.KEY_SPACE_NAME;
  private static Response roleCacheResponse;
  private static List<String> sunbirdPluginTableList = null;
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
    ProjectLogger.log("DataCacheHandler:run: Cache refresh started.", LoggerEnum.INFO.name());
    roleCache(roleMap);
    orgTypeCache(orgTypeMap);
    cacheSystemConfig(configSettings);
    cacheRoleForRead();
    cacheTelemetryPdata(telemetryPdata);
    createSunbirdPluginTableList();
    initLocationOrderMap();
    ProjectLogger.log("DataCacheHandler:run: Cache refresh completed.", LoggerEnum.INFO.name());
  }

  private void initLocationOrderMap() {
        if (orderMap == null) {
            orderMap = new HashMap<>();
            List<String> subTypeList = Arrays.asList(ProjectUtil.getConfigValue("sunbird_valid_location_types").split(";"));
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


  public void createSunbirdPluginTableList() {
      try {
          CassandraConnectionManager manager =
                  CassandraConnectionMngrFactory.getInstance();
          sunbirdPluginTableList = manager.getTableList(JsonKey.SUNBIRD_PLUGIN);
      } catch (Exception e) {
           ProjectLogger.log("Error occurred" + e.getMessage(), e);
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
        cassandraOperation.getAllRecords(KEY_SPACE_NAME, JsonKey.SYSTEM_SETTINGS_DB);
      ProjectLogger.log("DataCacheHandler:cacheSystemConfig: Cache system setting fields"+ response.getResult(), LoggerEnum.INFO.name());
    List<Map<String, Object>> responseList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (null != responseList && !responseList.isEmpty()) {
      for (Map<String, Object> resultMap : responseList) {
        if (((String) resultMap.get(JsonKey.FIELD)).equalsIgnoreCase(JsonKey.PHONE_UNIQUE)
            && StringUtils.isBlank((String) resultMap.get(JsonKey.VALUE))) {
          configSettings.put(((String) resultMap.get(JsonKey.FIELD)), String.valueOf(false));
        } else if (((String) resultMap.get(JsonKey.FIELD)).equalsIgnoreCase(JsonKey.EMAIL_UNIQUE)
            && StringUtils.isBlank((String) resultMap.get(JsonKey.VALUE))) {
          configSettings.put(((String) resultMap.get(JsonKey.FIELD)), String.valueOf(false));
        } else {
          configSettings.put(
              ((String) resultMap.get(JsonKey.FIELD)), (String) resultMap.get(JsonKey.VALUE));
        }
      }
    } else {
      configSettings.put(JsonKey.PHONE_UNIQUE, String.valueOf(false));
      configSettings.put(JsonKey.EMAIL_UNIQUE, String.valueOf(false));
    }
  }

  @SuppressWarnings("unchecked")
  private void orgTypeCache(Map<String, String> orgTypeMap) {
    Response response = cassandraOperation.getAllRecords(KEY_SPACE_NAME, JsonKey.ORG_TYPE_DB);
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
    Response response = cassandraOperation.getAllRecords(KEY_SPACE_NAME, JsonKey.ROLE_GROUP);
    List<Map<String, Object>> responseList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (null != responseList && !responseList.isEmpty()) {
      for (Map<String, Object> resultMap : responseList) {
        roleMap.put((String) resultMap.get(JsonKey.ID), resultMap.get(JsonKey.NAME));
      }
    }
    Response response2 = cassandraOperation.getAllRecords(KEY_SPACE_NAME, JsonKey.ROLE);
    List<Map<String, Object>> responseList2 =
        (List<Map<String, Object>>) response2.get(JsonKey.RESPONSE);
    if (null != responseList2 && !responseList2.isEmpty()) {
      for (Map<String, Object> resultMap2 : responseList2) {
        roleMap.put((String) resultMap2.get(JsonKey.ID), resultMap2.get(JsonKey.NAME));
      }
    }
  }
  /** @return the roleMap */
  public static Map<String, Object> getRoleMap() {
    return roleMap;
  }

  /** @param roleMap the roleMap to set */
  public static void setRoleMap(Map<String, Object> roleMap) {
    DataCacheHandler.roleMap = roleMap;
  }

  /** @return the orgTypeMap */
  public static Map<String, String> getOrgTypeMap() {
    return orgTypeMap;
  }

  /** @param orgTypeMap the orgTypeMap to set */
  public static void setOrgTypeMap(Map<String, String> orgTypeMap) {
    DataCacheHandler.orgTypeMap = orgTypeMap;
  }

  /** @return the configSettings */
  public static Map<String, String> getConfigSettings() {
    return configSettings;
  }

  /** @param configSettings the configSettings to set */
  public static void setConfigSettings(Map<String, String> configSettings) {
    DataCacheHandler.configSettings = configSettings;
  }

  public static Map<String, Map<String, List<Map<String, String>>>> getFrameworkCategoriesMap() {
    return frameworkCategoriesMap;
  }

  public static void setFrameworkCategoriesMap(
      Map<String, Map<String, List<Map<String, String>>>> frameworkCategoriesMap) {
    DataCacheHandler.frameworkCategoriesMap = frameworkCategoriesMap;
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

  public static void setHashtagIdFrameworkIdMap(Map<String, List<String>> hashtagIdFrameworkIdMap) {
    DataCacheHandler.hashtagIdFrameworkIdMap = hashtagIdFrameworkIdMap;
  }

  public static Map<String, List<String>> getHashtagIdFrameworkIdMap() {
    return hashtagIdFrameworkIdMap;
  }

  public static void updateHashtagIdFrameworkIdMap(String hashtagId, List<String> frameworkIds) {
    DataCacheHandler.hashtagIdFrameworkIdMap.put(hashtagId, frameworkIds);
  }

  public static List<String> getSunbirdPluginTableList() {return sunbirdPluginTableList;}

  public static Map<String, Integer> getLocationOrderMap() {return orderMap;}
}
