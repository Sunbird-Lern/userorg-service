package org.sunbird.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.telemetry.dto.TelemetryBJREvent;

public class InstructionEventGenerator {

  private static LoggerUtil logger = new LoggerUtil(InstructionEventGenerator.class);

  private static ObjectMapper mapper = new ObjectMapper();
  private static String beJobRequesteventId = "BE_JOB_REQUEST";
  private static int iteration = 1;

  private static String actorId = "Sunbird LMS Flink Job";
  private static String actorType = "System";
  private static String pdataId = "org.sunbird.platform";
  private static String pdataVersion = "1.0";

  public static void pushInstructionEvent(String topic, Map<String, Object> data) throws Exception {
    pushInstructionEvent("", topic, data);
  }

  public static void pushInstructionEvent(String key, String topic, Map<String, Object> data)
      throws Exception {
    String beJobRequestEvent = generateInstructionEventMetadata(data);
    if (StringUtils.isBlank(beJobRequestEvent)) {
      throw new ProjectCommonException(
          ResponseCode.invalidRequestData,
          "Event is not generated properly.",
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    if (StringUtils.isNotBlank(topic)) {
      if (StringUtils.isNotBlank(key)) KafkaClient.send(key, beJobRequestEvent, topic);
      else KafkaClient.send(beJobRequestEvent, topic);
    } else {
      throw new ProjectCommonException(
          ResponseCode.invalidRequestData,
          "Invalid topic id.",
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  private static String generateInstructionEventMetadata(Map<String, Object> data) {
    Map<String, Object> actor = new HashMap<>();
    Map<String, Object> context = new HashMap<>();
    Map<String, Object> object = new HashMap<>();
    Map<String, Object> edata = new HashMap<>();
    if (MapUtils.isNotEmpty((Map) data.get("actor"))) {
      actor.putAll((Map<String, Object>) data.get("actor"));
    } else {
      actor.put("id", actorId);
      actor.put("type", actorType);
    }

    if (MapUtils.isNotEmpty((Map) data.get("context"))) {
      context.putAll((Map<String, Object>) data.get("context"));
    }
    Map<String, Object> pdata = new HashMap<>();
    pdata.put("id", pdataId);
    pdata.put("ver", pdataVersion);
    context.put("pdata", pdata);
    context.put(JsonKey.CDATA,data.get(JsonKey.CDATA));
    if (MapUtils.isNotEmpty((Map) data.get("object"))) object.putAll((Map) data.get("object"));

    if (MapUtils.isNotEmpty((Map) data.get("edata"))) edata.putAll((Map) data.get("edata"));
    if (StringUtils.isNotBlank((String) data.get("action")))
      edata.put("action", data.get("action"));
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
    edata.put("iteration", iteration);

    te.setEid(beJobRequesteventId);
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
