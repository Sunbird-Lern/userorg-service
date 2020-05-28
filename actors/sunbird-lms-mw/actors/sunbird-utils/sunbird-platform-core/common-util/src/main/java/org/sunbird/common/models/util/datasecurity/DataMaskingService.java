/** */
package org.sunbird.common.models.util.datasecurity;

import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.models.util.JsonKey;

/** @author Manzarul */
public interface DataMaskingService {

  /**
   * This method will allow to mask user phone number.
   *
   * @param phone String
   * @return String
   */
  String maskPhone(String phone);

  /**
   * This method will allow user to mask email.
   *
   * @param email String
   * @return String
   */
  String maskEmail(String email);

  /**
   * @param data
   * @return
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
   * Mask an OTP
   * @param otp
   * @return Depending on the length - 6, 4, masks 1 character
   */
  default String maskOTP(String otp) {
    if (otp.length() >= 6) {
      return otp.replaceAll("(^[^*]{5}|(?!^)\\G)[^*]", "$1*");
    } else {
      return otp.replaceAll("(^[^*]{3}|(?!^)\\G)[^*]", "$1*");
    }
  }
}
