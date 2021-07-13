package org.sunbird.common.models.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;

/**
 * Helper class for validating email.
 *
 * @author Amit Kumar
 */
public class EmailValidator {

  private static Pattern pattern;
  private static final String EMAIL_PATTERN =
      "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
          + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

  private EmailValidator() {}

  static {
    pattern = Pattern.compile(EMAIL_PATTERN);
  }

  /**
   * Validates format of email.
   *
   * @param email Email value.
   * @return True, if email format is valid. Otherwise, return false.
   */
  public static boolean isEmailValid(String email) {
    if (StringUtils.isBlank(email)) {
      return false;
    }
    Matcher matcher = pattern.matcher(email);
    return matcher.matches();
  }
}
