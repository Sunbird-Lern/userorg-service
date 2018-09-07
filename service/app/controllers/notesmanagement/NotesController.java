package controllers.notesmanagement;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import controllers.notesmanagement.validator.NoteValidator;
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
    return handlePostRequest(
        ActorOperations.CREATE_NOTE.getValue(),
        request().body().asJson(),
        (request) -> {
          new NoteValidator().validateNote((Request) request);
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
          new NoteValidator().validateNoteId(noteId);
          return null;
        },
        noteId,
        JsonKey.NOTE_ID);
  }

  /**
   * Method to get the note details
   *
   * @param noteId
   * @return Promise<Result>
   */
  public Promise<Result> getNote(String noteId) {
    //    try {
    return handleRequest(
        ActorOperations.GET_NOTE.getValue(),
        null,
        (request) -> {
          new NoteValidator().validateNoteId(noteId);
          return null;
        },
        noteId,
        JsonKey.NOTE_ID);
  }

  /**
   * Method to search note
   *
   * @return
   */
  public Promise<Result> searchNote() {
    return handlePostRequest(
        ActorOperations.SEARCH_NOTE.getValue(), request().body().asJson(), null);
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
        null,
        (request) -> {
          new NoteValidator().validateNoteId(noteId);
          return null;
        },
        noteId,
        JsonKey.NOTE_ID);
  }
}
