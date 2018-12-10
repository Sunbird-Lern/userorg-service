package controllers.usermanagement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import controllers.BaseControllerTest;
import java.util.HashMap;
import java.util.Map;
import org.junit.Ignore;
import org.junit.Test;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;
import play.mvc.Result;

@Ignore
public class UserStatusControllerTest extends BaseControllerTest {

  private static String userId = "someUserId";

  @Test
  public void testBlockUserSuccess() {
    Result result = performTest("/v1/user/block", "POST", userStatusRequest(userId));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testBlockUserFailureWithoutUserId() {
    Result result = performTest("/v1/user/block", "POST", userStatusRequest(null));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testUnblockUserSuccess() {
    Result result = performTest("/v1/user/unblock", "POST", userStatusRequest(userId));
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testUnblockUserFailureWithoutUserId() {
    Result result = performTest("/v1/user/unblock", "POST", userStatusRequest(null));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  private Map userStatusRequest(String userId) {
    Map<String, Object> requestMap = new HashMap<>();

    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.USER_ID, userId);
    requestMap.put(JsonKey.REQUEST, innerMap);

    return requestMap;
  }
}
