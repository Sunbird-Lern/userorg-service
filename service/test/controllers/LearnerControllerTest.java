package controllers;

import static org.junit.Assert.assertEquals;
import static play.test.Helpers.route;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;
import play.libs.Json;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import play.test.Helpers;

/** @author arvind */
public class LearnerControllerTest extends BaseControllerTest {

  private static final String COURSE_ID = "course-123";
  private static final String USER_ID = "user-123";
  private static final String CONTENT_ID = "content-123";
  private static final String BATCH_ID = "batch-123";

  @Test
  public void testUpdateContentStateSuccess() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    List<Object> list = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.CONTENT_ID, CONTENT_ID);
    map.put(JsonKey.STATUS, "Active");
    map.put(JsonKey.LAST_UPDATED_TIME, "2017-12-18 10:47:30:707+0530");
    list.add(map);
    innerMap.put("contents", list);
    innerMap.put("courseId", COURSE_ID);
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/content/state/update").method("PATCH");
    req.headers(headerMap);
    Result result = route(req);
    String response = Helpers.contentAsString(result);
    System.out.println(response);
    assertEquals(200, result.status());
  }

  @Test
  public void testGetContentStateSuccess() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.BATCH_ID, BATCH_ID);
    innerMap.put(JsonKey.USER_ID, USER_ID);
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/content/state/read").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(200, result.status());
  }

  @Test
  public void testGetContentStateFailureWithInvalidFieldType() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put("courseId", COURSE_ID);
    innerMap.put(JsonKey.COURSE_IDS, new HashMap<>());
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);

    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/content/state/read").method("POST");

    req.headers(headerMap);
    Result result = route(req);
    assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), result.status());
  }

  @Test
  public void testGetContentStateFailureWithoutUserId() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put("courseId", COURSE_ID);
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/content/state/read").method("POST");
    req.headers(headerMap);
    Result result = route(req);
    assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), result.status());
  }
}
