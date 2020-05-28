package org.sunbird.common.models.util.datasecurity.impl;

import org.sunbird.common.models.util.datasecurity.DataMaskingService;

public class LogMaskServiceImpl implements DataMaskingService {
    /**
     * Mask an email
     * @param email
     * @return the first 4 or 2 characters in plain and masks the rest. The domain is
     * still in plain
     */
    public String maskEmail(String email) {
        if (email.indexOf("@") > 4) {
            return email.replaceAll("(^[^@]{4}|(?!^)\\G)[^@]", "$1*");
        } else {
            return email.replaceAll("(^[^@]{2}|(?!^)\\G)[^@]", "$1*");
        }
    }

    /**
     * Mask a phone number
     * @param phone
     * @return a string with the last digit masked
     */
    public String maskPhone(String phone) {
        return phone.replaceAll("(^[^*]{9}|(?!^)\\G)[^*]", "$1*");
    }
}
