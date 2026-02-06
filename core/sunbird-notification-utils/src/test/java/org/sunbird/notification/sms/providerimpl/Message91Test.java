package org.sunbird.notification.sms.providerimpl;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.sunbird.notification.sms.provider.ISmsProvider;
import org.sunbird.notification.utils.SMSFactory;
import org.sunbird.request.RequestContext;

/** General tests for Msg91SmsProvider targeting basic configuration and sending logic. */
public class Message91Test extends BaseMessageTest {

  /** Tests successful initialization of the Msg91 provider. */
  @Test
  public void testInitSuccess() {
    boolean response = Msg91SmsProvider.init();
    Assert.assertTrue(response);
  }

  /** Tests that SMSFactory correctly returns a Msg91SmsProvider instance by default. */
  @Test
  public void testGetInstanceSuccessWithoutName() {
    ISmsProvider object = SMSFactory.getInstance();
    Assert.assertTrue(object instanceof Msg91SmsProvider);
  }

  /** Tests that SMSFactory correctly returns a Msg91SmsProvider instance. */
  @Test
  public void testGetInstanceSuccessWithName() {
    ISmsProvider object = SMSFactory.getInstance();
    Assert.assertTrue(object instanceof Msg91SmsProvider);
  }

  /** Tests basic SMS sending functionality. */
  @Test
  public void testSendSuccess() {
    ISmsProvider object = SMSFactory.getInstance();
    boolean response = object.send("9666666666", "test sms", new RequestContext());
    Assert.assertFalse(response);
  }

  /** Tests sending behavior with a formatted phone number. */
  @Test
  public void testSendFailureWithFormattedPhone() {
    ISmsProvider object = SMSFactory.getInstance();
    boolean response = object.send("(966) 3890-445", "test sms 122", new RequestContext());
    Assert.assertFalse(response);
  }

  /** Tests sending behavior with a phone number including country code. */
  @Test
  public void testSendSuccessWithoutCountryCodeArg() {
    ISmsProvider object = SMSFactory.getInstance();
    boolean response = object.send("919666666666", "test sms 122", new RequestContext());
    Assert.assertFalse(response);
  }

  /** Tests sending behavior with a phone number including country code and '+' symbol. */
  @Test
  public void testSendSuccessWithoutCountryCodeArgAndPlus() {
    ISmsProvider object = SMSFactory.getInstance();
    boolean response = object.send("+919666666666", "test sms 122", new RequestContext());
    Assert.assertFalse(response);
  }

  /** Tests failure when providing an empty phone number. */
  @Test
  public void testSendFailureWithEmptyPhone() {
    ISmsProvider object = SMSFactory.getInstance();
    boolean response = object.send("", "test sms 122", new RequestContext());
    Assert.assertFalse(response);
  }

  /** Tests failure when providing an empty message content. */
  @Test
  public void testSendFailureWithEmptyMessage() {
    ISmsProvider object = SMSFactory.getInstance();
    boolean response = object.send("9663890445", "", new RequestContext());
    Assert.assertFalse(response);
  }

  /** Tests behavior with both empty phone and empty message. */
  @Test
  public void testSendWithEmptyPhoneAndMessage() {
    ISmsProvider object = SMSFactory.getInstance();
    boolean response = object.send("", "", new RequestContext());
    Assert.assertFalse(response);
  }

  /** Tests behavior with an invalid phone number. */
  @Test
  public void testSendFailureWithInvalidPhone() {
    ISmsProvider object = SMSFactory.getInstance();
    boolean response = object.send("981se12345", "some message", new RequestContext());
    Assert.assertFalse(response);
  }

  /** Tests behavior with a valid phone number. */
  @Test
  public void testSendSuccessWithValidPhone() {
    ISmsProvider object = SMSFactory.getInstance();
    boolean response = object.send("1111111111", "some message", new RequestContext());
    Assert.assertFalse(response);
  }

  /** Tests sending with an explicit country code. */
  @Test
  public void testSendSuccessWithCountryCode() {
    ISmsProvider object = SMSFactory.getInstance();
    boolean response = object.send("1234567898", "91", "some message", new RequestContext());
    Assert.assertFalse(response);
  }

  /** Tests sending with an explicit country code and '+' symbol. */
  @Test
  public void testSendSuccessWithCountryCodeAndPlus() {
    ISmsProvider object = SMSFactory.getInstance();
    boolean response = object.send("0000000000", "+91", "some message", new RequestContext());
    Assert.assertFalse(response);
  }

  /** Tests sending to multiple phone numbers. */
  @Test
  public void testSendSuccessWithMultiplePhones() {
    ISmsProvider object = SMSFactory.getInstance();
    List<String> phones = new ArrayList<>();
    phones.add("1234567898");
    phones.add("1111111111");
    boolean response = object.send(phones, "some message", new RequestContext());
    Assert.assertFalse(response);
  }

  /** Tests failure when multiple invalid phone numbers are provided. */
  @Test
  public void testSendFailureWithMultipleInvalidPhones() {
    ISmsProvider object = SMSFactory.getInstance();
    List<String> phones = new ArrayList<>();
    phones.add("12345678");
    phones.add("11111");
    boolean response = object.send(phones, "some message", new RequestContext());
    Assert.assertFalse(response);
  }

  /** Tests failure with invalid phones and an empty message. */
  @Test
  public void testSendFailureWithMultipleInvalidPhonesAndEmptyMsg() {
    ISmsProvider object = SMSFactory.getInstance();
    List<String> phones = new ArrayList<>();
    phones.add("12345678");
    phones.add("11111");
    boolean response = object.send(phones, " ", new RequestContext());
    Assert.assertFalse(response);
  }
}
