package org.sunbird.common.util;

import org.apache.commons.lang3.StringUtils;

/** this class is used to match the identifiers. */
public class Matcher {

  /**
   * this method will match the two arguments , equal or not if two string is null or empty this
   * method will return true
   *
   * @param firstVal
   * @param secondVal
   * @return boolean
   */
  public static boolean matchIdentifiers(String firstVal, String secondVal) {
    return StringUtils.equalsIgnoreCase(firstVal, secondVal);
  }
}
