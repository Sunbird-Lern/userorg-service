package org.sunbird.notification.sms.providerimpl;

import org.sunbird.notification.sms.provider.ISmsProvider;
import org.sunbird.notification.sms.provider.ISmsProviderFactory;

/** Factory class for creating and managing a singleton instance of {@link Msg91SmsProvider}. */
public class Msg91SmsProviderFactory implements ISmsProviderFactory {

  private static Msg91SmsProvider msg91SmsProvider = null;

  /**
   * Returns a singleton instance of {@link Msg91SmsProvider}.
   *
   * @return An instance of {@link ISmsProvider} implemented by Msg91.
   */
  @Override
  public ISmsProvider create() {
    if (msg91SmsProvider == null) {
      msg91SmsProvider = new Msg91SmsProvider();
    }
    return msg91SmsProvider;
  }
}
