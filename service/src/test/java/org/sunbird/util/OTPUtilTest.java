package org.sunbird.util;

import org.junit.Assert;
import org.junit.Test;

public class OTPUtilTest {

  @Test
  public void generateOtpTest() {
    for (int i = 0; i < 10000; i++) {
      String code = OTPUtil.generateOtp(null);
      Assert.assertTrue(code.length() >= 4);
    }
  }
}
