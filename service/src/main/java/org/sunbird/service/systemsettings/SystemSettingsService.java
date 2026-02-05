package org.sunbird.service.systemsettings;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.MapUtils;
import org.sunbird.dao.systemsettings.impl.SystemSettingDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.model.systemsettings.SystemSetting;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.util.DataCacheHandler;

public class SystemSettingsService {

  private final LoggerUtil logger = new LoggerUtil(SystemSettingsService.class);
  private final SystemSettingDaoImpl systemSettingDaoImpl = new SystemSettingDaoImpl();

  public SystemSetting getSystemSettingByKey(String key, RequestContext context) {
    String value = DataCacheHandler.getConfigSettings().get(key);
    SystemSetting setting;
    if (value != null) {
      setting = new SystemSetting(key, key, value);
    } else {
      setting = systemSettingDaoImpl.readByField(key, context);
      if (null == setting) {
        throw new ProjectCommonException(
            ResponseCode.resourceNotFound,
            ResponseCode.resourceNotFound.getErrorMessage(),
            ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
      }
      DataCacheHandler.getConfigSettings().put(key, setting.getValue());
    }
    return setting;
  }

  public List<SystemSetting> getAllSystemSettings(RequestContext context) {
    Map<String, String> systemSettings = DataCacheHandler.getConfigSettings();
    List<SystemSetting> allSystemSettings = null;
    if (MapUtils.isNotEmpty(systemSettings)) {
      allSystemSettings = new ArrayList<>();
      for (Map.Entry<String, String> setting : systemSettings.entrySet()) {
        allSystemSettings.add(
            new SystemSetting(setting.getKey(), setting.getKey(), setting.getValue()));
      }
    } else {
      allSystemSettings = systemSettingDaoImpl.readAll(context);
    }
    return allSystemSettings;
  }

  public Response setSystemSettings(Map<String, Object> request, RequestContext context) {
    ObjectMapper mapper = new ObjectMapper();
    SystemSetting systemSetting = mapper.convertValue(request, SystemSetting.class);
    return systemSettingDaoImpl.write(systemSetting, context);
  }

  public <T> T getSystemSettingByFieldAndKey(
      String field, String key, TypeReference typeReference, RequestContext context) {
    SystemSetting systemSetting = getSystemSettingByKey(field, context);
    ObjectMapper objectMapper = new ObjectMapper();
    if (systemSetting != null) {
      try {
        Map<String, Object> valueMap = objectMapper.readValue(systemSetting.getValue(), Map.class);
        String[] keys = key.split("\\.");
        int numKeys = keys.length;
        for (int i = 0; i < numKeys - 1; i++) {
          valueMap = objectMapper.convertValue(valueMap.get(keys[i]), Map.class);
        }
        return (T) objectMapper.convertValue(valueMap.get(keys[numKeys - 1]), typeReference);
      } catch (Exception e) {
        logger.error(
            context,
            "SystemSettingsService:getSystemSettingByFieldAndKey: Exception occurred with error message = "
                + e.getMessage(),
            e);
      }
    }
    return null;
  }
}
