package org.sunbird.validator;

import org.junit.Assert;
import org.junit.Test;

public class EmailValidatorTest {

  @Test
  public void testValidateEmail() {
    boolean isValid = EmailValidator.isEmailValid("xyz@xyz.com");
    Assert.assertTrue(isValid);
  }

  @Test
  public void testValidateEmail2() {
    boolean isValid = EmailValidator.isEmailValid("xy@z@xyz.com");
    Assert.assertFalse(isValid);
  }

  @Test
  public void testValidateEmail3() {
    boolean isValid = EmailValidator.isEmailValid("");
    Assert.assertFalse(isValid);
  }
}
