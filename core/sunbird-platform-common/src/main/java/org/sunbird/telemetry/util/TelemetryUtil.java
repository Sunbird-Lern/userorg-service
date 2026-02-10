package org.sunbird.telemetry.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;

/**
 * Utility class for generating and processing telemetry events.
 * Provides helper methods to construct telemetry objects and requests.
 */
public final class TelemetryUtil {

  private TelemetryUtil() {}

  /**
   * Generates a target object map for telemetry.
   *
   * @param id           The ID of the target object.
   * @param type         The type of the target object.
   * @param currentState The current state of the object.
   * @param prevState    The previous state of the object.
   * @return A map representing the target object.
   */
  public static Map<String, Object> generateTargetObject(
      String id, String type, String currentState, String prevState) {

    Map<String, Object> target = new HashMap<>();
    target.put(JsonKey.ID, id);
    target.put(JsonKey.TYPE, StringUtils.capitalize(type));
    target.put(JsonKey.CURRENT_STATE, currentState);
    target.put(JsonKey.PREV_STATE, prevState);
    return target;
  }

  /**
   * Generates the telemetry request map.
   *
   * @param targetObject     The target object map.
   * @param correlatedObject List of correlated objects.
   * @param eventType        The type of telemetry event.
   * @param params           Additional parameters for the event.
   * @param context          The context map.
   * @return A map representing the telemetry request.
   */
  public static Map<String, Object> generateTelemetryRequest(
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

  /**
   * Generates a correlated object and adds it to the provided list.
   *
   * @param id              The ID of the correlated object.
   * @param type            The type of the correlated object.
   * @param correlation     The relation string.
   * @param correlationList The list to which the correlated object will be added.
   */
  public static void generateCorrelatedObject(
      String id, String type, String correlation, List<Map<String, Object>> correlationList) {

    Map<String, Object> correlatedObject = new HashMap<>();
    correlatedObject.put(JsonKey.ID, id);
    correlatedObject.put(JsonKey.TYPE, StringUtils.capitalize(type));
    correlatedObject.put(JsonKey.RELATION, correlation);

    correlationList.add(correlatedObject);
  }

  /**
   * Adds rollup data to the target object.
   *
   * @param rollUpMap    The rollup map.
   * @param targetObject The target object to modify.
   */
  public static void addTargetObjectRollUp(
      Map<String, String> rollUpMap, Map<String, Object> targetObject) {
    targetObject.put(JsonKey.ROLLUP, rollUpMap);
  }

  /**
   * Processes the telemetry call for Audit events.
   *
   * @param request          The request properties map.
   * @param targetObject     The target object map.
   * @param correlatedObject List of correlated objects.
   * @param context          The context map.
   */
  public static void telemetryProcessingCall(
      Map<String, Object> request,
      Map<String, Object> targetObject,
      List<Map<String, Object>> correlatedObject,
      Map<String, Object> context) {
    Map<String, Object> params = new HashMap<>();
    params.put(JsonKey.PROPS, request);
    Request req = new Request();
    req.setRequest(
        TelemetryUtil.generateTelemetryRequest(
            targetObject, correlatedObject, TelemetryEvents.AUDIT.getName(), params, context));
    generateTelemetry(req);
  }

  /**
   * Processes the telemetry call for Audit events with a specific type.
   *
   * @param type             The type of the event.
   * @param request          The request properties map.
   * @param targetObject     The target object map.
   * @param correlatedObject List of correlated objects.
   * @param context          The context map.
   */
  public static void telemetryProcessingCall(
      String type,
      Map<String, Object> request,
      Map<String, Object> targetObject,
      List<Map<String, Object>> correlatedObject,
      Map<String, Object> context) {
    Map<String, Object> params = new HashMap<>();
    params.put(JsonKey.PROPS, request);
    params.put(JsonKey.TYPE, type);
    Request req = new Request();
    req.setRequest(
        TelemetryUtil.generateTelemetryRequest(
            targetObject, correlatedObject, TelemetryEvents.AUDIT.getName(), params, context));
    generateTelemetry(req);
  }

  /**
   * Helper method to trigger the telemetry writer.
   *
   * @param request The request object containing telemetry data.
   */
  private static void generateTelemetry(Request request) {
    TelemetryWriter.write(request);
  }
}
