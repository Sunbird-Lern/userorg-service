package org.sunbird.telemetry.util;

/**
 * Enumeration for telemetry events types.
 * Defines the standard event names supported by the telemetry system.
 */
public enum TelemetryEvents {

  /** AUDIT Telemetry Event */
  AUDIT("AUDIT"),

  /** SEARCH Telemetry Event */
  SEARCH("SEARCH"),

  /** LOG Telemetry Event */
  LOG("LOG"),

  /** ERROR Telemetry Event */
  ERROR("ERROR");

  private final String name;

  /**
   * Private constructor for enum.
   *
   * @param name The name of the event
   */
  TelemetryEvents(String name) {
    this.name = name;
  }

  /**
   * Gets the name of the telemetry event.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }
}
