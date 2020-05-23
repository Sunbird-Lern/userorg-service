package org.sunbird.actorutil;

import org.sunbird.actorutil.impl.InterServiceCommunicationImpl;

/**
 * @Desc Factory class for InterServiceCommunication.
 *
 * @author Arvind
 */
public class InterServiceCommunicationFactory {

  private static InterServiceCommunication instance;

  private InterServiceCommunicationFactory() {}

  static {
    instance = new InterServiceCommunicationImpl();
  }

  public static InterServiceCommunication getInstance() {
    if (null == instance) {
      instance = new InterServiceCommunicationImpl();
    }
    return instance;
  }
}
