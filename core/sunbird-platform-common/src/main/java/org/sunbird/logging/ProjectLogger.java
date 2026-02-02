package org.sunbird.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.logstash.logback.argument.StructuredArguments;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.ResponseCode;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.keys.JsonKey;
import org.sunbird.telemetry.util.TelemetryEvents;
import org.sunbird.telemetry.util.TelemetryWriter;

/**
 * Legacy logger class for project-level logging provided for backward compatibility.
 *
 * @deprecated Use {@link LoggerUtil} for all logging operations. This class will be removed in future versions.
 */
@Deprecated
public class ProjectLogger {

  private static String eVersion = "1.0";
  private static String pVersion = "1.0";
  private static String dataId = "Sunbird";
  private static ObjectMapper mapper = new ObjectMapper();
  private static Logger rootLogger = LoggerFactory.getLogger("defaultLogger");
  private static Logger queryLogger = LoggerFactory.getLogger("queryLogger");

  private ProjectLogger() {}

  /**
   * Logs a message with default log level (DEBUG).
   *
   * @param message Text message to be logged.
   */
  public static void log(String message) {
    log(message, null, LoggerEnum.DEBUG.name());
  }

  /**
   * Logs an exception message.
   *
   * @param message The message.
   * @param e The exception.
   */
  public static void log(String message, Throwable e) {
    log(message, null, e);
  }

  /**
   * Logs a message with exception and telemetry information.
   *
   * @param message The message.
   * @param e The exception.
   * @param telemetryInfo Telemetry data.
   */
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
    TelemetryWriter.write(request);
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

  /**
   * Logs a message with a specific LoggerEnum level.
   *
   * @param message The message.
   * @param logEnum The log level.
   */
  public static void log(String message, LoggerEnum logEnum) {
    info(message, null, logEnum);
  }

  /**
   * Logs message and data with a specific string log level.
   *
   * @param message The message.
   * @param data The data object.
   * @param logLevel The log level string.
   */
  public static void log(String message, Object data, String logLevel) {
    backendLog(message, data, null, logLevel);
  }

  /**
   * Logs message, data, and exception.
   *
   * @param message The message.
   * @param data The data object.
   * @param e The exception.
   */
  public static void log(String message, Object data, Throwable e) {
    backendLog(message, data, e, LoggerEnum.ERROR.name());
  }

  /**
   * Logs message, data, exception with a specific log level.
   *
   * @param message The message.
   * @param data The data object.
   * @param e The exception.
   * @param logLevel The log level.
   */
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
    return getBELog(logLevel, message, data, null, logEnum);
  }

  private static String getBELogEvent(String logLevel, String message, Object data) {
    return getBELog(logLevel, message, data, null, null);
  }

  private static String getBELogEvent(String logLevel, String message, Object data, Throwable e) {
    return getBELog(logLevel, message, data, e, null);
  }

  private static String getBELog(
      String logLevel, String message, Object data, Throwable exception, LoggerEnum logEnum) {
    String mid = dataId + "." + System.currentTimeMillis() + "." + UUID.randomUUID();
    long unixTime = System.currentTimeMillis();
    LogEvent te = new LogEvent();
    Map<String, Object> eks = new HashMap<>();
    eks.put(JsonKey.LEVEL, logLevel);
    eks.put(JsonKey.MESSAGE, message);

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
      // Avoid recursive calls to ProjectLogger.log if exception happens here
      rootLogger.error(e.getMessage(), e);
    }
    return jsonMessage;
  }

  public static void logQuery(String query, RequestContext requestContext) {
    if (isDebugEnabled(requestContext)) {
      queryLogger.debug(query, StructuredArguments.entries(requestContext.getContextMap()));
    } else {
      queryLogger.debug(query);
    }
  }

  private static boolean isDebugEnabled(RequestContext requestContext) {
    return (null != requestContext
        && StringUtils.equalsIgnoreCase("true", requestContext.getDebugEnabled()));
  }
}
