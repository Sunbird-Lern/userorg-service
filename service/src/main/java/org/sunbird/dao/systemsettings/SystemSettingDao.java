package org.sunbird.dao.systemsettings;

import java.util.List;
import org.sunbird.model.systemsettings.SystemSetting;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

public interface SystemSettingDao {
  /**
   * Update system setting.
   *
   * @param systemSetting Setting information
   * @param context
   * @return Response containing setting identifier.
   */
  Response write(SystemSetting systemSetting, RequestContext context);

  /**
   * Read system setting for given identifier.
   *
   * @param id System setting identifier
   * @param context
   * @return System setting information
   */
  SystemSetting readById(String id, RequestContext context);

  /**
   * Read system setting for given field name.
   *
   * @param field System setting field name
   * @param context
   * @return System setting information
   */
  SystemSetting readByField(String field, RequestContext context);

  /**
   * Read all system settings.
   *
   * @return Response containing list of system settings.
   * @param context
   */
  List<SystemSetting> readAll(RequestContext context);
}
