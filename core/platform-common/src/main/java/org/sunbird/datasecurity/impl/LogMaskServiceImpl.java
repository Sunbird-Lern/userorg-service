package org.sunbird.datasecurity.impl;

import org.sunbird.datasecurity.DataMaskingService;

public class LogMaskServiceImpl implements DataMaskingService {
  /**
   * Mask an email
   *
   * @param email
   * @return the first 2 characters in plain and masks the rest. The domain is still in plain
   */
  public String maskEmail(String email) {
    return email.replaceAll("(^[^@]{2}|(?!^)\\G)[^@]", "$1*");
  }

  /**
   * Mask a phone number
   *
   * @param phone
   * @return a string with the last 5 digit masked
   */
  public String maskPhone(String phone) {
    return phone.replaceAll("(^[^*]{5}|(?!^)\\G)[^*]", "$1*");
  }
}
