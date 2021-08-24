package org.sunbird.datasecurity.impl;

import static org.junit.Assert.*;

import java.util.HashMap;
import org.junit.Test;

public class LogMaskServiceImplTest {
  private LogMaskServiceImpl logMaskService = new LogMaskServiceImpl();

  @Test
  public void maskEmail() {
    HashMap<String, String> emailMaskExpectations =
        new HashMap<>() {
          {
            put("abc@gmail.com", "ab*@gmail.com");
            put("abcd@yahoo.com", "ab**@yahoo.com");
            put("abcdefgh@testmail.org", "ab******@testmail.org");
          }
        };
    emailMaskExpectations.forEach(
        (email, expectedResult) -> {
          assertEquals(expectedResult, logMaskService.maskEmail(email));
        });
  }

  @Test
  public void maskPhone() {
    HashMap<String, String> phoneMaskExpectations =
        new HashMap<>() {
          {
            put("0123456789", "01234*****");
            put("123-456-789", "123-4******");
            put("123", "123");
          }
        };
    phoneMaskExpectations.forEach(
        (phone, expectedResult) -> {
          assertEquals(expectedResult, logMaskService.maskPhone(phone));
        });
  }

  @Test
  public void maskOTP() {
    HashMap<String, String> phoneMaskExpectations =
        new HashMap<>() {
          {
            put("123456", "1234**");
            put("1234567", "1234***");

            put("1234", "12**");
            put("123", "12*");
          }
        };
    phoneMaskExpectations.forEach(
        (otp, expectedResult) -> {
          assertEquals(expectedResult, logMaskService.maskOTP(otp));
        });
  }
}
