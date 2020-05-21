package org.sunbird.location.dao.impl;

import org.sunbird.location.dao.LocationDao;

/** @author Amit Kumar */
public class LocationDaoFactory {

  /** private default constructor. */
  private LocationDaoFactory() {}

  private static LocationDao locationDao;

  static {
    locationDao = new LocationDaoImpl();
  }

  /**
   * This method will provide singleton instance for LocationDaoImpl.
   *
   * @return LocationDao
   */
  public static LocationDao getInstance() {
    return locationDao;
  }
}
