package org.sunbird.service.systemsettings;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.MapUtils;
import org.sunbird.dao.systemsettings.impl.SystemSettingDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.model.systemsettings.SystemSetting;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.util.DataCacheHandler;

public class SystemSettingsService {

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
            ResponseCode.resourceNotFound.getErrorCode(),
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
}
