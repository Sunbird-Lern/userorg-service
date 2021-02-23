package org.sunbird.notification.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerUtil;
import org.sunbird.helper.ServiceFactory;

public class DataCacheHandler implements Runnable {
  private static LoggerUtil logger = new LoggerUtil(DataCacheHandler.class);
  private static List<Map<String, String>> smsTemplateConfigList = new CopyOnWriteArrayList<>();
  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private static final String KEY_SPACE_NAME = JsonKey.SUNBIRD;

  @Override
  public void run() {
    logger.info("DataCacheHandler:run: Cache refresh started.");
    cacheSystemConfig();
    logger.info("DataCacheHandler:run: Cache refresh completed.");
  }

  private void cacheSystemConfig() {
    Response response =
        cassandraOperation.getAllRecords(KEY_SPACE_NAME, JsonKey.SYSTEM_SETTINGS_DB, null);
    logger.debug(
        "DataCacheHandler:cacheSystemConfig: Cache system setting fields" + response.getResult());
    List<Map<String, Object>> responseList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    Map<String, String> configSettings = new ConcurrentHashMap<>();
    if (null != responseList && !responseList.isEmpty()) {
      for (Map<String, Object> resultMap : responseList) {
        configSettings.put(
            ((String) resultMap.get(JsonKey.FIELD)), (String) resultMap.get(JsonKey.VALUE));
      }
    }
    String smsTemplateConfigString = configSettings.get(JsonKey.SMS_TEMPLATE_CONFIG);
    ObjectMapper mapper = new ObjectMapper();
    try {
      smsTemplateConfigList = mapper.readValue(smsTemplateConfigString, List.class);
    } catch (Exception e) {
      logger.error("Error occurred while reading sms template config" + e.getMessage(), e);
    }
  }

  public static List<Map<String, String>> getSmsTemplateConfigList() {
    return smsTemplateConfigList;
  }
}
