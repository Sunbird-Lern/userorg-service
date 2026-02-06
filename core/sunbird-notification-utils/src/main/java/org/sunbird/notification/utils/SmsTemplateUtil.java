package org.sunbird.notification.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ProjectUtil;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.response.Response;

/** Utility class for managing and retrieving SMS templates from the database. */
public class SmsTemplateUtil {
  private static final LoggerUtil logger = new LoggerUtil(SmsTemplateUtil.class);
  private static final CassandraOperation cassandraOperation = ServiceFactory.getInstance();

  /**
   * Fetches the SMS template configuration map from the system settings in Cassandra.
   *
   * @return A map containing provider-specific SMS templates, or an empty map if not found.
   */
  public static Map<String, Map<String, String>> getSmsTemplateConfigMap() {
    Response response =
        cassandraOperation.getRecordById(
            ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE),
            JsonKey.SYSTEM_SETTINGS_DB,
            JsonKey.SMS_TEMPLATE_CONFIG,
            null);
    List<Map<String, Object>> responseList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (null != responseList && !responseList.isEmpty()) {
      Map<String, Object> resultMap = responseList.get(0);
      String smsTemplateConfigString = (String) resultMap.get(JsonKey.VALUE);
      if (StringUtils.isNotBlank(smsTemplateConfigString)) {
        ObjectMapper mapper = new ObjectMapper();
        try {
          return mapper.readValue(smsTemplateConfigString, Map.class);
        } catch (Exception e) {
          logger.error("Error occurred while reading sms template config" + e.getMessage(), e);
        }
      }
    }
    return Collections.emptyMap();
  }
}
