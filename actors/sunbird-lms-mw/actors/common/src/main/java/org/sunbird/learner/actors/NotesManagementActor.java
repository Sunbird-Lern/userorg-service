package org.sunbird.learner.actors;

import java.util.*;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.operations.ActorOperations;
import org.sunbird.dto.SearchDTO;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.learner.util.Util;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.telemetry.dto.TelemetryEnvKey;
import org.sunbird.telemetry.util.TelemetryUtil;
import org.sunbird.util.ProjectUtil;
import scala.concurrent.Future;

/** This class provides API's to create, update, get and delete user note */
@ActorConfig(
  tasks = {"createNote", "getNote", "searchNote", "updateNote", "deleteNote"},
  asyncTasks = {}
)
public class NotesManagementActor extends BaseActor {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);

  /** Receives the actor message and perform the operation for user note */
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
        onReceiveUnsupportedOperation(request.getOperation());
    }
  }

  /**
   * Method to create note using userId, courseId or contentId, title and note along with tags which
   * are optional
   *
   * @param actorMessage
   */
  @SuppressWarnings("unchecked")
  private void createNote(Request actorMessage) {
    RequestContext context = actorMessage.getRequestContext();
    logger.info(context, "Create Note method call start");
    // object of telemetry event...
    Map<String, Object> targetObject = new HashMap<>();
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    try {
      Map<String, Object> req = actorMessage.getRequest();
      if (!validUser((String) req.get(JsonKey.USER_ID), context)) {
        ProjectCommonException exception =
            new ProjectCommonException(
                ResponseCode.invalidUserId.getErrorCode(),
                ResponseCode.invalidUserId.getErrorMessage(),
                ResponseCode.CLIENT_ERROR.getResponseCode());
        sender().tell(exception, self());
        return;
      }
      String uniqueId = ProjectUtil.getUniqueIdFromTimestamp(actorMessage.getEnv());
      req.put(JsonKey.ID, uniqueId);
      req.put(JsonKey.CREATED_DATE, ProjectUtil.getFormattedDate());
      req.put(JsonKey.UPDATED_DATE, ProjectUtil.getFormattedDate());
      String updatedBy = (String) actorMessage.getRequest().get(JsonKey.REQUESTED_BY);
      if (!(StringUtils.isBlank(updatedBy))) {
        req.put(JsonKey.CREATED_BY, updatedBy);
        req.put(JsonKey.UPDATED_BY, updatedBy);
      }
      req.put(JsonKey.IS_DELETED, false);
      Util.DbInfo userNotesDbInfo = Util.dbInfoMap.get(JsonKey.USER_NOTES_DB);
      Response result =
          cassandraOperation.insertRecord(
              userNotesDbInfo.getKeySpace(), userNotesDbInfo.getTableName(), req, context);
      result.getResult().put(JsonKey.ID, uniqueId);
      result.getResult().remove(JsonKey.RESPONSE);
      sender().tell(result, self());

      targetObject =
          TelemetryUtil.generateTargetObject(uniqueId, JsonKey.NOTE, JsonKey.CREATE, null);
      TelemetryUtil.generateCorrelatedObject(uniqueId, JsonKey.NOTE, null, correlatedObject);
      TelemetryUtil.generateCorrelatedObject(updatedBy, JsonKey.USER, null, correlatedObject);

      Map<String, String> rollup =
          prepareRollUpForObjectType(
              (String) req.get(JsonKey.CONTENT_ID), (String) req.get(JsonKey.COURSE_ID));
      TelemetryUtil.addTargetObjectRollUp(rollup, targetObject);

      TelemetryUtil.telemetryProcessingCall(
          actorMessage.getRequest(), targetObject, correlatedObject, actorMessage.getContext());

      Request request = new Request();
      request.setRequestContext(context);
      request.setOperation(ActorOperations.INSERT_USER_NOTES_ES.getValue());
      request.getRequest().put(JsonKey.NOTE, req);
      logger.info(context, "Calling background job to save org data into ES" + uniqueId);
      tellToAnother(request);
    } catch (Exception e) {
      logger.error(context, "Error occurred", e);
      sender().tell(e, self());
      return;
    }
  }

  /**
   * Method to update the tags, title and note details of the user note
   *
   * @param actorMessage containing noteId and requestedBy
   */
  @SuppressWarnings("unchecked")
  private void updateNote(Request actorMessage) {
    RequestContext context = actorMessage.getRequestContext();
    logger.info(context, "Update Note method call start");
    // object of telemetry event...
    Map<String, Object> targetObject = new HashMap<>();
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    try {
      String noteId = (String) actorMessage.getContext().get(JsonKey.NOTE_ID);
      String userId = (String) actorMessage.getContext().get(JsonKey.REQUESTED_BY);
      if (!validateUserForNoteUpdation(userId, noteId, context)) {
        throw new ProjectCommonException(
            ResponseCode.unAuthorized.getErrorCode(),
            ResponseCode.unAuthorized.getErrorMessage(),
            ResponseCode.UNAUTHORIZED.getResponseCode());
      }
      Map<String, Object> list = getNoteRecordById(noteId, context);
      if (list.isEmpty()) {
        ProjectCommonException exception =
            new ProjectCommonException(
                ResponseCode.invalidNoteId.getErrorCode(),
                ResponseCode.invalidNoteId.getErrorMessage(),
                ResponseCode.CLIENT_ERROR.getResponseCode());
        sender().tell(exception, self());
        return;
      }
      Map<String, Object> req = actorMessage.getRequest();
      req.remove(JsonKey.USER_ID);
      req.remove(JsonKey.COURSE_ID);
      req.remove(JsonKey.CONTENT_ID);
      req.remove(JsonKey.IS_DELETED);
      req.remove(JsonKey.NOTE_ID);
      req.put(JsonKey.ID, noteId);
      req.put(JsonKey.UPDATED_BY, userId);
      req.put(JsonKey.UPDATED_DATE, ProjectUtil.getFormattedDate());
      Util.DbInfo userNotesDbInfo = Util.dbInfoMap.get(JsonKey.USER_NOTES_DB);
      Response result =
          cassandraOperation.updateRecord(
              userNotesDbInfo.getKeySpace(), userNotesDbInfo.getTableName(), req, context);
      logger.info("Note data updated");
      result.getResult().put(JsonKey.ID, noteId);
      result.getResult().remove(JsonKey.RESPONSE);
      sender().tell(result, self());

      targetObject = TelemetryUtil.generateTargetObject(noteId, JsonKey.NOTE, JsonKey.UPDATE, null);
      TelemetryUtil.generateCorrelatedObject(noteId, JsonKey.NOTE, null, correlatedObject);
      TelemetryUtil.generateCorrelatedObject(userId, JsonKey.USER, null, correlatedObject);
      Map<String, String> rollup = new HashMap<>();
      TelemetryUtil.addTargetObjectRollUp(rollup, targetObject);

      TelemetryUtil.telemetryProcessingCall(
          actorMessage.getRequest(), targetObject, correlatedObject, actorMessage.getContext());

      Request request = new Request();
      request.setRequestContext(context);
      request.getRequest().put(JsonKey.NOTE, req);
      request.setOperation(ActorOperations.UPDATE_USER_NOTES_ES.getValue());
      tellToAnother(request);

    } catch (Exception e) {
      logger.error(context, "Error occurred", e);
      sender().tell(e, self());
      return;
    }
  }

  /**
   * Method to get the note for the given note Id of the user
   *
   * @param actorMessage containing noteId and requestedBy
   */
  private void getNote(Request actorMessage) {
    RequestContext context = actorMessage.getRequestContext();
    logger.info(context, "Update Note method call start");
    try {
      String noteId = (String) actorMessage.getContext().get(JsonKey.NOTE_ID);
      String userId = (String) actorMessage.getContext().get(JsonKey.REQUESTED_BY);
      if (!validateUserForNoteUpdation(userId, noteId, context)) {
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
      Map<String, Object> result = getElasticSearchData(request, context);
      if (!result.isEmpty() && ((Long) result.get(JsonKey.COUNT) == 0)) {
        ProjectCommonException exception =
            new ProjectCommonException(
                ResponseCode.invalidNoteId.getErrorCode(),
                ResponseCode.invalidNoteId.getErrorMessage(),
                ResponseCode.CLIENT_ERROR.getResponseCode());
        sender().tell(exception, self());
        return;
      }
      response.put(JsonKey.RESPONSE, result);
      sender().tell(response, self());
    } catch (Exception e) {
      logger.error(context, "Error occurred", e);
      sender().tell(e, self());
      return;
    }
  }

  /**
   * Method to search the note for the given request
   *
   * @param actorMessage containing search parameters
   */
  private void searchNote(Request actorMessage) {
    RequestContext context = actorMessage.getRequestContext();
    logger.info(context, "Update Note method call start");
    try {

      Map<String, Object> searchQueryMap = actorMessage.getRequest();
      searchQueryMap.put(JsonKey.REQUESTED_BY, actorMessage.getContext().get(JsonKey.REQUESTED_BY));
      Response response = new Response();
      Map<String, Object> result = getElasticSearchData(searchQueryMap, context);
      response.put(JsonKey.RESPONSE, result);
      sender().tell(response, self());
    } catch (Exception e) {
      logger.error(context, "Error occurred", e);
      sender().tell(e, self());
      return;
    }
  }

  /**
   * Method to get note data from ElasticSearch
   *
   * @param searchQueryMap
   * @return Map<String, Object> - note data
   */
  @SuppressWarnings("unchecked")
  private Map<String, Object> getElasticSearchData(
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
    SearchDTO searchDto = Util.createSearchDto(searchQueryMap);
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
    String[] types = {ProjectUtil.EsType.usernotes.getTypeName()};
    return result;
  }

  /**
   * Method to mark the note as deleted [Soft Delete i.e to set the isDeleted field to true]
   *
   * @param actorMessage
   */
  private void deleteNote(Request actorMessage) {
    RequestContext context = actorMessage.getRequestContext();
    logger.info(context, "Delete Note method call start");
    // object of telemetry event...
    Map<String, Object> targetObject = new HashMap<>();
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    try {
      String noteId = (String) actorMessage.getContext().get(JsonKey.NOTE_ID);
      String userId = (String) actorMessage.getContext().get(JsonKey.REQUESTED_BY);
      if (!validateUserForNoteUpdation(userId, noteId, context)) {
        throw new ProjectCommonException(
            ResponseCode.unAuthorized.getErrorCode(),
            ResponseCode.unAuthorized.getErrorMessage(),
            ResponseCode.UNAUTHORIZED.getResponseCode());
      }
      if (!noteIdExists(noteId, context)) {
        ProjectCommonException exception =
            new ProjectCommonException(
                ResponseCode.invalidNoteId.getErrorCode(),
                ResponseCode.invalidNoteId.getErrorMessage(),
                ResponseCode.CLIENT_ERROR.getResponseCode());
        sender().tell(exception, self());
        return;
      }
      Map<String, Object> req = new HashMap<>();
      req.put(JsonKey.ID, noteId);
      req.put(JsonKey.IS_DELETED, true);
      req.put(JsonKey.UPDATED_BY, userId);
      req.put(JsonKey.UPDATED_DATE, ProjectUtil.getFormattedDate());
      Util.DbInfo userNotesDbInfo = Util.dbInfoMap.get(JsonKey.USER_NOTES_DB);
      Response result =
          cassandraOperation.updateRecord(
              userNotesDbInfo.getKeySpace(), userNotesDbInfo.getTableName(), req, context);
      result.getResult().remove(JsonKey.RESPONSE);
      sender().tell(result, self());

      targetObject = TelemetryUtil.generateTargetObject(noteId, JsonKey.NOTE, JsonKey.DELETE, null);
      TelemetryUtil.generateCorrelatedObject(noteId, JsonKey.NOTE, null, correlatedObject);
      TelemetryUtil.generateCorrelatedObject(userId, JsonKey.USER, null, correlatedObject);

      TelemetryUtil.telemetryProcessingCall(
          actorMessage.getRequest(), targetObject, correlatedObject, actorMessage.getContext());

      Request request = new Request();
      request.getRequest().put(JsonKey.NOTE, req);
      request.setRequestContext(context);
      request.setOperation(ActorOperations.UPDATE_USER_NOTES_ES.getValue());
      tellToAnother(request);
    } catch (Exception e) {
      logger.error(context, "Error occurred", e);
      sender().tell(e, self());
      return;
    }
  }

  /**
   * Method to validate User based on userId from ElasticSearch Data
   *
   * @param userId
   * @return true if user data is present in ES else false
   */
  private Boolean validUser(String userId, RequestContext context) {
    Boolean result = false;

    if (!StringUtils.isBlank(userId)) {
      Future<Map<String, Object>> dataF =
          esService.getDataByIdentifier(ProjectUtil.EsType.user.getTypeName(), userId, context);
      Map<String, Object> data =
          (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(dataF);
      if (null != data && !data.isEmpty()) {
        result = true;
      }
    }
    return result;
  }

  /**
   * Method to validate note using noteId
   *
   * @param noteId
   * @return true if note exists in Cassandra else false
   */
  private Boolean noteIdExists(String noteId, RequestContext context) {
    Boolean result = false;
    Map<String, Object> list = getNoteRecordById(noteId, context);
    if (!list.isEmpty()) {
      result = true;
    }
    return result;
  }

  /**
   * Method to get Note details using note Id
   *
   * @param noteId
   * @return Note data as List<Map<String, Object>>
   */
  private Map<String, Object> getNoteRecordById(String noteId, RequestContext context) {
    Future<Map<String, Object>> resultF =
        esService.getDataByIdentifier(ProjectUtil.EsType.usernotes.getTypeName(), noteId, context);
    Map<String, Object> result =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
    return result;
  }

  private Boolean validateUserForNoteUpdation(
      String userId, String noteId, RequestContext context) {
    Boolean result = false;
    Map<String, Object> noteData = getNoteRecordById(noteId, context);
    if (MapUtils.isEmpty(noteData)) return result;
    if (!StringUtils.isBlank(userId)) {
      result = true;
    }
    if (!userId.equalsIgnoreCase((String) noteData.get(JsonKey.USER_ID))) {
      throw new ProjectCommonException(
          ResponseCode.errorForbidden.getErrorCode(),
          ResponseCode.errorForbidden.getErrorMessage(),
          ResponseCode.FORBIDDEN.getResponseCode());
    }
    return result;
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
