package org.sunbird.util.otp;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.KeyRepresentation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.BackgroundOperations;
import org.sunbird.datasecurity.DecryptionService;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.notification.sms.provider.ISmsProvider;
import org.sunbird.notification.utils.SMSFactory;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.service.otp.OTPService;
import org.sunbird.util.ProjectUtil;

public final class OTPUtil {
  private static LoggerUtil logger = new LoggerUtil(OTPUtil.class);
  private static DecryptionService decService =
      org.sunbird.datasecurity.impl.ServiceFactory.getDecryptionServiceInstance(null);

  private static final int MAXIMUM_OTP_LENGTH = 6;
  private static final int SECONDS_IN_MINUTES = 60;
  private static final int RETRY_COUNT = 2;
  private static final int MIN_OTP_LENGTH = 4;

  private OTPUtil() {}

  private static String generateOTP() {
    String otpSize = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_OTP_LENGTH);
    int codeDigits = StringUtils.isBlank(otpSize) ? MAXIMUM_OTP_LENGTH : Integer.valueOf(otpSize);
    GoogleAuthenticatorConfig config =
        new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
            .setCodeDigits(codeDigits)
            .setKeyRepresentation(KeyRepresentation.BASE64)
            .build();
    GoogleAuthenticator gAuth = new GoogleAuthenticator(config);
    GoogleAuthenticatorKey key = gAuth.createCredentials();
    String secret = key.getKey();
    int code = gAuth.getTotpPassword(secret);
    return String.valueOf(code);
  }

  /**
   * generates otp and ensures otp length is greater than or equal to 4 if otp length is less than 4
   * , regenerate otp (max retry 3)
   *
   * @return
   */
  public static String generateOtp(RequestContext context) {
    String otp = generateOTP();
    int noOfAttempts = 0;
    while (otp.length() < MIN_OTP_LENGTH && noOfAttempts < RETRY_COUNT) {
      otp = generateOTP();
      noOfAttempts++;
    }
    logger.info(context, "OTPUtil: generateOtp: otp generated in " + noOfAttempts + " attempts");
    return ensureOtpLength(otp);
  }

  /**
   * After 3 attempts, still otp length less that 4 multiply otp with 1000,
   *
   * @param otp
   * @return
   */
  private static String ensureOtpLength(String otp) {
    if (otp.length() < MIN_OTP_LENGTH) {
      int multiplier = (int) Math.pow(10, MAXIMUM_OTP_LENGTH - MIN_OTP_LENGTH + 1.0);
      otp = String.valueOf(Integer.valueOf(otp) * multiplier);
    }
    return otp;
  }

  public static boolean sendOTPViaSMS(Map<String, Object> otpMap, RequestContext context) {
    if (StringUtils.isBlank((String) otpMap.get(JsonKey.PHONE))) {
      return false;
    }

    Map<String, String> smsTemplate = new HashMap<>();
    String template = (String) otpMap.get(JsonKey.TEMPLATE_ID);
    smsTemplate.put(JsonKey.OTP, (String) otpMap.get(JsonKey.OTP));
    smsTemplate.put(
        JsonKey.OTP_EXPIRATION_IN_MINUTES, (String) otpMap.get(JsonKey.OTP_EXPIRATION_IN_MINUTES));
    smsTemplate.put(
        JsonKey.INSTALLATION_NAME,
        ProjectUtil.getConfigValue(JsonKey.SUNBIRD_INSTALLATION_DISPLAY_NAME));
    String sms = null;
    if (StringUtils.isBlank(template)) {
      sms = OTPService.getSmsBody(JsonKey.VERIFY_PHONE_OTP_TEMPLATE, smsTemplate, context);
    } else if (StringUtils.isNotBlank(template)
        && StringUtils.equals(template, JsonKey.WARD_LOGIN_OTP_TEMPLATE_ID)) {
      sms = OTPService.getSmsBody(JsonKey.OTP_PHONE_WARD_LOGIN_TEMPLATE, smsTemplate, context);
    } else {
      sms = OTPService.getSmsBody(JsonKey.OTP_PHONE_RESET_PASSWORD_TEMPLATE, smsTemplate, context);
    }
    logger.debug(context, "OTPUtil:sendOTPViaSMS: SMS text = " + sms);

    String countryCode = "";
    if (StringUtils.isBlank((String) otpMap.get(JsonKey.COUNTRY_CODE))) {
      countryCode = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_DEFAULT_COUNTRY_CODE);
    } else {
      countryCode = (String) otpMap.get(JsonKey.COUNTRY_CODE);
    }
    ISmsProvider smsProvider = SMSFactory.getInstance();

    logger.debug(
        context,
        "OTPUtil:sendOTPViaSMS: SMS OTP text = "
            + sms
            + " with phone = "
            + otpMap.get(JsonKey.PHONE));

    boolean response = smsProvider.send((String) otpMap.get(JsonKey.PHONE), countryCode, sms);

    logger.info(
        context,
        "OTPUtil:sendOTPViaSMS: OTP sent successfully to phone :"
            + otpMap.get(JsonKey.PHONE)
            + "is "
            + response);
    return response;
  }

  /**
   * This method will return either email or phone value of user based on the asked type in request
   *
   * @param userId
   * @param type value can be email, phone, prevUsedEmail or prevUsedPhone
   * @return
   */
  public static String getEmailPhoneByUserId(String userId, String type, RequestContext context) {
    Map<String, Object> user = OTPService.getUserById(userId, context);
    if (MapUtils.isNotEmpty(user)) {
      String emailPhone = decService.decryptData((String) user.get(type), context);
      if (StringUtils.isBlank(emailPhone)) {
        ProjectCommonException.throwClientErrorException(ResponseCode.invalidRequestData);
      }
      return emailPhone;
    } else {
      ProjectCommonException.throwClientErrorException(ResponseCode.userNotFound);
    }
    return null;
  }

  public static Request getRequestToSendOTPViaEmail(
      Map<String, Object> emailTemplateMap, String otpType, RequestContext context) {
    Request request = null;
    if ((StringUtils.isBlank((String) emailTemplateMap.get(JsonKey.EMAIL)))) {
      return request;
    }
    String envName = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_INSTALLATION_DISPLAY_NAME);
    String emailSubject = null;
    if ("resetPassword".equalsIgnoreCase(otpType)) {
      emailSubject = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_RESET_PASS_MAIL_SUBJECT);
    } else {
      // default fallback for all other otpType
      emailSubject = ProjectUtil.getConfigValue(JsonKey.ONBOARDING_MAIL_SUBJECT);
    }
    emailTemplateMap.put(JsonKey.SUBJECT, ProjectUtil.formatMessage(emailSubject, envName));
    List<String> reciptientsMail = new ArrayList<>();
    reciptientsMail.add((String) emailTemplateMap.get(JsonKey.EMAIL));
    emailTemplateMap.put(JsonKey.RECIPIENT_EMAILS, reciptientsMail);
    if (StringUtils.isBlank((String) emailTemplateMap.get(JsonKey.TEMPLATE_ID))) {
      emailTemplateMap.put(JsonKey.EMAIL_TEMPLATE_TYPE, JsonKey.OTP);
    } else if (StringUtils.isNotBlank((String) emailTemplateMap.get(JsonKey.TEMPLATE_ID))
        && StringUtils.equals(
            (String) emailTemplateMap.get(JsonKey.TEMPLATE_ID),
            JsonKey.WARD_LOGIN_OTP_TEMPLATE_ID)) {
      emailTemplateMap.put(JsonKey.EMAIL_TEMPLATE_TYPE, JsonKey.OTP_EMAIL_WARD_LOGIN_TEMPLATE);
    } else {
      // send otp to email while reseting password from portal
      emailTemplateMap.put(JsonKey.EMAIL_TEMPLATE_TYPE, JsonKey.OTP_EMAIL_RESET_PASSWORD_TEMPLATE);
    }
    emailTemplateMap.put(JsonKey.INSTALLATION_NAME, envName);
    request = new Request();
    request.setOperation(BackgroundOperations.emailService.name());
    request.put(JsonKey.EMAIL_REQUEST, emailTemplateMap);
    request.setRequestContext(context);
    return request;
  }

  public static Request getRequestToSendOTPViaEmail(
      Map<String, Object> emailTemplateMap, RequestContext context) {
    return getRequestToSendOTPViaEmail(emailTemplateMap, null, context);
  }

  public static String getOTPExpirationInMinutes() {
    String expirationInSeconds = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_OTP_EXPIRATION);
    int otpExpiration = Integer.parseInt(expirationInSeconds);
    int otpExpirationInMinutes = Math.floorDiv(otpExpiration, SECONDS_IN_MINUTES);
    return String.valueOf(otpExpirationInMinutes);
  }
}
