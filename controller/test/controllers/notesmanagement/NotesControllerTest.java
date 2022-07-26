package controllers.notesmanagement;

import static controllers.TestUtil.mapToJson;
import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseApplicationTest;
import controllers.DummyActor;
import java.util.HashMap;
import java.util.Map;
import modules.OnRequestHandler;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.keys.JsonKey;
import play.libs.Json;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import play.test.Helpers;
import util.ACTORS;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "jdk.internal.reflect.*", "javax.crypto.*"})
@PrepareForTest(OnRequestHandler.class)
@Ignore
public class NotesControllerTest extends BaseApplicationTest {

  private static String USER_ID = "{userId} uuiuhcf784508 8y8c79-fhh";
  private static String COURSE_ID = "someUserId";
  private static String CONTENT_ID = "someUserId";
  private static String NOTE = "someUserId";
  private static String TITLE = "someUserId";
  private static String NOTE_ID = "noteId";

  @Before
  public void before() {
    setup(ACTORS.NOTES_MANAGEMENT_ACTOR, DummyActor.class);
  }

  @Test
  @Ignore
  public void testCreateNoteSuccess() {
    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(
        JsonKey.REQUEST, getCreateNoteRequest(USER_ID, COURSE_ID, CONTENT_ID, NOTE, TITLE));
    String data = mapToJson(requestMap);
    JsonNode json = Json.parse(data);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/note/create").method("POST");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void testCreateNoteFailureWithInvalidRequestData() {
    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(JsonKey.REQUEST, getCreateNoteRequest(USER_ID, "", "", "", TITLE));
    String data = mapToJson(requestMap);

    JsonNode json = Json.parse(data);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/note/create").method("POST");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(400, result.status());
  }

  @Test
  public void testGetNoteSuccess() {
    RequestBuilder req = new RequestBuilder().uri("/v1/note/read/" + NOTE_ID).method("GET");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void testUpdateNoteSuccess() {
    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(
        JsonKey.REQUEST, getCreateNoteRequest(USER_ID, COURSE_ID, CONTENT_ID, NOTE, TITLE));
    String data = mapToJson(requestMap);

    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/note/update/" + NOTE_ID).method("PATCH");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void testDeleteNoteSuccess() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.ID, "123");
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);

    JsonNode json = Json.parse(data);
    RequestBuilder req =
        new RequestBuilder().bodyJson(json).uri("/v1/note/delete/123").method("DELETE");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  @Test
  public void testSearchNoteSuccess() {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    Map<String, Object> filterMap = new HashMap<>();
    filterMap.put(JsonKey.ID, "123");
    innerMap.put(JsonKey.FILTERS, filterMap);
    requestMap.put(JsonKey.REQUEST, innerMap);
    String data = mapToJson(requestMap);

    JsonNode json = Json.parse(data);
    RequestBuilder req = new RequestBuilder().bodyJson(json).uri("/v1/note/search").method("POST");
    // req.headers(headerMap);
    Result result = Helpers.route(application, req);
    assertEquals(200, result.status());
  }

  private Map<String, Object> getCreateNoteRequest(
      String userId, String courseId, String contentId, String note, String title) {
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.USER_ID, userId);
    innerMap.put(JsonKey.COURSE_ID, courseId);
    innerMap.put(JsonKey.CONTENT_ID, contentId);
    innerMap.put(JsonKey.NOTE, note);
    innerMap.put(JsonKey.TITLE, title);
    return innerMap;
  }
}
