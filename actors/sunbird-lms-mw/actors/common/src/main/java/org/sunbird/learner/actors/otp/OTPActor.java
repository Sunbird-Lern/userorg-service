package org.sunbird.learner.actors.otp;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.ClientErrorResponse;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.*;
import org.sunbird.common.models.util.datasecurity.impl.LogMaskServiceImpl;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.learner.actors.otp.service.OTPService;
import org.sunbird.learner.util.OTPUtil;
import org.sunbird.learner.util.Util;
import org.sunbird.ratelimit.limiter.OtpRateLimiter;
import org.sunbird.ratelimit.limiter.RateLimiter;
import org.sunbird.ratelimit.service.RateLimitService;
import org.sunbird.ratelimit.service.RateLimitServiceImpl;

import java.text.MessageFormat;
import java.util.Map;

@ActorConfig(
  tasks = {"generateOTP", "verifyOTP"},
  asyncTasks = {}
)
public class OTPActor extends BaseActor {

  private OTPService otpService = new OTPService();
  private static final String SUNBIRD_OTP_ALLOWED_ATTEMPT = "sunbird_otp_allowed_attempt";
  private static final String REMAINING_ATTEMPT = "remainingAttempt";
  private static final String MAX_ALLOWED_ATTEMPT = "maxAllowedAttempt";
  private RateLimitService rateLimitService = new RateLimitServiceImpl();
  private LogMaskServiceImpl logMaskService = new LogMaskServiceImpl();

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    if (ActorOperations.GENERATE_OTP.getValue().equals(request.getOperation())) {
      generateOTP(request);
    } else if (ActorOperations.VERIFY_OTP.getValue().equals(request.getOperation())) {
      verifyOTP(request);
    } else {
      onReceiveUnsupportedOperation("OTPActor");
    }
  }

  private String maskOTP(String otp) {
      return logMaskService.maskOTP(otp);
  }

  private String maskId(String id, String type) {
    if (JsonKey.EMAIL.equalsIgnoreCase(type)) {
      return logMaskService.maskEmail(id);
    } else if (JsonKey.PHONE.equalsIgnoreCase(type)) {
      return logMaskService.maskPhone(id);
    }
    return "";
  }

  private void generateOTP(Request request) {
    ProjectLogger.log("OTPActor:generateOTP method call start.", LoggerEnum.INFO.name());
    String type = (String) request.getRequest().get(JsonKey.TYPE);
    String key = getKey(type, request);
    String userId = (String) request.getRequest().get(JsonKey.USER_ID);
    if (StringUtils.isNotBlank(userId)) {
      key = OTPUtil.getEmailPhoneByUserId(userId, type);
      type = getType(type);
    }
    rateLimitService.throttleByKey(
        key, new RateLimiter[] {OtpRateLimiter.HOUR, OtpRateLimiter.DAY});

    String otp = null;
    Map<String, Object> details = otpService.getOTPDetails(type, key);

    if (MapUtils.isEmpty(details)) {
      otp = OTPUtil.generateOTP();
      ProjectLogger.log(
          "OTPActor:generateOTP: inserting otp Key = "
              + maskId(key, type)
              + " OTP = "
              + maskOTP(otp),
          LoggerEnum.INFO.name());
      otpService.insertOTPDetails(type, key, otp);
    } else {
      otp = (String) details.get(JsonKey.OTP);
      ProjectLogger.log(
              "OTPActor:generateOTP: Re-issuing otp Key = "
                      + maskId(key, type)
                      + " OTP = "
                      + maskOTP(otp),
              LoggerEnum.INFO.name());
    }

    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    sender().tell(response, self());

    sendOTP(request, otp, key);
  }

  private String getType(String type) {
    switch (type) {
      case JsonKey.PREV_USED_EMAIL:
        return JsonKey.EMAIL;
      case JsonKey.PREV_USED_PHONE:
        return JsonKey.PHONE;
      case JsonKey.EMAIL:
        return JsonKey.EMAIL;
      case JsonKey.PHONE:
        return JsonKey.PHONE;
      case JsonKey.RECOVERY_EMAIL:
        return JsonKey.EMAIL;
      case JsonKey.RECOVERY_PHONE:
        return JsonKey.PHONE;
      default:
        return null;
    }
  }

  private void verifyOTP(Request request) {
    String type = (String) request.getRequest().get(JsonKey.TYPE);
    String key = getKey(type, request);
    String otpInRequest = (String) request.getRequest().get(JsonKey.OTP);

    String userId = (String) request.getRequest().get(JsonKey.USER_ID);
    if (StringUtils.isNotBlank(userId)) {
      key = OTPUtil.getEmailPhoneByUserId(userId, type);
      type = getType(type);
    }
    Map<String, Object> otpDetails = otpService.getOTPDetails(type, key);

    if (MapUtils.isEmpty(otpDetails)) {
      ProjectLogger.log(
          "OTPActor:verifyOTP: Details not found for Key = "
              + maskId(key, type)
              + " type = "
              + type,
          LoggerEnum.INFO.name());
      ProjectCommonException.throwClientErrorException(ResponseCode.errorInvalidOTP);
    }
    String otpInDB = (String) otpDetails.get(JsonKey.OTP);
    if (StringUtils.isBlank(otpInDB) || StringUtils.isBlank(otpInRequest)) {
      ProjectLogger.log(
          "OTPActor:verifyOTP: Mismatch for Key = "
              + maskId(key, type) + " otpInRequest = "
              + maskOTP(otpInRequest)
              + " otpInDB = "
              + maskOTP(otpInDB),
          LoggerEnum.DEBUG);
      ProjectCommonException.throwClientErrorException(ResponseCode.errorInvalidOTP);
    }

    if (otpInRequest.equals(otpInDB)) {
      ProjectLogger.log(
          "OTPActor:verifyOTP: Verified successfully Key = "
              + maskId(key, type),
          LoggerEnum.INFO.name());
      otpService.deleteOtp(type, key);
      Response response = new Response();
      response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
      sender().tell(response, self());
    } else {
      ProjectLogger.log(
          "OTPActor:verifyOTP: Incorrect OTP Key = "
              + maskId(key, type)
                  + " otpInRequest = "
                  + maskOTP(otpInRequest)
                  + " otpInDB = "
                  + maskOTP(otpInDB),
          LoggerEnum.INFO.name());
      handleMismatchOtp(type, key, otpDetails);
    }
  }

  private void handleMismatchOtp(String type, String key, Map<String, Object> otpDetails) {
    int remainingCount = getRemainingAttemptedCount(otpDetails);
    ProjectLogger.log(
        "OTPActor:handleMismatchOtp: Key = "
            + maskId(key, type)
            + ",remaining attempt is "
            + remainingCount,
        LoggerEnum.INFO.name());
    int attemptedCount = (int) otpDetails.get(JsonKey.ATTEMPTED_COUNT);
    if (remainingCount <= 0) {
      otpService.deleteOtp(type, key);
    } else {
      otpDetails.put(JsonKey.ATTEMPTED_COUNT, attemptedCount + 1);
      otpService.updateAttemptCount(otpDetails);
    }
    ProjectCommonException ex =
        new ProjectCommonException(
            ResponseCode.otpVerificationFailed.getErrorCode(),
            MessageFormat.format(
                ResponseCode.otpVerificationFailed.getErrorMessage(), remainingCount),
            ResponseCode.CLIENT_ERROR.getResponseCode());

    ClientErrorResponse response = new ClientErrorResponse();
    response.setException(ex);
    response
        .getResult()
        .put(
                MAX_ALLOWED_ATTEMPT,
            Integer.parseInt(ProjectUtil.getConfigValue(SUNBIRD_OTP_ALLOWED_ATTEMPT)));
    response.getResult().put(REMAINING_ATTEMPT, remainingCount);
    sender().tell(response, self());
  }

  private int getRemainingAttemptedCount(Map<String, Object> otpDetails) {
    int allowedAttempt = Integer.parseInt(ProjectUtil.getConfigValue(SUNBIRD_OTP_ALLOWED_ATTEMPT));
    int attemptedCount = (int) otpDetails.get(JsonKey.ATTEMPTED_COUNT);
    return (allowedAttempt - (attemptedCount + 1));
  }

  private void sendOTP(Request request, String otp, String key) {
    Request sendOtpRequest = new Request();
    sendOtpRequest.getRequest().putAll(request.getRequest());
    sendOtpRequest.getRequest().put(JsonKey.KEY, key);
    sendOtpRequest.getRequest().put(JsonKey.OTP, otp);
    sendOtpRequest.setOperation(ActorOperations.SEND_OTP.getValue());
    tellToAnother(sendOtpRequest);
  }

  private String getKey(String type, Request request) {
    String key = (String) request.getRequest().get(JsonKey.KEY);
    if (JsonKey.EMAIL.equalsIgnoreCase(type) && key != null) {
      return key.toLowerCase();
    }
    return key;
  }
}
