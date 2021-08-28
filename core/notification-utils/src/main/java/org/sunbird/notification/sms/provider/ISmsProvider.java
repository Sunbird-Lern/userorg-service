package org.sunbird.notification.sms.provider;

import java.util.List;
import java.util.Map;
import org.sunbird.notification.utils.SmsTemplateUtil;
import org.sunbird.request.RequestContext;

public interface ISmsProvider {

  default String getTemplateId(String sms) {
    Map<String, String> smsTemplateConfig = SmsTemplateUtil.getSmsTemplateConfigMap();
    for (String key : smsTemplateConfig.keySet()) {
      String pattern = key.replaceAll("\\$[^ .]+", ".*?");
      if (sms.matches(pattern)) {
        return smsTemplateConfig.get(key);
      }
    }
    return "";
  }

  /**
   * This method will send the SMS with default country code. default country code value will differ
   * based on Installation, for sunbird default is 91
   *
   * @param phoneNumber String
   * @param smsText Sms text
   * @return boolean
   */
  boolean send(String phoneNumber, String smsText, RequestContext context);

  /**
   * This method will send SMS on user provider country code, basically it will override the value
   * of default country code.
   *
   * @param phoneNumber String
   * @param countryCode
   * @param smsText
   * @return boolean
   */
  boolean send(String phoneNumber, String countryCode, String smsText, RequestContext context);

  /**
   * This method will send the SMS to list of phone number at the same time. default country code
   * value will differ based on Installation, for sunbird default is 91
   *
   * @param phoneNumber List<String>
   * @param smsText Sms text
   * @return boolean
   */
  boolean send(List<String> phoneNumber, String smsText, RequestContext context);
}
