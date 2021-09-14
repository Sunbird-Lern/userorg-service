package org.sunbird.actor.notes;

import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.notes.NotesService;
import org.sunbird.telemetry.dto.TelemetryEnvKey;
import org.sunbird.telemetry.util.TelemetryUtil;
import org.sunbird.util.Util;

public class NotesManagementActor extends BaseActor {

  private NotesService notesService = new NotesService();

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    switch (request.getOperation()) {
      case "createNote":
        createNote(request);
        break;
      case "updateNote":
        updateNote(request);
        break;
      case "searchNote":
        searchNote(request);
        break;
      case "getNote":
        getNote(request);
        break;
      case "deleteNote":
        deleteNote(request);
        break;
      default:
        onReceiveUnsupportedOperation();
    }
  }

  private void createNote(Request actorMessage) {
    RequestContext context = actorMessage.getRequestContext();
    logger.debug(context, "Create Note method call start");
    // object of telemetry event...
    Map<String, Object> targetObject;
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    try {
      Map<String, Object> req = actorMessage.getRequest();
      if (!notesService.validUser((String) req.get(JsonKey.USER_ID), context)) {
        ProjectCommonException.throwClientErrorException(ResponseCode.invalidUserId);
      }
      Response response = notesService.createNote(actorMessage);
      sender().tell(response, self());
      String noteId = (String) response.getResult().get(JsonKey.ID);
      targetObject = TelemetryUtil.generateTargetObject(noteId, JsonKey.NOTE, JsonKey.CREATE, null);
      TelemetryUtil.generateCorrelatedObject(noteId, JsonKey.NOTE, null, correlatedObject);
      TelemetryUtil.generateCorrelatedObject(
          (String) req.get(JsonKey.REQUESTED_BY), JsonKey.USER, null, correlatedObject);

      Map<String, String> rollup =
          prepareRollUpForObjectType(
              (String) req.get(JsonKey.CONTENT_ID), (String) req.get(JsonKey.COURSE_ID));
      TelemetryUtil.addTargetObjectRollUp(rollup, targetObject);

      TelemetryUtil.telemetryProcessingCall(
          actorMessage.getRequest(), targetObject, correlatedObject, actorMessage.getContext());
    } catch (Exception e) {
      logger.error(context, "Exception while creating notes", e);
      sender().tell(e, self());
    }
  }

  private void updateNote(Request actorMessage) {
    RequestContext context = actorMessage.getRequestContext();
    logger.debug(context, "Update Note method call start");
    // object of telemetry event...
    Map<String, Object> targetObject;
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    try {
      String noteId = (String) actorMessage.getContext().get(JsonKey.NOTE_ID);
      String userId = (String) actorMessage.getContext().get(JsonKey.REQUESTED_BY);
      if (!notesService.validateUserForNoteUpdate(userId, noteId, context)) {
        ProjectCommonException.throwUnauthorizedErrorException();
      }
      Map<String, Object> list = notesService.getNoteById(noteId, context);
      if (list.isEmpty()) {
        ProjectCommonException.throwClientErrorException(ResponseCode.invalidNoteId);
      }
      Response response = notesService.updateNote(actorMessage);
      sender().tell(response, self());

      targetObject = TelemetryUtil.generateTargetObject(noteId, JsonKey.NOTE, JsonKey.UPDATE, null);
      TelemetryUtil.generateCorrelatedObject(noteId, JsonKey.NOTE, null, correlatedObject);
      TelemetryUtil.generateCorrelatedObject(userId, JsonKey.USER, null, correlatedObject);
      Map<String, String> rollup = new HashMap<>();
      TelemetryUtil.addTargetObjectRollUp(rollup, targetObject);

      TelemetryUtil.telemetryProcessingCall(
          actorMessage.getRequest(), targetObject, correlatedObject, actorMessage.getContext());
    } catch (Exception e) {
      logger.error(context, "Exception while updating note", e);
      sender().tell(e, self());
    }
  }

  private void getNote(Request actorMessage) {
    RequestContext context = actorMessage.getRequestContext();
    logger.debug(context, "Get Note method call start");
    try {
      String noteId = (String) actorMessage.getContext().get(JsonKey.NOTE_ID);
      String userId = (String) actorMessage.getContext().get(JsonKey.REQUESTED_BY);
      if (!notesService.validateUserForNoteUpdate(userId, noteId, context)) {
        throw new ProjectCommonException(
            ResponseCode.invalidParameterValue.getErrorCode(),
            ResponseCode.invalidParameterValue.getErrorMessage(),
            ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
      }
      Map<String, Object> request = new HashMap<>();
      Map<String, Object> filters = new HashMap<>();
      filters.put(JsonKey.ID, noteId);

      request.put(JsonKey.FILTERS, filters);
      Response response = new Response();
      Map<String, Object> result = notesService.searchNotes(request, context);
      if (!result.isEmpty() && ((Long) result.get(JsonKey.COUNT) == 0)) {
        ProjectCommonException.throwClientErrorException(ResponseCode.invalidNoteId);
      }
      response.put(JsonKey.RESPONSE, result);
      sender().tell(response, self());
    } catch (Exception e) {
      logger.error(context, "Exception while fetching note", e);
      sender().tell(e, self());
    }
  }

  private void searchNote(Request actorMessage) {
    RequestContext context = actorMessage.getRequestContext();
    logger.debug(context, "Search Note method call start");
    try {
      Map<String, Object> searchQueryMap = actorMessage.getRequest();
      searchQueryMap.put(JsonKey.REQUESTED_BY, actorMessage.getContext().get(JsonKey.REQUESTED_BY));
      Response response = new Response();
      Map<String, Object> result = notesService.searchNotes(searchQueryMap, context);
      response.put(JsonKey.RESPONSE, result);
      sender().tell(response, self());
    } catch (Exception e) {
      logger.error(context, "Exception while search", e);
      sender().tell(e, self());
    }
  }

  private void deleteNote(Request actorMessage) {
    RequestContext context = actorMessage.getRequestContext();
    logger.debug(context, "Delete Note method call start");
    // object of telemetry event...
    Map<String, Object> targetObject;
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    try {
      String noteId = (String) actorMessage.getContext().get(JsonKey.NOTE_ID);
      String userId = (String) actorMessage.getContext().get(JsonKey.REQUESTED_BY);
      if (!notesService.validateUserForNoteUpdate(userId, noteId, context)) {
        ProjectCommonException.throwUnauthorizedErrorException();
      }
      if (!notesService.noteIdExists(noteId, context)) {
        ProjectCommonException.throwClientErrorException(ResponseCode.invalidNoteId);
      }
      Response result = notesService.deleteNote(noteId, userId, context);
      result.getResult().remove(JsonKey.RESPONSE);
      sender().tell(result, self());

      targetObject = TelemetryUtil.generateTargetObject(noteId, JsonKey.NOTE, JsonKey.DELETE, null);
      TelemetryUtil.generateCorrelatedObject(noteId, JsonKey.NOTE, null, correlatedObject);
      TelemetryUtil.generateCorrelatedObject(userId, JsonKey.USER, null, correlatedObject);

      TelemetryUtil.telemetryProcessingCall(
          actorMessage.getRequest(), targetObject, correlatedObject, actorMessage.getContext());
    } catch (Exception e) {
      logger.error(context, "Exception while deleting note", e);
      sender().tell(e, self());
    }
  }

  /** This method will handle rollup values (for contentId and courseId) in object */
  public static Map<String, String> prepareRollUpForObjectType(String contentId, String courseId) {

    Map<String, String> rollupMap = new HashMap<>();

    if (StringUtils.isBlank(courseId)) { // if courseId is blank the level 1 should be contentId
      if (StringUtils.isNotBlank(contentId)) {
        rollupMap.put("l1", contentId);
      }
    } else {
      rollupMap.put("l1", courseId); // if courseId is not blank level 1 should be courseId
      if (StringUtils.isNotBlank(contentId)) {
        rollupMap.put("l2", contentId);
      }
    }
    return rollupMap;
  }
}
