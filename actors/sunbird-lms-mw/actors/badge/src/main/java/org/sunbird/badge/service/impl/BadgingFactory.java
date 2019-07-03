/** */
package org.sunbird.badge.service.impl;

import org.sunbird.badge.service.BadgingService;

/** @author Manzarul */
public class BadgingFactory {

  private static BadgingService service;

  static {
    service = new BadgrServiceImpl();
  }

  /** private default constructor. */
  private BadgingFactory() {}

  /**
   * This method will provide singleton instance for badging service impl.
   *
   * @return BadgingService
   */
  public static BadgingService getInstance() {
    return service;
  }
}
