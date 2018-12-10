package controllers.usermanagement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.route;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseControllerTest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Ignore;
import org.junit.Test;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;
import play.libs.Json;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import play.test.Helpers;

@Ignore
public class UserProfileControllerTest extends BaseControllerTest {

  private static String userId = "{userId} uuiuhcf784508 8y8c79-fhh";

  @Test
  public void testProfileVisibilitySuccess() {

    String data = getRequesteddata(userId);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/user/profile/visibility").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    assertTrue(response.contains("success"));
    assertEquals(200, result.status());
  }

  @Test
  public void testProfileVisibilityFailureWithDifferentUsedIdAndRequestedId() {

    String data = getRequesteddata("some-user-id");
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/user/profile/visibility").method("POST");
    req.headers(headerMap);
    Result result = route(req);
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
}
