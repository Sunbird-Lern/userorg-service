package org.sunbird.notification.sms.providerimpl;

import org.junit.Assert;
import org.junit.Test;
import org.sunbird.request.RequestContext;

/** Tests for Msg91SmsProvider targeting the SMS sending functionality using the HTTP GET method. */
public class Message91GetSMSTest extends BaseMessageTest {

  /** Tests successful SMS sending via the GET method. */
  @Test
  public void testSendSmsGetMethodSuccess() {
    Msg91SmsProvider megObj = new Msg91SmsProvider();
    boolean response = megObj.sendSmsGetMethod("4321111111", "say hai!", new RequestContext());
    Assert.assertTrue(response);
  }

  /** Tests failure when attempting to send an empty message via the GET method. */
  @Test
  public void testSendSmsGetMethodFailureWithoutMessage() {
    Msg91SmsProvider megObj = new Msg91SmsProvider();
    boolean response = megObj.sendSmsGetMethod("4321111111", "", new RequestContext());
    Assert.assertFalse(response);
  }

  /** Tests failure when attempting to send a whitespace-only message via the GET method. */
  @Test
  public void testSendSmsGetMethodFailureWithEmptySpace() {
    Msg91SmsProvider megObj = new Msg91SmsProvider();
    boolean response = megObj.sendSmsGetMethod("4321111111", "  ", new RequestContext());
    Assert.assertFalse(response);
  }
}
