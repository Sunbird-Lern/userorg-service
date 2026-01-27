package org.sunbird.telemetry.util;

import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.Request;
import org.sunbird.telemetry.collector.TelemetryAssemblerFactory;
import org.sunbird.telemetry.collector.TelemetryDataAssembler;
import org.sunbird.telemetry.validator.TelemetryObjectValidator;
import org.sunbird.telemetry.validator.TelemetryObjectValidatorV3;

/**
 * This class writes telemetry events to the configured logger.
 * It processes different types of telemetry events such as AUDIT, SEARCH, ERROR, and LOG.
 */
public class TelemetryWriter {

  private static final TelemetryDataAssembler telemetryDataAssembler =
      TelemetryAssemblerFactory.get();
  private static final TelemetryObjectValidator telemetryObjectValidator =
      new TelemetryObjectValidatorV3();
  private static final LoggerUtil logger = new LoggerUtil(TelemetryWriter.class);
  private static final Logger telemetryEventLogger =
      LoggerFactory.getLogger("TelemetryEventLogger");

  /**
   * Private constructor to prevent instantiation.
   */
  private TelemetryWriter() {}

  /**
   * Writes the telemetry event based on the request content.
   *
   * @param request The request object containing telemetry data.
   */
  public static void write(Request request) {
    try {
      String eventType = (String) request.getRequest().get(JsonKey.TELEMETRY_EVENT_TYPE);

      if (TelemetryEvents.AUDIT.getName().equalsIgnoreCase(eventType)) {
        processAuditEvent(request);
      } else if (TelemetryEvents.SEARCH.getName().equalsIgnoreCase(eventType)) {
        processSearchEvent(request);
      } else if (TelemetryEvents.ERROR.getName().equalsIgnoreCase(eventType)) {
        processErrorEvent(request);
      } else if (TelemetryEvents.LOG.getName().equalsIgnoreCase(eventType)) {
        processLogEvent(request);
      }
    } catch (Exception ex) {
      logger.info("Exception occurred while writing telemetry");
    }
  }

  /**
   * Processes LOG telemetry events.
   *
   * @param request The request object.
   */
  private static void processLogEvent(Request request) {
    Map<String, Object> context = (Map<String, Object>) request.getRequest().get(JsonKey.CONTEXT);
    Map<String, Object> params = (Map<String, Object>) request.getRequest().get(JsonKey.PARAMS);
    String telemetry = telemetryDataAssembler.log(context, params);
    
    // Validate and write telemetry event
    if (StringUtils.isNotBlank(telemetry) && telemetryObjectValidator.validateLog(telemetry)) {
      telemetryEventLogger.info(telemetry);
    } else {
      logger.info(
          "TelemetryWriter:processLogEvent: Audit Telemetry validation failed: " + telemetry);
    }
  }

  /**
   * Processes ERROR telemetry events.
   *
   * @param request The request object.
   */
  private static void processErrorEvent(Request request) {
    Map<String, Object> context = (Map<String, Object>) request.get(JsonKey.CONTEXT);
    Map<String, Object> params = (Map<String, Object>) request.get(JsonKey.PARAMS);
    String telemetry = telemetryDataAssembler.error(context, params);
    
    // Validate and write telemetry event
    if (StringUtils.isNotBlank(telemetry) && telemetryObjectValidator.validateError(telemetry)) {
      telemetryEventLogger.info(telemetry);
    }
  }

  /**
   * Processes SEARCH telemetry events.
   *
   * @param request The request object.
   */
  private static void processSearchEvent(Request request) {
    Map<String, Object> context = (Map<String, Object>) request.get(JsonKey.CONTEXT);
    Map<String, Object> params = (Map<String, Object>) request.get(JsonKey.PARAMS);
    String telemetry = telemetryDataAssembler.search(context, params);
    
    // Validate and write telemetry event
    if (StringUtils.isNotBlank(telemetry) && telemetryObjectValidator.validateSearch(telemetry)) {
      telemetryEventLogger.info(telemetry);
    }
  }

  /**
   * Processes AUDIT telemetry events.
   *
   * @param request The request object.
   */
  private static void processAuditEvent(Request request) {
    Map<String, Object> context = (Map<String, Object>) request.get(JsonKey.CONTEXT);
    Map<String, Object> targetObject = (Map<String, Object>) request.get(JsonKey.TARGET_OBJECT);
    List<Map<String, Object>> correlatedObjects =
        (List<Map<String, Object>>) request.get(JsonKey.CORRELATED_OBJECTS);
    Map<String, Object> params = (Map<String, Object>) request.get(JsonKey.PARAMS);
    Map<String, Object> props = (Map<String, Object>) params.get(JsonKey.PROPS);
    
    // Check for type in props and add to params if present
    if (props != null && props.containsKey(JsonKey.TYPE)) {
      String type = (String) props.get(JsonKey.TYPE);
      params.put(JsonKey.TYPE, type);
    }
    params.put(JsonKey.TARGET_OBJECT, targetObject);
    params.put(JsonKey.CORRELATED_OBJECTS, correlatedObjects);
    
    String telemetry = telemetryDataAssembler.audit(context, params);
    
    // Validate and write telemetry event
    if (StringUtils.isNotBlank(telemetry) && telemetryObjectValidator.validateAudit(telemetry)) {
      telemetryEventLogger.info(telemetry);
    }
  }
}
