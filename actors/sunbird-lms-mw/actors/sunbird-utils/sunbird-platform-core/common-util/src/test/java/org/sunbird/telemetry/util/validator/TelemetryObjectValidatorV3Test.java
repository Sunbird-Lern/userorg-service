package org.sunbird.telemetry.util.validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.telemetry.dto.Actor;
import org.sunbird.telemetry.dto.Context;
import org.sunbird.telemetry.dto.Telemetry;
import org.sunbird.telemetry.util.TelemetryEvents;
import org.sunbird.telemetry.validator.TelemetryObjectValidatorV3;

/** Created by arvind on 30/1/18. */
public class TelemetryObjectValidatorV3Test {

  private TelemetryObjectValidatorV3 validatorV3 = new TelemetryObjectValidatorV3();
  private ObjectMapper mapper = new ObjectMapper();

  @Test
  public void testAuditWithValidData() {

    Telemetry telemetry = new Telemetry();
    telemetry.setEid(TelemetryEvents.AUDIT.getName());
    telemetry.setMid("dummy msg id");
    telemetry.setVer("3.0");

    Actor actor = new Actor();
    actor.setId("1");
    actor.setType(JsonKey.USER);
    telemetry.setActor(actor);

    Context context = new Context();
    context.setEnv(JsonKey.ORGANISATION);
    context.setChannel("channel");

    telemetry.setContext(context);

    Map<String, Object> auditEdata = new HashMap<>();
    List<String> props = new ArrayList<>();
    props.add("username");
    props.add("org");
    auditEdata.put(JsonKey.PROPS, props);
    telemetry.setEdata(auditEdata);

    boolean result = false;
    try {
      result = validatorV3.validateAudit(mapper.writeValueAsString(telemetry));
    } catch (JsonProcessingException e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    Assert.assertTrue(result);
  }

  @Test
  public void testAuditWithoutActor() {

    Telemetry telemetry = new Telemetry();
    telemetry.setEid(TelemetryEvents.AUDIT.getName());
    telemetry.setMid("dummy msg id");
    telemetry.setVer("3.0");

    Context context = new Context();
    context.setEnv(JsonKey.ORGANISATION);
    context.setChannel("channel");

    telemetry.setContext(context);

    Map<String, Object> auditEdata = new HashMap<>();
    List<String> props = new ArrayList<>();
    props.add("username");
    props.add("org");
    auditEdata.put(JsonKey.PROPS, props);
    telemetry.setEdata(auditEdata);

    boolean result = true;
    try {
      result = validatorV3.validateAudit(mapper.writeValueAsString(telemetry));
    } catch (JsonProcessingException e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    Assert.assertFalse(result);
  }

  @Test
  public void testAuditWithoutChannel() {

    Telemetry telemetry = new Telemetry();
    telemetry.setEid(TelemetryEvents.AUDIT.getName());
    telemetry.setMid("dummy msg id");
    telemetry.setVer("3.0");

    Actor actor = new Actor();
    actor.setId("1");
    actor.setType(JsonKey.USER);
    telemetry.setActor(actor);

    Context context = new Context();
    context.setEnv(JsonKey.ORGANISATION);
    // context.setChannel("channel");

    telemetry.setContext(context);

    Map<String, Object> auditEdata = new HashMap<>();
    List<String> props = new ArrayList<>();
    props.add("username");
    props.add("org");
    auditEdata.put(JsonKey.PROPS, props);
    telemetry.setEdata(auditEdata);

    boolean result = true;
    try {
      result = validatorV3.validateAudit(mapper.writeValueAsString(telemetry));
    } catch (JsonProcessingException e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    Assert.assertFalse(result);
  }

  @Test
  public void testAuditWithoutEnv() {

    Telemetry telemetry = new Telemetry();
    telemetry.setEid(TelemetryEvents.AUDIT.getName());
    telemetry.setMid("dummy msg id");
    telemetry.setVer("3.0");

    Actor actor = new Actor();
    actor.setId("1");
    actor.setType(JsonKey.USER);
    telemetry.setActor(actor);

    Context context = new Context();
    // context.setEnv(JsonKey.ORGANISATION);
    context.setChannel("channel");

    telemetry.setContext(context);

    Map<String, Object> auditEdata = new HashMap<>();
    List<String> props = new ArrayList<>();
    props.add("username");
    props.add("org");
    auditEdata.put(JsonKey.PROPS, props);
    telemetry.setEdata(auditEdata);

    boolean result = true;
    try {
      result = validatorV3.validateAudit(mapper.writeValueAsString(telemetry));
    } catch (JsonProcessingException e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    Assert.assertFalse(result);
  }

  @Test
  public void testAuditWithoutEData() {

    Telemetry telemetry = new Telemetry();
    telemetry.setEid(TelemetryEvents.AUDIT.getName());
    telemetry.setMid("dummy msg id");
    telemetry.setVer("3.0");

    Actor actor = new Actor();
    actor.setId("1");
    actor.setType(JsonKey.USER);
    telemetry.setActor(actor);

    Context context = new Context();
    context.setEnv(JsonKey.ORGANISATION);
    context.setChannel("channel");

    telemetry.setContext(context);

    boolean result = true;
    try {
      result = validatorV3.validateAudit(mapper.writeValueAsString(telemetry));
    } catch (JsonProcessingException e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    Assert.assertFalse(result);
  }

  @Test
  public void testSearchWithValidData() {

    Telemetry telemetry = new Telemetry();
    telemetry.setEid(TelemetryEvents.SEARCH.getName());
    telemetry.setMid("dummy msg id");
    telemetry.setVer("3.0");

    Actor actor = new Actor();
    actor.setId("1");
    actor.setType(JsonKey.USER);
    telemetry.setActor(actor);

    Context context = new Context();
    context.setEnv(JsonKey.ORGANISATION);
    context.setChannel("channel");

    telemetry.setContext(context);

    Map<String, Object> searchEdata = new HashMap<>();
    searchEdata.put(JsonKey.TYPE, "user");
    searchEdata.put(
        JsonKey.QUERY,
        "\"filters\":{\n" + "           \"lastName\": \"Test\"\n" + "            \n" + "       }");
    searchEdata.put(JsonKey.SIZE, new Long(10));
    searchEdata.put(JsonKey.TOPN, new ArrayList<>());
    telemetry.setEdata(searchEdata);

    boolean result = false;
    try {
      result = validatorV3.validateSearch(mapper.writeValueAsString(telemetry));
    } catch (JsonProcessingException e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    Assert.assertTrue(result);
  }

  @Test
  public void testSearchWithoutQuerySize() {

    Telemetry telemetry = new Telemetry();
    telemetry.setEid(TelemetryEvents.SEARCH.getName());
    telemetry.setMid("dummy msg id");
    telemetry.setVer("3.0");

    Actor actor = new Actor();
    actor.setId("1");
    actor.setType(JsonKey.USER);
    telemetry.setActor(actor);

    Context context = new Context();
    context.setEnv(JsonKey.ORGANISATION);
    context.setChannel("channel");

    telemetry.setContext(context);

    Map<String, Object> searchEdata = new HashMap<>();
    searchEdata.put(JsonKey.TYPE, "user");
    telemetry.setEdata(searchEdata);

    boolean result = true;
    try {
      result = validatorV3.validateSearch(mapper.writeValueAsString(telemetry));
    } catch (JsonProcessingException e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    Assert.assertFalse(result);
  }

  @Test
  public void testLogWithValidData() {

    Telemetry telemetry = new Telemetry();
    telemetry.setEid(TelemetryEvents.LOG.getName());
    telemetry.setMid("dummy msg id");
    telemetry.setVer("3.0");

    Actor actor = new Actor();
    actor.setId("1");
    actor.setType(JsonKey.USER);
    telemetry.setActor(actor);

    Context context = new Context();
    context.setEnv(JsonKey.ORGANISATION);
    context.setChannel("channel");

    telemetry.setContext(context);

    Map<String, Object> logEdata = new HashMap<>();
    logEdata.put(JsonKey.TYPE, "info");
    logEdata.put(JsonKey.LEVEL, JsonKey.API_ACCESS);
    logEdata.put(JsonKey.MESSAGE, "");
    telemetry.setEdata(logEdata);

    boolean result = false;
    try {
      result = validatorV3.validateLog(mapper.writeValueAsString(telemetry));
    } catch (JsonProcessingException e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    Assert.assertTrue(result);
  }

  @Test
  public void testLogWithoutLogLevelType() {

    Telemetry telemetry = new Telemetry();
    telemetry.setEid(TelemetryEvents.LOG.getName());
    telemetry.setMid("dummy msg id");
    telemetry.setVer("3.0");

    Actor actor = new Actor();
    actor.setId("1");
    actor.setType(JsonKey.USER);
    telemetry.setActor(actor);

    Context context = new Context();
    context.setEnv(JsonKey.ORGANISATION);
    context.setChannel("channel");

    telemetry.setContext(context);

    Map<String, Object> logEdata = new HashMap<>();
    logEdata.put(JsonKey.MESSAGE, "");
    telemetry.setEdata(logEdata);

    boolean result = true;
    try {
      result = validatorV3.validateLog(mapper.writeValueAsString(telemetry));
    } catch (JsonProcessingException e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    Assert.assertFalse(result);
  }

  @Test
  public void testErrorWithValidData() {

    Telemetry telemetry = new Telemetry();
    telemetry.setEid(TelemetryEvents.ERROR.getName());
    telemetry.setMid("dummy msg id");
    telemetry.setVer("3.0");

    Actor actor = new Actor();
    actor.setId("1");
    actor.setType(JsonKey.USER);
    telemetry.setActor(actor);

    Context context = new Context();
    context.setEnv(JsonKey.ORGANISATION);
    context.setChannel("channel");
    telemetry.setContext(context);

    Map<String, Object> errorEdata = new HashMap<>();
    errorEdata.put(JsonKey.ERROR, "invalid user");
    errorEdata.put(JsonKey.ERR_TYPE, JsonKey.API_ACCESS);
    errorEdata.put(JsonKey.STACKTRACE, "error msg");
    telemetry.setEdata(errorEdata);

    boolean result = false;
    try {
      result = validatorV3.validateError(mapper.writeValueAsString(telemetry));
    } catch (JsonProcessingException e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    Assert.assertTrue(result);
  }

  @Test
  public void testErrorWithoutErrorTypeStackTrace() {

    Telemetry telemetry = new Telemetry();
    telemetry.setEid(TelemetryEvents.ERROR.getName());
    telemetry.setMid("dummy msg id");
    telemetry.setVer("3.0");

    Actor actor = new Actor();
    actor.setId("1");
    actor.setType(JsonKey.USER);
    telemetry.setActor(actor);

    Context context = new Context();
    context.setEnv(JsonKey.ORGANISATION);
    context.setChannel("channel");
    telemetry.setContext(context);

    Map<String, Object> errorEdata = new HashMap<>();
    telemetry.setEdata(errorEdata);

    boolean result = true;
    try {
      result = validatorV3.validateError(mapper.writeValueAsString(telemetry));
    } catch (JsonProcessingException e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    Assert.assertFalse(result);
  }
}
