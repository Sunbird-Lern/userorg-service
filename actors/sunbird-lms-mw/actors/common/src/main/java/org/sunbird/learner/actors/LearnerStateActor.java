package org.sunbird.learner.actors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
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
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.coursebatch.CourseEnrollmentActor;
import org.sunbird.learner.actors.coursebatch.service.UserCoursesService;
import org.sunbird.learner.util.ContentSearchUtil;
import org.sunbird.learner.util.CourseBatchSchedulerUtil;
import org.sunbird.learner.util.Util;
import scala.concurrent.Future;

/**
 * This actor will handle leaner's state operation like get course , get content etc.
 *
 * @author Manzarul
 * @author Arvind
 */
@ActorConfig(
  tasks = {"getCourse", "getUserCourse", "getContent"},
  asyncTasks = {}
)
public class LearnerStateActor extends BaseActor {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private UserCoursesService userCoursesService = new UserCoursesService();
  private ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);
  private static final String COMPLETE_PERCENT = "completionPercentage";

  /**
   * Receives the actor message and perform the operation like get course , get content etc.
   *
   * @param request Object
   */
  @Override
  public void onReceive(Request request) throws Exception {
    if (request.getOperation().equalsIgnoreCase(ActorOperations.GET_COURSE.getValue())) {
      getCourse(request);
    } else if (request
        .getOperation()
        .equalsIgnoreCase(ActorOperations.GET_USER_COURSE.getValue())) {
      getUserCourse(request);
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

  public void getCourse(Request request) throws Exception {
    String userId = (String) request.getRequest().get(JsonKey.USER_ID);
    Map<String, Object> result = userCoursesService.getActiveUserCourses(userId);
    List<Map<String, Object>> updatedCourses = calculateProgressForUserCourses(request, result);
    if (MapUtils.isNotEmpty(result)) {
      addCourseDetails(request, result);
    } else {
      ProjectLogger.log(
          "LearnerStateActor:getCourse: returning batch without course details",
          LoggerEnum.INFO.name());
    }
    Response response = new Response();
    response.put(JsonKey.COURSES, updatedCourses);
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
    String requestBody = prepareCourseSearchRequest(batches, null);
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
        esService.search(dto, ProjectUtil.EsType.courseBatch.getTypeName());
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

  private String prepareCourseSearchRequest(
      List<Map<String, Object>> batches, List<String> fields) {
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
    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(JsonKey.FILTERS, filters);
    if (fields != null) requestMap.put(JsonKey.FIELDS, fields);
    Map<String, Map<String, Object>> request = new HashMap<>();
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
    String batchId = (String) requestMap.get(JsonKey.BATCH_ID);
    String courseId = (String) requestMap.get(JsonKey.COURSE_ID);
    List<String> contentIds = (List<String>) requestMap.get(JsonKey.CONTENT_IDS);
    if (CollectionUtils.isEmpty(contentIds)) {
      Map<String, Object> courseData =
          CourseEnrollmentActor.getCourseObjectFromEkStep(
              courseId, CourseBatchSchedulerUtil.headerMap);
      if (MapUtils.isEmpty(courseData)) {
        throw new ProjectCommonException(
            ResponseCode.invalidCourseId.getErrorCode(),
            ResponseCode.invalidCourseId.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
      List<String> leafNodes = (List<String>) courseData.get("leafNodes");
      if (CollectionUtils.isNotEmpty(leafNodes)) {
        contentIds = leafNodes;
      }
    }
    List<Map<String, Object>> contentList = null;
    if (CollectionUtils.isEmpty(contentIds)) {
      contentList = new ArrayList<>();
    } else {
      contentList = getContents(userId, contentIds, batchId, courseId);
    }
    response.getResult().put(JsonKey.RESPONSE, contentList);
    return response;
  }

  private List<Map<String, Object>> getContents(
      String userId, List<String> contentIds, String batchId, String courseId) {
    List<Map<String, Object>> contentList = new ArrayList<Map<String, Object>>();
    Util.DbInfo dbInfo = Util.dbInfoMap.get(JsonKey.LEARNER_CONTENT_DB);
    Map<String, Object> filters =
        new HashMap<String, Object>() {
          {
            put("userid", userId);
            put("contentid", contentIds);
            put("batchid", batchId);
            put("courseid", courseId);
          }
        };
    Response response =
        cassandraOperation.getRecords(dbInfo.getKeySpace(), dbInfo.getTableName(), filters, null);
    contentList.addAll((List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE));
    return contentList;
  }

  private void removeUnwantedProperties(Response response) {
    List<Map<String, Object>> list =
        (List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE);
    for (Map<String, Object> map : list) {
      ProjectUtil.removeUnwantedFields(
          map, JsonKey.DATE_TIME, JsonKey.USER_ID, JsonKey.ADDED_BY, JsonKey.LAST_UPDATED_TIME);
    }
  }

  private List<Map<String, Object>> calculateProgressForUserCourses(
      Request request, Map<String, Object> result) throws Exception {
    List<Map<String, Object>> activeCourses =
        (List<Map<String, Object>>) (result.get(JsonKey.CONTENT));
    List<Map<String, Object>> contentsForCourses = getcontentsForCourses(request, activeCourses);

    Map<String, Map<String, Object>> contentIdsMapForCourses =
        contentsForCourses
            .stream()
            .collect(Collectors.toMap(cMap -> (String) cMap.get(JsonKey.IDENTIFIER), cMap -> cMap));

    List<Map<String, Object>> updatedCourses = new ArrayList<>();
    for (Map<String, Object> course : activeCourses) {
      course.put(COMPLETE_PERCENT, Integer.valueOf("0"));
      if (!contentIdsMapForCourses.containsKey(course.get(JsonKey.COURSE_ID))) {
        continue;
      }
      Map<String, Object> courseContent =
          contentIdsMapForCourses.get(course.get(JsonKey.COURSE_ID));
      course.put(JsonKey.COURSE_NAME, courseContent.get(JsonKey.NAME));
      course.put(JsonKey.DESCRIPTION, courseContent.get(JsonKey.DESCRIPTION));
      course.put(JsonKey.LEAF_NODE_COUNT, courseContent.get(JsonKey.LEAF_NODE_COUNT));
      course.put(JsonKey.COURSE_LOGO_URL, courseContent.get(JsonKey.APP_ICON));
      course.put(JsonKey.CONTENT_ID, course.get(JsonKey.COURSE_ID));
      List<String> leafNodes = (List<String>) courseContent.get("leafNodes");
      if (course.get("contentStatus") != null && CollectionUtils.isNotEmpty(leafNodes)) {
        Map<String, Object> contentStatus =
            new ObjectMapper()
                .readValue(
                    ((String) course.get("contentStatus")).replaceAll("\\\\", ""), Map.class);
        int contentIdscompleted =
            (int)
                contentStatus
                    .entrySet()
                    .stream()
                    .filter(
                        content ->
                            ProjectUtil.ProgressStatus.COMPLETED.getValue()
                                == (Integer) content.getValue())
                    .filter(content -> (leafNodes).contains((String) content.getKey()))
                    .count();

        Integer completionPercentage =
            (int) Math.round((contentIdscompleted * 100.0) / (leafNodes).size());
        course.put(JsonKey.PROGRESS, contentIdscompleted);
        course.put(COMPLETE_PERCENT, completionPercentage);
      }
      updatedCourses.add(course);
    }
    return updatedCourses;
  }

  private List<Map<String, Object>> getcontentsForCourses(
      Request request, List<Map<String, Object>> activeCourses) {
    List<String> fields = new ArrayList<>();
    fields.add(JsonKey.IDENTIFIER);
    fields.add(JsonKey.DESCRIPTION);
    fields.add(JsonKey.NAME);
    fields.add(JsonKey.LEAF_NODE_COUNT);
    fields.add(JsonKey.APP_ICON);
    fields.add("leafNodes");
    String requestBody = prepareCourseSearchRequest(activeCourses, fields);
    ProjectLogger.log(
        "LearnerStateActor:getcontentsForCourses: Request Body = " + requestBody,
        LoggerEnum.INFO.name());
    Map<String, Object> contentsList =
        ContentSearchUtil.searchContentSync(
            null, requestBody, (Map<String, String>) request.getRequest().get(JsonKey.HEADER));
    if (contentsList == null) {
      new ProjectCommonException(
          ResponseCode.internalError.getErrorCode(),
          ResponseCode.internalError.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    return ((List<Map<String, Object>>) (contentsList.get(JsonKey.CONTENTS)));
  }

  public void getUserCourse(Request request) throws Exception {
    String userId = (String) request.getRequest().get(JsonKey.USER_ID);
    String batchId = (String) request.getRequest().get(JsonKey.BATCH_ID);
    String id = UserCoursesService.generateUserCourseESId(batchId, userId);
    Future<Map<String, Object>> mapF =
        esService.getDataByIdentifier(ProjectUtil.EsType.usercourses.getTypeName(), id);

    Map<String, Object> map = (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(mapF);
    if (MapUtils.isEmpty(map)
        || !Boolean.TRUE.equals(Boolean.valueOf((String) map.get(JsonKey.ACTIVE)))) {
      ProjectCommonException.throwClientErrorException(ResponseCode.userNotEnrolledCourse);
    }
    List<Map<String, Object>> content = new ArrayList<>();
    content.add(map);
    Map<String, Object> result = new HashMap<>();
    result.put(JsonKey.CONTENT, content);
    List<Map<String, Object>> updatedCourses = calculateProgressForUserCourses(request, result);
    if (MapUtils.isEmpty(result)) {
      ProjectLogger.log(
          "LearnerStateActor:getCourse: returning batch without course details",
          LoggerEnum.INFO.name());
    }
    Response response = new Response();
    response.put(JsonKey.COURSE, updatedCourses.get(0));
    sender().tell(response, self());
  }
}
