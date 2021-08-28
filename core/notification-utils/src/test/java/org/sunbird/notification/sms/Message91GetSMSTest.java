package org.sunbird.notification.sms;

import org.junit.Assert;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.sunbird.notification.sms.providerimpl.Msg91SmsProvider;
import org.sunbird.request.RequestContext;

@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
public class Message91GetSMSTest extends BaseMessageTest {

  // @Test
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
