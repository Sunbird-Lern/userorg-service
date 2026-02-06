package org.sunbird.notification.sms.provider;

/** Factory interface for creating instances of {@link ISmsProvider}. */
public interface ISmsProviderFactory {

  /**
   * Creates and returns an instance of an SMS provider.
   *
   * @return An implementation of {@link ISmsProvider}.
   */
  ISmsProvider create();
}
