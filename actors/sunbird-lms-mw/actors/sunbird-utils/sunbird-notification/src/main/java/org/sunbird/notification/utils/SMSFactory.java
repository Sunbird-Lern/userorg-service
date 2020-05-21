/** */
package org.sunbird.notification.utils;

import org.sunbird.notification.sms.provider.ISmsProvider;
import org.sunbird.notification.sms.provider.ISmsProviderFactory;
import org.sunbird.notification.sms.providerimpl.Msg91SmsProviderFactory;

/**
 * This class will provide object of factory.
 *
 * @author Manzarul
 */
public class SMSFactory {

  /**
   * This method will provide SMS Provide object to trigger the SMS it will by default return
   * Msg91SmsProvider class instance
   *
   * @param objectName String ,{"91SMS","some other impl"}
   * @return ISmsProvider
   */
  public static ISmsProvider getInstance(String objectName) {
    if ("91SMS".equalsIgnoreCase(objectName)) {
      ISmsProviderFactory factory = new Msg91SmsProviderFactory();
      return factory.create();
    } else {
      ISmsProviderFactory factory = new Msg91SmsProviderFactory();
      return factory.create();
    }
  }
}
