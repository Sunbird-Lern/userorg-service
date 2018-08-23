package controllers.skills.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;

@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
public class UserSkillRequestValidatorTest {

  @Test
  public void testValidateSkillUpdateSuccess() {
    boolean response = false;
    try {
      new UserSkillRequestValidator()
          .validateUpdateSkillRequest(createRequest("111", Arrays.asList("C", "C++"), "111"));
      response = true;
    } catch (ProjectCommonException e) {
      fail();
    }
    assertEquals(true, response);
  }

  @Test(expected = ProjectCommonException.class)
  public void testValidateSkillUpdateFailureWithUserIdMissing() {
    new UserSkillRequestValidator()
        .validateUpdateSkillRequest(createRequest(null, Arrays.asList("C", "C++"), "111"));
  }

  @Test(expected = ProjectCommonException.class)
  public void testValidateSkillUpdateSuccessWithEmptySkillsArray() {
    new UserSkillRequestValidator().validateUpdateSkillRequest(createRequest("111", null, "111"));
  }

  @Test(expected = ProjectCommonException.class)
  public void testValidateSkillUpdateFailureWithDifferentUser() {
    new UserSkillRequestValidator()
        .validateUpdateSkillRequest(createRequest("111", Arrays.asList("C", "C++"), "112"));
  }

  private Request createRequest(String userId, List skills, String requestedByUserId) {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();

    if (userId != null) requestObj.put(JsonKey.USER_ID, userId);
    if (!CollectionUtils.isEmpty(skills)) requestObj.put(JsonKey.SKILLS, skills);
    if (requestedByUserId != null)
      request.getContext().put(JsonKey.REQUESTED_BY, requestedByUserId);

    request.setRequest(requestObj);
    return request;
  }
}
