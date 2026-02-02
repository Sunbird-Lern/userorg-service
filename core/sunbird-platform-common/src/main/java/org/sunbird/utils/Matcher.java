package org.sunbird.utils;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility class for matching identifiers and other string values.
 *
 * <p>This class provides standardized static methods for string comparison, primarily dealing with
 * case-insensitive matching logic used throughout the application for identifiers.
 */
public class Matcher {

  /** Private constructor to prevent instantiation of utility class. */
  private Matcher() {}

  /**
   * Compares two identifier strings for equality, ignoring case considerations.
   *
   * <p>This method delegates to {@link StringUtils#equalsIgnoreCase(CharSequence, CharSequence)}.
   * It handles {@code null} inputs gracefully:
   *
   * <ul>
   *   <li>If both identifiers are {@code null}, it returns {@code true}.
   *   <li>If one is {@code null} and the other is not, it returns {@code false}.
   *   <li>Otherwise, it compares them ignoring case (e.g., "abc" equals "ABC").
   * </ul>
   *
   * @param firstVal The first identifier string to compare.
   * @param secondVal The second identifier string to compare.
   * @return {@code true} if the identifiers are equal (ignoring case), {@code false} otherwise.
   */
  public static boolean matchIdentifiers(String firstVal, String secondVal) {
    return StringUtils.equalsIgnoreCase(firstVal, secondVal);
  }
}
