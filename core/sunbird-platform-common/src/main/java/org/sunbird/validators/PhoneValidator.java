package org.sunbird.validators;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.common.PropertiesCache;
import org.sunbird.response.ResponseCode;

/**
 * Utility class for validating phone numbers and country codes.
 * Uses Google's libphonenumber for validation.
 */
public class PhoneValidator {

  private static final LoggerUtil logger = new LoggerUtil(PhoneValidator.class);
  private static final int ERROR_CODE = ResponseCode.CLIENT_ERROR.getResponseCode();

  private PhoneValidator() {}

  /**
   * Validates a phone number against a country code.
   *
   * @param phone The phone number to validate.
   * @param countryCode The country code for the phone number.
   * @return True if valid.
   * @throws ProjectCommonException If the phone number or country code is invalid.
   */
  public static boolean validatePhoneNumber(String phone, String countryCode) {
    if (phone.contains("+")) {
      throw new ProjectCommonException(
          ResponseCode.invalidPhoneNumber.getErrorCode(),
          ResponseCode.invalidPhoneNumber.getErrorMessage(),
          ERROR_CODE);
    }
    if (StringUtils.isNotBlank(countryCode)) {
      boolean isCountryCodeValid = validateCountryCode(countryCode);
      if (!isCountryCodeValid) {
        throw new ProjectCommonException(
            ResponseCode.invalidCountryCode.getErrorCode(),
            ResponseCode.invalidCountryCode.getErrorMessage(),
            ERROR_CODE);
      }
    }
    if (validatePhone(phone, countryCode)) {
      return true;
    } else {
      throw new ProjectCommonException(
          ResponseCode.phoneNoFormatError.getErrorCode(),
          ResponseCode.phoneNoFormatError.getErrorMessage(),
          ERROR_CODE);
    }
  }

  /**
   * Validates if the provided country code string is in a valid format.
   *
   * @param countryCode The country code to check.
   * @return True if format is valid, false otherwise.
   */
  public static boolean validateCountryCode(String countryCode) {
    String countryCodePattern = "^(?:[+] ?){0,1}(?:[0-9] ?){1,3}";
    try {
      Pattern pattern = Pattern.compile(countryCodePattern);
      Matcher matcher = pattern.matcher(countryCode);
      return matcher.matches();
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Validates phone number using Google's PhoneNumberUtil.
   *
   * @param phone The phone number.
   * @param countryCode The country code.
   * @return True if the number is valid for the region.
   */
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

  /**
   * Validates a phone number using a basic regex pattern for Indian numbers.
   *
   * @param phoneNumber The phone number string.
   * @return True if matches pattern.
   */
  public static boolean validatePhoneNumber(String phoneNumber) {
    if (StringUtils.isBlank(phoneNumber)) {
      return false;
    }
    String phonePattern = "([+]?(91)?[-]?[0-9]{10}$)";
    Pattern pattern = Pattern.compile(phonePattern);
    Matcher matcher = pattern.matcher(phoneNumber);
    return matcher.matches();
  }
}
