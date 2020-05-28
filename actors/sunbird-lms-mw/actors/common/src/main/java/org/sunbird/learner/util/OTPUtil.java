package org.sunbird.learner.util;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.KeyRepresentation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.background.BackgroundOperations;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.datasecurity.DecryptionService;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.otp.SendOTPActor;
import org.sunbird.learner.actors.otp.service.OTPService;
import org.sunbird.notification.sms.provider.ISmsProvider;
import org.sunbird.notification.utils.SMSFactory;

public final class OTPUtil {

  private static CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private static DecryptionService decService =
      org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getDecryptionServiceInstance(
          null);

  private static final int MINIMUM_OTP_LENGTH = 6;
  private static final int SECONDS_IN_MINUTES = 60;

  private OTPUtil() {}

  public static String generateOTP() {
    String otpSize = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_OTP_LENGTH);
    int codeDigits = StringUtils.isBlank(otpSize) ? MINIMUM_OTP_LENGTH : Integer.valueOf(otpSize);
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

  public static void sendOTPViaSMS(Map<String, Object> otpMap) {
    if (StringUtils.isBlank((String) otpMap.get(JsonKey.PHONE))) {
      return;
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
      sms = OTPService.getSmsBody(JsonKey.VERIFY_PHONE_OTP_TEMPLATE, smsTemplate);
    } else {
      sms = OTPService.getSmsBody(JsonKey.OTP_PHONE_RESET_PASSWORD_TEMPLATE, smsTemplate);
    }
    ProjectLogger.log("OTPUtil:sendOTPViaSMS: SMS text = " + sms, LoggerEnum.INFO.name());

    String countryCode = "";
    if (StringUtils.isBlank((String) otpMap.get(JsonKey.COUNTRY_CODE))) {
      countryCode = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_DEFAULT_COUNTRY_CODE);
    } else {
      countryCode = (String) otpMap.get(JsonKey.COUNTRY_CODE);
    }
    ISmsProvider smsProvider = SMSFactory.getInstance("91SMS");

    ProjectLogger.log(
        "OTPUtil:sendOTPViaSMS: SMS OTP text = "
            + sms
            + " with phone = "
            + (String) otpMap.get(JsonKey.PHONE),
        LoggerEnum.INFO.name());

    boolean response = smsProvider.send((String) otpMap.get(JsonKey.PHONE), countryCode, sms);

    ProjectLogger.log(
        "OTPUtil:sendOTPViaSMS: Response from SMS provider: " + response, LoggerEnum.INFO.name());
    ProjectLogger.log(
        "OTPUtil:sendOTPViaSMS: OTP sent successfully to phone :"
            + otpMap.get(JsonKey.PHONE)
            + "is "
            + response,
        LoggerEnum.INFO.name());
  }

  /**
   * This method will return either email or phone value of user based on the asked type in request
   *
   * @param userId
   * @param type value can be email, phone, prevUsedEmail or prevUsedPhone
   * @return
   */
  public static String getEmailPhoneByUserId(String userId, String type) {
    Util.DbInfo usrDbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
    Response response =
        cassandraOperation.getRecordById(usrDbInfo.getKeySpace(), usrDbInfo.getTableName(), userId);
    List<Map<String, Object>> userList = (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (CollectionUtils.isNotEmpty(userList)) {
      Map<String, Object> user = userList.get(0);
      String emailPhone = decService.decryptData((String) user.get(type));
      if (StringUtils.isBlank(emailPhone)) {
        ProjectCommonException.throwClientErrorException(ResponseCode.invalidRequestData);
      }
      return emailPhone;
    } else {
      ProjectCommonException.throwClientErrorException(ResponseCode.userNotFound);
    }
    return null;
  }

  public static Request sendOTPViaEmail(Map<String, Object> emailTemplateMap, String otpType) {
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
    } else {
      // send otp to email while reseting password from portal
      emailTemplateMap.put(JsonKey.EMAIL_TEMPLATE_TYPE, JsonKey.OTP_EMAIL_RESET_PASSWORD_TEMPLATE);
    }
    emailTemplateMap.put(JsonKey.INSTALLATION_NAME, envName);
    request = new Request();
    request.setOperation(BackgroundOperations.emailService.name());
    request.put(JsonKey.EMAIL_REQUEST, emailTemplateMap);
    return request;
  }

  public static Request sendOTPViaEmail(Map<String, Object> emailTemplateMap) {
    return sendOTPViaEmail(emailTemplateMap, null);
  }

  public static String getOTPExpirationInMinutes() {
    String expirationInSeconds = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_OTP_EXPIRATION);
    int otpExpiration = Integer.valueOf(expirationInSeconds);
    int otpExpirationInMinutes = Math.floorDiv(otpExpiration, SECONDS_IN_MINUTES);
    return String.valueOf(otpExpirationInMinutes);
  }
}
