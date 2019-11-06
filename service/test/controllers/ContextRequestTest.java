package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.HeaderParam;
import play.libs.Json;
import play.mvc.Http;

@Ignore
public class ContextRequestTest extends BaseApplicationTest {

  private static String userId = "{userId} uuiuhcf784508 8y8c79-fhh";
  private static Map<String, String[]> headerMap;

  @Before
  public void before() {
    setup(DummyActor.class);
    headerMap = new HashMap<>();
    headerMap.put(HeaderParam.X_APP_ID.getName(), new String[] {"Some app Id"});
  }

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
