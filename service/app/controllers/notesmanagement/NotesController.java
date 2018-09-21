package controllers.notesmanagement;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import controllers.notesmanagement.validator.NoteRequestValidator;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
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
    return handleRequest(
        ActorOperations.CREATE_NOTE.getValue(),
        request().body().asJson(),
        (request) -> {
          new NoteRequestValidator().validateNote((Request) request);
          return null;
        });
  }

  /**
   * Method to update the note
   *
   * @param noteId
   * @return Promise<Result>
   */
  public Promise<Result> updateNote(String noteId) {
    JsonNode requestData = request().body().asJson();
    return handleRequest(
        ActorOperations.UPDATE_NOTE.getValue(),
        requestData,
        (request) -> {
          new NoteRequestValidator().validateNoteId(noteId);
          return null;
        },
        noteId,
        JsonKey.NOTE_ID,
        true);
  }

  /**
   * Method to get the note details
   *
   * @param noteId
   * @return Promise<Result>
   */
  public Promise<Result> getNote(String noteId) {
    return handleRequest(
        ActorOperations.GET_NOTE.getValue(),
        (request) -> {
          new NoteRequestValidator().validateNoteId(noteId);
          return null;
        },
        noteId,
        JsonKey.NOTE_ID,
        false);
  }

  /**
   * Method to search note
   *
   * @return
   */
  public Promise<Result> searchNote() {
    return handleRequest(ActorOperations.SEARCH_NOTE.getValue(), request().body().asJson());
  }

  /**
   * Method to delete the note
   *
   * @param noteId
   * @return Promise<Result>
   */
  public Promise<Result> deleteNote(String noteId) {
    return handleRequest(
        ActorOperations.DELETE_NOTE.getValue(),
        (request) -> {
          new NoteRequestValidator().validateNoteId(noteId);
          return null;
        },
        noteId,
        JsonKey.NOTE_ID,
        false);
  }
}
