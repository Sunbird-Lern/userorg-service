package controllers.usermanagement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.BaseApplicationTest;
import controllers.DummyActor;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import modules.OnRequestHandler;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.response.Response;
import org.sunbird.response.ResponseParams;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;
import util.ACTORS;

@PrepareForTest(OnRequestHandler.class)
@PowerMockIgnore({"javax.management.*", "jdk.internal.reflect.*", "javax.crypto.*"})
@Ignore
public class UserStatusControllerTest extends BaseApplicationTest {

  private static String userId = "someUserId";

  @Before
  public void before() throws Exception {
    setup(ACTORS.USER_STATUS_ACTOR, DummyActor.class);
  }

  @Test
  public void testBlockUserSuccess() {
    Result result = performTest("/v1/user/block", "POST", userStatusRequest(userId));
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testBlockUserFailureWithoutUserId() {
    Result result = performTest("/v1/user/block", "POST", userStatusRequest(null));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testUnblockUserSuccess() {
    Result result = performTest("/v1/user/unblock", "POST", userStatusRequest(userId));
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
    assertTrue(getResponseStatus(result) == 200);
  }

  @Test
  public void testUnblockUserFailureWithoutUserId() {
    Result result = performTest("/v1/user/unblock", "POST", userStatusRequest(null));
    assertEquals(getResponseCode(result), ResponseCode.CLIENT_ERROR.name());
    assertTrue(getResponseStatus(result) == 400);
  }

  private Map userStatusRequest(String userId) {
    Map<String, Object> requestMap = new HashMap<>();

    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.USER_ID, userId);
    requestMap.put(JsonKey.REQUEST, innerMap);

    return requestMap;
  }

  public Result performTest(String url, String method, Map map) {
    String data = mapToJson(map);
    Http.RequestBuilder req;
    if (StringUtils.isNotBlank(data)) {
      JsonNode json = Json.parse(data);
      req = new Http.RequestBuilder().bodyJson(json).uri(url).method(method);
    } else {
      req = new Http.RequestBuilder().uri(url).method(method);
    }
    // req.headers(new Http.Headers(headerMap));
    Result result = Helpers.route(application, req);
    return result;
  }

  public String mapToJson(Map map) {
    ObjectMapper mapperObj = new ObjectMapper();
    String jsonResp = "";

    if (map != null) {
      try {
        jsonResp = mapperObj.writeValueAsString(map);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return jsonResp;
  }

  public String getResponseCode(Result result) {
    String responseStr = Helpers.contentAsString(result);
    ObjectMapper mapper = new ObjectMapper();

    try {
      Response response = mapper.readValue(responseStr, Response.class);
      ResponseParams params = response.getParams();
      if (result.status() != 200) {
        return response.getResponseCode().name();
      } else {
        return params.getStatus();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "";
  }

  public int getResponseStatus(Result result) {
    return result.status();
  }
}
