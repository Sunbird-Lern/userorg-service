package org.sunbird.datasecurity.impl;

import org.sunbird.datasecurity.DataMaskingService;

/**
 * Implementation of DataMaskingService for logging purposes.
 * Provides masking logic suitable for log outputs.
 */
public class LogMaskServiceImpl implements DataMaskingService {

  /**
   * Masks an email address for logging.
   * If the local part (before @) is longer than 4 characters, keeps the first 4 visible.
   * Otherwise, keeps the first 2 visible.
   * The domain part is kept visible.
   *
   * @param email The email address to mask.
   * @return The masked email address.
   */
  @Override
  public String maskEmail(String email) {
    if (email.indexOf("@") > 4) {
      return email.replaceAll("(^[^@]{4}|(?!^)\\G)[^@]", "$1*");
    } else {
      return email.replaceAll("(^[^@]{2}|(?!^)\\G)[^@]", "$1*");
    }
  }

  /**
   * Masks a phone number for logging.
   * Masks all but the last digit (assuming 10-digit standard for the regex logic).
   *
   * @param phone The phone number to mask.
   * @return The masked phone number.
   */
  @Override
  public String maskPhone(String phone) {
    return phone.replaceAll("(^[^*]{9}|(?!^)\\G)[^*]", "$1*");
  }
}
