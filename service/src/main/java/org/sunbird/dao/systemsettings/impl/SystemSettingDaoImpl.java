package org.sunbird.dao.systemsettings.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.dao.systemsettings.SystemSettingDao;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.systemsettings.SystemSetting;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.common.ProjectUtil;

public class SystemSettingDaoImpl implements SystemSettingDao {

  private final CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private final ObjectMapper mapper = new ObjectMapper();
  private static final String KEYSPACE_NAME = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE);
  private static final String TABLE_NAME = JsonKey.SYSTEM_SETTINGS_DB;

  @Override
  public Response write(SystemSetting systemSetting, RequestContext context) {
    Map<String, Object> map = mapper.convertValue(systemSetting, Map.class);
    Response response = cassandraOperation.upsertRecord(KEYSPACE_NAME, TABLE_NAME, map, context);
    response.put(JsonKey.ID, map.get(JsonKey.ID));
    return response;
  }

  @Override
  public SystemSetting readById(String id, RequestContext context) {
    Response response = cassandraOperation.getRecordById(KEYSPACE_NAME, TABLE_NAME, id, context);
    List<Map<String, Object>> list = (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (CollectionUtils.isEmpty(list)) {
      return null;
    }
    return getSystemSetting(list);
  }

  @Override
  public SystemSetting readByField(String field, RequestContext context) {
    return readById(field, context);
  }

  @Override
  public List<SystemSetting> readAll(RequestContext context) {
    Response response = cassandraOperation.getAllRecords(KEYSPACE_NAME, TABLE_NAME, context);
    List<Map<String, Object>> list = (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    List<SystemSetting> systemSettings = new ArrayList<>();
    list.forEach(
        map -> {
          SystemSetting systemSetting = mapper.convertValue(map, SystemSetting.class);
          systemSettings.add(systemSetting);
        });
    return systemSettings;
  }

  private SystemSetting getSystemSetting(List<Map<String, Object>> list) {
    return mapper.convertValue(list.get(0), SystemSetting.class);
  }
}
