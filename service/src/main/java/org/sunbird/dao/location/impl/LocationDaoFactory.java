package org.sunbird.dao.location.impl;

import org.sunbird.dao.location.LocationDao;

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
