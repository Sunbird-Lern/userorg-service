package org.sunbird.notification.sms;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.notification.sms.provider.ISmsProvider;
import org.sunbird.notification.sms.providerimpl.Msg91SmsProvider;
import org.sunbird.notification.utils.SMSFactory;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*"
})
public class Message91Test extends BaseMessageTest {

  @Test
  public void testInitSuccess() {
    boolean response = Msg91SmsProvider.init();
    Assert.assertTrue(response);
  }

  @Test
  public void testGetInstanceSuccessWithoutName() {
    ISmsProvider object = SMSFactory.getInstance();
    Assert.assertTrue(object instanceof Msg91SmsProvider);
  }

  @Test
  public void testGetInstanceSuccessWithName() {
    ISmsProvider object = SMSFactory.getInstance();
    Assert.assertTrue(object instanceof Msg91SmsProvider);
  }

  @Test
  public void testSendSuccess() {
    PowerMockito.mockStatic(SMSFactory.class);
    ISmsProvider msg91SmsProvider = PowerMockito.mock(Msg91SmsProvider.class);
    PowerMockito.when(SMSFactory.getInstance()).thenReturn(msg91SmsProvider);
    PowerMockito.when(msg91SmsProvider.send(Mockito.any(String.class), Mockito.any(String.class)))
        .thenReturn(true);
    ISmsProvider object = SMSFactory.getInstance();
    boolean response = object.send("9666666666", "test sms");
    Assert.assertTrue(response);
  }

  @Test
  public void testSendFailureWithFormattedPhone() {
    PowerMockito.mockStatic(SMSFactory.class);
    ISmsProvider msg91SmsProvider = PowerMockito.mock(Msg91SmsProvider.class);
    PowerMockito.when(SMSFactory.getInstance()).thenReturn(msg91SmsProvider);
    PowerMockito.when(msg91SmsProvider.send(Mockito.any(String.class), Mockito.any(String.class)))
        .thenReturn(false);
    ISmsProvider object = SMSFactory.getInstance();
    boolean response = object.send("(966) 3890-445", "test sms 122");
    Assert.assertFalse(response);
  }

  @Test
  public void testSendSuccessWithoutCountryCodeArg() {
    PowerMockito.mockStatic(SMSFactory.class);
    ISmsProvider msg91SmsProvider = PowerMockito.mock(Msg91SmsProvider.class);
    PowerMockito.when(SMSFactory.getInstance()).thenReturn(msg91SmsProvider);
    PowerMockito.when(msg91SmsProvider.send(Mockito.any(String.class), Mockito.any(String.class)))
        .thenReturn(true);
    ISmsProvider object = SMSFactory.getInstance();
    boolean response = object.send("919666666666", "test sms 122");
    Assert.assertTrue(response);
  }

  @Test
  public void testSendSuccessWithoutCountryCodeArgAndPlus() {
    PowerMockito.mockStatic(SMSFactory.class);
    ISmsProvider msg91SmsProvider = PowerMockito.mock(Msg91SmsProvider.class);
    PowerMockito.when(SMSFactory.getInstance()).thenReturn(msg91SmsProvider);
    PowerMockito.when(msg91SmsProvider.send(Mockito.any(String.class), Mockito.any(String.class)))
        .thenReturn(true);
    ISmsProvider object = SMSFactory.getInstance();
    boolean response = object.send("+919666666666", "test sms 122");
    Assert.assertTrue(response);
  }

  @Test
  public void testSendFailureWithEmptyPhone() {
    PowerMockito.mockStatic(SMSFactory.class);
    ISmsProvider msg91SmsProvider = PowerMockito.mock(Msg91SmsProvider.class);
    PowerMockito.when(SMSFactory.getInstance()).thenReturn(msg91SmsProvider);
    PowerMockito.when(msg91SmsProvider.send(Mockito.any(String.class), Mockito.any(String.class)))
        .thenReturn(false);
    ISmsProvider object = SMSFactory.getInstance();
    boolean response = object.send("", "test sms 122");
    Assert.assertFalse(response);
  }

  @Test
  public void testSendFailureWithEmptyMessage() {
    PowerMockito.mockStatic(SMSFactory.class);
    ISmsProvider msg91SmsProvider = PowerMockito.mock(Msg91SmsProvider.class);
    PowerMockito.when(SMSFactory.getInstance()).thenReturn(msg91SmsProvider);
    PowerMockito.when(msg91SmsProvider.send(Mockito.any(String.class), Mockito.any(String.class)))
        .thenReturn(false);
    ISmsProvider object = SMSFactory.getInstance();
    boolean response = object.send("9663890445", "");
    Assert.assertFalse(response);
  }

  @Test
  public void testSendWithEmptyPhoneAndMessage() {
    PowerMockito.mockStatic(SMSFactory.class);
    ISmsProvider msg91SmsProvider = PowerMockito.mock(Msg91SmsProvider.class);
    PowerMockito.when(SMSFactory.getInstance()).thenReturn(msg91SmsProvider);
    PowerMockito.when(msg91SmsProvider.send(Mockito.any(String.class), Mockito.any(String.class)))
        .thenReturn(false);
    ISmsProvider object = SMSFactory.getInstance();
    boolean response = object.send("", "");
    Assert.assertFalse(response);
  }

  @Test
  public void testSendFailureWithInvalidPhone() {
    PowerMockito.mockStatic(SMSFactory.class);
    ISmsProvider msg91SmsProvider = PowerMockito.mock(Msg91SmsProvider.class);
    PowerMockito.when(SMSFactory.getInstance()).thenReturn(msg91SmsProvider);
    PowerMockito.when(msg91SmsProvider.send(Mockito.any(String.class), Mockito.any(String.class)))
        .thenReturn(false);
    ISmsProvider object = SMSFactory.getInstance();
    boolean response = object.send("981se12345", "some message");
    Assert.assertFalse(response);
  }

  @Test
  public void testSendSuccessWithValidPhone() {
    PowerMockito.mockStatic(SMSFactory.class);
    ISmsProvider msg91SmsProvider = PowerMockito.mock(Msg91SmsProvider.class);
    PowerMockito.when(SMSFactory.getInstance()).thenReturn(msg91SmsProvider);
    PowerMockito.when(msg91SmsProvider.send(Mockito.any(String.class), Mockito.any(String.class)))
        .thenReturn(true);
    ISmsProvider object = SMSFactory.getInstance();
    boolean response = object.send("1111111111", "some message");
    Assert.assertTrue(response);
  }

  @Test
  public void testSendSuccessWithCountryCode() {
    PowerMockito.mockStatic(SMSFactory.class);
    ISmsProvider msg91SmsProvider = PowerMockito.mock(Msg91SmsProvider.class);
    PowerMockito.when(SMSFactory.getInstance()).thenReturn(msg91SmsProvider);
    PowerMockito.when(
            msg91SmsProvider.send(
                Mockito.any(String.class), Mockito.any(String.class), Mockito.any(String.class)))
        .thenReturn(true);
    ISmsProvider object = SMSFactory.getInstance();
    boolean response = object.send("1234567898", "91", "some message");
    Assert.assertTrue(response);
  }

  @Test
  public void testSendSuccessWithCountryCodeAndPlus() {
    PowerMockito.mockStatic(SMSFactory.class);
    ISmsProvider msg91SmsProvider = PowerMockito.mock(Msg91SmsProvider.class);
    PowerMockito.when(SMSFactory.getInstance()).thenReturn(msg91SmsProvider);
    PowerMockito.when(
            msg91SmsProvider.send(
                Mockito.any(String.class), Mockito.any(String.class), Mockito.any(String.class)))
        .thenReturn(true);
    ISmsProvider object = SMSFactory.getInstance();
    boolean response = object.send("0000000000", "+91", "some message");
    Assert.assertTrue(response);
  }

  @Test
  public void testSendSuccessWithMultiplePhones() {
    PowerMockito.mockStatic(SMSFactory.class);
    ISmsProvider msg91SmsProvider = PowerMockito.mock(Msg91SmsProvider.class);
    PowerMockito.when(SMSFactory.getInstance()).thenReturn(msg91SmsProvider);
    PowerMockito.when(msg91SmsProvider.send(Mockito.any(List.class), Mockito.any(String.class)))
        .thenReturn(true);
    ISmsProvider object = SMSFactory.getInstance();
    List<String> phones = new ArrayList<>();
    phones.add("1234567898");
    phones.add("1111111111");
    boolean response = object.send(phones, "some message");
    Assert.assertTrue(response);
  }

  @Test
  public void testSendFailureWithMultipleInvalidPhones() {
    PowerMockito.mockStatic(SMSFactory.class);
    ISmsProvider msg91SmsProvider = PowerMockito.mock(Msg91SmsProvider.class);
    PowerMockito.when(SMSFactory.getInstance()).thenReturn(msg91SmsProvider);
    PowerMockito.when(msg91SmsProvider.send(Mockito.any(List.class), Mockito.any(String.class)))
        .thenReturn(false);
    ISmsProvider object = SMSFactory.getInstance();
    List<String> phones = new ArrayList<>();
    phones.add("12345678");
    phones.add("11111");
    boolean response = object.send(phones, "some message");
    Assert.assertFalse(response);
  }

  @Test
  public void testSendFailureWithMultipleInvalidPhonesAndEmptyMsg() {
    PowerMockito.mockStatic(SMSFactory.class);
    ISmsProvider msg91SmsProvider = PowerMockito.mock(Msg91SmsProvider.class);
    PowerMockito.when(SMSFactory.getInstance()).thenReturn(msg91SmsProvider);
    PowerMockito.when(msg91SmsProvider.send(Mockito.any(List.class), Mockito.any(String.class)))
        .thenReturn(false);
    ISmsProvider object = SMSFactory.getInstance();
    List<String> phones = new ArrayList<>();
    phones.add("12345678");
    phones.add("11111");
    boolean response = object.send(phones, " ");
    Assert.assertFalse(response);
  }
}
