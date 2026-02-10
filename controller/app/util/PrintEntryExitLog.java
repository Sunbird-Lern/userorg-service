package util;

import static util.Common.createResponseParamObj;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.datasecurity.DataMaskingService;
import org.sunbird.datasecurity.impl.DefaultDataMaskServiceImpl;
import org.sunbird.datasecurity.impl.LogMaskServiceImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.operations.userorg.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import org.sunbird.response.ResponseParams;
import org.sunbird.logging.EntryExitLogEvent;
import org.sunbird.common.ProjectUtil;

public class PrintEntryExitLog {

  private static final LoggerUtil logger = new LoggerUtil(PrintEntryExitLog.class);
  private static final LogMaskServiceImpl logMaskService = new LogMaskServiceImpl();
  private static final DataMaskingService service = new DefaultDataMaskServiceImpl();
  private static final ObjectMapper objectMapper = new ObjectMapper();

  public static void printEntryLog(Request request) {
    try {
      EntryExitLogEvent entryLogEvent = getLogEvent(request, "ENTRY");
      List<Map<String, Object>> params = new ArrayList<>();
      Map<String, Object> reqMap = request.getRequest();
      Map<String, Object> newReqMap = SerializationUtils.clone(new HashMap<>(reqMap));
      String url = (String) request.getContext().get(JsonKey.URL);
      if (url.contains("otp")) {
        if (MapUtils.isNotEmpty(newReqMap)) {
          maskOtpAttributes(newReqMap);
        }
      }
      params.add(newReqMap);
      entryLogEvent.setEdataParams(params);
      logger.info(request.getRequestContext(), maskPIIData(entryLogEvent.toString()));
    } catch (Exception ex) {
      logger.error("Exception occurred while logging entry log", ex);
    }
  }

  public static void printExitLogOnSuccessResponse(
      org.sunbird.request.Request request, Response response) {
    try {
      if (ActorOperations.HEALTH_CHECK.getValue().equalsIgnoreCase(request.getOperation())) {
        return;
      }
      EntryExitLogEvent exitLogEvent = getLogEvent(request, "EXIT");
      String url = (String) request.getContext().get(JsonKey.URL);
      List<Map<String, Object>> params = new ArrayList<>();
      if (null != response) {
        if (MapUtils.isNotEmpty(response.getResult())) {
          if (null != url && url.equalsIgnoreCase("/private/user/v1/lookup")) {
            if (CollectionUtils.isNotEmpty(
                (List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE))) {
              List<Map<String, Object>> resList =
                  (List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE);
              params.add(resList.get(0));
            }
          } else {
            Map<String, Object> resMap = response.getResult();
            Map<String, Object> newRespMap = new HashMap<>();
            newRespMap.putAll(resMap);
            params.add(newRespMap);
          }
        }

        if (null != response.getParams()) {
          Map<String, Object> resParam = new HashMap<>();
          resParam.putAll(objectMapper.convertValue(response.getParams(), Map.class));
          resParam.put(JsonKey.RESPONSE_CODE, response.getResponseCode().getResponseCode());
          params.add(resParam);
        }
      }
      exitLogEvent.setEdataParams(params);
      logger.info(request.getRequestContext(), maskPIIData(exitLogEvent.toString()));
    } catch (Exception ex) {
      logger.error("Exception occurred while logging exit log", ex);
    }
  }

  public static void printExitLogOnFailure(
      org.sunbird.request.Request request, ProjectCommonException exception) {
    try {
      EntryExitLogEvent exitLogEvent = getLogEvent(request, "EXIT");
      String requestId = request.getRequestContext().getReqId();
      List<Map<String, Object>> params = new ArrayList<>();
      if (null == exception) {
        exception =
            new ProjectCommonException(
                ResponseCode.serverError,
                ResponseCode.serverError.getErrorMessage(),
                ResponseCode.SERVER_ERROR.getResponseCode());
      }

      ResponseCode code = exception.getResponseCodeEnum();
      if (code == null) {
        code = ResponseCode.SERVER_ERROR;
      }
      ResponseParams responseParams =
          createResponseParamObj(code, exception.getMessage(), requestId);
      if (responseParams != null) {
        responseParams.setErr(exception.getErrorCode());
        if (!StringUtils.isBlank(responseParams.getErrmsg())
            && responseParams.getErrmsg().contains("{0}")) {
          responseParams.setErrmsg(exception.getMessage());
        }
      }
      if (null != responseParams) {
        Map<String, Object> resParam = new HashMap<>();
        resParam.putAll(objectMapper.convertValue(responseParams, Map.class));
        resParam.put(JsonKey.RESPONSE_CODE, exception.getErrorResponseCode());
        params.add(resParam);
      }
      exitLogEvent.setEdataParams(params);
      logger.info(request.getRequestContext(), exitLogEvent.toString());
    } catch (Exception ex) {
      logger.error("Exception occurred while logging exit log", ex);
    }
  }

  private static EntryExitLogEvent getLogEvent(Request request, String logType) {
    EntryExitLogEvent entryLogEvent = new EntryExitLogEvent();
    entryLogEvent.setEid("LOG");
    String url = (String) request.getContext().get(JsonKey.URL);
    String entryLogMsg =
        logType
            + " LOG: method : "
            + request.getContext().get(JsonKey.METHOD)
            + ", url: "
            + maskPIIData(url)
            + " , For Operation : "
            + request.getOperation();
    String requestId =
        request.getRequestContext() != null ? request.getRequestContext().getReqId() : "";
    entryLogEvent.setEdata("system", "trace", requestId, entryLogMsg, null);
    return entryLogEvent;
  }

  private static String maskPIIData(String logString) {
    try {
      StringBuilder builder = new StringBuilder(logString);
      // Mask Email
      StringBuilder emailRegex = new StringBuilder(ProjectUtil.EMAIL_PATTERN);
      emailRegex.deleteCharAt(emailRegex.length() - 1);
      emailRegex.deleteCharAt(0);
      String EMAIL_PATTERN = emailRegex.toString();
      Pattern emailPattern = Pattern.compile(EMAIL_PATTERN);
      Matcher emailMatcher = emailPattern.matcher(logString);
      while (emailMatcher.find()) {
        String tempStr = emailMatcher.group();
        builder.replace(emailMatcher.start(), emailMatcher.end(), service.maskEmail(tempStr));
      }
      // Mask Phone
      String PHONE_PATTERN = "[0-9]{10}";
      Pattern phonePattern = Pattern.compile(PHONE_PATTERN);
      Matcher phoneMatcher = phonePattern.matcher(logString);
      while (phoneMatcher.find()) {
        String tempStr = phoneMatcher.group();
        if (ProjectUtil.validatePhone(tempStr, "")) {
          builder.replace(phoneMatcher.start(), phoneMatcher.end(), service.maskPhone(tempStr));
        }
      }
      return builder.toString();
    } catch (Exception ex) {
      logger.error("Exception occurred while masking PII data", ex);
    }
    return logString;
  }

  private static void maskOtpAttributes(Map<String, Object> otpReqMap) {
    String otp = (String) otpReqMap.get(JsonKey.OTP);
    if (StringUtils.isNotBlank(otp)) {
      otpReqMap.put(JsonKey.OTP, logMaskService.maskOTP(otp));
    }
  }
}
