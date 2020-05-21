package org.sunbird.telemetry.collector;

import java.util.Map;
import org.sunbird.telemetry.util.TelemetryGenerator;

/** Created by arvind on 5/1/18. */
public class TelemetryDataAssemblerImpl implements TelemetryDataAssembler {

  @Override
  public String audit(Map<String, Object> context, Map<String, Object> params) {
    return TelemetryGenerator.audit(context, params);
  }

  @Override
  public String search(Map<String, Object> context, Map<String, Object> params) {
    return TelemetryGenerator.search(context, params);
  }

  @Override
  public String log(Map<String, Object> context, Map<String, Object> params) {
    return TelemetryGenerator.log(context, params);
  }

  @Override
  public String error(Map<String, Object> context, Map<String, Object> params) {
    return TelemetryGenerator.error(context, params);
  }
}
