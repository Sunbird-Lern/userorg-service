package org.sunbird.telemetry.collector;

import java.util.Map;

/** Created by arvind on 16/1/18. */
public interface TelemetryDataAssembler {

  public String audit(Map<String, Object> context, Map<String, Object> params);

  public String search(Map<String, Object> context, Map<String, Object> params);

  public String log(Map<String, Object> context, Map<String, Object> params);

  public String error(Map<String, Object> context, Map<String, Object> params);
}
