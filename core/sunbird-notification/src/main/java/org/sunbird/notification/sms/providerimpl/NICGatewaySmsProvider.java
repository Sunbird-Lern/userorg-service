package org.sunbird.notification.sms.providerimpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.notification.sms.provider.ISmsProvider;
import org.sunbird.notification.utils.JsonUtil;
import org.sunbird.notification.utils.PropertiesCache;

public class NICGatewaySmsProvider implements ISmsProvider {
  private static LoggerUtil logger = new LoggerUtil(NICGatewaySmsProvider.class);

  private static String baseUrl = null;
  private static String senderId = null;
  private static String userName = null;
  private static String password = null;
  private static String dltEntityId = null;

  static {
    boolean response = init();
    logger.info("SMS configuration values are set : " + response);
  }

  @Override
  public boolean send(String phoneNumber, String smsText) {
    return sendSms(phoneNumber, smsText);
  }

  @Override
  public boolean send(String phoneNumber, String countryCode, String smsText) {
    return sendSms(phoneNumber, smsText);
  }

  @Override
  public boolean send(List<String> phoneNumbers, String smsText) {
    phoneNumbers
        .stream()
        .forEach(
            phone -> {
              sendSms(phone, smsText);
            });
    return true;
  }

  public boolean sendSms(String mobileNumber, String smsText) {
    try {
      String recipient = mobileNumber;
      if (recipient.length() == 10) {
        // add country code to mobile number
        recipient = "91" + recipient;
      }
      String messageBody = smsText;
      // add dlt template
      String dltTemplateId = getTemplateId(smsText);
      // URL encode message body
      messageBody = URLEncoder.encode(messageBody, Consts.UTF_8);
      // Construct URL
      StringBuffer URI = new StringBuffer(baseUrl);
      URI.append("?username=" + URLEncoder.encode(userName, Consts.UTF_8));
      URI.append("&pin=" + URLEncoder.encode(password, Consts.UTF_8));
      URI.append("&signature=" + senderId);
      URI.append("&mnumber=" + recipient);
      URI.append("&message=" + URLEncoder.encode(messageBody, Consts.UTF_8));
      URI.append("&msgType=" + "UC");
      URI.append("&dlt_entity_id=" + dltEntityId);
      URI.append("&dlt_template_id=" + dltTemplateId);

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");
      headers.put("Accept", "application/json");
      String response = HttpClientUtil.get(URI.toString(), headers);
      if (StringUtils.isNotBlank(response)) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> resultMap;
        resultMap = mapper.readValue(response, Map.class);
        logger.info("NICGatewaySmsProvider:Result:" + resultMap);
        return true;
      } else {
        return false;
      }
    } catch (Exception ex) {
      logger.error("Exception occurred while sending sms.", ex);
      return false;
    }
  }

  /** this method will do the SMS properties initialization. */
  public static boolean init() {
    baseUrl = System.getenv("nic_sms_gateway_provider_base_url");
    if (JsonUtil.isStringNullOREmpty(baseUrl)) {
      baseUrl = PropertiesCache.getInstance().getProperty("nic_sms_gateway_provider_base_url");
    }
    senderId = System.getenv("nic_sms_gateway_provider_senderid");
    if (JsonUtil.isStringNullOREmpty(senderId)) {
      senderId = PropertiesCache.getInstance().getProperty("nic_sms_gateway_provider_senderid");
    }
    userName = System.getenv("nic_sms_gateway_provider_username");
    if (JsonUtil.isStringNullOREmpty(userName)) {
      userName = PropertiesCache.getInstance().getProperty("nic_sms_gateway_provider_username");
    }
    password = System.getenv("nic_sms_gateway_provider_password");
    if (JsonUtil.isStringNullOREmpty(password)) {
      password = PropertiesCache.getInstance().getProperty("nic_sms_gateway_provider_password");
    }
    dltEntityId = System.getenv("dlt_entity_id");
    if (JsonUtil.isStringNullOREmpty(dltEntityId)) {
      dltEntityId = PropertiesCache.getInstance().getProperty("dlt_entity_id");
    }
    return validateSettings();
  }

  private static boolean validateSettings() {
    if (!JsonUtil.isStringNullOREmpty(senderId)
        && !JsonUtil.isStringNullOREmpty(userName)
        && !JsonUtil.isStringNullOREmpty(password)
        && !JsonUtil.isStringNullOREmpty(dltEntityId)) {
      return true;
    }
    return false;
  }
}
