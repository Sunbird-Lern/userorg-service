/** */
package org.sunbird.common.models.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.telemetry.util.TelemetryEvents;

/**
 * This class will used to log the project message in any level.
 *
 * @author Manzarul
 */
public class ProjectLogger {

  private static String eVersion = "1.0";
  private static String pVersion = "1.0";
  private static String dataId = "Sunbird";
  private static ObjectMapper mapper = new ObjectMapper();
  private static Logger rootLogger = (Logger) LogManager.getLogger("defaultLogger");
  //	private static TelemetryLmaxWriter lmaxWriter = TelemetryLmaxWriter.getInstance();

  /** To log only message. */
  public static void log(String message) {
    log(message, null, LoggerEnum.DEBUG.name());
  }

  public static void log(String message, Throwable e) {
    log(message, null, e);
  }

  public static void log(String message, Throwable e, Map<String, Object> telemetryInfo) {
    log(message, null, e);
    telemetryProcess(telemetryInfo, e);
  }

  private static void telemetryProcess(Map<String, Object> telemetryInfo, Throwable e) {

    ProjectCommonException projectCommonException = null;
    if (e instanceof ProjectCommonException) {
      projectCommonException = (ProjectCommonException) e;
    } else {
      projectCommonException =
          new ProjectCommonException(
              ResponseCode.internalError.getErrorCode(),
              ResponseCode.internalError.getErrorMessage(),
              ResponseCode.SERVER_ERROR.getResponseCode());
    }
    Request request = new Request();
    telemetryInfo.put(JsonKey.TELEMETRY_EVENT_TYPE, TelemetryEvents.ERROR.getName());

    Map<String, Object> params = (Map<String, Object>) telemetryInfo.get(JsonKey.PARAMS);
    params.put(JsonKey.ERROR, projectCommonException.getCode());
    params.put(JsonKey.STACKTRACE, generateStackTrace(e.getStackTrace()));
    request.setRequest(telemetryInfo);
    //		lmaxWriter.submitMessage(request);

  }

  private static String generateStackTrace(StackTraceElement[] elements) {
    StringBuilder builder = new StringBuilder("");
    for (StackTraceElement element : elements) {
      builder.append(element.toString());
    }
    return builder.toString();
  }

  public static void log(String message, String logLevel) {
    log(message, null, logLevel);
  }

  /** To log message, data in used defined log level. */
  public static void log(String message, LoggerEnum logEnum) {
    info(message, null, logEnum);
  }

  /** To log message, data in used defined log level. */
  public static void log(String message, Object data, String logLevel) {
    backendLog(message, data, null, logLevel);
  }

  /** To log exception with message and data. */
  public static void log(String message, Object data, Throwable e) {
    backendLog(message, data, e, LoggerEnum.ERROR.name());
  }

  /** To log exception with message and data for user specific log level. */
  public static void log(String message, Object data, Throwable e, String logLevel) {
    backendLog(message, data, e, logLevel);
  }

  private static void info(String message, Object data) {
    rootLogger.info(getBELogEvent(LoggerEnum.INFO.name(), message, data));
  }

  private static void info(String message, Object data, LoggerEnum loggerEnum) {
    rootLogger.info(getBELogEvent(LoggerEnum.INFO.name(), message, data, loggerEnum));
  }

  private static void debug(String message, Object data) {
    rootLogger.debug(getBELogEvent(LoggerEnum.DEBUG.name(), message, data));
  }

  private static void error(String message, Object data, Throwable exception) {
    rootLogger.error(getBELogEvent(LoggerEnum.ERROR.name(), message, data, exception));
  }

  private static void warn(String message, Object data, Throwable exception) {
    rootLogger.warn(getBELogEvent(LoggerEnum.WARN.name(), message, data, exception));
  }

  private static void backendLog(String message, Object data, Throwable e, String logLevel) {
    if (!StringUtils.isBlank(logLevel)) {

      switch (logLevel) {
        case "INFO":
          info(message, data);
          break;
        case "DEBUG":
          debug(message, data);
          break;
        case "WARN":
          warn(message, data, e);
          break;
        case "ERROR":
          error(message, data, e);
          break;
        default:
          debug(message, data);
          break;
      }
    }
  }

  private static String getBELogEvent(
      String logLevel, String message, Object data, LoggerEnum logEnum) {
    String logData = getBELog(logLevel, message, data, null, logEnum);
    return logData;
  }

  private static String getBELogEvent(String logLevel, String message, Object data) {
    String logData = getBELog(logLevel, message, data, null, null);
    return logData;
  }

  private static String getBELogEvent(String logLevel, String message, Object data, Throwable e) {
    String logData = getBELog(logLevel, message, data, e, null);
    return logData;
  }

  private static String getBELog(
      String logLevel, String message, Object data, Throwable exception, LoggerEnum logEnum) {
    String mid = dataId + "." + System.currentTimeMillis() + "." + UUID.randomUUID();
    long unixTime = System.currentTimeMillis();
    LogEvent te = new LogEvent();
    Map<String, Object> eks = new HashMap<String, Object>();
    eks.put(JsonKey.LEVEL, logLevel);
    eks.put(JsonKey.MESSAGE, message);
    String msgId = ExecutionContext.getRequestId();
    if (null != msgId) {
      eks.put(JsonKey.REQUEST_MESSAGE_ID, msgId);
    }
    if (null != data) {
      eks.put(JsonKey.DATA, data);
    }
    if (null != exception) {
      eks.put(JsonKey.STACKTRACE, ExceptionUtils.getStackTrace(exception));
    }
    if (logEnum != null) {
      te.setEid(logEnum.name());
    } else {
      te.setEid(LoggerEnum.BE_LOG.name());
    }
    te.setEts(unixTime);
    te.setMid(mid);
    te.setVer(eVersion);
    te.setContext(dataId, pVersion);
    String jsonMessage = null;
    try {
      te.setEdata(eks);
      jsonMessage = mapper.writeValueAsString(te);
    } catch (Exception e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    return jsonMessage;
  }
}
