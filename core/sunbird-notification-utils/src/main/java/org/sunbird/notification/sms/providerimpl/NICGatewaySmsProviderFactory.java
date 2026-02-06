package org.sunbird.notification.sms.providerimpl;

import org.sunbird.notification.sms.provider.ISmsProvider;
import org.sunbird.notification.sms.provider.ISmsProviderFactory;

/**
 * Factory class for creating and managing a singleton instance of {@link NICGatewaySmsProvider}.
 */
public class NICGatewaySmsProviderFactory implements ISmsProviderFactory {

  private static NICGatewaySmsProvider nicSmsProvider = null;

  /**
   * Returns a singleton instance of {@link NICGatewaySmsProvider}.
   *
   * @return An instance of {@link ISmsProvider} implemented by NIC Gateway.
   */
  @Override
  public ISmsProvider create() {
    if (nicSmsProvider == null) {
      nicSmsProvider = new NICGatewaySmsProvider();
    }
    return nicSmsProvider;
  }
}
