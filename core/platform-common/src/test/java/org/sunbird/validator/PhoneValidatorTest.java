package org.sunbird.validator;

import org.junit.Assert;
import org.junit.Test;

public class PhoneValidatorTest {

  @Test
  public void testValidatePhoneWithCountryCode() {
    boolean isValid = PhoneValidator.validatePhone("9742501212", "91");
    Assert.assertTrue(isValid);
  }

  @Test
  public void testValidatePhoneWithoutCountryCode() {
    boolean isValid = PhoneValidator.validatePhone("9742501212", "");
    Assert.assertTrue(isValid);
  }

  @Test
  public void testValidatePhoneWithInvalidPhone() {
    boolean isValid = PhoneValidator.validatePhone("00000000000", "");
    Assert.assertFalse(isValid);
  }
}
