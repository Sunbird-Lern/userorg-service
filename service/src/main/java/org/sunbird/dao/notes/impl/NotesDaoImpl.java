package org.sunbird.dao.notes.impl;

import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.dao.notes.NotesDao;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.common.ProjectUtil;
import scala.concurrent.Future;

public class NotesDaoImpl implements NotesDao {
  public final LoggerUtil logger = new LoggerUtil(NotesDaoImpl.class);
  private final CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private final ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);

  private static final String KEYSPACE_NAME = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE);
  private static final String TABLE_NAME = "user_notes";

  private static NotesDao notesDao;

  public static NotesDao getInstance() {
    if (notesDao == null) {
      notesDao = new NotesDaoImpl();
    }
    return notesDao;
  }

  @Override
  public Response createNote(Map<String, Object> request, RequestContext context) {
    Response response =
        cassandraOperation.insertRecord(KEYSPACE_NAME, TABLE_NAME, request, context);
    String id = (String) request.get(JsonKey.ID);
    insertDataToElastic(id, request, context);
    return response;
  }

  @Override
  public Response updateNote(Map<String, Object> request, RequestContext context) {
    Response response =
        cassandraOperation.updateRecord(KEYSPACE_NAME, TABLE_NAME, request, context);
    String id = (String) request.get(JsonKey.ID);
    updateDataToElastic(id, request, context);
    return response;
  }

  @Override
  public Map<String, Object> searchNotes(
      Map<String, Object> searchQueryMap, RequestContext context) {
    Map<String, Object> filters = new HashMap<>();
    if (searchQueryMap.containsKey(JsonKey.FILTERS)) {
      filters = (Map<String, Object>) searchQueryMap.get(JsonKey.FILTERS);
    }
    if (null != searchQueryMap.get(JsonKey.REQUESTED_BY)) {
      filters.put(JsonKey.USER_ID, searchQueryMap.get(JsonKey.REQUESTED_BY));
    }
    filters.put(JsonKey.IS_DELETED, false);
    searchQueryMap.put(JsonKey.FILTERS, filters);
    SearchDTO searchDto = ElasticSearchHelper.createSearchDTO(searchQueryMap);
    List<String> excludedFields = new ArrayList<>();
    if (null != searchDto.getExcludedFields()) {
      excludedFields = searchDto.getExcludedFields();
    }
    excludedFields.add(JsonKey.IS_DELETED);
    searchDto.setExcludedFields(excludedFields);
    Future<Map<String, Object>> resultF =
        esService.search(searchDto, ProjectUtil.EsType.usernotes.getTypeName(), context);
    Map<String, Object> result =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
    if (result != null) {
      Object count = result.get(JsonKey.COUNT);
      Object note = result.get(JsonKey.CONTENT);
      result = new LinkedHashMap<>();
      result.put(JsonKey.COUNT, count);
      result.put(JsonKey.NOTE, note);
      result.put(JsonKey.CONTENT, note);
    } else {
      result = new HashMap<>();
    }
    return result;
  }

  @Override
  public Map<String, Object> getNoteById(String noteId, RequestContext context) {
    Future<Map<String, Object>> resultF =
        esService.getDataByIdentifier(ProjectUtil.EsType.usernotes.getTypeName(), noteId, context);
    return (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
  }

  private boolean insertDataToElastic(
      String identifier, Map<String, Object> data, RequestContext context) {
    String type = ProjectUtil.EsType.usernotes.getTypeName();
    Future<String> responseF = esService.save(type, identifier, data, context);
    String response = (String) ElasticSearchHelper.getResponseFromFuture(responseF);
    if (StringUtils.isNotBlank(response)) {
      return true;
    }
    logger.debug(context, "unable to save the data inside ES with identifier " + identifier);
    return false;
  }

  private boolean updateDataToElastic(
      String identifier, Map<String, Object> data, RequestContext context) {
    String type = ProjectUtil.EsType.usernotes.getTypeName();
    Future<Boolean> responseF = esService.update(type, identifier, data, context);
    boolean response = (boolean) ElasticSearchHelper.getResponseFromFuture(responseF);
    if (response) {
      return true;
    }
    logger.debug(context, "unable to save the data to ES with identifier " + identifier);
    return false;
  }
}
