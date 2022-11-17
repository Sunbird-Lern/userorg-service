package org.sunbird.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.RequestContext;

public class FormApiUtil {
  public static Map<String, Object> getProfileConfig(String stateCode, RequestContext context) {
    Map<String, Map<String, Object>> stateProfileConfigMap =
            DataCacheHandler.getFormApiDataConfigMap();
    if (MapUtils.isEmpty(stateProfileConfigMap)
            || MapUtils.isEmpty(stateProfileConfigMap.get(stateCode))) {
      Map<String, Object> profileConfigMap;
      if (Boolean.parseBoolean(ProjectUtil.getConfigValue(JsonKey.IS_FORM_VALIDATION_REQUIRED))) {
        profileConfigMap = FormApiUtilHandler.getFormApiConfig(stateCode, context);
      }else{
        profileConfigMap = getFormApiConfig();
      }
      if (MapUtils.isNotEmpty(profileConfigMap)) {
        stateProfileConfigMap.put(stateCode, profileConfigMap);
      }
    }
    return stateProfileConfigMap.get(stateCode);
  }

  public static Map<String, List<String>> getUserTypeConfig(Map<String, Object> formData) {
    Map<String, List<String>> userTypeOrSubTypeConfigMap = new HashMap<>();
    if (MapUtils.isNotEmpty(formData)) {
      Map<String, Object> formDataMap = (Map<String, Object>) formData.get(JsonKey.FORM);
      if (MapUtils.isNotEmpty(formDataMap)) {
        Map<String, Object> dataMap = (Map<String, Object>) formDataMap.get(JsonKey.DATA);
        if (MapUtils.isNotEmpty(dataMap)) {
          List<Map<String, Object>> fields =
              (List<Map<String, Object>>) dataMap.get(JsonKey.FIELDS);
          for (Map<String, Object> field : fields) {
            if (JsonKey.PERSONA.equals(field.get(JsonKey.CODE))) {
              Map<String, Object> childrenMap = (Map<String, Object>) field.get(JsonKey.CHILDREN);
              for (Map.Entry<String, Object> entryItr : childrenMap.entrySet()) {
                String userType = entryItr.getKey();
                List<Map<String, Object>> personaConfigLists =
                    (List<Map<String, Object>>) entryItr.getValue();
                List<String> userSubTypes = new ArrayList<>();
                for (Map<String, Object> configMap : personaConfigLists) {
                  if (JsonKey.SUB_PERSONA.equals(configMap.get(JsonKey.CODE))) {
                    Map<String, Object> userSubPersonConfigMap =
                        (Map<String, Object>) configMap.get(JsonKey.TEMPLATE_OPTIONS);
                    if (MapUtils.isNotEmpty(userSubPersonConfigMap)) {
                      List<Map<String, Object>> userSubTypeLists =
                          (List<Map<String, Object>>) userSubPersonConfigMap.get(JsonKey.OPTIONS);
                      if (CollectionUtils.isNotEmpty(userSubTypeLists)) {
                        for (Map<String, Object> userSubType : userSubTypeLists) {
                          userSubTypes.add((String) userSubType.get(JsonKey.VALUE));
                        }
                      }
                    }
                  }
                }
                userTypeOrSubTypeConfigMap.put(userType, userSubTypes);
              }
            }
          }
        }
      }
    }
    return userTypeOrSubTypeConfigMap;
  }

  public static List<String> getLocationTypeConfigMap(Map<String, Object> formData) {
    List<String> locationTypeList = new ArrayList<>();
    if (MapUtils.isNotEmpty(formData)) {
      Map<String, Object> formDataMap = (Map<String, Object>) formData.get(JsonKey.FORM);
      if (MapUtils.isNotEmpty(formDataMap)) {
        Map<String, Object> dataMap = (Map<String, Object>) formDataMap.get(JsonKey.DATA);
        if (MapUtils.isNotEmpty(dataMap)) {
          List<Map<String, Object>> fields =
              (List<Map<String, Object>>) dataMap.get(JsonKey.FIELDS);
          for (Map<String, Object> field : fields) {
            if (JsonKey.PERSONA.equals(field.get(JsonKey.CODE))) {
              Map<String, Object> childrenMap = (Map<String, Object>) field.get(JsonKey.CHILDREN);
              if (MapUtils.isNotEmpty(childrenMap)) {
                Map.Entry<String, Object> entryItr = childrenMap.entrySet().iterator().next();
                List<Map<String, Object>> typeConfigList =
                    (List<Map<String, Object>>) entryItr.getValue();
                for (Map<String, Object> locationType : typeConfigList) {
                  locationTypeList.add((String) locationType.get(JsonKey.CODE));
                }
              }
            }
          }
        }
      }
    }
    return locationTypeList;
  }

  public static Map<String, Object> getFormApiConfig() {
    Map<String, Object> formData = new HashMap<>();
    Map<String, Object> formMap = new HashMap<>();
    Map<String, Object> dataMap = new HashMap<>();
    List<Map<String, Object>> fieldsList = new ArrayList<>();
    Map<String, Object> field = new HashMap<>();
    Map<String, Object> children = new HashMap<>();
    List<Map<String, Object>> userTypeConfigList = new ArrayList<>();
    Map<String, Object> schoolConfig = new HashMap<>();
    schoolConfig.put(JsonKey.CODE, JsonKey.LOCATION_TYPE_SCHOOL);
    userTypeConfigList.add(schoolConfig);
    children.put("teacher", userTypeConfigList);
    field.put(JsonKey.CODE, JsonKey.PERSONA);
    field.put(JsonKey.CHILDREN, children);
    fieldsList.add(field);
    dataMap.put(JsonKey.FIELDS, fieldsList);
    formMap.put(JsonKey.DATA, dataMap);
    formData.put(JsonKey.FORM, formMap);
    return formData;
  }
}
