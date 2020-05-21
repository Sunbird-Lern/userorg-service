package org.sunbird.telemetry.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;

/**
 * Class to Receive the telemetry messages and once queue reached threshold value flush the messages
 * to the appropriate consumer
 *
 * @author arvind
 */
public class TelemetryFlush {

  private Queue<String> queue = new ConcurrentLinkedQueue<>();
  private int thresholdSize = 20;
  private static ObjectMapper mapper = new ObjectMapper();
  private static TelemetryFlush telemetryFlush;
  SunbirdTelemetryEventConsumer consumer = SunbirdTelemetryEventConsumer.getInstance();

  public static TelemetryFlush getInstance() {
    if (telemetryFlush == null) {
      synchronized (TelemetryFlush.class) {
        if (telemetryFlush == null) {
          telemetryFlush = new TelemetryFlush();
        }
      }
    }
    return telemetryFlush;
  }

  /** Constructor that initialize the telemetry flush attributes like queue threshold size */
  public TelemetryFlush() {
    String queueThreshold = ProjectUtil.getConfigValue(JsonKey.TELEMETRY_QUEUE_THRESHOLD_VALUE);
    if (!StringUtils.isBlank(queueThreshold)
        && !queueThreshold.equalsIgnoreCase(JsonKey.TELEMETRY_QUEUE_THRESHOLD_VALUE)) {
      try {
        this.thresholdSize = Integer.parseInt(queueThreshold.trim());
      } catch (Exception ex) {
        ProjectLogger.log(
            "TelemetryFlush:TelemetryFlush: Threshold size from config is not integer", ex);
      }
    }
  }

  /**
   * Method to flush the telemetry message to the destination
   *
   * @param message Telemetry message
   */
  public void flushTelemetry(String message) {
    writeToQueue(message);
  }

  private void writeToQueue(String message) {
    queue.offer(message);
    if (queue.size() >= thresholdSize) {
      List<String> list = new ArrayList<>();
      for (int i = 1; i <= thresholdSize; i++) {
        String obj = queue.poll();
        if (obj == null) {
          break;
        } else {
          list.add(obj);
        }
      }
      Request req = createTelemetryRequest(list);
      consumer.consume(req);
    }
  }

  public Request createTelemetryRequest(List<String> eventList) {
    Request req = null;
    try {
      List<Map<String, Object>> jsonList =
          mapper.readValue(eventList.toString(), new TypeReference<List<Map<String, Object>>>() {});
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.ETS, System.currentTimeMillis());
      map.put(JsonKey.EVENTS, jsonList);
      req = new Request();
      req.getRequest().putAll(map);
      return req;
    } catch (Exception e) {
      ProjectLogger.log(
          "TelemetryFlush:createTelemetryRequest: Failed to create request for telemetry flush.",
          e);
    }
    return req;
  }
}
