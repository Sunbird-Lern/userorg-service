package controllers.notesmanagement;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import controllers.notesmanagement.validator.NoteValidator;
import java.util.HashMap;
import java.util.Map;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestValidator;
import play.libs.F.Promise;
import play.mvc.Result;

/**
 * Controller class to handle Notes related operation such as create, read/get, search, update and
 * delete
 */
public class NotesController extends BaseController {

  /**
   * Method to create Note
   *
   * @return Promise<Result>
   */
  public Promise<Result> createNote() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("Create note request: " + requestData, LoggerEnum.INFO.name());
      //      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData,
      // Request.class);
      Request request = createAndInitRequest(ActorOperations.CREATE_NOTE.getValue(), requestData);
      NoteValidator validator = new NoteValidator();
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.NOTE, request.getRequest());
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      request.setRequest(innerMap);
      validator.validateNote(request);
      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      ProjectLogger.log("Error in controller", e);
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * Method to update the note
   *
   * @param noteId
   * @return Promise<Result>
   */
  public Promise<Result> updateNote(String noteId) {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("Update note request: " + requestData, LoggerEnum.INFO.name());
      Request request = createAndInitRequest(ActorOperations.UPDATE_NOTE.getValue(), requestData);
      NoteValidator validator = new NoteValidator();
      validator.validateNoteId(noteId);
      validator.validateNote(request);
      Map<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.NOTE, request.getRequest());
      innerMap.put(JsonKey.NOTE_ID, noteId);
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));

      request.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      ProjectLogger.log("Error in controller", e);
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * Method to get the note details
   *
   * @param noteId
   * @return Promise<Result>
   */
  public Promise<Result> getNote(String noteId) {
    try {
      ProjectLogger.log("Get Note request: " + noteId, LoggerEnum.INFO.name());
      RequestValidator.validateNoteId(noteId);
      Request request = createAndInitRequest(ActorOperations.GET_NOTE.getValue());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      innerMap.put(JsonKey.NOTE_ID, noteId);
      request.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      ProjectLogger.log("Error in controller", e);
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * Method to search note
   *
   * @return
   */
  public Promise<Result> searchNote() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("Search Note request: " + requestData, LoggerEnum.INFO.name());
      Request reqObj = createAndInitRequest(ActorOperations.SEARCH_NOTE.getValue());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      innerMap.putAll(reqObj.getRequest());
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      ProjectLogger.log("Error in controller", e);
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * Method to delete the note
   *
   * @param noteId
   * @return Promise<Result>
   */
  public Promise<Result> deleteNote(String noteId) {
    try {
      ProjectLogger.log("Delete Note request: " + noteId, LoggerEnum.INFO.name());
      Request reqObj = createAndInitRequest(ActorOperations.DELETE_NOTE.getValue());
      NoteValidator validator = new NoteValidator();
      validator.validateNoteId(noteId);
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.NOTE_ID, noteId);
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      ProjectLogger.log("Error in controller", e);
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }
}
