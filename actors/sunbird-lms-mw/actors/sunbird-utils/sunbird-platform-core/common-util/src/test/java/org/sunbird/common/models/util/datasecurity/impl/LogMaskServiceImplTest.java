package org.sunbird.common.models.util.datasecurity.impl;

import org.junit.Test;
import org.sunbird.common.request.UserRequestValidator;

import java.util.HashMap;

import static org.junit.Assert.*;

public class LogMaskServiceImplTest {
    private LogMaskServiceImpl logMaskService = new LogMaskServiceImpl();

    @Test
    public void maskEmail() {
        HashMap<String, String> emailMaskExpectations = new HashMap<String, String>(){
            {
                put("abc@gmail.com", "ab*@gmail.com");
                put("abcd@yahoo.com", "ab**@yahoo.com");
                put("abcdefgh@testmail.org", "abcd****@testmail.org");
            }
        };
        emailMaskExpectations.forEach((email, expectedResult) -> {
            assertEquals(expectedResult, logMaskService.maskEmail(email));
        });
    }

    @Test
    public void maskPhone() {
        HashMap<String, String> phoneMaskExpectations = new HashMap<String, String>(){
            {
                put("0123456789", "012345678*");
                put("123-456-789", "123-456-7**");
                put("123", "123");
            }
        };
        phoneMaskExpectations.forEach((phone, expectedResult) -> {
            assertEquals(expectedResult, logMaskService.maskPhone(phone));
        });
    }

    @Test
    public void maskOTP() {
        HashMap<String, String> phoneMaskExpectations = new HashMap<String, String>(){
            {
                put("123456", "12345*");
                put("1234567", "12345**");

                put("1234", "123*");
                put("123", "123");
            }
        };
        phoneMaskExpectations.forEach((otp, expectedResult) -> {
            assertEquals(expectedResult, logMaskService.maskOTP(otp));
        });
    }
}