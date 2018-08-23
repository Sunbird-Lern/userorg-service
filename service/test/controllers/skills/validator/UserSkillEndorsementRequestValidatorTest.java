package controllers.skills.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;

@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
public class UserSkillEndorsementRequestValidatorTest {
  @Test
  public void testValidateSkillAddEndorsementSuccess() {
    boolean response = false;
    try {
      new UserSkillEndorsementRequestValidator()
          .validateSkillEndorsementRequest(createRequest("111", "112", "C", "111"));
      response = true;
    } catch (ProjectCommonException e) {
      fail();
    }
    assertEquals(true, response);
  }

  @Test(expected = ProjectCommonException.class)
  public void testValidateSkillAddEndorsementFailureWithUserIdMissing() {

    new UserSkillEndorsementRequestValidator()
        .validateSkillEndorsementRequest(createRequest(null, "112", "C", "111"));
  }

  @Test(expected = ProjectCommonException.class)
  public void testValidateSkillAddEndorsementFailureForSelf() {
    new UserSkillEndorsementRequestValidator()
        .validateSkillEndorsementRequest(createRequest("111", "111", "C", "111"));
  }

  @Test(expected = ProjectCommonException.class)
  public void testValidateSkillAddEndorsementFailureWithoutEndorsedUserId() {
    new UserSkillEndorsementRequestValidator()
        .validateSkillEndorsementRequest(createRequest("111", null, "C", "111"));
  }

  @Test(expected = ProjectCommonException.class)
  public void testValidateSkillAddEndorsementFailureWithoutSkillName() {
    new UserSkillEndorsementRequestValidator()
        .validateSkillEndorsementRequest(createRequest("111", "112", null, "111"));
  }

  private Request createRequest(
      String userId, String endorseUserId, String skillName, String requestedByUserId) {
    Request request = new Request();
    Map<String, Object> requestObj = new HashMap<>();
    if (userId != null) requestObj.put(JsonKey.USER_ID, userId);
    if (endorseUserId != null) requestObj.put(JsonKey.ENDORSED_USER_ID, endorseUserId);
    if (skillName != null) requestObj.put(JsonKey.SKILL_NAME, skillName);
    if (requestedByUserId != null)
      request.getContext().put(JsonKey.REQUESTED_BY, requestedByUserId);
    request.setRequest(requestObj);

    return request;
  }
}
