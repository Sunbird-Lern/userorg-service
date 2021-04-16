package org.sunbird.notification.sms.providerimpl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import org.sunbird.common.models.util.LoggerUtil;
import org.sunbird.notification.sms.provider.ISmsProvider;
import org.sunbird.notification.utils.JsonUtil;
import org.sunbird.notification.utils.PropertiesCache;
import org.sunbird.notification.utils.SmsTemplateUtil;

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
  public boolean send(List<String> phoneNumber, String smsText) {
    return false;
  }

  public boolean sendSms(String mobileNumber, String smsText) {
    try {
      String recipient = mobileNumber;
      String messageBody = smsText;
      // add dlt template
      String dltTemplateId = getTemplateId(smsText);
      // URL encode message body
      messageBody = URLEncoder.encode(messageBody, "UTF-8");
      // Construct URL
      StringBuffer URI = new StringBuffer();
      URI.append(baseUrl);
      URI.append("?username=" + userName);
      URI.append("&pin=" + password);
      URI.append("&signature=" + senderId);
      URI.append("&mnumber=" + recipient);
      URI.append("&message=" + messageBody);
      URI.append("&msgType=" + "UC");
      URI.append("&dlt_entity_id=" + dltEntityId);
      URI.append("&dlt_template_id=" + dltTemplateId);
      String result = "";

      URL url = new URL(URI.toString());
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      if (connection.getResponseCode() != 200) {
        return false;
      }
      BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      StringBuffer sb = new StringBuffer();
      String line;
      while ((line = rd.readLine()) != null) {
        sb.append(line);
      }
      rd.close();
      result = sb.toString();
      logger.info("NICGatewaySmsProvider:Result:" + result);
      return true;
    } catch (Exception ex) {
      logger.error("Exception occurred while sending sms.", ex);
      return false;
    }
  }

  private String getTemplateId(String sms) {
    Map<String, String> smsTemplateConfig = SmsTemplateUtil.getSmsTemplateConfigMap();
    for (String key : smsTemplateConfig.keySet()) {
      String pattern = key.replaceAll("\\$[^ .]+", ".*?");
      if (sms.matches(pattern)) {
        return smsTemplateConfig.get(key);
      }
    }
    return "";
  }

  /** this method will do the SMS properties initialization. */
  public static boolean init() {
    baseUrl = PropertiesCache.getInstance().getProperty("nic_sms_gateway_provider_base_url");
    senderId = System.getenv("nic_sms_gateway_provider_senderid");
    userName = System.getenv("nic_sms_gateway_provider_username");
    password = System.getenv("nic_sms_gateway_provider_password");
    dltEntityId = System.getenv("diksha_dlt_entity_id");
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
