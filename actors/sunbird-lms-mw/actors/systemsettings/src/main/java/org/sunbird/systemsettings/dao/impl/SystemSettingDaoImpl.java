package org.sunbird.systemsettings.dao.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.models.systemsetting.SystemSetting;
import org.sunbird.systemsettings.dao.SystemSettingDao;

public class SystemSettingDaoImpl implements SystemSettingDao {
  private CassandraOperation cassandraOperation;
  private ObjectMapper mapper = new ObjectMapper();
  private static final String KEYSPACE_NAME = JsonKey.SUNBIRD;
  private static final String TABLE_NAME = JsonKey.SYSTEM_SETTINGS_DB;

  public SystemSettingDaoImpl(CassandraOperation cassandraOperation) {
    this.cassandraOperation = cassandraOperation;
  }

  @Override
  public Response write(SystemSetting systemSetting) {
    Map<String, Object> map = mapper.convertValue(systemSetting, Map.class);
    Response response = cassandraOperation.upsertRecord(KEYSPACE_NAME, TABLE_NAME, map);
    response.put(JsonKey.ID, map.get(JsonKey.ID));
    return response;
  }

  @Override
  public SystemSetting readById(String id) {
    Response response = cassandraOperation.getRecordById(KEYSPACE_NAME, TABLE_NAME, id);
    List<Map<String, Object>> list = (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (CollectionUtils.isEmpty(list)) {
      return null;
    }
    return getSystemSetting(list);
  }

  @Override
  public SystemSetting readByField(String field) {
    Response response =
        cassandraOperation.getRecordsByIndexedProperty(
            KEYSPACE_NAME, TABLE_NAME, JsonKey.FIELD, field);
    List<Map<String, Object>> list = (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (CollectionUtils.isEmpty(list)) {
      return null;
    }
    return getSystemSetting(list);
  }

  public List<SystemSetting> readAll() {
    Response response = cassandraOperation.getAllRecords(KEYSPACE_NAME, TABLE_NAME);
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
    try {
      String jsonString = mapper.writeValueAsString((list.get(0)));
      return mapper.readValue(jsonString, SystemSetting.class);
    } catch (IOException e) {
      ProjectLogger.log(
          "SystemSetting:getSystemSetting: Exception occurred with error messgae = "
              + e.getMessage(),
          LoggerEnum.ERROR.name());
      ProjectCommonException.throwServerErrorException(
          ResponseCode.SERVER_ERROR, ResponseCode.SERVER_ERROR.getErrorMessage());
    }
    return null;
  }
}
