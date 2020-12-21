package org.sunbird.validator.user;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.models.user.UserType;

public class UserBulkUploadRequestValidatorTest {

  @Test
  public void testValidateOrganisationIdWithMandatoryParamMissing() {
    Map<String, Object> userMap = new HashMap<>();
    userMap.put(JsonKey.USER_TYPE, UserType.TEACHER.getTypeName());
    try {
      UserBulkUploadRequestValidator.validateUserBulkUploadRequest(userMap);
    } catch (Exception e) {
      Assert.assertEquals("Mandatory parameter orgId or orgExternalId is missing.", e.getMessage());
    }
  }

  @Test
  public void testValidateUserTypeWithMissingUserTypeParam() {
    Map<String, Object> userMap = new HashMap<>();
    try {
      userMap.put(JsonKey.USER_TYPE, "invalid");
      UserBulkUploadRequestValidator.validateUserBulkUploadRequest(userMap);
    } catch (Exception e) {
      Assert.assertTrue(e.getMessage().contains("Invalid userType"));
    }
  }

  @Test
  public void testUserBulkRequestValidatorSuccess() {
    Map<String, Object> userMap = new HashMap<>();
    try {
      userMap.put(JsonKey.USER_TYPE, UserType.TEACHER.getTypeName());
      userMap.put(JsonKey.ORG_ID, "anyOrgId");
      UserBulkUploadRequestValidator.validateUserBulkUploadRequest(userMap);
    } catch (Exception e) {
      System.out.println(e.getMessage());
      Assert.assertTrue(false);
    }
  }
}
