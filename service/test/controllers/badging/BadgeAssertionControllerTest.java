package controllers.badging;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.when;

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
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.models.util.BadgingJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import play.libs.Json;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import play.test.Helpers;
import util.RequestInterceptor;

/** Created by arvind on 5/3/18. */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "jdk.internal.reflect.*"})
@PrepareForTest(OnRequestHandler.class)
public class BadgeAssertionControllerTest extends BaseApplicationTest {

  @Before
  public void before() {
    setup(DummyActor.class);
  }

  @Test
  public void testAssertionCreate() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(BadgingJsonKey.ISSUER_ID, "issuerId");
    innerMap.put(BadgingJsonKey.BADGE_CLASS_ID, "badgeid");
    innerMap.put(BadgingJsonKey.RECIPIENT_EMAIL, "abc@gmail.com");
    innerMap.put(BadgingJsonKey.RECIPIENT_ID, "userIdorcontentId");
    innerMap.put(BadgingJsonKey.RECIPIENT_TYPE, "user");
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);

    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/issuer/badge/assertion/create").method("POST");
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void createAssertionWithInvalidData() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(BadgingJsonKey.BADGE_CLASS_ID, "badgeid");
    innerMap.put(BadgingJsonKey.RECIPIENT_EMAIL, "abc@gmail.com");
    innerMap.put(BadgingJsonKey.RECIPIENT_ID, "userIdorcontentId");
    innerMap.put(BadgingJsonKey.RECIPIENT_TYPE, "user");
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);

    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/issuer/badge/assertion/create").method("POST");
    Result result = Helpers.route(application, req);
    assertEquals(400, result.status());
  }

  @Test
  public void assertionWithInvalidRecipent() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(BadgingJsonKey.BADGE_CLASS_ID, "badgeid");
    innerMap.put(BadgingJsonKey.RECIPIENT_EMAIL, "abc@gmail.com");
    innerMap.put(BadgingJsonKey.RECIPIENT_ID, "userIdorcontentId");
    innerMap.put(BadgingJsonKey.RECIPIENT_TYPE, "textbook");
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);

    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/issuer/badge/assertion/create").method("POST");
    Result result = Helpers.route(application, req);
    assertEquals(400, result.status());
  }

  @Test
  public void getAssertionTest() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
    Map<String, Object> requestMap = new HashMap<>();
    RequestBuilder req =
        new RequestBuilder().uri("/v1/issuer/badge/assertion/read/assertionId").method("GET");
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  @Ignore
  public void getAssertionListTest() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    List<String> assertionList = new ArrayList<String>();
    assertionList.add("assertionId");
    innerMap.put(JsonKey.FILTERS, assertionList);
    Map<String, Object> dataMap = new HashMap<>();
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/issuer/badge/assertion/search").method("POST");
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void getAssertionListWithInvalidTYpe() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(BadgingJsonKey.ASSERTION_ID, "assertionId");
    List<Map<String, Object>> list = new ArrayList<>();
    list.add(innerMap);
    Map<String, Object> dataMap = new HashMap<>();
    dataMap.put(BadgingJsonKey.BADGE_ASSERTIONS, list);
    requestMap.put(JsonKey.REQUEST, dataMap);
    String data = mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/issuer/badge/assertion/search").method("POST");
    Result result = Helpers.route(application, req);
    assertEquals(400, result.status());
  }

  @Test
  public void revokeAssertionTest() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(BadgingJsonKey.ASSERTION_ID, "assertionId");
    innerMap.put(BadgingJsonKey.RECIPIENT_ID, "userIdorcontentId");
    innerMap.put(BadgingJsonKey.RECIPIENT_TYPE, "user");
    innerMap.put(BadgingJsonKey.REVOCATION_REASON, "some reason");
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder()
            .bodyJson(json)
            .uri("/v1/issuer/badge/assertion/delete")
            .method("DELETE");
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void revokeAssertionFailureTest() {
    PowerMockito.mockStatic(RequestInterceptor.class);
    when(RequestInterceptor.verifyRequestData(Mockito.anyObject()))
        .thenReturn("{userId} uuiuhcf784508 8y8c79-fhh");
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(BadgingJsonKey.ASSERTION_ID, "assertionId");
    innerMap.put(BadgingJsonKey.RECIPIENT_ID, "userIdorcontentId");
    innerMap.put(BadgingJsonKey.RECIPIENT_TYPE, "user");
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder()
            .bodyJson(json)
            .uri("/v1/issuer/badge/assertion/delete")
            .method("DELETE");
    Result result = Helpers.route(application, req);
    assertEquals(400, result.status());
  }

  private static String mapToJson(Map map) {
    ObjectMapper mapperObj = new ObjectMapper();
    String jsonResp = "";
    try {
      jsonResp = mapperObj.writeValueAsString(map);
    } catch (IOException e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    return jsonResp;
  }
}
