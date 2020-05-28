package org.sunbird.learner.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class UserFlagEnumTest {


    @Test
    public void testUserFlagEnumPhoneValue(){
        Assert.assertEquals(1,UserFlagEnum.PHONE_VERIFIED.getUserFlagValue());
        Assert.assertEquals(2,UserFlagEnum.EMAIL_VERIFIED.getUserFlagValue());
        Assert.assertEquals(4,UserFlagEnum.STATE_VALIDATED.getUserFlagValue());
    }
    @Test
    public void testUserFlagEnumPhoneType(){
        Assert.assertEquals("phoneVerified",UserFlagEnum.PHONE_VERIFIED.getUserFlagType());
        Assert.assertEquals("emailVerified",UserFlagEnum.EMAIL_VERIFIED.getUserFlagType());
        Assert.assertEquals("stateValidated",UserFlagEnum.STATE_VALIDATED.getUserFlagType());
    }
}