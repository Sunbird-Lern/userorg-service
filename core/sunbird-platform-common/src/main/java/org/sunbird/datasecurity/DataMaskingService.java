/** */
package org.sunbird.datasecurity;

import org.apache.commons.lang3.StringUtils;
import org.sunbird.keys.JsonKey;

/**
 * Service interface for masking sensitive data such as phone numbers, emails, and OTPs.
 * Provides default implementations for generic data and OTP masking.
 */
public interface DataMaskingService {


  /**
   * Checks if the given data string contains masked characters (asterisks).
   *
   * @param data The string to check.
   * @return true if the data contains an asterisk, false otherwise.
   */
  default boolean isMasked(String data) {
    return data.contains(JsonKey.REPLACE_WITH_ASTERISK);
  }

  /**
   * Masks a phone number.
   *
   * @param phone The phone number to mask.
   * @return The masked phone number.
   */
  String maskPhone(String phone);

  /**
   * Masks an email address.
   *
   * @param email The email address to mask.
   * @return The masked email address.
   */
  String maskEmail(String email);

  /**
   * Masks generic data strings.
   * If the data is blank or has a length of 3 or less, it is returned as is.
   * Otherwise, it masks characters with asterisks, leaving the last 4 characters visible.
   *
   * @param data The data string to mask.
   * @return The masked data string.
   */
  default String maskData(String data) {
    if (StringUtils.isBlank(data) || data.length() <= 3) {
      return data;
    }
    int lenght = data.length() - 4;
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < data.length(); i++) {
      if (i < lenght) {
        builder.append(JsonKey.REPLACE_WITH_ASTERISK);
      } else {
        builder.append(data.charAt(i));
      }
    }
    return builder.toString();
  }

  /**
   * Masks an OTP (One Time Password).
   * Depending on the length (>= 6 or < 6), it masks all but the first 4 or 2 characters respectively.
   *
   * @param otp The OTP string to mask.
   * @return The masked OTP string.
   */
  default String maskOTP(String otp) {
    if (otp.length() >= 6) {
      return otp.replaceAll("(^[^*]{4}|(?!^)\\G)[^*]", "$1*");
    } else {
      return otp.replaceAll("(^[^*]{2}|(?!^)\\G)[^*]", "$1*");
    }
  }
}
