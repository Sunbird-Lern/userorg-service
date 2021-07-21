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

public class TelemetryWriter {

  private static TelemetryDataAssembler telemetryDataAssembler = TelemetryAssemblerFactory.get();
  private static TelemetryObjectValidator telemetryObjectValidator =
      new TelemetryObjectValidatorV3();
  private static LoggerUtil logger = new LoggerUtil(TelemetryWriter.class);
  private static Logger telemetryEventLogger = LoggerFactory.getLogger("TelemetryEventLogger");

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

  private static void processLogEvent(Request request) {
    Map<String, Object> context = (Map<String, Object>) request.getRequest().get(JsonKey.CONTEXT);
    Map<String, Object> params = (Map<String, Object>) request.getRequest().get(JsonKey.PARAMS);
    String telemetry = telemetryDataAssembler.log(context, params);
    if (StringUtils.isNotBlank(telemetry) && telemetryObjectValidator.validateLog(telemetry)) {
      telemetryEventLogger.info(telemetry);
    } else {
      logger.info(
          "TelemetryWriter:processLogEvent: Audit Telemetry validation failed: " + telemetry);
    }
  }

  private static void processErrorEvent(Request request) {
    Map<String, Object> context = (Map<String, Object>) request.get(JsonKey.CONTEXT);
    Map<String, Object> params = (Map<String, Object>) request.get(JsonKey.PARAMS);
    String telemetry = telemetryDataAssembler.error(context, params);
    if (StringUtils.isNotBlank(telemetry) && telemetryObjectValidator.validateError(telemetry)) {
      telemetryEventLogger.info(telemetry);
    }
  }

  private static void processSearchEvent(Request request) {
    Map<String, Object> context = (Map<String, Object>) request.get(JsonKey.CONTEXT);
    Map<String, Object> params = (Map<String, Object>) request.get(JsonKey.PARAMS);
    String telemetry = telemetryDataAssembler.search(context, params);
    if (StringUtils.isNotBlank(telemetry) && telemetryObjectValidator.validateSearch(telemetry)) {
      telemetryEventLogger.info(telemetry);
    }
  }

  private static void processAuditEvent(Request request) {
    Map<String, Object> context = (Map<String, Object>) request.get(JsonKey.CONTEXT);
    Map<String, Object> targetObject = (Map<String, Object>) request.get(JsonKey.TARGET_OBJECT);
    List<Map<String, Object>> correlatedObjects =
        (List<Map<String, Object>>) request.get(JsonKey.CORRELATED_OBJECTS);
    Map<String, Object> params = (Map<String, Object>) request.get(JsonKey.PARAMS);
    Map<String, Object> props = (Map<String, Object>) params.get(JsonKey.PROPS);
    if (props != null && props.containsKey(JsonKey.TYPE)) {
      String type = (String) props.get(JsonKey.TYPE);
      params.put(JsonKey.TYPE, type);
    }
    params.put(JsonKey.TARGET_OBJECT, targetObject);
    params.put(JsonKey.CORRELATED_OBJECTS, correlatedObjects);
    String telemetry = telemetryDataAssembler.audit(context, params);
    if (StringUtils.isNotBlank(telemetry) && telemetryObjectValidator.validateAudit(telemetry)) {
      telemetryEventLogger.info(telemetry);
    }
  }
}
