package org.sunbird.learner.util;

import org.junit.Assert;
import org.junit.Test;

public class OTPUtilTest {

  @Test
  public void generateOtpTest() {
    String code = OTPUtil.generateOTP();
    Assert.assertTrue(code.length() > 4);
  }
}
