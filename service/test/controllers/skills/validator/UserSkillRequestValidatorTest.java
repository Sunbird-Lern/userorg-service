package controllers.skills.validator;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;

/** Created by rajatgupta on 21/08/18. */
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
public class UserSkillRequestValidatorTest {

  @Test
  public void validateSkillUpdateSuccess() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.USER_ID, "111");
    List<String> skills = Arrays.asList("C", "C++");
    requestObj.put(JsonKey.SKILLS, skills);
    request.getContext().put(JsonKey.REQUESTED_BY, "111");

    request.setRequest(requestObj);
    try {
      new UserSkillRequestValidator().validateUpdateSkillRequest(request);
      response = true;
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals(true, response);
  }

  @Test(expected = ProjectCommonException.class)
  public void validateSkillUpdateWithUserIdMissingFailure() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    request.getContext().put(JsonKey.REQUESTED_BY, "111");
    List<String> skills = Arrays.asList("C", "C++");
    requestObj.put(JsonKey.SKILLS, skills);
    request.setRequest(requestObj);
    new UserSkillRequestValidator().validateUpdateSkillRequest(request);
  }

  @Test(expected = ProjectCommonException.class)
  public void validateSkillUpdateWithoutSkillsFailure() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.USER_ID, "111");
    request.getContext().put(JsonKey.REQUESTED_BY, "111");

    request.setRequest(requestObj);
    new UserSkillRequestValidator().validateUpdateSkillRequest(request);
  }

  @Test(expected = ProjectCommonException.class)
  public void validateSkillUpdateWithDifferentUserFailure() {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    requestObj.put(JsonKey.USER_ID, "111");
    List<String> skills = Arrays.asList("C", "C++");
    requestObj.put(JsonKey.SKILLS, skills);
    request.getContext().put(JsonKey.REQUESTED_BY, "112");
    request.setRequest(requestObj);

    new UserSkillRequestValidator().validateUpdateSkillRequest(request);
  }
}
