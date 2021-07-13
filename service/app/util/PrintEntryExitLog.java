package util;

import static util.Common.createResponseParamObj;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.response.ResponseParams;
import org.sunbird.operations.ActorOperations;
import org.sunbird.common.models.util.EntryExitLogEvent;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerUtil;
import org.sunbird.datasecurity.impl.LogMaskServiceImpl;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

public class PrintEntryExitLog {

  private static LoggerUtil logger = new LoggerUtil(PrintEntryExitLog.class);
  private static LogMaskServiceImpl logMaskService = new LogMaskServiceImpl();
  private static ObjectMapper objectMapper = new ObjectMapper();

  public static void printEntryLog(Request request) {
    try {
      EntryExitLogEvent entryLogEvent = getLogEvent(request, "ENTRY");
      List<Map<String, Object>> params = new ArrayList<>();
      Map<String, Object> reqMap = request.getRequest();
      Map<String, Object> newReqMap = SerializationUtils.clone(new HashMap<>(reqMap));
      String url = (String) request.getContext().get(JsonKey.URL);
      if (url.contains("search")) {
        Map<String, Object> filters = (Map<String, Object>) newReqMap.get(JsonKey.FILTERS);
        if (MapUtils.isNotEmpty(filters)) {
          maskAttributes(filters);
        }
      }
      if (url.contains("otp")) {
        if (MapUtils.isNotEmpty(newReqMap)) {
          maskOtpAttributes(newReqMap);
        }
      }
      if (url.contains("lookup")) {
        if (MapUtils.isNotEmpty(newReqMap)) {
          maskId((String) newReqMap.get(JsonKey.VALUE), (String) newReqMap.get(JsonKey.KEY));
        }
      }
      maskAttributes(newReqMap);
      params.add(newReqMap);
      entryLogEvent.setEdataParams(params);
      logger.info(request.getRequestContext(), entryLogEvent.toString());
    } catch (Exception ex) {
      logger.error("Exception occurred while logging entry log", ex);
    }
  }

  public static void printExitLogOnSuccessResponse(
      org.sunbird.common.request.Request request, Response response) {
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
            maskAttributes(newRespMap);
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
      logger.info(request.getRequestContext(), exitLogEvent.toString());
    } catch (Exception ex) {
      logger.error("Exception occurred while logging exit log", ex);
    }
  }

  public static void printExitLogOnFailure(
      org.sunbird.common.request.Request request, ProjectCommonException exception) {
    try {
      EntryExitLogEvent exitLogEvent = getLogEvent(request, "EXIT");
      String requestId = request.getRequestContext().getReqId();
      List<Map<String, Object>> params = new ArrayList<>();
      if (null == exception) {
        exception =
            new ProjectCommonException(
                ResponseCode.internalError.getErrorCode(),
                ResponseCode.internalError.getErrorMessage(),
                ResponseCode.SERVER_ERROR.getResponseCode());
      }

      ResponseCode code = ResponseCode.getResponse(exception.getCode());
      if (code == null) {
        code = ResponseCode.SERVER_ERROR;
      }
      ResponseParams responseParams =
          createResponseParamObj(code, exception.getMessage(), requestId);
      if (responseParams != null) {
        responseParams.setStatus(JsonKey.FAILED);
        if (exception.getCode() != null) {
          responseParams.setStatus(JsonKey.FAILED);
        }
        if (!StringUtils.isBlank(responseParams.getErrmsg())
            && responseParams.getErrmsg().contains("{0}")) {
          responseParams.setErrmsg(exception.getMessage());
        }
      }
      if (null != responseParams) {
        Map<String, Object> resParam = new HashMap<>();
        resParam.putAll(objectMapper.convertValue(responseParams, Map.class));
        resParam.put(JsonKey.RESPONSE_CODE, exception.getResponseCode());
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
            + url
            + " , For Operation : "
            + request.getOperation();
    String requestId =
        request.getRequestContext() != null ? request.getRequestContext().getReqId() : "";
    entryLogEvent.setEdata("system", "trace", requestId, entryLogMsg, null);
    return entryLogEvent;
  }

  private static String maskId(String value, String type) {
    if (JsonKey.EMAIL.equalsIgnoreCase(type)) {
      return logMaskService.maskEmail(value);
    } else if (JsonKey.PHONE.equalsIgnoreCase(type)) {
      return logMaskService.maskPhone(value);
    } else if (JsonKey.OTP.equalsIgnoreCase(type)) {
      return logMaskService.maskOTP(value);
    }
    return "";
  }

  private static void maskAttributes(Map<String, Object> filters) {
    String phone = (String) filters.get(JsonKey.PHONE);
    if (StringUtils.isNotBlank(phone)) {
      filters.put(JsonKey.PHONE, maskId(phone, JsonKey.PHONE));
    }
    String email = (String) filters.get(JsonKey.EMAIL);
    if (StringUtils.isNotBlank(email)) {
      filters.put(JsonKey.EMAIL, maskId(email, JsonKey.EMAIL));
    }
    String otp = (String) filters.get(JsonKey.OTP);
    if (StringUtils.isNotBlank(otp)) {
      filters.put(JsonKey.OTP, maskId(otp, JsonKey.OTP));
    }
    String password = (String) filters.get(JsonKey.PASSWORD);
    if (StringUtils.isNotBlank(password)) {
      filters.put(JsonKey.PASSWORD, "**********");
    }
  }

  private static void maskOtpAttributes(Map<String, Object> otpReqMap) {
    String type = (String) otpReqMap.get(JsonKey.TYPE);
    String key = (String) otpReqMap.get(JsonKey.KEY);
    String otp = (String) otpReqMap.get(JsonKey.OTP);
    if (StringUtils.isNotBlank(type)) {
      if (type.equalsIgnoreCase(JsonKey.PHONE)) {
        otpReqMap.put(JsonKey.TYPE, maskId(key, JsonKey.PHONE));
      }
      if (type.equalsIgnoreCase(JsonKey.EMAIL)) {
        otpReqMap.put(JsonKey.TYPE, maskId(key, JsonKey.EMAIL));
      }
    }
    if (StringUtils.isNotBlank(otp)) {
      otpReqMap.put(JsonKey.OTP, maskId(otp, JsonKey.OTP));
    }
  }
}
