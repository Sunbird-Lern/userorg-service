package org.sunbird.common.models.util;

/**
 * Helper class for String formatting operations.
 *
 * @author Amit Kumar
 */
public class StringFormatter {

  public static final String DOT = ".";
  public static final String AND = " and ";
  public static final String OR = " or ";
  public static final String COMMA = ", ";

  private StringFormatter() {}

  /**
   * Helper method to construct dot formatted string.
   *
   * @param params One or more strings to be joined by dot
   * @return Dot formatted string
   */
  public static String joinByDot(String... params) {
    return String.join(DOT, params);
  }

  /**
   * Helper method to construct or formatted string.
   *
   * @param params One or more strings to be joined by or
   * @return Or formatted string
   */
  public static String joinByOr(String... params) {
    return String.join(OR, params);
  }

  /**
   * Helper method to construct and formatted string.
   *
   * @param params One or more strings to be joined by and
   * @return and formatted string
   */
  public static String joinByAnd(String... params) {
    return String.join(AND, params);
  }

  /**
   * Helper method to construct and formatted string.
   *
   * @param params One or more strings to be joined by comma
   * @return and formatted string
   */
  public static String joinByComma(String... params) {
    return String.join(COMMA, params);
  }
}
