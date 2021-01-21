package org.sunbird.telemetry.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.telemetry.dto.Telemetry;
import org.sunbird.telemetry.util.TelemetryEvents;

/** @author arvind */
public class TelemetryObjectValidatorV3 implements TelemetryObjectValidator {

  ObjectMapper mapper = new ObjectMapper();

  @Override
  public boolean validateAudit(String jsonString) {

    boolean validationSuccess = true;
    List<String> missingFields = new ArrayList<>();
    Telemetry telemetryObj = null;
    try {
      telemetryObj = mapper.readValue(jsonString, Telemetry.class);
      validateBasics(telemetryObj, missingFields);
      validateAuditEventData(telemetryObj.getEdata(), missingFields);
      if (!missingFields.isEmpty()) {
        ProjectLogger.log(
            "Telemetry Object Creation Error for event : "
                + TelemetryEvents.AUDIT.getName()
                + "  missing required fields :"
                + String.join(",", missingFields));
        validationSuccess = false;
      }
    } catch (IOException e) {
      validationSuccess = false;
      ProjectLogger.log(e.getMessage(), e);
    }
    return validationSuccess;
  }

  @Override
  public boolean validateSearch(String jsonString) {

    boolean validationSuccess = true;
    List<String> missingFields = new ArrayList<>();
    Telemetry telemetryObj = null;
    try {
      telemetryObj = mapper.readValue(jsonString, Telemetry.class);
      validateBasics(telemetryObj, missingFields);
      validateSearchEventData(telemetryObj.getEdata(), missingFields);
      if (!missingFields.isEmpty()) {
        ProjectLogger.log(
            "Telemetry Object Creation Error for event : "
                + TelemetryEvents.SEARCH.getName()
                + "  missing required fields :"
                + String.join(",", missingFields));
        validationSuccess = false;
      }
    } catch (IOException e) {
      validationSuccess = false;
      ProjectLogger.log(e.getMessage(), e);
    }
    return validationSuccess;
  }

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

  private void validateAuditEventData(Map<String, Object> edata, List<String> missingFields) {
    if (edata == null) {
      missingFields.add("edata");
    }
  }

  private void validateBasics(Telemetry telemetryObj, List<String> missingFields) {

    if (StringUtils.isBlank(telemetryObj.getEid())) {
      missingFields.add("eid");
    }
    if (StringUtils.isBlank(telemetryObj.getMid())) {
      missingFields.add("mid");
    }
    if (StringUtils.isBlank(telemetryObj.getVer())) {
      missingFields.add("ver");
    }

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

  @Override
  public boolean validateLog(String jsonString) {

    boolean validationSuccess = true;
    List<String> missingFields = new ArrayList<>();
    Telemetry telemetryObj = null;
    try {
      telemetryObj = mapper.readValue(jsonString, Telemetry.class);
      validateBasics(telemetryObj, missingFields);
      validateLogEventData(telemetryObj.getEdata(), missingFields);
      if (!missingFields.isEmpty()) {
        ProjectLogger.log(
            "Telemetry Object Creation Error for event : "
                + TelemetryEvents.LOG.getName()
                + "  missing required fields :"
                + String.join(",", missingFields));
        validationSuccess = false;
      }
    } catch (IOException e) {
      validationSuccess = false;
      ProjectLogger.log(e.getMessage(), e);
    }
    return validationSuccess;
  }

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

  @Override
  public boolean validateError(String jsonString) {

    boolean validationSuccess = true;
    List<String> missingFields = new ArrayList<>();
    Telemetry telemetryObj = null;
    try {
      telemetryObj = mapper.readValue(jsonString, Telemetry.class);
      validateBasics(telemetryObj, missingFields);
      validateErrorEventData(telemetryObj.getEdata(), missingFields);
      if (!missingFields.isEmpty()) {
        ProjectLogger.log(
            "Telemetry Object Creation Error for event : "
                + TelemetryEvents.ERROR.getName()
                + "  missing required fields :"
                + String.join(",", missingFields));
        validationSuccess = false;
      }
    } catch (IOException e) {
      validationSuccess = false;
      ProjectLogger.log(e.getMessage(), e);
    }
    return validationSuccess;
  }

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
