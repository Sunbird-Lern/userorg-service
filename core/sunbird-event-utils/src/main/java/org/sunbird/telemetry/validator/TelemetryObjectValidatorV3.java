package org.sunbird.telemetry.validator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.telemetry.dto.Telemetry;
import org.sunbird.telemetry.util.TelemetryEvents;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Validator class for Version 3 Telemetry events.
 * Implements the TelemetryObjectValidator interface to provide validation logic for various telemetry event types.
 */
public class TelemetryObjectValidatorV3 implements TelemetryObjectValidator {
  
  private static final LoggerUtil logger = new LoggerUtil(TelemetryObjectValidatorV3.class);
  private static TelemetryObjectValidator telemetryObjectValidator = null;
  private final ObjectMapper mapper = new ObjectMapper();

  /**
   * Returns the singleton instance of TelemetryObjectValidatorV3.
   *
   * @return The singleton instance.
   */
  public static TelemetryObjectValidator getInstance() {
    if (telemetryObjectValidator == null) {
      telemetryObjectValidator = new TelemetryObjectValidatorV3();
    }
    return telemetryObjectValidator;
  }

  @Override
  public boolean validateAudit(String jsonString) {
    boolean validationSuccess = true;
    List<String> missingFields = new ArrayList<>();
    Telemetry telemetryObj = null;
    try {
      // Parse JSON string to Telemetry object
      telemetryObj = mapper.readValue(jsonString, Telemetry.class);
      
      // Validate basic fields
      validateBasics(telemetryObj, missingFields);
      // Validate Audit specific data
      validateAuditEventData(telemetryObj.getEdata(), missingFields);
      
      if (!missingFields.isEmpty()) {
        logger.info(
            "TelemetryObjectValidatorV3:validateAudit: Validation failed for event: "
                + TelemetryEvents.AUDIT.getName()
                + ". Missing required fields: "
                + String.join(", ", missingFields));
        validationSuccess = false;
      }
    } catch (IOException e) {
      validationSuccess = false;
      logger.error("TelemetryObjectValidatorV3:validateAudit: Error parsing JSON: " + e.getMessage(), e);
    }
    return validationSuccess;
  }

  @Override
  public boolean validateSearch(String jsonString) {
    boolean validationSuccess = true;
    List<String> missingFields = new ArrayList<>();
    Telemetry telemetryObj = null;
    try {
      // Parse JSON string to Telemetry object
      telemetryObj = mapper.readValue(jsonString, Telemetry.class);
      
      // Validate basic fields
      validateBasics(telemetryObj, missingFields);
      // Validate Search specific data
      validateSearchEventData(telemetryObj.getEdata(), missingFields);
      
      if (!missingFields.isEmpty()) {
        logger.info(
            "TelemetryObjectValidatorV3:validateSearch: Validation failed for event: "
                + TelemetryEvents.SEARCH.getName()
                + ". Missing required fields: "
                + String.join(", ", missingFields));
        validationSuccess = false;
      }
    } catch (IOException e) {
      validationSuccess = false;
      logger.error("TelemetryObjectValidatorV3:validateSearch: Error parsing JSON: " + e.getMessage(), e);
    }
    return validationSuccess;
  }

  /**
   * Validates search event data structure.
   *
   * @param edata         The event data map.
   * @param missingFields List to populate with missing keys.
   */
  private void validateSearchEventData(Map<String, Object> edata, List<String> missingFields) {
    if (edata == null || edata.isEmpty()) {
      missingFields.add("edata");
    } else {
      if (null == edata.get(JsonKey.QUERY)) {
        missingFields.add(JsonKey.QUERY);
      }
      if (null == edata.get(JsonKey.SIZE)) {
        missingFields.add(JsonKey.SIZE);
      }
      if (null == edata.get(JsonKey.TOPN)) {
        missingFields.add(JsonKey.TOPN);
      }
    }
  }

  /**
   * Validates audit event data presence.
   *
   * @param edata         The event data map.
   * @param missingFields List to populate with missing keys.
   */
  private void validateAuditEventData(Map<String, Object> edata, List<String> missingFields) {
    if (edata == null) {
      missingFields.add("edata");
    }
  }

  /**
   * Validates basic telemetry fields (eid, mid, ver, actor, context).
   *
   * @param telemetryObj   The telemetry object.
   * @param missingFields  List to populate with missing keys.
   */
  private void validateBasics(Telemetry telemetryObj, List<String> missingFields) {
    // Check mandatory top-level fields
    if (StringUtils.isBlank(telemetryObj.getEid())) {
      missingFields.add("eid");
    }
    if (StringUtils.isBlank(telemetryObj.getMid())) {
      missingFields.add("mid");
    }
    if (StringUtils.isBlank(telemetryObj.getVer())) {
      missingFields.add("ver");
    }

    // Check actor details
    if (null == telemetryObj.getActor()) {
      missingFields.add("actor");
    } else {
      if (StringUtils.isBlank(telemetryObj.getActor().getId())) {
        missingFields.add("actor.id");
      }
      if (StringUtils.isBlank(telemetryObj.getActor().getType())) {
        missingFields.add("actor.type");
      }
    }

    // Check context details
    if (null == telemetryObj.getContext()) {
      missingFields.add(JsonKey.CONTEXT);
    } else {
      if (StringUtils.isBlank(telemetryObj.getContext().getChannel())) {
        missingFields.add(JsonKey.CONTEXT + "." + JsonKey.CHANNEL);
      }
      if (StringUtils.isBlank(telemetryObj.getContext().getEnv())) {
        missingFields.add(JsonKey.CONTEXT + "." + JsonKey.ENV);
      }
    }
  }

  /**
   * Validates a LOG telemetry event.
   *
   * @param jsonString The JSON string representation of the telemetry event.
   * @return true if valid, false otherwise.
   */
  @Override
  public boolean validateLog(String jsonString) {
    boolean validationSuccess = true;
    List<String> missingFields = new ArrayList<>();
    Telemetry telemetryObj = null;
    try {
      // Parse JSON string to Telemetry object
      telemetryObj = mapper.readValue(jsonString, Telemetry.class);
      
      // Validate basic fields
      validateBasics(telemetryObj, missingFields);
      // Validate Log specific data
      validateLogEventData(telemetryObj.getEdata(), missingFields);
      
      if (!missingFields.isEmpty()) {
        logger.info(
            "TelemetryObjectValidatorV3:validateLog: Validation failed for event: "
                + TelemetryEvents.LOG.getName()
                + ". Missing required fields: "
                + String.join(", ", missingFields));
        validationSuccess = false;
      }
    } catch (IOException e) {
      validationSuccess = false;
      logger.error("TelemetryObjectValidatorV3:validateLog: Error parsing JSON: " + e.getMessage(), e);
    }
    return validationSuccess;
  }

  /**
   * Validates log event data structure.
   *
   * @param edata         The event data map.
   * @param missingFields List to populate with missing keys.
   */
  private void validateLogEventData(Map<String, Object> edata, List<String> missingFields) {
    if (edata == null || edata.isEmpty()) {
      missingFields.add("edata");
    } else {
      if (StringUtils.isBlank((String) edata.get(JsonKey.TYPE))) {
        missingFields.add(JsonKey.TYPE);
      }
      if (StringUtils.isBlank((String) edata.get(JsonKey.LEVEL))) {
        missingFields.add(JsonKey.LEVEL);
      }
      // TODO: remember and make this change at the time of re-factoring.
      if (StringUtils.isBlank((String) edata.get(JsonKey.MESSAGE))) {
        edata.remove(JsonKey.MESSAGE);
      }
    }
  }

  /**
   * Validates an ERROR telemetry event.
   *
   * @param jsonString The JSON string representation of the telemetry event.
   * @return true if valid, false otherwise.
   */
  @Override
  public boolean validateError(String jsonString) {
    boolean validationSuccess = true;
    List<String> missingFields = new ArrayList<>();
    Telemetry telemetryObj = null;
    try {
      // Parse JSON string to Telemetry object
      telemetryObj = mapper.readValue(jsonString, Telemetry.class);
      
      // Validate basic fields
      validateBasics(telemetryObj, missingFields);
      // Validate Error specific data
      validateErrorEventData(telemetryObj.getEdata(), missingFields);
      
      if (!missingFields.isEmpty()) {
        logger.info(
            "TelemetryObjectValidatorV3:validateError: Validation failed for event: "
                + TelemetryEvents.ERROR.getName()
                + ". Missing required fields: "
                + String.join(", ", missingFields));
        validationSuccess = false;
      }
    } catch (IOException e) {
      validationSuccess = false;
      logger.error("TelemetryObjectValidatorV3:validateError: Error parsing JSON: " + e.getMessage(), e);
    }
    return validationSuccess;
  }

  /**
   * Validates error event data structure.
   *
   * @param edata         The event data map.
   * @param missingFields List to populate with missing keys.
   */
  private void validateErrorEventData(Map<String, Object> edata, List<String> missingFields) {
    if (edata == null || edata.isEmpty()) {
      missingFields.add("edata");
    } else {
      if (StringUtils.isBlank((String) edata.get(JsonKey.ERROR))) {
        missingFields.add(JsonKey.ERROR);
      }
      if (StringUtils.isBlank((String) edata.get(JsonKey.ERR_TYPE))) {
        missingFields.add(JsonKey.ERR_TYPE);
      }
      if (StringUtils.isBlank((String) edata.get(JsonKey.STACKTRACE))) {
        missingFields.add(JsonKey.STACKTRACE);
      }
    }
  }
}
