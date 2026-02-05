package org.sunbird.telemetry.validator;

/**
 * Interface for validating telemetry event objects against their schemas.
 */
public interface TelemetryObjectValidator {

  /**
   * Validates an AUDIT telemetry event JSON string.
   *
   * @param jsonString The JSON string representation of the telemetry event.
   * @return true if valid, false otherwise.
   */
  boolean validateAudit(String jsonString);

  /**
   * Validates a SEARCH telemetry event JSON string.
   *
   * @param jsonString The JSON string representation of the telemetry event.
   * @return true if valid, false otherwise.
   */
  boolean validateSearch(String jsonString);

  /**
   * Validates a LOG telemetry event JSON string.
   *
   * @param jsonString The JSON string representation of the telemetry event.
   * @return true if valid, false otherwise.
   */
  boolean validateLog(String jsonString);

  /**
   * Validates an ERROR telemetry event JSON string.
   *
   * @param jsonString The JSON string representation of the telemetry event.
   * @return true if valid, false otherwise.
   */
  boolean validateError(String jsonString);
}
