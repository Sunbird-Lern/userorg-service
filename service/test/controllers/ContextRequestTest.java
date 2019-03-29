package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.HeaderParam;
import play.libs.Json;
import play.mvc.Http;

public class ContextRequestTest extends BaseControllerTest {

  private static String userId = "{userId} uuiuhcf784508 8y8c79-fhh";

  @Test
  public void testContextRequest() {

    String data = getRequesteddata(userId);
    JsonNode json = Json.parse(data);
    Http.RequestBuilder req =
        new Http.RequestBuilder().bodyJson(json).uri("/v1/user/profile/visibility").method("POST");
    String appId = headerMap.get(HeaderParam.X_APP_ID)[0];
    Assert.assertTrue(!(appId.charAt(0) >= 65 && appId.charAt(0) <= 90));
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
