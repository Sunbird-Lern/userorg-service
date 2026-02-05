package controllers.usermanagement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.BaseApplicationTest;
import controllers.DummyActor;
import java.io.IOException;
import java.util.Map;
import modules.OnRequestHandler;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.sunbird.response.ResponseCode;
import org.sunbird.response.Response;
import org.sunbird.response.ResponseParams;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;
import util.ACTORS;

@Ignore
@PrepareForTest(OnRequestHandler.class)
@PowerMockIgnore({"javax.management.*", "jdk.internal.reflect.*", "javax.crypto.*"})
public class UserTypeControllerTest extends BaseApplicationTest {
  @Before
  public void before() {
    setup(ACTORS.USER_TYPE_ACTOR, DummyActor.class);
  }

  @Test
  public void testGetUserTypeSuccess() {
    Result result = performTest("/v1/user/type/list", "GET", null);
    assertEquals(getResponseCode(result), ResponseCode.SUCCESS.name());
    assertTrue(getResponseStatus(result) == 200);
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
