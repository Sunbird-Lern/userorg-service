package org.sunbird.notification.sms.providerimpl;

import org.junit.Assert;
import org.junit.Test;
import org.sunbird.request.RequestContext;

public class Message91GetSMSTest extends BaseMessageTest {

  @Test
  public void testSendSmsGetMethodSuccess() {
    Msg91SmsProvider megObj = new Msg91SmsProvider();
    boolean response = megObj.sendSmsGetMethod("4321111111", "say hai!", new RequestContext());
    Assert.assertTrue(response);
  }

  @Test
  public void testSendSmsGetMethodFailureWithoutMessage() {
    Msg91SmsProvider megObj = new Msg91SmsProvider();
    boolean response = megObj.sendSmsGetMethod("4321111111", "", new RequestContext());
    Assert.assertFalse(response);
  }

  @Test
  public void testSendSmsGetMethodFailureWithEmptySpace() {
    Msg91SmsProvider megObj = new Msg91SmsProvider();
    boolean response = megObj.sendSmsGetMethod("4321111111", "  ", new RequestContext());
    Assert.assertFalse(response);
  }
}
