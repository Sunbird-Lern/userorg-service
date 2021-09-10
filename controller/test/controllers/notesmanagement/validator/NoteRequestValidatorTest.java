package controllers.notesmanagement.validator;

import controllers.BaseApplicationTest;
import java.util.*;
import modules.OnRequestHandler;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.HeaderParam;
import org.sunbird.request.Request;

@PrepareForTest(OnRequestHandler.class)
@PowerMockIgnore({"javax.management.*", "jdk.internal.reflect.*", "javax.crypto.*"})
public class NoteRequestValidatorTest extends BaseApplicationTest {

  public static Map<String, List<String>> headerMap;
  private NoteRequestValidator noteRequestValidator;
  private Request request;

  @Before
  public void before() {
    request = new Request();
    headerMap = new HashMap<>();
    headerMap.put(HeaderParam.X_Consumer_ID.getName(), Arrays.asList("Some consumer ID"));
    headerMap.put(HeaderParam.X_Device_ID.getName(), Arrays.asList("Some device ID"));
    headerMap.put(
        HeaderParam.X_Authenticated_Userid.getName(), Arrays.asList("Some authenticated user ID"));
    headerMap.put(JsonKey.MESSAGE_ID, Arrays.asList("Some message ID"));
    headerMap.put(HeaderParam.X_APP_ID.getName(), Arrays.asList("Some app Id"));
    noteRequestValidator = new NoteRequestValidator();
  }

  @Test(expected = ProjectCommonException.class)
  public void testValidateNoteIdFailure() {
    noteRequestValidator.validateNoteId("");
  }

  @Test()
  public void testValidateNoteIdSuccess() {
    noteRequestValidator.validateNoteId("notId");
  }

  @Test(expected = ProjectCommonException.class)
  public void testValidateNoteReqWhenUserIdAbsent() {
    Map<String, Object> reqmap = new HashMap<>();
    request.setRequest(reqmap);
    noteRequestValidator.validateNote(request);
  }

  @Test(expected = ProjectCommonException.class)
  public void testValidateNoteReqWhenTitleAbsent() {
    Map<String, Object> reqmap = new HashMap<>();
    reqmap.put(JsonKey.USER_ID, "userId");
    request.setRequest(reqmap);
    noteRequestValidator.validateNote(request);
  }

  @Test(expected = ProjectCommonException.class)
  public void testValidateNoteReqWhenNoteAbsent() {
    Map<String, Object> reqmap = new HashMap<>();
    reqmap.put(JsonKey.USER_ID, "userId");
    reqmap.put(JsonKey.TITLE, "title");
    request.setRequest(reqmap);
    noteRequestValidator.validateNote(request);
  }

  @Test(expected = ProjectCommonException.class)
  public void testValidateNoteReqWhenContentIdAbsent() {
    Map<String, Object> reqmap = new HashMap<>();
    reqmap.put(JsonKey.USER_ID, "userId");
    reqmap.put(JsonKey.TITLE, "title");
    reqmap.put(JsonKey.NOTE, "note");
    request.setRequest(reqmap);
    noteRequestValidator.validateNote(request);
  }

  @Test(expected = ProjectCommonException.class)
  public void testValidateNoteReqWhenTagsAbsent() {
    Map<String, Object> reqmap = new HashMap<>();
    reqmap.put(JsonKey.USER_ID, "userId");
    reqmap.put(JsonKey.TITLE, "title");
    reqmap.put(JsonKey.NOTE, "note");
    reqmap.put(JsonKey.CONTENT_ID, "contentId");
    reqmap.put(JsonKey.TAGS, new ArrayList<>());
    request.setRequest(reqmap);
    noteRequestValidator.validateNote(request);
  }
}
