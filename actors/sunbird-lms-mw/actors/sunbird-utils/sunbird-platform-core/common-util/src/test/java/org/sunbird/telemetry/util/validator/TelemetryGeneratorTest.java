package org.sunbird.telemetry.util.validator;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.telemetry.dto.Context;
import org.sunbird.telemetry.dto.Producer;
import org.sunbird.telemetry.util.TelemetryGenerator;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TelemetryGenerator.class})
public class TelemetryGeneratorTest {

  private static Map<String, Object> context;
  private static Map<String, Object> rollup;

  @Before
  public void setUp() throws Exception {
    context = new HashMap<String, Object>();
    rollup = new HashMap<String, Object>();
    context.put("actorType", "consumer");
    context.put("telemetry_pdata_pid", "learning-service");
    context.put("actorId", "X-Consumer-ID");
    context.put("requestId", "8e27cbf5-e299-43b0-bca7-8347f7e5abcf");
    context.put("channel", "ORG_001");
    context.put("telemetry_pdata_ver", "1.15");
    context.put("REQUEST_ID", "8e27cbf5-e299-43b0-bca7-8347f7e5abcf");
    context.put("env", "User");
    context.put("did", "postman");
  }

  @Test
  public void testGetContextWithoutRollUp()
      throws InvocationTargetException, IllegalAccessException {
    Method method = Whitebox.getMethod(TelemetryGenerator.class, "getContext", Map.class);
    Context ctx = (Context) method.invoke(null, context);
    assertEquals("postman", ctx.getDid());
    assertEquals("ORG_001", ctx.getChannel());
    assertEquals("User", ctx.getEnv());
  }

  @Test
  public void testGetContextWithRollUp() throws InvocationTargetException, IllegalAccessException {
    rollup.put("id", 1);
    context.put("rollup", rollup);
    Method method = Whitebox.getMethod(TelemetryGenerator.class, "getContext", Map.class);
    Context ctx = (Context) method.invoke(null, context);
    assertTrue(rollup.equals(ctx.getRollup()));
  }

  @Test
  public void testRemoveAttributes() throws InvocationTargetException, IllegalAccessException {
    Method method =
        Whitebox.getMethod(TelemetryGenerator.class, "removeAttributes", Map.class, String.class);
    String[] removableProperty = {JsonKey.DEVICE_ID};
    method.invoke(null, context, removableProperty);
    assertFalse(context.containsKey(JsonKey.DEVICE_ID));
  }

  @Test()
  public void testGetProducerWithContextNull()
      throws InvocationTargetException, IllegalAccessException {

    Map<String, Object> nullContext = null;
    Method method = Whitebox.getMethod(TelemetryGenerator.class, "getProducer", Map.class);
    Producer producer = (Producer) method.invoke(null, nullContext);
    assertEquals("", producer.getId());
    assertEquals("", producer.getPid());
    assertEquals("", producer.getVer());
  }

  @Test
  public void testGetProducerWithAppId() throws InvocationTargetException, IllegalAccessException {
    context.put("appId", "random");
    Method method = Whitebox.getMethod(TelemetryGenerator.class, "getProducer", Map.class);
    Producer producer = (Producer) method.invoke(null, context);
    assertEquals("random", producer.getId());
  }

  @Test
  public void testGetProducerWithoutAppId()
      throws InvocationTargetException, IllegalAccessException {
    context.put("telemetry_pdata_id", "local.sunbird.learning.service");
    Method method = Whitebox.getMethod(TelemetryGenerator.class, "getProducer", Map.class);
    Producer producer = (Producer) method.invoke(null, context);
    assertEquals("local.sunbird.learning.service", producer.getId());
  }

  @AfterClass
  public static void tearDown() throws Exception {
    context.clear();
  }
}
