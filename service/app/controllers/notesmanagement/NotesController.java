package controllers.notesmanagement;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import controllers.notesmanagement.validator.NoteValidator;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;
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
      Request request = createAndInitRequest(ActorOperations.CREATE_NOTE.getValue(), requestData);
      new NoteValidator().validateNote(request);
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
      NoteValidator validator = new NoteValidator();
      validator.validateNoteId(noteId);
      Request request =
          createAndInitRequest(
              ActorOperations.UPDATE_NOTE.getValue(), requestData, noteId, JsonKey.NOTE_ID);
      validator.validateNote(request);
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
      new NoteValidator().validateNoteId(noteId);
      Request request =
          createAndInitRequest(ActorOperations.GET_NOTE.getValue(), null, noteId, JsonKey.NOTE_ID);
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
      new NoteValidator().validateNoteId(noteId);
      Request reqObj =
          createAndInitRequest(
              ActorOperations.DELETE_NOTE.getValue(), null, noteId, JsonKey.NOTE_ID);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      ProjectLogger.log("Error in controller", e);
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }
}
