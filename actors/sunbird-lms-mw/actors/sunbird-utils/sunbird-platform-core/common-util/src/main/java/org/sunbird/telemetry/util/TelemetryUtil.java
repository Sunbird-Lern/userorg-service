package org.sunbird.telemetry.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.HeaderParam;
import org.sunbird.common.request.Request;

/** @author arvind */
public final class TelemetryUtil {

  private TelemetryUtil() {}

  public static Map<String, Object> generateTargetObject(
      String id, String type, String currentState, String prevState) {

    Map<String, Object> target = new HashMap<>();
    target.put(JsonKey.ID, id);
    target.put(JsonKey.TYPE, StringUtils.capitalize(type));
    target.put(JsonKey.CURRENT_STATE, currentState);
    target.put(JsonKey.PREV_STATE, prevState);
    return target;
  }

  public static Map<String, Object> genarateTelemetryRequest(
      Map<String, Object> targetObject,
      List<Map<String, Object>> correlatedObject,
      String eventType,
      Map<String, Object> params) {

    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.TARGET_OBJECT, targetObject);
    map.put(JsonKey.CORRELATED_OBJECTS, correlatedObject);
    map.put(JsonKey.TELEMETRY_EVENT_TYPE, eventType);
    map.put(JsonKey.PARAMS, params);

    // combine context info into one i.e. request level and system level info into
    // one place...

    Map<String, Object> context = getTelemetryContext();
    map.put(JsonKey.CONTEXT, context);
    return map;
  }

  public static void generateCorrelatedObject(
      String id, String type, String corelation, List<Map<String, Object>> correlationList) {

    Map<String, Object> correlatedObject = new HashMap<>();
    correlatedObject.put(JsonKey.ID, id);
    correlatedObject.put(JsonKey.TYPE, StringUtils.capitalize(type));
    correlatedObject.put(JsonKey.RELATION, corelation);

    correlationList.add(correlatedObject);
  }

  public static Map<String, Object> genarateTelemetryInfoForError(String objectType) {

    Map<String, Object> map = new HashMap<>();
    Map<String, Object> contextInfo = TelemetryUtil.getTelemetryContext();

    Map<String, Object> params = new HashMap<>();
    params.put(JsonKey.OBJECT_TYPE, objectType);
    params.put(JsonKey.ERR_TYPE, JsonKey.API_ACCESS);

    map.put(JsonKey.CONTEXT, contextInfo);
    map.put(JsonKey.PARAMS, params);
    return map;
  }

  public static Map<String, Object> getTelemetryContext() {

    Map<String, Object> context = new HashMap<>();
    context.putAll(ExecutionContext.getCurrent().getRequestContext());
    context.putAll(ExecutionContext.getCurrent().getGlobalContext());
    return context;
  }

  public static void addTargetObjectRollUp(
      Map<String, String> rollUpMap, Map<String, Object> targetObject) {
    targetObject.put(JsonKey.ROLLUP, rollUpMap);
  }

  public static void telemetryProcessingCall(
      Map<String, Object> request,
      Map<String, Object> targetObject,
      List<Map<String, Object>> correlatedObject) {
    Map<String, Object> params = new HashMap<>();
    // set additional props for edata related things that will be used for getting the requested
    // fields name while telemetry processing
    params.put(JsonKey.PROPS, request);
    Request req = new Request();
    req.setRequest(
        TelemetryUtil.genarateTelemetryRequest(
            targetObject, correlatedObject, TelemetryEvents.AUDIT.getName(), params));
    generateTelemetry(req);
  }

  public static void telemetryProcessingCall(
      Map<String, Object> request,
      Map<String, Object> targetObject,
      List<Map<String, Object>> correlatedObject,
      String eventType) {

    if (eventType.equalsIgnoreCase(TelemetryEvents.AUDIT.getName())) {
      Map<String, Object> params = new HashMap<>();
      // set additional props for edata related things ...
      params.put(
          JsonKey.PROPS,
          request.entrySet().stream().map(entry -> entry.getKey()).collect(Collectors.toList()));

      Request req = new Request();
      req.setRequest(
          TelemetryUtil.genarateTelemetryRequest(
              targetObject, correlatedObject, TelemetryEvents.AUDIT.getName(), params));
      generateTelemetry(req);
    } else if (eventType.equalsIgnoreCase(TelemetryEvents.LOG.getName())) {
      Map<String, Object> logInfo = request;
      long endTime = System.currentTimeMillis();
      logInfo.put(JsonKey.END_TIME, endTime);
      Request req = new Request();
      req.setRequest(
          generateTelemetryRequest(eventType, logInfo, TelemetryUtil.getTelemetryContext()));
      generateTelemetry(req);
    }
  }

  private static Map<String, Object> generateTelemetryRequest(
      String eventType, Map<String, Object> params, Map<String, Object> context) {

    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.TELEMETRY_EVENT_TYPE, eventType);
    map.put(JsonKey.CONTEXT, context);
    map.put(JsonKey.PARAMS, params);
    return map;
  }

  private static void generateTelemetry(Request request) {
    // set request id to the context so that can be captured into the telemetry c-data section ...
    ExecutionContext.getCurrent()
        .getRequestContext()
        .put(
            JsonKey.REQUEST_ID,
            ExecutionContext.getCurrent()
                .getGlobalContext()
                .get(HeaderParam.REQUEST_ID.getParamName()));
    request
        .getContext()
        .put(JsonKey.TELEMETRY_CONTEXT, ExecutionContext.getCurrent().getRequestContext());
    TelemetryLmaxWriter.getInstance().submitMessage(request);
  }
}
