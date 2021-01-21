package org.sunbird.common.models.util;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.responsecode.ResponseCode;

/**
 * This class will provide helper method to validate phone number and its country code.
 *
 * @author Amit Kumar
 */
public class PhoneValidator {

  private static final int ERROR_CODE = ResponseCode.CLIENT_ERROR.getResponseCode();

  private PhoneValidator() {}

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
      ProjectLogger.log(
          "PhoneValidator:validatePhone: Exception occurred while validating phone number = ", e);
    }
    return false;
  }

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
