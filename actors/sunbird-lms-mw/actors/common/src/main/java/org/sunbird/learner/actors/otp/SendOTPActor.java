package org.sunbird.learner.actors.otp;

import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;
import org.sunbird.learner.util.OTPUtil;

import java.util.HashMap;
import java.util.Map;

@ActorConfig(
  tasks = {},
  asyncTasks = {"sendOTP"}
)
public class SendOTPActor extends BaseActor {

  public static final String RESET_PASSWORD = "resetPassword";

  @Override
  public void onReceive(Request request) throws Throwable {

    if (ActorOperations.SEND_OTP.getValue().equals(request.getOperation())) {
      sendOTP(request);
    } else {
      onReceiveUnsupportedOperation("SendOTPActor");
    }
  }

  private void sendOTP(Request request) {
    String type = (String) request.getRequest().get(JsonKey.TYPE);
    String key = (String) request.getRequest().get(JsonKey.KEY);
    String otp = (String) request.getRequest().get(JsonKey.OTP);
    String template = (String) request.getRequest().get(JsonKey.TEMPLATE_ID);
    if (JsonKey.EMAIL.equalsIgnoreCase(type)
        || JsonKey.PREV_USED_EMAIL.equalsIgnoreCase(type)
        || JsonKey.RECOVERY_EMAIL.equalsIgnoreCase(type)) {
      String userId = (String) request.get(JsonKey.USER_ID);
      ProjectLogger.log(
          "SendOTPActor:sendOTP : Sending OTP via email for key "
              + key
              + " or userId "
              + userId
              + " otp is "
              + otp,
          LoggerEnum.INFO.name());
      sendOTPViaEmail(key, otp, userId, template);
    } else if (JsonKey.PHONE.equalsIgnoreCase(type)
        || JsonKey.PREV_USED_PHONE.equalsIgnoreCase(type)
        || JsonKey.RECOVERY_PHONE.equalsIgnoreCase(type)) {
      ProjectLogger.log(
          "SendOTPActor:sendOTP : Sending OTP via sms for key " + key + " otp is " + otp,
          LoggerEnum.INFO.name());
      sendOTPViaSMS(key, otp, template);
    }
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    sender().tell(response, self());
  }

  private void sendOTPViaEmail(String key, String otp, String otpType, String template) {
    Map<String, Object> emailTemplateMap = new HashMap<>();
    emailTemplateMap.put(JsonKey.EMAIL, key);
    emailTemplateMap.put(JsonKey.OTP, otp);
    emailTemplateMap.put(JsonKey.OTP_EXPIRATION_IN_MINUTES, OTPUtil.getOTPExpirationInMinutes());
    emailTemplateMap.put(JsonKey.TEMPLATE_ID, template);
    Request emailRequest = null;
    if (StringUtils.isBlank(otpType)) {
      emailRequest = OTPUtil.sendOTPViaEmail(emailTemplateMap);
    } else {
      emailRequest = OTPUtil.sendOTPViaEmail(emailTemplateMap, RESET_PASSWORD);
    }
    tellToAnother(emailRequest);
  }

  private void sendOTPViaSMS(String key, String otp, String template) {
    Map<String, Object> otpMap = new HashMap<>();
    otpMap.put(JsonKey.PHONE, key);
    otpMap.put(JsonKey.OTP, otp);
    otpMap.put(JsonKey.TEMPLATE_ID, template);
    otpMap.put(JsonKey.OTP_EXPIRATION_IN_MINUTES, OTPUtil.getOTPExpirationInMinutes());
    OTPUtil.sendOTPViaSMS(otpMap);
  }
}
