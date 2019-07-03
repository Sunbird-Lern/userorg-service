/** */
package org.sunbird.learner.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.helper.ServiceFactory;

/**
 * This class will handle the data cache.
 *
 * @author Amit Kumar
 */
public class DataCacheHandler implements Runnable {
  /**
   * pageMap is the map of (orgId:pageName) and page Object (i.e map of string , object) sectionMap
   * is the map of section Id and section Object (i.e map of string , object)
   */
  private static Map<String, Map<String, Object>> pageMap = new ConcurrentHashMap<>();

  private static Map<String, Map<String, Object>> sectionMap = new ConcurrentHashMap<>();
  private static Map<String, Object> roleMap = new ConcurrentHashMap<>();
  private static Map<String, String> orgTypeMap = new ConcurrentHashMap<>();
  private static Map<String, String> configSettings = new ConcurrentHashMap<>();
  private static Map<String, Map<String, List<Map<String, String>>>> frameworkCategoriesMap =
      new ConcurrentHashMap<>();
  private static Map<String, List<String>> frameworkFieldsConfig = new ConcurrentHashMap<>();
  private static Map<String, List<String>> hashtagIdFrameworkIdMap = new HashMap<>();
  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private static final String KEY_SPACE_NAME = "sunbird";

  @Override
  public void run() {
    ProjectLogger.log("DataCacheHandler:run: Cache refresh started.", LoggerEnum.INFO.name());
    cache(pageMap, "page_management");
    cache(sectionMap, "page_section");
    roleCache(roleMap);
    orgTypeCache(orgTypeMap);
    cacheSystemConfig(configSettings);
    ProjectLogger.log("DataCacheHandler:run: Cache refresh completed.", LoggerEnum.INFO.name());
  }

  @SuppressWarnings("unchecked")
  private void cacheSystemConfig(Map<String, String> configSettings) {
    Response response =
        cassandraOperation.getAllRecords(KEY_SPACE_NAME, JsonKey.SYSTEM_SETTINGS_DB);
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

  @SuppressWarnings("unchecked")
  private void cache(Map<String, Map<String, Object>> map, String tableName) {
    try {
      Response response = cassandraOperation.getAllRecords(KEY_SPACE_NAME, tableName);
      List<Map<String, Object>> responseList =
          (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
      if (null != responseList && !responseList.isEmpty()) {
        for (Map<String, Object> resultMap : responseList) {
          if (tableName.equalsIgnoreCase(JsonKey.PAGE_SECTION)) {
            map.put((String) resultMap.get(JsonKey.ID), resultMap);
          } else {
            String orgId =
                (((String) resultMap.get(JsonKey.ORGANISATION_ID)) == null
                    ? "NA"
                    : (String) resultMap.get(JsonKey.ORGANISATION_ID));
            map.put(orgId + ":" + ((String) resultMap.get(JsonKey.PAGE_NAME)), resultMap);
          }
        }
      }
      ProjectLogger.log("pagemap keyset " + map.keySet());
      ProjectLogger.log(tableName + " cache size: " + map.size(), LoggerEnum.INFO.name());
    } catch (Exception e) {
      ProjectLogger.log(
          "DataCacheHandler:cache: Exception in retrieving page section " + e.getMessage(), e);
    }
  }

  /** @return the pageMap */
  public static Map<String, Map<String, Object>> getPageMap() {
    return pageMap;
  }

  /** @param pageMap the pageMap to set */
  public static void setPageMap(Map<String, Map<String, Object>> pageMap) {
    DataCacheHandler.pageMap = pageMap;
  }

  /** @return the sectionMap */
  public static Map<String, Map<String, Object>> getSectionMap() {
    return sectionMap;
  }

  /** @param sectionMap the sectionMap to set */
  public static void setSectionMap(Map<String, Map<String, Object>> sectionMap) {
    DataCacheHandler.sectionMap = sectionMap;
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
}
