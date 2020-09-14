package org.sunbird.learner.util;

import org.junit.Assert;
import org.junit.Test;

public class OTPUtilTest {

  @Test
  public void otpLengthTest() {
    String code = OTPUtil.generateOTP();
    code = OTPUtil.ensureOtpLength(code, null);
    Assert.assertTrue(code.length() >= 4);
  }

  @Test
  public void generateOtpTest() {
    for (int i = 0; i < 1000; i++) {
      String code = OTPUtil.generateOTP();
      code = OTPUtil.ensureOtpLength(code, null);
      Assert.assertTrue(code.length() >= 4);
    }
  }
}
