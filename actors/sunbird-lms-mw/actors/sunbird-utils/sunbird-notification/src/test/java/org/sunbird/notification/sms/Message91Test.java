package org.sunbird.notification.sms;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.sunbird.notification.sms.provider.ISmsProvider;
import org.sunbird.notification.sms.providerimpl.Msg91SmsProvider;
import org.sunbird.notification.utils.SMSFactory;

public class Message91Test extends BaseMessageTest {

	@Test
	public void testInitSuccess() {
		boolean response = Msg91SmsProvider.init();
		Assert.assertTrue(response);
	}

	@Test
	public void testGetInstanceSuccessWithoutName() {
		ISmsProvider object = SMSFactory.getInstance(null);
		Assert.assertTrue(object instanceof Msg91SmsProvider);
	}

	@Test
	public void testGetInstanceSuccessWithName() {
		ISmsProvider object = SMSFactory.getInstance("91SMS");
		Assert.assertTrue(object instanceof Msg91SmsProvider);
	}

	@Test
	public void testSendSuccess() {
		ISmsProvider object = SMSFactory.getInstance("91SMS");
		boolean response = object.send("9666666666", "test sms");
		Assert.assertTrue(response);
	}

	@Test
	public void testSendFailureWithFormattedPhone() {
		ISmsProvider object = SMSFactory.getInstance("91SMS");
		boolean response = object.send("(966) 3890-445", "test sms 122");
		Assert.assertFalse(response);
	}

	@Test
	public void testSendSuccessWithoutCountryCodeArg() {
		ISmsProvider object = SMSFactory.getInstance("91SMS");
		boolean response = object.send("919666666666", "test sms 122");
		Assert.assertTrue(response);
	}

	@Test
	public void testSendSuccessWithoutCountryCodeArgAndPlus() {
		ISmsProvider object = SMSFactory.getInstance("91SMS");
		boolean response = object.send("+919666666666", "test sms 122");
		Assert.assertTrue(response);
	}

	@Test
	public void testSendFailureWithEmptyPhone() {
		ISmsProvider object = SMSFactory.getInstance("91SMS");
		boolean response = object.send("", "test sms 122");
		Assert.assertFalse(response);
	}

	@Test
	public void testSendFailureWithEmptyMessage() {
		ISmsProvider object = SMSFactory.getInstance("91SMS");
		boolean response = object.send("9663890445", "");
		Assert.assertFalse(response);
	}

	@Test
	public void testSendWithEmptyPhoneAndMessage() {
		ISmsProvider object = SMSFactory.getInstance("91SMS");
		boolean response = object.send("", "");
		Assert.assertFalse(response);
	}

	@Test
	public void testSendFailureWithInvalidPhone() {
		ISmsProvider object = SMSFactory.getInstance("91SMS");
		boolean response = object.send("981se12345", "some message");
		Assert.assertFalse(response);
	}

	@Test
	public void testSendSuccessWithValidPhone() {
		ISmsProvider object = SMSFactory.getInstance("91SMS");
		boolean response = object.send("1111111111", "some message");
		Assert.assertTrue(response);
	}

	@Test
	public void testSendSuccessWithCountryCode() {
		ISmsProvider object = SMSFactory.getInstance("91SMS");
		boolean response = object.send("1234567898", "91", "some message");
		Assert.assertTrue(response);
	}

	@Test
	public void testSendSuccessWithCountryCodeAndPlus() {
		ISmsProvider object = SMSFactory.getInstance("91SMS");
		boolean response = object.send("0000000000", "+91", "some message");
		Assert.assertTrue(response);
	}

	@Test
	public void testSendSuccessWithMultiplePhones() {
		ISmsProvider object = SMSFactory.getInstance("91SMS");
		List<String> phones = new ArrayList<>();
		phones.add("1234567898");
		phones.add("1111111111");
		boolean response = object.send(phones, "some message");
		Assert.assertTrue(response);
	}

	@Test
	public void testSendFailureWithMultipleInvalidPhones() {
		ISmsProvider object = SMSFactory.getInstance("91SMS");
		List<String> phones = new ArrayList<>();
		phones.add("12345678");
		phones.add("11111");
		boolean response = object.send(phones, "some message");
		Assert.assertFalse(response);
	}

	@Test
	public void testSendFailureWithMultipleInvalidPhonesAndEmptyMsg() {
		ISmsProvider object = SMSFactory.getInstance("91SMS");
		List<String> phones = new ArrayList<>();
		phones.add("12345678");
		phones.add("11111");
		boolean response = object.send(phones, " ");
		Assert.assertFalse(response);
	}

}
