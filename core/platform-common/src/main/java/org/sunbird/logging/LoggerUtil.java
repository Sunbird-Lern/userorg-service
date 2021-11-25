package org.sunbird.logging;

import java.util.HashMap;
import java.util.Map;
import net.logstash.logback.marker.Markers;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.telemetry.util.TelemetryEvents;
import org.sunbird.telemetry.util.TelemetryWriter;

public class LoggerUtil {

  private Logger logger;

  public LoggerUtil(Class c) {
    logger = LoggerFactory.getLogger(c);
  }

  public void info(RequestContext requestContext, String message) {
    if (null != requestContext) {
      logger.info(Markers.appendEntries(requestContext.getContextMap()), message);
    } else {
      logger.info(message);
    }
  }

  public void info(String message) {
    logger.info(message);
  }

  public void error(RequestContext requestContext, String message, Throwable e) {
    Map<String, Object> context =
        (Map<String, Object>) requestContext.getTelemetryContext().get(JsonKey.CONTEXT);
    Map<String, Object> params = new HashMap<>();
    params.put(JsonKey.ERR_TYPE, JsonKey.API_ACCESS);
    Map<String, Object> telemetryInfo = new HashMap<>();
    telemetryInfo.put(JsonKey.CONTEXT, context);
    telemetryInfo.put(JsonKey.PARAMS, params);
    error(requestContext, message, e, telemetryInfo);
  }

  public void error(String message, Throwable e) {
    logger.error(message, e);
  }

  public void error(
      RequestContext requestContext,
      String message,
      Throwable e,
      Map<String, Object> telemetryInfo) {

    telemetryProcess(requestContext, telemetryInfo, e, message);
  }

  public void warn(RequestContext requestContext, String message, Throwable e) {
    if (null != requestContext) {
      logger.warn(Markers.appendEntries(requestContext.getContextMap()), message, e);
    } else {
      logger.warn(message, e);
    }
  }

  public void debug(RequestContext requestContext, String message) {
    if (isDebugEnabled(requestContext)) {
      logger.info(Markers.appendEntries(requestContext.getContextMap()), message);
    } else {
      logger.debug(message);
    }
  }

  public void debug(String message) {
    logger.debug(message);
  }

  private static boolean isDebugEnabled(RequestContext requestContext) {
    return (null != requestContext
        && StringUtils.equalsIgnoreCase("true", requestContext.getDebugEnabled()));
  }

  private void telemetryProcess(
      RequestContext requestContext,
      Map<String, Object> telemetryInfo,
      Throwable e,
      String message) {
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
    request.setRequestContext(requestContext);
    telemetryInfo.put(JsonKey.TELEMETRY_EVENT_TYPE, TelemetryEvents.ERROR.getName());

    Map<String, Object> params = (Map<String, Object>) telemetryInfo.get(JsonKey.PARAMS);
    params.put(JsonKey.ERROR, projectCommonException.getCode());
    params.put(JsonKey.STACKTRACE, generateStackTrace(e.getStackTrace(), message));
    request.setRequest(telemetryInfo);
    TelemetryWriter.write(request);
  }

  private String generateStackTrace(StackTraceElement[] elements, String errMsg) {
    StringBuilder builder = new StringBuilder(errMsg + " ");
    for (StackTraceElement element : elements) {
      builder.append(element.toString());
    }
    return builder.toString();
  }
}
