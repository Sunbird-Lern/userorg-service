package org.sunbird.notification.sms.providerimpl;

import org.sunbird.notification.sms.provider.ISmsProvider;
import org.sunbird.notification.sms.provider.ISmsProviderFactory;

public class NICGatewaySmsProviderFactory implements ISmsProviderFactory {

  private static NICGatewaySmsProvider nicSmsProvider = null;

  @Override
  public ISmsProvider create() {
    if (nicSmsProvider == null) {
      nicSmsProvider = new NICGatewaySmsProvider();
    }
    return nicSmsProvider;
  }
}
