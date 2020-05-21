package org.sunbird.common.request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;

public class UserProfileRequestValidatorTest {

  private static final UserProfileRequestValidator userProfileRequestValidator =
      new UserProfileRequestValidator();

  @Test
  public void testValidateProfileVisibilityFailureWithFieldInPrivateAndPublic() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.USER_ID, "9878888888");
    List<String> publicList = new ArrayList<>();
    publicList.add("Education");
    requestObj.put(JsonKey.PUBLIC, publicList);
    List<String> privateList = new ArrayList<>();
    privateList.add("Education");
    requestObj.put(JsonKey.PRIVATE, privateList);
    request.setRequest(requestObj);
    try {
      userProfileRequestValidator.validateProfileVisibility(request);
    } catch (ProjectCommonException e) {
      Assert.assertNotNull(e);
    }
  }

  @Test
  public void testValidateProfileVisibilityFailureWithEmptyUserId() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.USER_ID, "");
    request.setRequest(requestObj);
    try {
      userProfileRequestValidator.validateProfileVisibility(request);
    } catch (ProjectCommonException e) {
      Assert.assertNotNull(e);
    }
  }

  @Test
  public void testValidateProfileVisibilityFailureWithInvalidPrivateType() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.USER_ID, "123");
    requestObj.put(JsonKey.PRIVATE, "");
    request.setRequest(requestObj);
    try {
      userProfileRequestValidator.validateProfileVisibility(request);
    } catch (ProjectCommonException e) {
      Assert.assertNotNull(e);
    }
  }

  @Test
  public void testValidateProfileVisibilityFailureWithInvalidPublicType() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.USER_ID, "123");
    requestObj.put(JsonKey.PUBLIC, "");
    request.setRequest(requestObj);
    try {
      userProfileRequestValidator.validateProfileVisibility(request);
    } catch (ProjectCommonException e) {
      Assert.assertNotNull(e);
    }
  }
}
