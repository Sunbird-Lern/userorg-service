package org.sunbird.dao.notes.impl;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import org.apache.pekko.dispatch.Futures;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.dao.notes.NotesDao;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  ElasticSearchRestHighImpl.class,
  EsClientFactory.class,
  ElasticSearchHelper.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class NotesDaoImplTest {

  @Before
  public void setUp() {
    PowerMockito.mockStatic(EsClientFactory.class);
    ElasticSearchRestHighImpl esSearch = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esSearch);
    Map<String, Object> esResponse = new HashMap<>();
    esResponse.put(JsonKey.CONTENT, new ArrayList<>());
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(esResponse);
    when(esSearch.search(Mockito.any(SearchDTO.class), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
    Promise<Boolean> booleanPromise = Futures.promise();
    booleanPromise.success(true);
    when(esSearch.update(Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(booleanPromise.future());
    when(esSearch.getDataByIdentifier(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
    PowerMockito.mockStatic(ServiceFactory.class);
    CassandraOperationImpl cassandraOperation = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getSuccessResponse());
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getSuccessResponse());
  }

  @Test
  public void createNote() {
    NotesDao notesDao = NotesDaoImpl.getInstance();

    Map<String, Object> note = new HashMap<>();
    note.put(JsonKey.ID, "noteId");
    note.put(JsonKey.USER_ID, "userId");
    note.put(JsonKey.CONTENT_ID, "contentId");
    note.put(JsonKey.COURSE_ID, "courseId");
    note.put(JsonKey.NOTE, "note");

    Response response = notesDao.createNote(note, new RequestContext());
    Assert.assertNotNull(response);
  }

  @Test
  public void updateNote() {
    NotesDao notesDao = NotesDaoImpl.getInstance();

    Map<String, Object> note = new HashMap<>();
    note.put(JsonKey.ID, "noteId");
    note.put(JsonKey.USER_ID, "userId");
    note.put(JsonKey.CONTENT_ID, "contentId");
    note.put(JsonKey.COURSE_ID, "courseId");
    note.put(JsonKey.NOTE, "note");

    Response response = notesDao.updateNote(note, new RequestContext());
    Assert.assertNotNull(response);
  }

  @Test
  public void searchNote() {
    NotesDao notesDao = NotesDaoImpl.getInstance();

    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.ID, "noteId");
    Map<String, Object> searchQueryMap = new HashMap<>();
    searchQueryMap.put(JsonKey.FILTERS, filters);
    searchQueryMap.put(JsonKey.REQUESTED_BY, "requestedBy");

    Map<String, Object> response = notesDao.searchNotes(searchQueryMap, new RequestContext());
    Assert.assertNotNull(response);
  }

  @Test
  public void getNoteById() {
    NotesDao notesDao = NotesDaoImpl.getInstance();
    Map<String, Object> response = notesDao.getNoteById("noteId", new RequestContext());
    Assert.assertNotNull(response);
  }

  private static Response getSuccessResponse() {
    Response response = new Response();
    response.put(JsonKey.ID, "noteId");
    return response;
  }
}
