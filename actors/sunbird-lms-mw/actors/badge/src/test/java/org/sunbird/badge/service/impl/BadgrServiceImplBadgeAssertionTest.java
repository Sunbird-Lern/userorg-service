package org.sunbird.badge.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.HttpUtilResponse;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.BadgingJsonKey;
import org.sunbird.common.models.util.HttpUtil;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.telemetry.util.TelemetryUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HttpUtil.class, TelemetryUtil.class, BadgrServiceImpl.class})
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
public class BadgrServiceImplBadgeAssertionTest {
  private BadgrServiceImpl badgrServiceImpl;

  private Request request;

  private static final String BADGE_ASSERTION_REVOKE_RESPONSE_FAILURE =
      "Assertion is already revoked.";

  private static final String VALUE_ASSERTION_ID = "2093cf30-f82e-4975-88f8-35230832db14";
  private static final String VALUE_RECIPIENT_ID = "bf5c4aa7-7d70-4488-915e-6e6ba3f7099b";
  private static final String VALUE_RECIPIENT_TYPE_USER = "user";
  private static final String VALUE_REVOCATION_REASON = "some reason";
  private static final String VALUE_RECIPIENT_EMAIL = "someone@someorg.com";

  @Before
  public void setUp() throws Exception {
    PowerMockito.mockStatic(HttpUtil.class);

    PowerMockito.mockStatic(TelemetryUtil.class);
    PowerMockito.doNothing()
        .when(
            TelemetryUtil.class,
            "telemetryProcessingCall",
            Mockito.anyMap(),
            Mockito.anyMap(),
            Mockito.anyList());

    PowerMockito.mockStatic(BadgrServiceImpl.class);

    badgrServiceImpl = new BadgrServiceImpl();
    request = new Request();
  }

  @Test
  public void testRevokeAssertionSuccess() throws IOException {
    PowerMockito.when(
            HttpUtil.sendDeleteRequest(Mockito.anyString(), Mockito.anyMap(), Mockito.anyString()))
        .thenReturn(new HttpUtilResponse("", 200));
    PowerMockito.when(badgrServiceImpl.getEmail(Mockito.any(), Mockito.any()))
        .thenReturn(VALUE_RECIPIENT_EMAIL);

    request.put(BadgingJsonKey.ASSERTION_ID, VALUE_ASSERTION_ID);
    request.put(BadgingJsonKey.RECIPIENT_ID, VALUE_RECIPIENT_ID);
    request.put(BadgingJsonKey.RECIPIENT_TYPE, VALUE_RECIPIENT_TYPE_USER);
    request.put(BadgingJsonKey.REVOCATION_REASON, VALUE_REVOCATION_REASON);

    Response response = badgrServiceImpl.revokeAssertion(request);

    assertNotEquals(null, response);
    assertEquals(ResponseCode.OK, response.getResponseCode());
  }

  @Test
  public void testRevokeAssertionFailure() throws IOException {
    PowerMockito.when(
            HttpUtil.sendDeleteRequest(Mockito.anyString(), Mockito.anyMap(), Mockito.anyString()))
        .thenReturn(new HttpUtilResponse(BADGE_ASSERTION_REVOKE_RESPONSE_FAILURE, 400));
    PowerMockito.when(badgrServiceImpl.getEmail(Mockito.any(), Mockito.any()))
        .thenReturn(VALUE_RECIPIENT_EMAIL);

    request.put(BadgingJsonKey.ASSERTION_ID, VALUE_ASSERTION_ID);
    request.put(BadgingJsonKey.RECIPIENT_ID, VALUE_RECIPIENT_ID);
    request.put(BadgingJsonKey.RECIPIENT_TYPE, VALUE_RECIPIENT_TYPE_USER);
    request.put(BadgingJsonKey.REVOCATION_REASON, VALUE_REVOCATION_REASON);

    boolean thrown = false;

    try {
      badgrServiceImpl.revokeAssertion(request);
    } catch (ProjectCommonException exception) {
      thrown = true;
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), exception.getResponseCode());
    }

    assertEquals(true, thrown);
  }
}
