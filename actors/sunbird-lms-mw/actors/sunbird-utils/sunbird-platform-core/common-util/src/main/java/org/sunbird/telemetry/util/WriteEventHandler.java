package org.sunbird.telemetry.util;

import com.lmax.disruptor.EventHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.telemetry.collector.TelemetryAssemblerFactory;
import org.sunbird.telemetry.collector.TelemetryDataAssembler;
import org.sunbird.telemetry.validator.TelemetryObjectValidator;
import org.sunbird.telemetry.validator.TelemetryObjectValidatorV3;

/**
 * Handler class for telemetry write event
 *
 * @author arvind
 */
public class WriteEventHandler implements EventHandler<Request> {

  private TelemetryFlush telemetryFlush = TelemetryFlush.getInstance();
  private TelemetryDataAssembler telemetryDataAssembler = TelemetryAssemblerFactory.get();
  private TelemetryObjectValidator telemetryObjectValidator = new TelemetryObjectValidatorV3();
  private SunbirdTelemetryEventConsumer consumer = SunbirdTelemetryEventConsumer.getInstance();

  @Override
  public void onEvent(Request request, long l, boolean b) throws Exception {
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
      ProjectLogger.log(
          "WriteEventHandler:onEvent: Exception in disruptor consumer - index: "
              + l
              + " exception = "
              + ex,
          LoggerEnum.ERROR.name());
    }
  }

  private boolean processLogEvent(Request request) {

    boolean success = false;
    Map<String, Object> context = (Map<String, Object>) request.getRequest().get(JsonKey.CONTEXT);
    Map<String, Object> params = (Map<String, Object>) request.getRequest().get(JsonKey.PARAMS);
    String telemetry = telemetryDataAssembler.log(context, params);
    if (StringUtils.isNotBlank(telemetry) && telemetryObjectValidator.validateLog(telemetry)) {
      telemetryFlush.flushTelemetry(telemetry);
      success = true;
    } else {
      ProjectLogger.log(
          "WriteEventHandler:processLogEvent: Audit Telemetry validation failed: ",
          telemetry,
          LoggerEnum.ERROR.name());
    }
    return success;
  }

  private boolean processErrorEvent(Request request) {

    boolean success = false;
    Map<String, Object> context = (Map<String, Object>) request.get(JsonKey.CONTEXT);
    Map<String, Object> params = (Map<String, Object>) request.get(JsonKey.PARAMS);
    String telemetry = telemetryDataAssembler.error(context, params);
    if (StringUtils.isNotBlank(telemetry) && telemetryObjectValidator.validateError(telemetry)) {
      telemetryFlush.flushTelemetry(telemetry);
      success = true;
    } else {
      ProjectLogger.log(
          "WriteEventHandler:processLogEvent: Error Telemetry validation failed: ",
          telemetry,
          LoggerEnum.ERROR.name());
    }
    return success;
  }

  private boolean processSearchEvent(Request request) {

    boolean success = false;
    Map<String, Object> context = (Map<String, Object>) request.get(JsonKey.CONTEXT);
    Map<String, Object> params = (Map<String, Object>) request.get(JsonKey.PARAMS);
    String telemetry = telemetryDataAssembler.search(context, params);
    if (StringUtils.isNotBlank(telemetry) && telemetryObjectValidator.validateSearch(telemetry)) {
      telemetryFlush.flushTelemetry(telemetry);
      success = true;
    } else {
      ProjectLogger.log(
          "WriteEventHandler:processLogEvent: Search Telemetry validation failed: ",
          telemetry,
          LoggerEnum.ERROR.name());
    }
    return success;
  }

  private boolean processAuditEvent(Request request) {
    boolean success = false;
    Map<String, Object> context = (Map<String, Object>) request.get(JsonKey.CONTEXT);
    Map<String, Object> targetObject = (Map<String, Object>) request.get(JsonKey.TARGET_OBJECT);
    List<Map<String, Object>> correlatedObjects =
        (List<Map<String, Object>>) request.get(JsonKey.CORRELATED_OBJECTS);
    Map<String, Object> params = (Map<String, Object>) request.get(JsonKey.PARAMS);
    params.put(JsonKey.TARGET_OBJECT, targetObject);
    params.put(JsonKey.CORRELATED_OBJECTS, correlatedObjects);
    String telemetry = telemetryDataAssembler.audit(context, params);
    if (StringUtils.isNotBlank(telemetry) && telemetryObjectValidator.validateAudit(telemetry)) {
      if (!Boolean.parseBoolean(
          ProjectUtil.getConfigValue(JsonKey.SUNBIRD_AUDIT_EVENT_BATCH_ALLOWED))) {
        ProjectLogger.log(
            "WriteEventHandler:processLogEvent: Audit Event is going to be processed = ",
            LoggerEnum.INFO.name());
        List<String> list = new ArrayList<String>();
        list.add(telemetry);
        Request auditRequest = telemetryFlush.createTelemetryRequest(list);
        consumer.consume(auditRequest);
      } else {
        telemetryFlush.flushTelemetry(telemetry);
      }
      success = true;
    } else {
      ProjectLogger.log(
          "WriteEventHandler:processLogEvent: Audit Telemetry validation failed: ",
          telemetry,
          LoggerEnum.ERROR.name());
    }
    return success;
  }
}
