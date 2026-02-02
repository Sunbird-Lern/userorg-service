package org.sunbird.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.response.ResponseCode;
import org.sunbird.telemetry.dto.TelemetryBJREvent;

/**
 * Helper class to generate and push instruction events to Kafka.
 */
public class InstructionEventGenerator {

  private static final LoggerUtil logger = new LoggerUtil(InstructionEventGenerator.class);
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String BE_JOB_REQUEST_EVENT_ID = "BE_JOB_REQUEST";
  private static final int ITERATION = 1;

  private static final String ACTOR_ID = "Sunbird LMS Flink Job";
  private static final String ACTOR_TYPE = "System";
  private static final String PDATA_ID = "org.sunbird.platform";
  private static final String PDATA_VERSION = "1.0";

  private InstructionEventGenerator() {}

  /**
   * Pushes an instruction event to the specified Kafka topic.
   *
   * @param topic The Kafka topic to push the event to.
   * @param data The data map containing actor, context, object, edata, etc.
   * @throws Exception If event generation fails or topic is invalid.
   */
  public static void pushInstructionEvent(String topic, Map<String, Object> data) throws Exception {
    pushInstructionEvent(null, topic, data);
  }

  /**
   * Pushes an instruction event to the specified Kafka topic with a specific key.
   *
   * @param key   The Kafka message key.
   * @param topic The Kafka topic to push the event to.
   * @param data  The data map containing actor, context, object, edata, etc.
   * @throws Exception If event generation fails or topic is invalid.
   */
  public static void pushInstructionEvent(String key, String topic, Map<String, Object> data)
      throws Exception {
    String beJobRequestEvent = generateInstructionEventMetadata(data);
    
    if (StringUtils.isBlank(beJobRequestEvent)) {
      throw new ProjectCommonException(
          ResponseCode.invalidRequestData.getErrorCode(),
          "Event is not generated properly.",
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    
    if (StringUtils.isBlank(topic)) {
      throw new ProjectCommonException(
          ResponseCode.invalidRequestData.getErrorCode(),
          "Invalid topic id.",
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    if (StringUtils.isNotBlank(key)) {
      KafkaClient.send(key, beJobRequestEvent, topic);
    } else {
      KafkaClient.send(beJobRequestEvent, topic);
    }
  }

  private static String generateInstructionEventMetadata(Map<String, Object> data) {
    Map<String, Object> actor = new HashMap<>();
    Map<String, Object> context = new HashMap<>();
    Map<String, Object> object = new HashMap<>();
    Map<String, Object> edata = new HashMap<>();

    Map<String, Object> actorData = (Map<String, Object>) data.get("actor");
    if (MapUtils.isNotEmpty(actorData)) {
      actor.putAll(actorData);
    } else {
      actor.put("id", ACTOR_ID);
      actor.put("type", ACTOR_TYPE);
    }

    Map<String, Object> contextData = (Map<String, Object>) data.get("context");
    if (MapUtils.isNotEmpty(contextData)) {
      context.putAll(contextData);
    }

    Map<String, Object> pdata = new HashMap<>();
    pdata.put("id", PDATA_ID);
    pdata.put("ver", PDATA_VERSION);
    context.put("pdata", pdata);

    if (data.containsKey(JsonKey.CDATA)) {
      context.put(JsonKey.CDATA, data.get(JsonKey.CDATA));
    }

    Map<String, Object> objectData = (Map<String, Object>) data.get("object");
    if (MapUtils.isNotEmpty(objectData)) {
      object.putAll(objectData);
    }

    Map<String, Object> edataData = (Map<String, Object>) data.get("edata");
    if (MapUtils.isNotEmpty(edataData)) {
      edata.putAll(edataData);
    }

    if (StringUtils.isNotBlank((String) data.get("action"))) {
      edata.put("action", data.get("action"));
    }

    return logInstructionEvent(actor, context, object, edata);
  }

  private static String logInstructionEvent(
      Map<String, Object> actor,
      Map<String, Object> context,
      Map<String, Object> object,
      Map<String, Object> edata) {

    TelemetryBJREvent te = new TelemetryBJREvent();
    long unixTime = System.currentTimeMillis();
    String mid = "LP." + System.currentTimeMillis() + "." + UUID.randomUUID();
    edata.put("iteration", ITERATION);

    te.setEid(BE_JOB_REQUEST_EVENT_ID);
    te.setEts(unixTime);
    te.setMid(mid);
    te.setActor(actor);
    te.setContext(context);
    te.setObject(object);
    te.setEdata(edata);

    String jsonMessage = null;
    try {
      jsonMessage = mapper.writeValueAsString(te);
    } catch (Exception e) {
      logger.error("Error logging BE_JOB_REQUEST event: " + e.getMessage(), e);
    }
    return jsonMessage;
  }
}