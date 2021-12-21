package org.sunbird.logging;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.request.RequestContext;
import org.sunbird.telemetry.collector.TelemetryAssemblerFactory;
import org.sunbird.telemetry.collector.TelemetryDataAssembler;
import org.sunbird.telemetry.validator.TelemetryObjectValidator;
import org.sunbird.telemetry.validator.TelemetryObjectValidatorV3;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
@PrepareForTest({
  TelemetryAssemblerFactory.class,
  TelemetryDataAssembler.class,
  TelemetryObjectValidatorV3.class
})
public class LoggerUtilTest {
  private static LoggerUtil loggerUtil;
  private static TelemetryDataAssembler telemetryDataAssembler;
  private static TelemetryObjectValidator telemetryObjectValidator;
  private static TelemetryObjectValidatorV3 telemetryObjectValidatorV3;

  @Before
  public void setup() throws Exception {
    loggerUtil = Mockito.mock(LoggerUtil.class);
    Mockito.mock(TelemetryAssemblerFactory.class);
    PowerMockito.mockStatic(TelemetryAssemblerFactory.class);
    telemetryDataAssembler = Mockito.mock(TelemetryDataAssembler.class);
    telemetryObjectValidator = Mockito.mock(TelemetryObjectValidator.class);
    telemetryObjectValidatorV3 = Mockito.mock(TelemetryObjectValidatorV3.class);
    PowerMockito.mockStatic(TelemetryObjectValidatorV3.class);
    // PowerMockito.whenNew(LoggerUtil.class).withAnyArguments().thenReturn(loggerUtil);
    PowerMockito.when(TelemetryAssemblerFactory.get()).thenReturn(telemetryDataAssembler);
    PowerMockito.when(TelemetryObjectValidatorV3.getInstance())
        .thenReturn(telemetryObjectValidator);
  }

  @Test
  public void debug() {
    loggerUtil = new LoggerUtil(this.getClass());
    RequestContext requestContext =
        new RequestContext(
            "someUid",
            "someDid",
            "someSid",
            "someAppId",
            "someAppVer",
            "someReqId",
            "someSource",
            "true",
            "operation");
    PowerMockito.when(telemetryDataAssembler.log(Mockito.any(), Mockito.anyMap()))
        .thenReturn("telemetry string");
    PowerMockito.when(telemetryObjectValidator.validateLog(Mockito.anyString())).thenReturn(true);
    loggerUtil.debug(requestContext, "debug message");
    Mockito.verify(telemetryDataAssembler, Mockito.times(1)).log(Mockito.any(), Mockito.anyMap());
  }
}
