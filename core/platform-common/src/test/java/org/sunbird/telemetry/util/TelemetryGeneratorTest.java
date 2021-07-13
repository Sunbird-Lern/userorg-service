package org.sunbird.telemetry.util;

import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.keys.JsonKey;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*"
})
public class TelemetryGeneratorTest {

  private static Map<String, Object> context;
  private static Map<String, Object> rollup;
  private static Map<String, Object> params;

  @Before
  public void setUp() throws Exception {
    context = new HashMap<String, Object>();
    rollup = new HashMap<String, Object>();
    rollup.put("managedToken", "123456789012345678901234567890");
    context.put("actorType", "consumer");
    context.put("telemetry_pdata_pid", "learning-service");
    context.put("telemetry_pdata_id", "local.sunbird.learning.service");
    context.put("actorId", "Internal");
    context.put("requestId", "8e27cbf5-e299-43b0-bca7-8347f7e5abcf");
    context.put("channel", "ORG_001");
    context.put("telemetry_pdata_ver", "1.15");
    context.put("x-request-id", "8e27cbf5-e299-43b0-bca7-8347f7e5abcf");
    context.put("env", "User");
    context.put("rollup", rollup);
    context.put("did", "postman");

    Map<String, Object> target = new HashMap<>();
    target.put(JsonKey.ID, "1324567897564");
    target.put(JsonKey.TYPE, StringUtils.capitalize(JsonKey.USER));
    target.put(JsonKey.CURRENT_STATE, null);
    target.put(JsonKey.PREV_STATE, null);

    params = new HashMap<>();
    params.put(JsonKey.FIRST_NAME, "Name");
    params.put(JsonKey.LAST_NAME, "LName");
    params.put(JsonKey.ID, "1234785963014789564123");
    params.put(JsonKey.USER_ID, "9512357468214597623");
    params.put("targetObject", target);
    params.put(JsonKey.QUERY, "hello");
    params.put(JsonKey.FILTERS, new HashMap<>());
    params.put(JsonKey.LOG_TYPE, "INFO");
    params.put(JsonKey.LOG_LEVEL, "Level");
    params.put(JsonKey.MESSAGE, "message");
    params.put(JsonKey.ERROR, "Error");
    params.put(JsonKey.ERR_TYPE, "type");
    params.put(JsonKey.STACKTRACE, "stacktrace");
  }

  @Test
  public void testAudit() {
    String audit = TelemetryGenerator.audit(context, params);
    assertNotNull(audit);
  }

  @Test
  public void testSearch() {
    String audit = TelemetryGenerator.search(context, params);
    assertNotNull(audit);
  }

  @Test
  public void testLog() {
    String audit = TelemetryGenerator.log(context, params);
    assertNotNull(audit);
  }

  @Test
  public void testError() {
    String audit = TelemetryGenerator.error(context, params);
    assertNotNull(audit);
  }

  @AfterClass
  public static void tearDown() throws Exception {
    context.clear();
  }
}
