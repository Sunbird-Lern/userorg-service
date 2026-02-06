package org.sunbird.notification.sms.providerimpl;

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
import org.sunbird.request.RequestContext;

/**
 * Implementation of {@link ISmsProvider} for the NIC SMS Gateway service. This class handles the
 * configuration and execution of SMS requests via the NIC Gateway API.
 */
public class NICGatewaySmsProvider implements ISmsProvider {
  private static final LoggerUtil logger = new LoggerUtil(NICGatewaySmsProvider.class);

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
  public boolean send(String phoneNumber, String smsText, RequestContext context) {
    return sendSms(phoneNumber, smsText, context);
  }

  @Override
  public boolean send(
      String phoneNumber, String countryCode, String smsText, RequestContext context) {
    return sendSms(phoneNumber, smsText, context);
  }

  @Override
  public boolean send(List<String> phoneNumbers, String smsText, RequestContext context) {
    phoneNumbers.stream()
        .forEach(
            phone -> {
              sendSms(phone, smsText, context);
            });
    return true;
  }

  /**
   * Internal method to execute the SMS sending logic to the NIC Gateway.
   *
   * @param mobileNumber The recipient's mobile number.
   * @param smsText The content of the SMS.
   * @param context Request context for logging.
   * @return true if the SMS was sent successfully.
   */
  public boolean sendSms(String mobileNumber, String smsText, RequestContext context) {
    try {
      String recipient = mobileNumber;
      if (recipient.length() == 10) {
        // add country code to mobile number
        recipient = "91" + recipient;
      }
      String messageBody = smsText;
      // add dlt template
      String dltTemplateId = getTemplateId(smsText, NIC_PROVIDER);
      if (StringUtils.isBlank(dltTemplateId)) {
        logger.info(context, "dlt template id is empty for sms : " + smsText);
      }
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
      String response = HttpClientUtil.get(URI.toString(), headers, context);
      if (StringUtils.isNotBlank(response)) {
        logger.info(context, "NICGatewaySmsProvider:Result:" + response);
        return true;
      } else {
        return false;
      }
    } catch (Exception ex) {
      logger.error(context, "Exception occurred while sending sms.", ex);
      return false;
    }
  }

  /**
   * Initializes the NIC Gateway configuration properties from system environment variables or
   * property files.
   *
   * @return true if mandatory settings are correctly configured.
   */
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

  /**
   * Validates global mandatory settings.
   *
   * @return true if all mandatory settings are present.
   */
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
