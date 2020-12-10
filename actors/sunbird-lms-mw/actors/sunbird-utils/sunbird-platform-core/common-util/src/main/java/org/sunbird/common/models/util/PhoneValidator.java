package org.sunbird.common.models.util;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.apache.commons.lang3.StringUtils;

/**
 * This class will provide helper method to validate phone number and its country code.
 *
 * @author Amit Kumar
 */
public class PhoneValidator {
  private static LoggerUtil logger = new LoggerUtil(PhoneValidator.class);

  private PhoneValidator() {}

  public static boolean validatePhone(String phone, String countryCode) {
    PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
    String code = countryCode;
    if (StringUtils.isNotBlank(countryCode) && (countryCode.charAt(0) != '+')) {
      code = "+" + countryCode;
    }
    Phonenumber.PhoneNumber phoneNumber = null;
    try {
      if (StringUtils.isBlank(countryCode)) {
        code = PropertiesCache.getInstance().getProperty("sunbird_default_country_code");
      }
      String isoCode = phoneNumberUtil.getRegionCodeForCountryCode(Integer.parseInt(code));
      phoneNumber = phoneNumberUtil.parse(phone, isoCode);
      return phoneNumberUtil.isValidNumber(phoneNumber);
    } catch (NumberParseException e) {
      logger.error(
          "PhoneValidator:validatePhone: Exception occurred while validating phone number = ", e);
    }
    return false;
  }
}
