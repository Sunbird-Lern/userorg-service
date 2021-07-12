package org.sunbird.telemetry.collector;

/** Created by arvind on 16/1/18. */
public class TelemetryAssemblerFactory {

  private static TelemetryDataAssembler telemetryDataAssembler = null;

  public static TelemetryDataAssembler get() {
    if (telemetryDataAssembler == null) {
      synchronized (TelemetryAssemblerFactory.class) {
        if (telemetryDataAssembler == null) {
          telemetryDataAssembler = new TelemetryDataAssemblerImpl();
        }
      }
    }
    return telemetryDataAssembler;
  }
}
