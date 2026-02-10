package org.sunbird.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.telemetry.util.TelemetryEvents;
import org.sunbird.telemetry.util.TelemetryWriter;

import java.util.Map;

/**
 * Utility class for structured logging using SLF4J and Jackson.
 * Provides methods for logging info, debug, error, and warn messages with context and telemetry support.
 */
public class LoggerUtil {

  private Logger logger;
  private String infoLevel = "INFO";
  private String debugLevel = "DEBUG";
  private String errorLevel = "ERROR";
  private String warnLevel = "WARN";
  private Logger defaultLogger;
  private final ObjectMapper mapper = new ObjectMapper();

  /**
   * Constructor to initialize LoggerUtil for a specific class.
   *
   * @param c The class for which the logger is created.
   */
  public LoggerUtil(Class c) {
    logger = LoggerFactory.getLogger(c);
    defaultLogger = LoggerFactory.getLogger("defaultLogger");
  }

  /**
   * Logs an INFO message with structured data and request context.
   *
   * @param requestContext The request context containing tracing information.
   * @param message The message to log.
   * @param object Additional object data to log.
   * @param param Additional parameters to log.
   */
  public void info(
      RequestContext requestContext,
      String message,
      Map<String, Object> object,
      Map<String, Object> param) {
    if (requestContext != null) {
      requestContext.setLoggerLevel(infoLevel);
      logger.info(jsonMapper(requestContext, message, object, param));
    } else {
      defaultLogger.info(message);
    }
  }

  /**
   * Logs an INFO message with request context.
   *
   * @param requestContext The request context.
   * @param message The message to log.
   */
  public void info(RequestContext requestContext, String message) {
    info(requestContext, message, null, null);
  }

  /**
   * Logs a simple INFO message without context.
   *
   * @param message The message to log.
   */
  public void info(String message) {
    info(null, message, null, null);
  }

  /**
   * Logs a DEBUG message with structured data if debug is enabled.
   *
   * @param requestContext The request context.
   * @param message The message to log.
   * @param object Additional object data.
   * @param param Additional parameters.
   */
  public void debug(
      RequestContext requestContext,
      String message,
      Map<String, Object> object,
      Map<String, Object> param) {
    if (isDebugEnabled(requestContext)) {
      requestContext.setLoggerLevel(debugLevel);
      logger.info(jsonMapper(requestContext, message, object, param));
    } else {
      defaultLogger.debug(message);
    }
  }

  /**
   * Logs a DEBUG message with request context.
   *
   * @param requestContext The request context.
   * @param message The message to log.
   */
  public void debug(RequestContext requestContext, String message) {
    debug(requestContext, message, null, null);
  }

  /**
   * Logs a simple DEBUG message.
   *
   * @param message The message to log.
   */
  public void debug(String message) {
    debug(null, message, null, null);
  }

  /**
   * Logs an ERROR message with exception details and optional telemetry.
   *
   * @param requestContext The request context.
   * @param message The error message.
   * @param object Additional object data.
   * @param param Additional parameters.
   * @param e The exception/throwable.
   */
  public void error(
      RequestContext requestContext,
      String message,
      Map<String, Object> object,
      Map<String, Object> param,
      Throwable e) {
    if (requestContext != null) {
      requestContext.setLoggerLevel(errorLevel);
      logger.error(jsonMapper(requestContext, message, object, param), e);
    } else {
      defaultLogger.error(message, e);
    }
  }

  /**
   * Logs an ERROR message with context, telemetry info, and exception.
   *
   * @param requestContext The request context.
   * @param message The error message.
   * @param object Additional object data.
   * @param param Additional parameters.
   * @param e The exception.
   * @param telemetryInfo Telemetry information map.
   */
  public void error(
      RequestContext requestContext,
      String message,
      Map<String, Object> object,
      Map<String, Object> param,
      Throwable e,
      Map<String, Object> telemetryInfo) {
    if (requestContext != null) {
      requestContext.setLoggerLevel(errorLevel);
      logger.error(jsonMapper(requestContext, message, object, param), e);
    } else {
      defaultLogger.error(message, e);
    }
    telemetryProcess(requestContext, telemetryInfo, e);
  }

  /**
   * Logs an ERROR message with context and exception.
   *
   * @param requestContext The request context.
   * @param message The error message.
   * @param e The exception.
   */
  public void error(RequestContext requestContext, String message, Throwable e) {
    error(requestContext, message, null, null, e);
  }

  /**
   * Logs a simple ERROR message with exception.
   *
   * @param message The error message.
   * @param e The exception.
   */
  public void error(String message, Throwable e) {
    error(null, message, null, null, e);
  }

  /**
   * Logs an ERROR message with context, exception, and telemetry info.
   *
   * @param requestContext The request context.
   * @param message The error message.
   * @param e The exception.
   * @param telemetryInfo Telemetry data.
   */
  public void error(
      RequestContext requestContext, String message, Throwable e, Map<String, Object> telemetryInfo) {
    error(requestContext, message, null, null, e, telemetryInfo);
  }

  /**
   * Logs a WARN message with structured data.
   *
   * @param requestContext The request context.
   * @param message The warning message.
   * @param object Additional object data.
   * @param param Additional parameters.
   * @param e The exception (if any).
   */
  public void warn(
      RequestContext requestContext,
      String message,
      Map<String, Object> object,
      Map<String, Object> param,
      Throwable e) {
    if (requestContext != null) {
      requestContext.setLoggerLevel(warnLevel);
      logger.warn((jsonMapper(requestContext, message, object, param)), e);
    } else {
      defaultLogger.warn(message, e);
    }
  }

  /**
   * Logs a WARN message with context and exception.
   *
   * @param requestContext The request context.
   * @param message The warning message.
   * @param e The exception.
   */
  public void warn(RequestContext requestContext, String message, Throwable e) {
    warn(requestContext, message, null, null, e);
  }

  /**
   * Logs a simple WARN message with exception.
   *
   * @param message The warning message.
   * @param e The exception.
   */
  public void warn(String message, Throwable e) {
    warn(null, message, null, null, e);
  }

  /**
   * Checks if debug logging is enabled for the current request.
   *
   * @param requestContext The request context.
   * @return True if debug is enabled, false otherwise.
   */
  private static boolean isDebugEnabled(RequestContext requestContext) {
    return (null != requestContext
        && StringUtils.equalsIgnoreCase("true", requestContext.getDebugEnabled()));
  }

  /**
   * Processes telemetry for error events.
   *
   * @param requestContext The request context.
   * @param telemetryInfo The telemetry info map.
   * @param e The exception causing the error.
   */
  private void telemetryProcess(
      RequestContext requestContext, Map<String, Object> telemetryInfo, Throwable e) {
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
    Request request = new Request(requestContext);
    telemetryInfo.put(JsonKey.TELEMETRY_EVENT_TYPE, TelemetryEvents.ERROR.getName());

    Map<String, Object> params = (Map<String, Object>) telemetryInfo.get(JsonKey.PARAMS);
    params.put(JsonKey.ERROR, projectCommonException.getCode());
    params.put(JsonKey.STACKTRACE, generateStackTrace(e.getStackTrace()));
    request.setRequest(telemetryInfo);
    //		lmaxWriter.submitMessage(request);
    TelemetryWriter.write(request);
  }

  /**
   * Generates a string representation of the stack trace.
   *
   * @param elements Stack trace elements.
   * @return The stack trace as a string.
   */
  private String generateStackTrace(StackTraceElement[] elements) {
    StringBuilder builder = new StringBuilder("");
    for (StackTraceElement element : elements) {
      builder.append(element.toString());
    }
    return builder.toString();
  }

  /**
   * Converts log data into a JSON string using CustomLogFormat.
   *
   * @param requestContext The request context.
   * @param message The log message.
   * @param object Additional object data.
   * @param param Additional parameters.
   * @return JSON string of the log event.
   */
  private String jsonMapper(
      RequestContext requestContext,
      String message,
      Map<String, Object> object,
      Map<String, Object> param) {
    try {
      return mapper.writeValueAsString(
          new CustomLogFormat(requestContext, message, object, param).getEventMap());
    } catch (JsonProcessingException e) {
      error(requestContext, e.getMessage(), e);
    }
    return "";
  }
}
