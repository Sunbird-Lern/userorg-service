package org.sunbird.notification.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerUtil;
import org.sunbird.helper.ServiceFactory;

public class SmsTemplateUtil {
  private static LoggerUtil logger = new LoggerUtil(SmsTemplateUtil.class);
  private static CassandraOperation cassandraOperation = ServiceFactory.getInstance();

  public static List<Map<String, String>> getSmsTemplateConfigList() {
    Response response =
        cassandraOperation.getRecordById(
            JsonKey.SUNBIRD, JsonKey.SYSTEM_SETTINGS_DB, JsonKey.SMS_TEMPLATE_CONFIG, null);
    logger.debug(
        "DataCacheHandler:cacheSystemConfig: Cache system setting fields" + response.getResult());
    List<Map<String, Object>> responseList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (null != responseList && !responseList.isEmpty()) {
      Map<String, Object> resultMap = responseList.get(0);
      String smsTemplateConfigString = (String) resultMap.get(JsonKey.VALUE);
      if (StringUtils.isNotBlank(smsTemplateConfigString)) {
        ObjectMapper mapper = new ObjectMapper();
        try {
          return mapper.readValue(smsTemplateConfigString, List.class);
        } catch (Exception e) {
          logger.error("Error occurred while reading sms template config" + e.getMessage(), e);
        }
      }
    }
    return Collections.emptyList();
  }
}
