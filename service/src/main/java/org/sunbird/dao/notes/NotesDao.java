package org.sunbird.dao.notes;

import java.util.Map;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

public interface NotesDao {

  Response createNote(Map<String, Object> request, RequestContext context);

  Response updateNote(Map<String, Object> request, RequestContext context);

  Map<String, Object> searchNotes(Map<String, Object> searchQueryMap, RequestContext context);

  Map<String, Object> getNoteById(String noteId, RequestContext context);
}
