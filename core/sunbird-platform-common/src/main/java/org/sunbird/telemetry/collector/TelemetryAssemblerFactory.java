package org.sunbird.telemetry.collector;

/**
 * Factory class to provide an instance of TelemetryDataAssembler.
 * This class follows the creation pattern to ensure a single instance of TelemetryDataAssembler is used.
 */
public class TelemetryAssemblerFactory {

  private static TelemetryDataAssembler telemetryDataAssembler = null;

  /**
   * Private constructor to prevent instantiation.
   */
  private TelemetryAssemblerFactory() {}

  /**
   * Returns the singleton instance of TelemetryDataAssembler.
   * If the instance supports lazy initialization, it creates one in a thread-safe manner.
   *
   * @return TelemetryDataAssembler instance
   */
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
