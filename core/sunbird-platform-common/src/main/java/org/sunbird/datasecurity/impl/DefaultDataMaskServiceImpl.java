package org.sunbird.datasecurity.impl;

import org.apache.commons.lang3.StringUtils;
import org.sunbird.datasecurity.DataMaskingService;
import org.sunbird.keys.JsonKey;
import org.sunbird.common.ProjectUtil;

/**
 * Default implementation of the {@link DataMaskingService} interface.
 * Provides functionality to mask phone numbers and email addresses.
 */
public class DefaultDataMaskServiceImpl implements DataMaskingService {

  /**
   * Masks a phone number by keeping the last 4 digits visible.
   * Masking character is defined in JsonKey.REPLACE_WITH_ASTERISK.
   *
   * @param phone The phone number to mask.
   * @return The masked phone number, or the original if it is blank or shorter than 10 characters.
   */
  @Override
  public String maskPhone(String phone) {
    if (StringUtils.isBlank(phone) || phone.length() < 10) {
      return phone;
    }
    String tempPhone = "";
    StringBuilder builder = new StringBuilder();
    tempPhone = phone.trim().replace("-", "");
    int length = tempPhone.length();
    for (int i = 0; i < length; i++) {
      if (i < length - 4) {
        builder.append(JsonKey.REPLACE_WITH_ASTERISK);
      } else {
        builder.append(tempPhone.charAt(i));
      }
    }
    return builder.toString();
  }

  /**
   * Masks an email address.
   * Keeps the first 2 characters and the domain part (after the last @) visible.
   * Masks characters in between.
   *
   * @param email The email address to mask.
   * @return The masked email address, or the original if it is blank or invalid.
   */
  @Override
  public String maskEmail(String email) {
    if ((StringUtils.isBlank(email)) || (!ProjectUtil.isEmailvalid(email))) {
      return email;
    }
    StringBuilder builder = new StringBuilder();
    String[] emails = email.split("@");
    int length = emails[0].length();
    for (int i = 0; i < email.length(); i++) {
      if (i < 2 || i >= length) {
        builder.append(email.charAt(i));
      } else {
        builder.append(JsonKey.REPLACE_WITH_ASTERISK);
      }
    }
    return builder.toString();
  }
}


