package org.sunbird.service.notes;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.dao.notes.NotesDao;
import org.sunbird.dao.notes.impl.NotesDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.common.ProjectUtil;

public class NotesService {

  private final NotesDao notesDao = NotesDaoImpl.getInstance();
  private final UserService userService = UserServiceImpl.getInstance();

  public Response createNote(Request request) {
    Map<String, Object> req = request.getRequest();
    String uniqueId = ProjectUtil.getUniqueIdFromTimestamp(request.getEnv());
    req.put(JsonKey.ID, uniqueId);
    req.put(JsonKey.CREATED_DATE, ProjectUtil.getFormattedDate());
    req.put(JsonKey.UPDATED_DATE, ProjectUtil.getFormattedDate());
    String updatedBy = (String) request.getRequest().get(JsonKey.REQUESTED_BY);
    if (StringUtils.isNotBlank(updatedBy)) {
      req.put(JsonKey.CREATED_BY, updatedBy);
      req.put(JsonKey.UPDATED_BY, updatedBy);
    }
    req.put(JsonKey.IS_DELETED, false);
    Response result = notesDao.createNote(req, request.getRequestContext());
    result.getResult().put(JsonKey.ID, uniqueId);
    result.getResult().remove(JsonKey.RESPONSE);
    return result;
  }

  public Response updateNote(Request request) {
    String noteId = (String) request.getContext().get(JsonKey.NOTE_ID);
    String userId = (String) request.getContext().get(JsonKey.REQUESTED_BY);
    Map<String, Object> req = request.getRequest();
    req.remove(JsonKey.USER_ID);
    req.remove(JsonKey.COURSE_ID);
    req.remove(JsonKey.CONTENT_ID);
    req.remove(JsonKey.IS_DELETED);
    req.remove(JsonKey.NOTE_ID);
    req.put(JsonKey.ID, noteId);
    req.put(JsonKey.UPDATED_BY, userId);
    req.put(JsonKey.UPDATED_DATE, ProjectUtil.getFormattedDate());
    Response result = notesDao.updateNote(req, request.getRequestContext());
    result.getResult().put(JsonKey.ID, noteId);
    result.getResult().remove(JsonKey.RESPONSE);
    return result;
  }

  public Map<String, Object> searchNotes(Map<String, Object> searchQuery, RequestContext context) {
    return notesDao.searchNotes(searchQuery, context);
  }

  public Response deleteNote(String noteId, String userId, RequestContext context) {
    Map<String, Object> req = new HashMap<>();
    req.put(JsonKey.ID, noteId);
    req.put(JsonKey.IS_DELETED, true);
    req.put(JsonKey.UPDATED_BY, userId);
    req.put(JsonKey.UPDATED_DATE, ProjectUtil.getFormattedDate());
    Response result = notesDao.updateNote(req, context);
    result.getResult().remove(JsonKey.RESPONSE);
    return result;
  }

  public Map<String, Object> getNoteById(String noteId, RequestContext context) {
    return notesDao.getNoteById(noteId, context);
  }

  public boolean validUser(String userId, RequestContext context) {
    if (!StringUtils.isBlank(userId)) {
      try {
        userService.getUserById(userId, context);
      } catch (Exception ex) {
        return false;
      }
      return true;
    }
    return false;
  }

  public Boolean noteIdExists(String noteId, RequestContext context) {
    Boolean result = false;
    Map<String, Object> list = getNoteById(noteId, context);
    if (!list.isEmpty()) {
      result = true;
    }
    return result;
  }

  public boolean validateUserForNoteUpdate(String userId, String noteId, RequestContext context) {
    Map<String, Object> noteData = getNoteById(noteId, context);
    if (MapUtils.isEmpty(noteData)) return false;
    if (!StringUtils.isBlank(userId)) {
      return true;
    }
    if (!userId.equalsIgnoreCase((String) noteData.get(JsonKey.USER_ID))) {
      throw new ProjectCommonException(
          ResponseCode.errorForbidden,
          ResponseCode.errorForbidden.getErrorMessage(),
          ResponseCode.FORBIDDEN.getResponseCode());
    }
    return false;
  }
}
