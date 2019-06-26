package org.sunbird.learner.actors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.MessageFormatter;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.datasecurity.OneWayHashing;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.coursebatch.service.UserCoursesService;
import org.sunbird.learner.util.ContentSearchUtil;
import org.sunbird.learner.util.Util;
import scala.concurrent.Future;

/**
 * This actor will handle leaner's state operation like get course , get content etc.
 *
 * @author Manzarul
 * @author Arvind
 */
@ActorConfig(
  tasks = {"getCourse", "getContent"},
  asyncTasks = {}
)
public class LearnerStateActor extends BaseActor {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private UserCoursesService userCoursesService = new UserCoursesService();
  private ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);

  /**
   * Receives the actor message and perform the operation like get course , get content etc.
   *
   * @param request Object
   */
  @Override
  public void onReceive(Request request) throws Exception {
    if (request.getOperation().equalsIgnoreCase(ActorOperations.GET_COURSE.getValue())) {
      getCourse(request);
    } else if (request.getOperation().equalsIgnoreCase(ActorOperations.GET_CONTENT.getValue())) {

      Response res = new Response();
      Map<String, Object> requestMap = request.getRequest();
      String userId = (String) request.getRequest().get(JsonKey.USER_ID);

      res = getCourseContentState(userId, requestMap);
      removeUnwantedProperties(res);
      sender().tell(res, self());
    } else {
      onReceiveUnsupportedOperation(request.getOperation());
    }
  }

  public void getCourse(Request request) {
    String userId = (String) request.getRequest().get(JsonKey.USER_ID);
    Map<String, Object> result = userCoursesService.getActiveUserCourses(userId);
    if (MapUtils.isNotEmpty(result)) {
      addCourseDetails(request, result);
    } else {
      ProjectLogger.log(
          "LearnerStateActor:getCourse: returning batch without course details",
          LoggerEnum.INFO.name());
    }
    Response response = new Response();
    response.put(JsonKey.COURSES, result.get(JsonKey.CONTENT));
    sender().tell(response, self());
  }

  @SuppressWarnings("unchecked")
  private void addCourseDetails(Request request, Map<String, Object> userCoursesResult) {
    List<Map<String, Object>> batches =
        (List<Map<String, Object>>) userCoursesResult.get(JsonKey.CONTENT);

    ProjectLogger.log(
        "LearnerStateActor:addCourseDetails: batches size = " + batches.size(),
        LoggerEnum.INFO.name());

    if (CollectionUtils.isEmpty(batches)) {
      return;
    }
    String requestBody = prepareCourseSearchRequest(batches);
    ProjectLogger.log(
        MessageFormatter.format(
                "LearnerStateActor:addCourseDetails: request body = {0}, query string = {1}",
                requestBody, (String) request.getContext().get(JsonKey.URL_QUERY_STRING))
            .getMessage(),
        LoggerEnum.INFO.name());

    Map<String, Object> contents = null;
    contents =
        ContentSearchUtil.searchContentSync(
            (String) request.getContext().get(JsonKey.URL_QUERY_STRING),
            requestBody,
            (Map<String, String>) request.getRequest().get(JsonKey.HEADER));

    Map<String, Object> courseBatchesMap = null;
    List<String> requestedFields = null;
    String[] queryParams = (String[]) request.getContext().get(JsonKey.BATCH_DETAILS);

    if (queryParams != null && queryParams.length > 0) {

      ProjectLogger.log(
          "LearnerStateActor:addCourseDetails: queryParam[0] = " + queryParams[0],
          LoggerEnum.INFO.name());
      requestedFields = new ArrayList<>(Arrays.asList(queryParams[0].split(",")));
      if (CollectionUtils.isNotEmpty(requestedFields))
        courseBatchesMap = getCourseBatch(batches, requestedFields);
    }

    Map<String, Object> courseBatches = new HashMap<>();
    if (MapUtils.isNotEmpty(courseBatchesMap)) {
      List<Map<String, Object>> courses =
          (List<Map<String, Object>>) courseBatchesMap.get(JsonKey.CONTENT);
      if (CollectionUtils.isNotEmpty(courses)) {
        courses.forEach(
            course -> courseBatches.put((String) course.get(JsonKey.IDENTIFIER), course));
      }
      ProjectLogger.log(
          "LearnerStateActor:addCourseDetails: coursesBathces = " + courseBatches,
          LoggerEnum.INFO.name());
    }
    mergeDetailsAndSendCourses(contents, batches, courseBatches);
  }

  private Map<String, Object> getCourseBatch(
      List<Map<String, Object>> batches, List<String> requestedFields) {
    List<String> courseBatchIds =
        (List<String>)
            batches
                .stream()
                .map(batch -> (String) batch.get(JsonKey.BATCH_ID))
                .collect(Collectors.toList());
    ProjectLogger.log(
        "LearnerStateActor:getCourseBatch: coursesBatchIds = " + courseBatchIds,
        LoggerEnum.INFO.name());

    Map<String, Object> esQueryMap = new HashMap<>();
    esQueryMap.put(JsonKey.IDENTIFIER, courseBatchIds);
    SearchDTO dto = new SearchDTO();
    requestedFields.add(JsonKey.IDENTIFIER);
    dto.setFields(requestedFields);
    dto.getAdditionalProperties().put(JsonKey.FILTERS, esQueryMap);

    Future<Map<String, Object>> responseF =
        esService.search(dto, ProjectUtil.EsType.course.getTypeName());
    Map<String, Object> response =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(responseF);
    return response;
  }

  public void mergeDetailsAndSendCourses(
      Map<String, Object> coursesContents,
      List<Map<String, Object>> batches,
      Map<String, Object> courseBatches) {

    ProjectLogger.log(
        "LearnerStateActor:mergeDetailsAndSendCourses coursesContents =" + coursesContents,
        LoggerEnum.INFO.name());

    if (MapUtils.isNotEmpty(courseBatches)) {
      ProjectLogger.log(
          "LearnerStateActor:mergeDetailsAndSendCourses courseBatchContents ="
              + "for requested data "
              + courseBatches,
          LoggerEnum.INFO.name());
    }

    Map<String, Object> contentsByCourseId = getContentAsMap(coursesContents);

    List<Map<String, Object>> batchesWithCourseDetails =
        getMergedContents(batches, contentsByCourseId, JsonKey.CONTENT, JsonKey.COURSE_ID);
    batchesWithCourseDetails =
        getMergedContents(batchesWithCourseDetails, courseBatches, JsonKey.BATCH, JsonKey.BATCH_ID);

    Response response = new Response();
    response.put(JsonKey.COURSES, batchesWithCourseDetails);
    sender().tell(response, self());
  }

  public List<Map<String, Object>> getMergedContents(
      List<Map<String, Object>> batches,
      Map<String, Object> contentsById,
      String valueType,
      String idType) {
    List<Map<String, Object>> batchesWithCourseDetails = batches;
    if (MapUtils.isNotEmpty(contentsById)) {

      batchesWithCourseDetails =
          batches
              .stream()
              .map(
                  batch -> {
                    if (contentsById.containsKey((String) batch.get(idType))) {
                      batch.put(valueType, contentsById.get((String) batch.get(idType)));
                    }

                    return batch;
                  })
              .collect(Collectors.toList());
    }
    ProjectLogger.log(
        "LearnerStateActor:mergeDetailsAndSendCourses batchesWithCourseDetails ="
            + batchesWithCourseDetails,
        LoggerEnum.INFO.name());
    return batchesWithCourseDetails;
  }

  private Map<String, Object> getContentAsMap(Map<String, Object> coursesContents) {
    Map<String, Object> contentsByCourseId = new HashMap<>();
    if (MapUtils.isNotEmpty(coursesContents)) {
      List<Map<String, Object>> courses =
          (List<Map<String, Object>>) coursesContents.get(JsonKey.CONTENTS);
      if (CollectionUtils.isNotEmpty(courses)) {
        courses.forEach(
            course -> contentsByCourseId.put((String) course.get(JsonKey.IDENTIFIER), course));
      }
    }
    return contentsByCourseId;
  }

  private String prepareCourseSearchRequest(List<Map<String, Object>> batches) {
    Set<String> courseIds =
        batches
            .stream()
            .map(batch -> (String) batch.get(JsonKey.COURSE_ID))
            .collect(Collectors.toSet());

    Map<String, Object> filters = new HashMap<String, Object>();
    filters.put(JsonKey.CONTENT_TYPE, new String[] {JsonKey.COURSE});
    filters.put(JsonKey.IDENTIFIER, courseIds);
    ProjectLogger.log(
        "LearnerStateActor:prepareCourseSearchRequest: courseIds = " + courseIds,
        LoggerEnum.INFO.name());
    Map<String, Map<String, Object>> requestMap = new HashMap<>();
    requestMap.put(JsonKey.FILTERS, filters);

    Map<String, Map<String, Map<String, Object>>> request = new HashMap<>();
    request.put(JsonKey.REQUEST, requestMap);

    String requestJson = null;
    try {
      requestJson = new ObjectMapper().writeValueAsString(request);
    } catch (JsonProcessingException e) {
      ProjectLogger.log(
          "LearnerStateActor:prepareCourseSearchRequest: Exception occurred with error message = "
              + e.getMessage(),
          e);
    }

    return requestJson;
  }

  private Response getCourseContentState(String userId, Map<String, Object> requestMap) {

    Response response = new Response();
    List<Map<String, Object>> contentList = new ArrayList<>();

    String batchId = (String) requestMap.get(JsonKey.BATCH_ID);
    List<String> courseIds = (List<String>) requestMap.get(JsonKey.COURSE_IDS);
    List<String> contentIds = (List<String>) requestMap.get(JsonKey.CONTENT_IDS);

    if (null != courseIds) {
      if (courseIds.size() > 1 && StringUtils.isNotBlank(batchId)) {
        ProjectLogger.log(
            "LearnerStateActor:getContentByBatch: multiple course ids not allowed for batch",
            LoggerEnum.ERROR.name());
        throw new ProjectCommonException(
            ResponseCode.multipleCoursesNotAllowedForBatch.getErrorCode(),
            ResponseCode.multipleCoursesNotAllowedForBatch.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
    }

    if (CollectionUtils.isNotEmpty(contentIds)
        && contentIds.size() > 0
        && CollectionUtils.isNotEmpty(courseIds)
        && courseIds.size() == 1
        && StringUtils.isNotBlank(batchId)) {
      List<Object> primaryKeyList = new ArrayList<>();
      String courseId = courseIds.get(0);
      for (String contentId : contentIds) {
        String key = generatePrimaryKeyForContent(userId, batchId, courseId, contentId);
        primaryKeyList.add(key);
      }
      contentList = getContentByPrimaryKeys(primaryKeyList);
    } else if (StringUtils.isNotBlank(batchId)) {
      contentList = getContentByBatch(userId, batchId);
      if (CollectionUtils.isNotEmpty(contentIds)) {
        contentList = filterForMatchingContentIds(contentList, requestMap);
      }
    } else if (CollectionUtils.isNotEmpty(courseIds)) {
      contentList = getContentByCourses(userId, requestMap);
      if (courseIds.size() == 1) {
        if (CollectionUtils.isNotEmpty(contentIds)) {
          contentList = filterForMatchingContentIds(contentList, requestMap);
        }
      }
    } else if (CollectionUtils.isNotEmpty(contentIds)) {
      contentList = getContentByContentIds(userId, requestMap);
    }
    response.getResult().put(JsonKey.RESPONSE, contentList);
    return response;
  }

  private List<Map<String, Object>> filterForMatchingContentIds(
      List<Map<String, Object>> contentList, Map<String, Object> requestMap) {

    List<String> contentIds =
        new ArrayList<String>((List<String>) requestMap.get(JsonKey.CONTENT_IDS));
    List<Map<String, Object>> matchedContentList = new ArrayList<>();

    if (CollectionUtils.isNotEmpty(contentIds)) {
      for (Map<String, Object> content : contentList) {
        for (int i = 0; i < contentIds.size(); i++) {
          String contentId = contentIds.get(i);
          if (contentId.equals((String) content.get(JsonKey.CONTENT_ID))) {
            matchedContentList.add(content);
            break;
          }
        }
      }
    }
    return matchedContentList;
  }

  private List<Map<String, Object>> getContentByBatch(String userId, String batchId) {

    Util.DbInfo dbInfo = Util.dbInfoMap.get(JsonKey.LEARNER_CONTENT_DB);
    List<Map<String, Object>> contentList = new ArrayList<Map<String, Object>>();
    Map<String, Object> queryMap = new HashMap<String, Object>();
    queryMap.put(JsonKey.USER_ID, userId);
    queryMap.put(JsonKey.BATCH_ID, batchId);
    Response response =
        cassandraOperation.getRecordsByProperties(
            dbInfo.getKeySpace(), dbInfo.getTableName(), queryMap);
    contentList.addAll((List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE));
    return contentList;
  }

  private List<Map<String, Object>> getContentByPrimaryKeys(List<Object> primaryKeyList) {

    Util.DbInfo dbInfo = Util.dbInfoMap.get(JsonKey.LEARNER_CONTENT_DB);

    List<Map<String, Object>> contentList = new ArrayList<Map<String, Object>>();
    Response response =
        cassandraOperation.getRecordsByProperty(
            dbInfo.getKeySpace(), dbInfo.getTableName(), JsonKey.ID, primaryKeyList);
    contentList.addAll((List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE));
    return contentList;
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> getContentByCourses(
      String userId, Map<String, Object> request) {

    Response response = new Response();
    Util.DbInfo dbInfo = Util.dbInfoMap.get(JsonKey.LEARNER_CONTENT_DB);
    List<String> courseIds = new ArrayList<String>((List<String>) request.get(JsonKey.COURSE_IDS));
    List<Map<String, Object>> contentList = new ArrayList<Map<String, Object>>();
    Map<String, Object> queryMap = new HashMap<String, Object>();
    queryMap.put(JsonKey.USER_ID, userId);

    for (String courseId : courseIds) {
      queryMap.put(JsonKey.COURSE_ID, courseId);
      response =
          cassandraOperation.getRecordsByProperties(
              dbInfo.getKeySpace(), dbInfo.getTableName(), queryMap);
      contentList.addAll((List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE));
    }
    return contentList;
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> getContentByContentIds(
      String userId, Map<String, Object> request) {
    Response response = null;
    Util.DbInfo dbInfo = Util.dbInfoMap.get(JsonKey.LEARNER_CONTENT_DB);
    List<String> contentIds =
        new ArrayList<String>((List<String>) request.get(JsonKey.CONTENT_IDS));

    LinkedHashMap<String, Object> queryMap = new LinkedHashMap<String, Object>();
    queryMap.put(JsonKey.USER_ID, userId);

    List<Map<String, Object>> contentList = new ArrayList<Map<String, Object>>();

    for (String contentId : contentIds) {
      queryMap.put(JsonKey.CONTENT_ID, contentId);
      response =
          cassandraOperation.getRecordsByProperties(
              dbInfo.getKeySpace(), dbInfo.getTableName(), queryMap);
      contentList.addAll((List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE));
    }
    return contentList;
  }

  @SuppressWarnings("unchecked")
  private void removeUnwantedProperties(Response response) {
    List<Map<String, Object>> list =
        (List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE);
    for (Map<String, Object> map : list) {
      ProjectUtil.removeUnwantedFields(
          map, JsonKey.DATE_TIME, JsonKey.USER_ID, JsonKey.ADDED_BY, JsonKey.LAST_UPDATED_TIME);
    }
  }

  private String generatePrimaryKeyForContent(
      String userId, String batchId, String courseId, String contentId) {
    String key =
        userId
            + JsonKey.PRIMARY_KEY_DELIMETER
            + contentId
            + JsonKey.PRIMARY_KEY_DELIMETER
            + courseId
            + JsonKey.PRIMARY_KEY_DELIMETER
            + batchId;
    return OneWayHashing.encryptVal(key);
  }
}
