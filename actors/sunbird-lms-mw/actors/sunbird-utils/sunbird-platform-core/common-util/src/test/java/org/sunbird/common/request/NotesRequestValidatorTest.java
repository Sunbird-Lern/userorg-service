package org.sunbird.common.request;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;

/** Test class for notes request validation */
public class NotesRequestValidatorTest {

  /** Method to test create note when userId in request is empty */
  @Test
  public void testCreateNoteBlankUserId() {
    try {
      Request request = new Request();
      Map<String, Object> requestObj = new HashMap<>();
      requestObj.put(JsonKey.USER_ID, "");
      requestObj.put(JsonKey.TITLE, "test title");
      requestObj.put(JsonKey.NOTE, "This is a test Note");
      requestObj.put(JsonKey.COURSE_ID, "org.ekstep.test");
      requestObj.put(JsonKey.CONTENT_ID, "org.ekstep.test");
      request.setRequest(requestObj);
      RequestValidator.validateNote(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.userIdRequired.getErrorCode(), e.getCode());
    }
  }

  /** Method to test create note when title in request is empty */
  @Test
  public void testCreateNoteBlankTitle() {
    try {
      Request request = new Request();
      Map<String, Object> requestObj = new HashMap<>();
      requestObj.put(JsonKey.USER_ID, "testUser");
      requestObj.put(JsonKey.TITLE, "");
      requestObj.put(JsonKey.NOTE, "This is a test Note");
      requestObj.put(JsonKey.COURSE_ID, "org.ekstep.test");
      requestObj.put(JsonKey.CONTENT_ID, "org.ekstep.test");
      request.setRequest(requestObj);
      RequestValidator.validateNote(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.titleRequired.getErrorCode(), e.getCode());
    }
  }

  /** Method to test create note when note in request is empty */
  @Test
  public void testCreateNoteBlankNote() {
    try {
      Request request = new Request();
      Map<String, Object> requestObj = new HashMap<>();
      requestObj.put(JsonKey.USER_ID, "testUser");
      requestObj.put(JsonKey.TITLE, "test title");
      requestObj.put(JsonKey.NOTE, "");
      requestObj.put(JsonKey.COURSE_ID, "org.ekstep.test");
      requestObj.put(JsonKey.CONTENT_ID, "org.ekstep.test");
      request.setRequest(requestObj);
      RequestValidator.validateNote(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.noteRequired.getErrorCode(), e.getCode());
    }
  }

  /** Method to test create note without courseId and contentId in request */
  @Test
  public void testCreateNoteWithoutCourseAndContentId() {
    try {
      Request request = new Request();
      Map<String, Object> requestObj = new HashMap<>();
      requestObj.put(JsonKey.USER_ID, "testUser");
      requestObj.put(JsonKey.TITLE, "test title");
      requestObj.put(JsonKey.NOTE, "This is a test Note");
      requestObj.put(JsonKey.COURSE_ID, "");
      requestObj.put(JsonKey.CONTENT_ID, "");
      request.setRequest(requestObj);
      RequestValidator.validateNote(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.contentIdError.getErrorCode(), e.getCode());
    }
  }

  /** Method to test create note when tags in request is string */
  @Test
  public void testCreateNoteWithTagsAsString() {
    try {
      Request request = new Request();
      Map<String, Object> requestObj = new HashMap<>();
      requestObj.put(JsonKey.USER_ID, "testUser");
      requestObj.put(JsonKey.TITLE, "test title");
      requestObj.put(JsonKey.NOTE, "This is a test Note");
      requestObj.put(JsonKey.COURSE_ID, "org.ekstep.test");
      requestObj.put(JsonKey.TAGS, "test tag");
      request.setRequest(requestObj);
      RequestValidator.validateNote(request);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.invalidTags.getErrorCode(), e.getCode());
    }
  }

  /** Method to test validate node id when note id is empty */
  @Test
  public void testValidateNoteOperationWithOutNoteId() {
    try {
      String noteId = "";
      RequestValidator.validateNoteId(noteId);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.invalidNoteId.getErrorCode(), e.getCode());
    }
  }

  /** Method to test validate node id when note id is null */
  @Test
  public void testValidateNoteOperationWithNoteIdAsNull() {
    try {
      String noteId = null;
      RequestValidator.validateNoteId(noteId);
    } catch (ProjectCommonException e) {
      assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), e.getResponseCode());
      assertEquals(ResponseCode.invalidNoteId.getErrorCode(), e.getCode());
    }
  }
}
