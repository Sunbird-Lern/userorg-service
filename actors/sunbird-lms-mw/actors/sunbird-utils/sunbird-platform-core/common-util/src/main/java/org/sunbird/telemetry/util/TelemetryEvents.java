package org.sunbird.telemetry.util;

/**
 * enum for telemetry events
 *
 * @author arvind.
 */
public enum TelemetryEvents {
  AUDIT("AUDIT"),
  SEARCH("SEARCH"),
  LOG("LOG"),
  ERROR("ERROR");
  private String name;

  TelemetryEvents(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
