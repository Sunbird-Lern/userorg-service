package org.sunbird.utils;

/**
 * Helper class for String formatting operations.
 * Provides methods for joining strings with various delimiters (dot, comma, 'and', 'or').
 */
public class StringFormatter {

  public static final String DOT = ".";
  public static final String AND = " and ";
  public static final String OR = " or ";
  public static final String COMMA = ", ";

  private StringFormatter() {}

  /**
   * Joins multiple strings with a dot delimiter.
   *
   * @param params One or more strings to be joined.
   * @return The dot-separated string.
   */
  public static String joinByDot(String... params) {
    return String.join(DOT, params);
  }

  /**
   * Joins multiple strings with an 'or' delimiter.
   *
   * @param params One or more strings to be joined.
   * @return The 'or'-separated string.
   */
  public static String joinByOr(String... params) {
    return String.join(OR, params);
  }

  /**
   * Joins multiple strings with an 'and' delimiter.
   *
   * @param params One or more strings to be joined.
   * @return The 'and'-separated string.
   */
  public static String joinByAnd(String... params) {
    return String.join(AND, params);
  }

  /**
   * Joins multiple strings with a comma delimiter.
   *
   * @param params One or more strings to be joined.
   * @return The comma-separated string.
   */
  public static String joinByComma(String... params) {
    return String.join(COMMA, params);
  }
}
