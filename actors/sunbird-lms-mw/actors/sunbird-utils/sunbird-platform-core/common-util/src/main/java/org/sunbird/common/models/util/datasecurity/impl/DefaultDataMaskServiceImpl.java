/** */
package org.sunbird.common.models.util.datasecurity.impl;

import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.datasecurity.DataMaskingService;

/** @author Manzarul */
public class DefaultDataMaskServiceImpl implements DataMaskingService {

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
