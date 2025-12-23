package org.sunbird.actor.otp;

import org.apache.pekko.actor.ActorRef;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.datasecurity.impl.LogMaskServiceImpl;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.util.otp.OTPUtil;

public class SendOTPActor extends BaseActor {
  private final LogMaskServiceImpl logMaskService = new LogMaskServiceImpl();

  @Inject
  @Named("email_service_actor")
  private ActorRef emailServiceActor;

  @Override
  public void onReceive(Request request) throws Throwable {
    if (ActorOperations.SEND_OTP.getValue().equals(request.getOperation())) {
      sendOTP(request);
    } else {
      onReceiveUnsupportedOperation();
    }
  }

  private void sendOTP(Request request) {
    String type = (String) request.getRequest().get(JsonKey.TYPE);
    String key = (String) request.getRequest().get(JsonKey.KEY);
    String otp = (String) request.getRequest().get(JsonKey.OTP);
    String templateId = (String) request.getRequest().get(JsonKey.TEMPLATE_ID);
    if (JsonKey.EMAIL.equalsIgnoreCase(type)
        || JsonKey.PREV_USED_EMAIL.equalsIgnoreCase(type)
        || JsonKey.RECOVERY_EMAIL.equalsIgnoreCase(type)) {
      String userId = (String) request.get(JsonKey.USER_ID);
      logger.info(
          request.getRequestContext(),
          "SendOTPActor:sendOTP : Sending OTP via email for Key = "
              + logMaskService.maskEmail(key)
              + " or userId "
              + userId);
      sendOTPViaEmail(key, otp, templateId, request.getRequestContext());
    } else if (JsonKey.PHONE.equalsIgnoreCase(type)
        || JsonKey.PREV_USED_PHONE.equalsIgnoreCase(type)
        || JsonKey.RECOVERY_PHONE.equalsIgnoreCase(type)) {
      logger.info(
          request.getRequestContext(),
          "SendOTPActor:sendOTP : Sending OTP via sms for Key = " + logMaskService.maskPhone(key));
      sendOTPViaSMS(key, otp, templateId, request.getRequestContext());
    } else {
      logger.info(request.getRequestContext(), "SendOTPActor:sendOTP : No Email/Phone provided.");
    }
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    sender().tell(response, self());
  }

  private void sendOTPViaEmail(String key, String otp, String templateId, RequestContext context) {
    Map<String, Object> emailTemplateMap = new HashMap<>();
    emailTemplateMap.put(JsonKey.EMAIL, key);
    emailTemplateMap.put(JsonKey.OTP, otp);
    emailTemplateMap.put(JsonKey.OTP_EXPIRATION_IN_MINUTES, OTPUtil.getOTPExpirationInMinutes());
    emailTemplateMap.put(JsonKey.TEMPLATE_ID, templateId);
    Request emailRequest = OTPUtil.getRequestToSendOTPViaEmail(emailTemplateMap, context);
    emailRequest.setRequestContext(context);
    logger.info(
        context,
        "SendOTPActor:sendOTPViaEmail : Calling EmailServiceActor for Key = "
            + logMaskService.maskEmail(key));
    try {
      emailServiceActor.tell(emailRequest, self());
    } catch (Exception ex) {
      logger.error(context, "Exception while sending OTP via email", ex);
    }
  }

  private void sendOTPViaSMS(String key, String otp, String template, RequestContext context) {
    Map<String, Object> otpMap = new HashMap<>();
    otpMap.put(JsonKey.PHONE, key);
    otpMap.put(JsonKey.OTP, otp);
    otpMap.put(JsonKey.TEMPLATE_ID, template);
    otpMap.put(JsonKey.OTP_EXPIRATION_IN_MINUTES, OTPUtil.getOTPExpirationInMinutes());
    logger.info(
        context,
        "SendOTPActor:sendOTPViaSMS : Calling OTPUtil.sendOTPViaSMS for Key = "
            + logMaskService.maskPhone(key));
    OTPUtil.sendOTPViaSMS(otpMap, context);
  }
}
