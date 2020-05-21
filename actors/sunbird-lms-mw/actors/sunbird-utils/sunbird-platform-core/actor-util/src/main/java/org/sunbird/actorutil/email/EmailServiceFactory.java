package org.sunbird.actorutil.email;

import org.sunbird.actorutil.email.impl.EmailServiceClientImpl;

public class EmailServiceFactory {

  private static EmailServiceClient instance;

  private EmailServiceFactory() {}

  static {
    instance = new EmailServiceClientImpl();
  }

  public static EmailServiceClient getInstance() {
    if (null == instance) {
      instance = new EmailServiceClientImpl();
    }
    return instance;
  }
}
