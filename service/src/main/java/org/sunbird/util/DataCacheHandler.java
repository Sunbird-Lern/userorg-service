package org.sunbird.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.organisation.validator.OrgTypeValidator;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ProjectUtil;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.model.organisation.OrganisationType;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.role.RoleService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * This class will handle the data cache.
 *
 * @author Amit Kumar
 */
public class DataCacheHandler implements Runnable {
  private static LoggerUtil logger = new LoggerUtil(DataCacheHandler.class);

  private static Map<String, Object> roleMap = new ConcurrentHashMap<>();
  private static Map<String, Object> telemetryPdata = new ConcurrentHashMap<>(3);
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
  private static final String KEY_SPACE_NAME = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE);
  private static Response roleCacheResponse;
  private static Map<String, Integer> orderMap;
  public static String[] bulkUserAllowedFields = {
    JsonKey.FIRST_NAME,
    JsonKey.LAST_NAME,
    JsonKey.PHONE,
    JsonKey.COUNTRY_CODE,
    JsonKey.EMAIL,
    JsonKey.USERNAME,
    JsonKey.ROLES,
    JsonKey.POSITION,
    JsonKey.LOCATION,
    JsonKey.DOB,
    JsonKey.LANGUAGE,
    JsonKey.PROFILE_SUMMARY,
    JsonKey.SUBJECT,
    JsonKey.EXTERNAL_ID_PROVIDER,
    JsonKey.EXTERNAL_ID,
    JsonKey.EXTERNAL_ID_TYPE,
    JsonKey.EXTERNAL_IDS
  };
  public static String[] bulkOrgAllowedFields = {
    JsonKey.ORGANISATION_NAME,
    JsonKey.CHANNEL,
    JsonKey.IS_TENANT,
    JsonKey.PROVIDER,
    JsonKey.EXTERNAL_ID,
    JsonKey.DESCRIPTION,
    JsonKey.HOME_URL,
    JsonKey.ORG_TYPE,
    JsonKey.CONTACT_DETAILS,
    JsonKey.LOC_ID,
    JsonKey.LOCATION_CODE
  };

  @Override
  public void run() {
    try {
      logger.info("DataCacheHandler:run: Cache refresh started.");
      roleCache();
      cacheSystemConfig();
      cacheRoleForRead();
      cacheTelemetryPdata();
      cacheFormApiDataConfig();
      initLocationOrderMap();
      initOrgTypeMap();
      logger.info("DataCacheHandler:run: Cache refresh completed.");
    } catch(Exception e) {
      logger.error("Failed to initialize DataCacheHandler", e);
    }
  }

  // Get form data config
  private void cacheFormApiDataConfig() {
    formApiDataConfigMap = new ConcurrentHashMap<>();
    userTypeOrSubTypeConfigMap = new ConcurrentHashMap<>();
    stateLocationTypeConfigMap = new ConcurrentHashMap<>();

    for (Map.Entry<String, Map<String, Object>> itr : formApiDataConfigMap.entrySet()) {
      String stateCode = itr.getKey();
      RequestContext reqContext = new RequestContext();
      reqContext.setReqId(UUID.randomUUID().toString());
      reqContext.setDebugEnabled("false");
      Map<String, Object> formDataMap = FormApiUtilHandler.getFormApiConfig(stateCode, reqContext);
      logger.info(
          reqContext,
          String.format("Cache update for form api stateCode:%s is not found", stateCode));
      if (MapUtils.isNotEmpty(formDataMap)) {
        formApiDataConfigMap.put(stateCode, formDataMap);
        cacheUserTypeOrSubTypeConfig();
        cacheLocationCodeTypeConfig();
      }
    }
  }

  // Update userType or SubType cache for the state which are fetched from form api
  private void cacheUserTypeOrSubTypeConfig() {
    if (MapUtils.isNotEmpty(formApiDataConfigMap)) {
      for (Map.Entry<String, Map<String, Object>> itr : formApiDataConfigMap.entrySet()) {
        String stateCode = itr.getKey();
        Map<String, Object> formData = itr.getValue();
        Map<String, List<String>> userTypeConfigMap = FormApiUtil.getUserTypeConfig(formData);
        if (MapUtils.isNotEmpty(userTypeConfigMap)) {
          userTypeOrSubTypeConfigMap.put(stateCode, userTypeConfigMap);
        } else {
          userTypeOrSubTypeConfigMap.remove(stateCode);
        }
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
        if (CollectionUtils.isNotEmpty(locationCodeLists)) {
          stateLocationTypeConfigMap.put(stateCode, locationCodeLists);
        } else {
          stateLocationTypeConfigMap.remove(stateCode);
        }
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

  private void cacheTelemetryPdata() {
    String telemetryPdataVer = DataCacheHandler.getConfigSettings().get("telemetry_pdata_ver");
    if (StringUtils.isBlank(telemetryPdataVer)) {
      telemetryPdataVer = ProjectUtil.getConfigValue("telemetry_pdata_ver");
    }
    telemetryPdata.put("telemetry_pdata_id", ProjectUtil.getConfigValue("telemetry_pdata_id"));
    telemetryPdata.put("telemetry_pdata_pid", ProjectUtil.getConfigValue("telemetry_pdata_pid"));
    telemetryPdata.put("telemetry_pdata_ver", telemetryPdataVer);
  }

  private void cacheRoleForRead() {
    roleCacheResponse = new RoleService().getUserRoles(null);
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
  private void cacheSystemConfig() {
    Map<String, String> tempConfigSettings = new ConcurrentHashMap();
    Response response =
        cassandraOperation.getAllRecords(KEY_SPACE_NAME, JsonKey.SYSTEM_SETTINGS_DB, null);
    logger.debug(
        "DataCacheHandler:cacheSystemConfig: Cache system setting fields" + response.getResult());
    List<Map<String, Object>> responseList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (null != responseList && !responseList.isEmpty()) {
      for (Map<String, Object> resultMap : responseList) {
        tempConfigSettings.put(
            ((String) resultMap.get(JsonKey.FIELD)), (String) resultMap.get(JsonKey.VALUE));
      }
    }
    tempConfigSettings.put(JsonKey.PHONE_UNIQUE, String.valueOf(true));
    tempConfigSettings.put(JsonKey.EMAIL_UNIQUE, String.valueOf(true));
    updateFrameWorkCache(tempConfigSettings.get(JsonKey.USER_PROFILE_CONFIG));
    configSettings = tempConfigSettings;
  }

  private void updateFrameWorkCache(String userProfileConfig) {
    if (StringUtils.isNotBlank(userProfileConfig)) {
      try {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> valueMap = objectMapper.readValue(userProfileConfig, Map.class);
        setFrameworkFieldsConfig((Map<String, List<String>>) valueMap.get(JsonKey.FRAMEWORK));
      } catch (Exception ex) {
        logger.error("Exception occurred while parsing framework details.", ex);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void roleCache() {
    Map<String, Object> tempRoleMap = new ConcurrentHashMap();
    Response response = cassandraOperation.getAllRecords(KEY_SPACE_NAME, JsonKey.ROLE_GROUP, null);
    List<Map<String, Object>> responseList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    Set<String> roleSet = new HashSet<>();
    if (CollectionUtils.isNotEmpty(responseList)) {
      for (Map<String, Object> resultMap : responseList) {
        if (!roleSet.contains(((String) resultMap.get(JsonKey.ID)).trim())) {
          roleSet.add(((String) resultMap.get(JsonKey.ID)).trim());
          tempRoleMap.put(
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
          tempRoleMap.put(
              ((String) resultMap.get(JsonKey.ID)).trim(),
              ((String) resultMap.get(JsonKey.NAME)).trim());
        }
      }
    }
    List<Map<String, String>> tempRoleList = new CopyOnWriteArrayList<>();
    tempRoleMap
        .entrySet()
        .parallelStream()
        .forEach(
            (roleSetItem) -> {
              if (roleSet.contains(roleSetItem.getKey().trim())) {
                Map<String, String> role = new HashMap<>();
                role.put(JsonKey.ID, roleSetItem.getKey().trim());
                role.put(JsonKey.NAME, ((String) roleSetItem.getValue()).trim());
                tempRoleList.add(role);
                roleSet.remove(roleSetItem.getKey().trim());
              }
            });
    roleMap = tempRoleMap;
    roleList = tempRoleList;
  }

  /** @return the roleMap */
  public static Map<String, Object> getRoleMap() {
    return roleMap;
  }

  /** @return the roleList */
  public static List<Map<String, String>> getUserReadRoleList() {
    return roleList;
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

  private void initOrgTypeMap() {
    String orgTypeConfig = getConfigSettings().get(JsonKey.ORG_TYPE_CONFIG);
    logger.info("Using DataCacheHandler value. " + orgTypeConfig);
    Map<String, Object> orgTypeConfigMap;
    try {
      orgTypeConfigMap = (new ObjectMapper()).readValue(orgTypeConfig,
              new TypeReference<HashMap<String, Object>>() {
              });
      if (orgTypeConfigMap.containsKey(JsonKey.FIELDS)) {
        List<OrganisationType> orgTypeList = (new ObjectMapper()).convertValue(orgTypeConfigMap.get(JsonKey.FIELDS),
                new TypeReference<List<OrganisationType>>() {
                });
        OrgTypeValidator.getInstance().initializeOrgTypeFromCache(orgTypeList);
      }
    } catch (Exception e) {
      logger.error("Failed to Map orgTypeConfig to OrganisationType member", e);
    }
  }
}
