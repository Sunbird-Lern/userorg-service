package controllers.tenantmigration;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.BaseApplicationTest;
import controllers.DummyActor;
import controllers.TestUtil;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import modules.OnRequestHandler;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.response.ResponseParams;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.HeaderParam;
import org.sunbird.common.responsecode.ResponseCode;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;

@PrepareForTest(OnRequestHandler.class)
public class TenantMigrationControllerTest extends BaseApplicationTest {

  public static Map<String, List<String>> headerMap;

  @Before
  public void before() {
    setup(DummyActor.class);
    headerMap = new HashMap<>();
    headerMap.put(HeaderParam.X_Consumer_ID.getName(), Arrays.asList("Some consumer ID"));
    headerMap.put(HeaderParam.X_Device_ID.getName(), Arrays.asList("Some device ID"));
    headerMap.put(
        HeaderParam.X_Authenticated_Userid.getName(), Arrays.asList("Some authenticated user ID"));
    headerMap.put(JsonKey.MESSAGE_ID, Arrays.asList("Some message ID"));
    headerMap.put(HeaderParam.X_APP_ID.getName(), Arrays.asList("Some app Id"));
  }

  @After
  public void tearDown() throws Exception {
    headerMap.clear();
  }

  // @Test
  public void testMigrationUserSuccess() {
    Result result = performTest("/v1/user/migrate", "POST", getSuccessMigrationReq());
    assertEquals(
        ResponseCode.success.getErrorCode().toLowerCase(), TestUtil.getResponseCode(result));
  }

  @Test
  public void testMigrationUserFailure() {
    Result result =
        performTest("/v1/user/migrate", "POST", getFailureMigrationReq(JsonKey.CHANNEL));
    assertEquals(
        ResponseCode.mandatoryParamsMissing.getErrorCode(), TestUtil.getResponseCode(result));
  }

  @Test
  public void tesPrivatetMigrationUserSuccess() {
    Result result = performTest("/private/user/v1/migrate", "PATCH", getSuccessMigrationReq());
    assertEquals(
        ResponseCode.success.getErrorCode().toLowerCase(), TestUtil.getResponseCode(result));
  }

  private Map<String, Object> getSuccessMigrationReq() {
    Map<String, Object> request = new HashMap<>();
    Map<String, String> reqMap = new HashMap<>();
    reqMap.put(JsonKey.ACTION, "accept");
    reqMap.put(JsonKey.USER_ID, "userId");
    reqMap.put(JsonKey.USER_EXT_ID, "abc_ext_id");
    reqMap.put(JsonKey.CHANNEL, "TN");
    request.put(JsonKey.REQUEST, reqMap);
    return request;
  }

  private Map<String, Object> getFailureMigrationReq(String param) {
    Map<String, Object> request = new HashMap<>();
    Map<String, String> reqMap = new HashMap<>();
    reqMap.put(JsonKey.ACTION, "accept");
    reqMap.put(JsonKey.USER_ID, "userId");
    reqMap.put(JsonKey.USER_EXT_ID, "abc_ext_id");
    reqMap.put(JsonKey.CHANNEL, "TN");
    reqMap.remove(param);
    request.put(JsonKey.REQUEST, reqMap);
    return request;
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
        ProjectLogger.log(e.getMessage(), e);
      }
    }
    return jsonResp;
  }

  public String getResponseCode(Result result) {
    String responseStr = Helpers.contentAsString(result);
    ObjectMapper mapper = new ObjectMapper();

    try {
      Response response = mapper.readValue(responseStr, Response.class);

      if (response != null) {
        ResponseParams params = response.getParams();
        return params.getStatus();
      }
    } catch (Exception e) {
      ProjectLogger.log(
          "BaseControllerTest:getResponseCode: Exception occurred with error message = "
              + e.getMessage(),
          LoggerEnum.ERROR.name());
    }
    return "";
  }

  public int getResponseStatus(Result result) {
    return result.status();
  }
}
