package org.sunbird.systemsettings.dao;

import java.util.List;
import org.sunbird.common.models.response.Response;
import org.sunbird.models.systemsetting.SystemSetting;

public interface SystemSettingDao {
  /**
   * Update system setting.
   *
   * @param systemSetting Setting information
   * @return Response containing setting identifier.
   */
  Response write(SystemSetting systemSetting);

  /**
   * Read system setting for given identifier.
   *
   * @param id System setting identifier
   * @return System setting information
   */
  SystemSetting readById(String id);

  /**
   * Read system setting for given field name.
   *
   * @param field System setting field name
   * @return System setting information
   */
  SystemSetting readByField(String field);

  /**
   * Read all system settings.
   *
   * @return Response containing list of system settings.
   */
  List<SystemSetting> readAll();
}
