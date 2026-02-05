package org.sunbird.telemetry.collector;

import java.util.Map;
import org.sunbird.telemetry.util.TelemetryGenerator;

/**
 * Implementation of the TelemetryDataAssembler interface.
 * Delegates the actual generation of telemetry events to the TelemetryGenerator utility.
 */
public class TelemetryDataAssemblerImpl implements TelemetryDataAssembler {

  /**
   * Generates an AUDIT telemetry event using TelemetryGenerator.
   *
   * @param context Context map containing telemetry context information
   * @param params  Parameters map containing event-specific data
   * @return The generated telemetry event as a JSON string
   */
  @Override
  public String audit(Map<String, Object> context, Map<String, Object> params) {
    return TelemetryGenerator.audit(context, params);
  }

  /**
   * Generates a SEARCH telemetry event using TelemetryGenerator.
   *
   * @param context Context map containing telemetry context information
   * @param params  Parameters map containing search-specific data
   * @return The generated telemetry event as a JSON string
   */
  @Override
  public String search(Map<String, Object> context, Map<String, Object> params) {
    return TelemetryGenerator.search(context, params);
  }

  /**
   * Generates a LOG telemetry event using TelemetryGenerator.
   *
   * @param context Context map containing telemetry context information
   * @param params  Parameters map containing log-specific data
   * @return The generated telemetry event as a JSON string
   */
  @Override
  public String log(Map<String, Object> context, Map<String, Object> params) {
    return TelemetryGenerator.log(context, params);
  }

  /**
   * Generates an ERROR telemetry event using TelemetryGenerator.
   *
   * @param context Context map containing telemetry context information
   * @param params  Parameters map containing error-specific data
   * @return The generated telemetry event as a JSON string
   */
  @Override
  public String error(Map<String, Object> context, Map<String, Object> params) {
    return TelemetryGenerator.error(context, params);
  }
}
