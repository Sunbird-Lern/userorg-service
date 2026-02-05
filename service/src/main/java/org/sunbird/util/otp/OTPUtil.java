package org.sunbird.util.otp;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.KeyRepresentation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.datasecurity.impl.LogMaskServiceImpl;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.notification.sms.provider.ISmsProvider;
import org.sunbird.notification.utils.SMSFactory;
import org.sunbird.operations.userorg.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.service.otp.OTPService;
import org.sunbird.common.ProjectUtil;

public final class OTPUtil {
  private static final LoggerUtil logger = new LoggerUtil(OTPUtil.class);
  private static final OTPService otpService = new OTPService();
  private static final LogMaskServiceImpl logMaskService = new LogMaskServiceImpl();
  private static final int MAXIMUM_OTP_LENGTH = 6;
  private static final int SECONDS_IN_MINUTES = 60;
  private static final int RETRY_COUNT = 2;
  private static final int MIN_OTP_LENGTH = 4;

  private OTPUtil() {}

  /**
   * generates otp and ensures otp length is greater than or equal to 4 if otp length is less than 4
   * , regenerate otp (max retry 3)
   *
   * @return
   */
  public static String generateOTP(RequestContext context) {
    String otp = generateOTP();
    int noOfAttempts = 0;
    while (otp.length() < MIN_OTP_LENGTH && noOfAttempts < RETRY_COUNT) {
      otp = generateOTP();
      noOfAttempts++;
    }
    logger.info(context, "OTPUtil: generateOTP: otp generated in " + noOfAttempts + " attempts");
    return ensureOtpLength(otp);
  }

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
    String templateId = (String) otpMap.get(JsonKey.TEMPLATE_ID);
    smsTemplate.put(JsonKey.OTP, (String) otpMap.get(JsonKey.OTP));
    smsTemplate.put(JsonKey.OTP_EXPIRATION_IN_MINUTES, (String) otpMap.get(JsonKey.OTP_EXPIRATION_IN_MINUTES));
    smsTemplate.put(JsonKey.INSTALLATION_NAME, ProjectUtil.getConfigValue(JsonKey.SUNBIRD_INSTALLATION_DISPLAY_NAME));
    smsTemplate.put(JsonKey.SUPPORT_EMAIL, ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SUPPORT_EMAIL));
    String sms = "";
    if (StringUtils.isBlank(templateId)) {
      sms = otpService.getSmsBody(JsonKey.VERIFY_PHONE_OTP_TEMPLATE, smsTemplate, context);
    } else if (StringUtils.equals(JsonKey.WARD_LOGIN_OTP_TEMPLATE_ID, templateId)) {
      sms = otpService.getSmsBody(JsonKey.OTP_PHONE_WARD_LOGIN_TEMPLATE, smsTemplate, context);
    } else if (StringUtils.equals(JsonKey.RESET_PASSWORD_TEMPLATE_ID, templateId)) {
      sms = otpService.getSmsBody(JsonKey.OTP_PHONE_RESET_PASSWORD_TEMPLATE, smsTemplate, context);
    } else if (StringUtils.equals(JsonKey.CONTACT_UPDATE_TEMPLATE_ID, templateId)) {
      sms = otpService.getSmsBody(JsonKey.OTP_CONTACT_UPDATE_TEMPLATE_SMS, smsTemplate, context);
    } else if (StringUtils.equals(JsonKey.OTP_DELETE_USER_TEMPLATE_ID, templateId)) {
      sms = otpService.getSmsBody(JsonKey.OTP_DELETE_USER_TEMPLATE_SMS, smsTemplate, context);
    }
    logger.debug(context, "OTPUtil:sendOTPViaSMS: SMS text = " + sms);

    String countryCode;
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

    boolean response =
        smsProvider.send((String) otpMap.get(JsonKey.PHONE), countryCode, sms, context);

    logger.info(
        context,
        "OTPUtil:sendOTPViaSMS: OTP sent successfully to phone :"
            + otpMap.get(JsonKey.PHONE)
            + " is "
            + response);
    return response;
  }

  public static Request getRequestToSendOTPViaEmail(
      Map<String, Object> emailTemplateMap, RequestContext context) {
    Request request;
    if ((StringUtils.isBlank((String) emailTemplateMap.get(JsonKey.EMAIL)))) {
      return null;
    }
    String templateId = (String) emailTemplateMap.get(JsonKey.TEMPLATE_ID);
    String envName = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_INSTALLATION_DISPLAY_NAME);
    String supportEmail = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_SUPPORT_EMAIL);
    List<String> reciptientsMail = new ArrayList<>();
    reciptientsMail.add((String) emailTemplateMap.get(JsonKey.EMAIL));
    emailTemplateMap.put(JsonKey.RECIPIENT_EMAILS, reciptientsMail);
    if (StringUtils.isBlank(templateId)) {
      emailTemplateMap.put(JsonKey.EMAIL_TEMPLATE_TYPE, JsonKey.OTP);
      emailTemplateMap.put(JsonKey.SUBJECT, JsonKey.EMAIL_VERIFICATION_SUBJECT);
    } else if (StringUtils.equalsIgnoreCase(JsonKey.WARD_LOGIN_OTP_TEMPLATE_ID, templateId)) {
      emailTemplateMap.put(JsonKey.EMAIL_TEMPLATE_TYPE, JsonKey.OTP_EMAIL_WARD_LOGIN_TEMPLATE);
      String emailSubject = ProjectUtil.getConfigValue(JsonKey.ONBOARDING_MAIL_SUBJECT);
      emailTemplateMap.put(JsonKey.SUBJECT, ProjectUtil.formatMessage(emailSubject, envName));
    } else if (StringUtils.equalsIgnoreCase(JsonKey.RESET_PASSWORD_TEMPLATE_ID, templateId)) {
      emailTemplateMap.put(JsonKey.EMAIL_TEMPLATE_TYPE, JsonKey.OTP_EMAIL_RESET_PASSWORD_TEMPLATE);
      emailTemplateMap.put(
          JsonKey.SUBJECT, ProjectUtil.getConfigValue(JsonKey.SUNBIRD_RESET_PASS_MAIL_SUBJECT));
    } else if (StringUtils.equalsIgnoreCase(JsonKey.CONTACT_UPDATE_TEMPLATE_ID, templateId)) {
      emailTemplateMap.put(JsonKey.EMAIL_TEMPLATE_TYPE, JsonKey.OTP_CONTACT_UPDATE_TEMPLATE_EMAIL);
      emailTemplateMap.put(JsonKey.SUBJECT, JsonKey.CONTACT_DETAILS_UPDATE_VERIFICATION_SUBJECT);
    } else if (StringUtils.equalsIgnoreCase(JsonKey.OTP_DELETE_USER_TEMPLATE_ID, templateId)) {
      emailTemplateMap.put(JsonKey.EMAIL_TEMPLATE_TYPE, JsonKey.OTP_DELETE_USER_EMAIL_TEMPLATE);
      emailTemplateMap.put(JsonKey.SUBJECT, JsonKey.DELETE_USER_VERIFICATION_SUBJECT);
    }
    emailTemplateMap.put(JsonKey.INSTALLATION_NAME, envName);
    emailTemplateMap.put(JsonKey.SUPPORT_EMAIL, supportEmail);
    request = new Request();
    request.setOperation(ActorOperations.EMAIL_SERVICE.getValue());
    request.put(JsonKey.EMAIL_REQUEST, emailTemplateMap);
    request.setRequestContext(context);
    return request;
  }

  public static String getOTPExpirationInMinutes() {
    String expirationInSeconds = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_OTP_EXPIRATION);
    int otpExpiration = Integer.parseInt(expirationInSeconds);
    int otpExpirationInMinutes = Math.floorDiv(otpExpiration, SECONDS_IN_MINUTES);
    return String.valueOf(otpExpirationInMinutes);
  }

  public static String maskOTP(String otp) {
    return logMaskService.maskOTP(otp);
  }

  public static String maskId(String id, String type) {
    if (JsonKey.EMAIL.equalsIgnoreCase(type)) {
      return logMaskService.maskEmail(id);
    } else if (JsonKey.PHONE.equalsIgnoreCase(type)) {
      return logMaskService.maskPhone(id);
    }
    return "";
  }
}
