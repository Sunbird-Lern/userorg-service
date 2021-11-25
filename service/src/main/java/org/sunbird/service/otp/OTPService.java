package org.sunbird.service.otp;

import java.util.Map;
import org.sunbird.dao.otp.OTPDao;
import org.sunbird.dao.otp.impl.OTPDaoImpl;
import org.sunbird.request.RequestContext;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.util.SMSTemplateProvider;

public class OTPService {

  private final OTPDao otpDao = OTPDaoImpl.getInstance();
  private final UserService userService = UserServiceImpl.getInstance();

  public Map<String, Object> getOTPDetails(String type, String key, RequestContext context) {
    return otpDao.getOTPDetails(type, key, context);
  }

  public void insertOTPDetails(String type, String key, String otp, RequestContext context) {
    otpDao.insertOTPDetails(type, key, otp, context);
  }

  public void deleteOtp(String type, String key, RequestContext context) {
    otpDao.deleteOtp(type, key, context);
  }

  /**
   * This method will return either email or phone value of user based on the asked type in request
   *
   * @param userId
   * @param type value can be email, phone, recoveryEmail, recoveryPhone , prevUsedEmail or
   *     prevUsedPhone
   * @return
   */
  public String getEmailPhoneByUserId(String userId, String type, RequestContext context) {
    return userService.getDecryptedEmailPhoneByUserId(userId, type, context);
  }

  public String getSmsBody(
      String templateFile, Map<String, String> smsTemplate, RequestContext requestContext) {
    return SMSTemplateProvider.getSMSBody(templateFile, smsTemplate, requestContext);
  }

  public void updateAttemptCount(Map<String, Object> otpDetails, RequestContext context) {
    otpDao.updateAttemptCount(otpDetails, context);
  }
}
