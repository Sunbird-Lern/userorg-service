package org.sunbird.telemetry.collector;

import java.util.Map;

/**
 * Interface defining the contract for assembling and generating various telemetry events.
 */
public interface TelemetryDataAssembler {

  /**
   * Generates an AUDIT telemetry event.
   *
   * @param context Context map containing telemetry context information (e.g., channel, pdata, env, etc.)
   * @param params  Parameters map containing event-specific data
   * @return The generated telemetry event as a JSON string
   */
  String audit(Map<String, Object> context, Map<String, Object> params);

  /**
   * Generates a SEARCH telemetry event.
   *
   * @param context Context map containing telemetry context information
   * @param params  Parameters map containing search-specific data (e.g., query, filters, sort, correlation)
   * @return The generated telemetry event as a JSON string
   */
  String search(Map<String, Object> context, Map<String, Object> params);

  /**
   * Generates a LOG telemetry event.
   *
   * @param context Context map containing telemetry context information
   * @param params  Parameters map containing log-specific data (e.g., type, level, message, params)
   * @return The generated telemetry event as a JSON string
   */
  String log(Map<String, Object> context, Map<String, Object> params);

  /**
   * Generates an ERROR telemetry event.
   *
   * @param context Context map containing telemetry context information
   * @param params  Parameters map containing error-specific data (e.g., err, errtype, stacktrace)
   * @return The generated telemetry event as a JSON string
   */
  String error(Map<String, Object> context, Map<String, Object> params);
}
