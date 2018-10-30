package controllers.usermanagement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import controllers.BaseControllerTest;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;
import play.mvc.Result;
import play.test.Helpers;

public class UserStatusControllerTest extends BaseControllerTest {

  private static String userId = "user-id";

  @Test
  public void testBlockUserSuccess() {

    Result result = performTest("/v1/user/block", "POST", userStatusRequest(userId));
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains("success"));
    assertEquals(200, result.status());
  }

  @Test
  public void testBlockUserFailureWithouUserId() {

    Result result = performTest("/v1/user/block", "POST", userStatusRequest(null));
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }

  @Test
  public void testUnBlockUserSuccess() {

    Result result = performTest("/v1/user/unblock", "POST", userStatusRequest(userId));
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains("success"));
    assertEquals(200, result.status());
  }

  @Test
  public void testUnBlockUserFailureWithoutUserId() {

    Result result = performTest("/v1/user/unblock", "POST", userStatusRequest(null));
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.mandatoryParamsMissing.getErrorCode()));
    assertEquals(400, result.status());
  }

  private Map userStatusRequest(String userId) {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.USER_ID, userId);
    requestMap.put(JsonKey.REQUEST, innerMap);
    return requestMap;
  }
}
