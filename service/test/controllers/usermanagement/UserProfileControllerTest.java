package controllers.usermanagement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.BaseApplicationTest;
import controllers.DummyActor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import modules.OnRequestHandler;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.HeaderParam;
import org.sunbird.common.responsecode.ResponseCode;
import play.libs.Json;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import play.test.Helpers;

@PrepareForTest(OnRequestHandler.class)
@PowerMockIgnore({"javax.management.*", "jdk.internal.reflect.*"})
public class UserProfileControllerTest extends BaseApplicationTest {

  private static String userId = "hhuuiuhcf784508 8y8c79-fhh";
  private static Map<String, String[]> headerMap;

  @Before
  public void before() {
    setup(DummyActor.class);
    headerMap = new HashMap<String, String[]>();
    headerMap.put(HeaderParam.X_Consumer_ID.getName(), new String[] {"Service test consumer"});
    headerMap.put(HeaderParam.X_Device_ID.getName(), new String[] {"Some Device Id"});
    headerMap.put(
        HeaderParam.X_Authenticated_Userid.getName(), new String[] {"Authenticated user id"});
    headerMap.put(JsonKey.MESSAGE_ID, new String[] {"Unique Message id"});
  }

  @Test
  public void testProfileVisibilityFailure() {

    String data = getRequesteddata(userId);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/user/profile/visibility").method("POST");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    String response = Helpers.contentAsString(result);
    assertEquals(400, result.status());
  }

  @Test
  public void testProfileVisibilityFailureWithDifferentUsedIdAndRequestedId() {

    String data = getRequesteddata("some-user-id");
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/user/profile/visibility").method("POST");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains(ResponseCode.invalidParameterValue.getErrorCode()));
    assertEquals(400, result.status());
  }

  private String getRequesteddata(String userId) {

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.USER_ID, userId);
    innerMap.put(JsonKey.PRIVATE_FIELDS, new ArrayList<>());
    innerMap.put(JsonKey.PUBLIC_FIELDS, new ArrayList<>());
    List<String> privateFields = new ArrayList<>();
    privateFields.add(JsonKey.PHONE);
    List<String> publicFields = new ArrayList<>();
    publicFields.add(JsonKey.EMAIL);
    innerMap.put(JsonKey.PRIVATE, privateFields);
    innerMap.put(JsonKey.PUBLIC, publicFields);
    requestMap.put(JsonKey.REQUEST, innerMap);
    return mapToJson(requestMap);
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
}
