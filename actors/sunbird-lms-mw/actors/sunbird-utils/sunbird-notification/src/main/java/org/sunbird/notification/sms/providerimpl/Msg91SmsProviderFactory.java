package org.sunbird.notification.sms.providerimpl;

import org.sunbird.notification.sms.provider.ISmsProvider;
import org.sunbird.notification.sms.provider.ISmsProviderFactory;

public class Msg91SmsProviderFactory implements ISmsProviderFactory {

  private static Msg91SmsProvider msg91SmsProvider = null;

  @Override
  public ISmsProvider create() {
    if (msg91SmsProvider == null) {
      msg91SmsProvider = new Msg91SmsProvider();
    }
    return msg91SmsProvider;
  }
}
