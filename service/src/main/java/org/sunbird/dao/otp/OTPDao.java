package org.sunbird.dao.otp;

import java.util.Map;
import org.sunbird.request.RequestContext;

public interface OTPDao {

  /**
   * Fetch OTP details based on type (phone / email) and key.
   *
   * @param type Type of key (phone / email)
   * @param key Phone number or email address
   * @param context
   * @return OTP details
   */
  Map<String, Object> getOTPDetails(String type, String key, RequestContext context);

  /**
   * Insert OTP details for given type (phone / email) and key
   *
   * @param type Type of key (phone / email)
   * @param key Phone number or email address
   * @param otp Generated OTP
   * @param context
   */
  void insertOTPDetails(String type, String key, String otp, RequestContext context);

  /**
   * this method will be used to delete the Otp
   *
   * @param type
   * @param key
   * @param context
   */
  void deleteOtp(String type, String key, RequestContext context);

  void updateAttemptCount(Map<String, Object> otpDetails, RequestContext context);
}
