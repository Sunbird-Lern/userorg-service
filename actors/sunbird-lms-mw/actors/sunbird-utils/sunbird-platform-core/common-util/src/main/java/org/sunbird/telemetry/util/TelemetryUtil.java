package org.sunbird.telemetry.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.models.util.JsonKey;
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
      Map<String, Object> params,
      Map<String, Object> context) {

    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.TARGET_OBJECT, targetObject);
    map.put(JsonKey.CORRELATED_OBJECTS, correlatedObject);
    map.put(JsonKey.TELEMETRY_EVENT_TYPE, eventType);
    map.put(JsonKey.PARAMS, params);
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

  public static void addTargetObjectRollUp(
      Map<String, String> rollUpMap, Map<String, Object> targetObject) {
    targetObject.put(JsonKey.ROLLUP, rollUpMap);
  }

  public static void telemetryProcessingCall(
      Map<String, Object> request,
      Map<String, Object> targetObject,
      List<Map<String, Object>> correlatedObject,
      Map<String, Object> context) {
    Map<String, Object> params = new HashMap<>();
    params.put(JsonKey.PROPS, request);
    Request req = new Request();
    req.setRequest(
        TelemetryUtil.genarateTelemetryRequest(
            targetObject, correlatedObject, TelemetryEvents.AUDIT.getName(), params, context));
    generateTelemetry(req);
  }

  public static void telemetryProcessingCall(String type,
          Map<String, Object> request,
          Map<String, Object> targetObject,
          List<Map<String, Object>> correlatedObject,
          Map<String, Object> context) {
    Map<String, Object> params = new HashMap<>();
    params.put(JsonKey.PROPS, request);
    params.put(JsonKey.TYPE, type);
    Request req = new Request();
    req.setRequest(
            TelemetryUtil.genarateTelemetryRequest(
                    targetObject, correlatedObject, TelemetryEvents.AUDIT.getName(), params, context));
    generateTelemetry(req);
  }

  private static void generateTelemetry(Request request) {
    TelemetryWriter.write(request);
  }
}
