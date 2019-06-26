package org.sunbird.learner.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.MapUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.ElasticSearchTcpImpl;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.util.HttpUtil;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.request.HeaderParam;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.coursebatch.CourseEnrollmentActor;
import scala.concurrent.Future;

/**
 * This class will update course batch count to EKStep. First it will get batch details from ES ,
 * then collect old open/private batch count value form EKStep then update cassandra db and EKStep
 * course instance count under EKStep.
 *
 * @author Manzarul
 */
public final class CourseBatchSchedulerUtil {
  public static Map<String, String> headerMap = new HashMap<>();
  private static ElasticSearchService esService = new ElasticSearchTcpImpl();

  static {
    String header = ProjectUtil.getConfigValue(JsonKey.EKSTEP_AUTHORIZATION);
    header = JsonKey.BEARER + header;
    headerMap.put(JsonKey.AUTHORIZATION, header);
    headerMap.put("Content-Type", "application/json");
  }

  private CourseBatchSchedulerUtil() {}

  /**
   * @param startDate
   * @param endDate
   * @return
   */
  public static Map<String, Object> getBatchDetailsFromES(String startDate, String endDate) {
    ProjectLogger.log(
        "CourseBatchSchedulerUtil:getBatchDetailsFromES: method call start to collect get course batch data -"
            + startDate
            + " "
            + endDate,
        LoggerEnum.INFO.name());
    Map<String, Object> response = new HashMap<>();
    List<Map<String, Object>> courseBatchStartedList = getToBeUpdatedCoursesByDate(startDate, true);
    if (!courseBatchStartedList.isEmpty()) {
      response.put(JsonKey.START_DATE, courseBatchStartedList);
    }
    List<Map<String, Object>> courseBatchCompletedList =
        getToBeUpdatedCoursesByDate(endDate, false);
    if (!courseBatchCompletedList.isEmpty()) {
      response.put(JsonKey.END_DATE, courseBatchCompletedList);
    }
    List<Map<String, Object>> courseBatchStartStatusList = getToBeUpdatedCoursesByStatus(startDate);
    if (!courseBatchStartStatusList.isEmpty()) {
      response.put(JsonKey.STATUS, courseBatchStartStatusList);
    }
    ProjectLogger.log(
        "CourseBatchSchedulerUtil:getBatchDetailsFromES: method call end to collect get course batch data -"
            + startDate
            + " "
            + endDate,
        LoggerEnum.INFO.name());
    return response;
  }
  /**
   * Method to update course batch status to db as well as EkStep .
   *
   * @param increment
   * @param map
   */
  public static void updateCourseBatchDbStatus(Map<String, Object> map, Boolean increment) {
    ProjectLogger.log(
        "CourseBatchSchedulerUtil:updateCourseBatchDbStatus: updating course batch details start",
        LoggerEnum.INFO.name());
    try {
      boolean response =
          doOperationInEkStepCourse(
              (String) map.get(JsonKey.COURSE_ID),
              increment,
              (String) map.get(JsonKey.ENROLLMENT_TYPE));
      ProjectLogger.log("Geeting response code back for update content == " + response);
      if (response) {
        boolean flag = updateDataIntoES(map);
        if (flag) {
          updateDataIntoCassandra(map);
        }
      } else {
        ProjectLogger.log(
            "CourseBatchSchedulerUtil:updateCourseBatchDbStatus: Ekstep content update failed for courseId "
                + (String) map.get(JsonKey.COURSE_ID),
            LoggerEnum.INFO.name());
      }
    } catch (Exception e) {
      ProjectLogger.log(
          "CourseBatchSchedulerUtil:updateCourseBatchDbStatus: Exception occurred while savin data to course batch db "
              + e.getMessage(),
          LoggerEnum.INFO.name());
    }
  }

  /** @param map */
  public static boolean updateDataIntoES(Map<String, Object> map) {
    boolean flag = true;
    try {
      Future<Boolean> flagF =
          esService.update(
              ProjectUtil.EsType.course.getTypeName(), (String) map.get(JsonKey.ID), map);
      flag = (boolean) ElasticSearchHelper.getResponseFromFuture(flagF);
    } catch (Exception e) {
      ProjectLogger.log(
          "CourseBatchSchedulerUtil:updateDataIntoES: Exception occurred while saving course batch data to ES",
          e);
      flag = false;
    }
    return flag;
  }

  /** @param map */
  public static void updateDataIntoCassandra(Map<String, Object> map) {
    CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    Util.DbInfo courseBatchDBInfo = Util.dbInfoMap.get(JsonKey.COURSE_BATCH_DB);
    cassandraOperation.updateRecord(
        courseBatchDBInfo.getKeySpace(), courseBatchDBInfo.getTableName(), map);
    ProjectLogger.log(
        "CourseBatchSchedulerUtil:updateDataIntoCassandra: Update Successful for batchId "
            + map.get(JsonKey.ID),
        LoggerEnum.INFO);
  }

  private static void addHeaderProps(Map<String, String> header, String key, String value) {
    header.put(key, value);
  }
  /**
   * Method to update the content state at ekstep : batch count
   *
   * @param courseId
   * @param increment
   * @param enrollmentType
   * @return
   */
  public static boolean doOperationInEkStepCourse(
      String courseId, boolean increment, String enrollmentType) {
    String contentName = getCountName(enrollmentType);
    boolean response = false;
    Map<String, Object> ekStepContent =
        CourseEnrollmentActor.getCourseObjectFromEkStep(courseId, getBasicHeader());
    if (MapUtils.isNotEmpty(ekStepContent)) {
      int val = getUpdatedBatchCount(ekStepContent, contentName, increment);
      if (ekStepContent.get(JsonKey.CHANNEL) != null) {
        ProjectLogger.log(
            "Channel value coming from content is "
                + (String) ekStepContent.get(JsonKey.CHANNEL)
                + " Id "
                + courseId,
            LoggerEnum.INFO.name());
        addHeaderProps(
            getBasicHeader(),
            HeaderParam.CHANNEL_ID.getName(),
            (String) ekStepContent.get(JsonKey.CHANNEL));
      } else {
        ProjectLogger.log(
            "No channel value available in content with Id " + courseId, LoggerEnum.INFO.name());
      }
      response = updateEkstepContent(courseId, contentName, val);
    } else {
      ProjectLogger.log(
          "EKstep content not found for course id==" + courseId, LoggerEnum.INFO.name());
    }
    return response;
  }

  private static Map<String, String> getBasicHeader() {
    return headerMap;
  }

  private static List<Map<String, Object>> getToBeUpdatedCoursesByDate(
      String date, boolean isStartDate) {
    String dateAttribute = isStartDate ? JsonKey.START_DATE : JsonKey.END_DATE;
    String counterAttribute =
        isStartDate ? JsonKey.COUNTER_INCREMENT_STATUS : JsonKey.COUNTER_DECREMENT_STATUS;
    int status =
        isStartDate
            ? ProjectUtil.ProgressStatus.NOT_STARTED.getValue()
            : ProjectUtil.ProgressStatus.STARTED.getValue();
    SearchDTO dto = new SearchDTO();
    Map<String, Object> map = new HashMap<>();
    Map<String, String> dateRangeFilter = new HashMap<>();
    dateRangeFilter.put("<=", date);
    map.put(dateAttribute, dateRangeFilter);
    map.put(JsonKey.STATUS, status);
    map.put(counterAttribute, false);
    dto.addAdditionalProperty(JsonKey.FILTERS, map);
    Map<String, Object> sortMap = new HashMap<>();
    sortMap.put(dateAttribute, JsonKey.DESC);
    dto.setSortBy(sortMap);
    dto.setLimit(500);
    return searchContent(dto);
  }

  private static List<Map<String, Object>> getToBeUpdatedCoursesByStatus(String startDate) {
    SearchDTO dto = new SearchDTO();
    Map<String, Object> map = new HashMap<>();
    Map<String, String> dateRangeFilter = new HashMap<>();
    dateRangeFilter.put("<=", startDate);
    map.put(JsonKey.START_DATE, dateRangeFilter);
    map.put(JsonKey.COUNTER_INCREMENT_STATUS, true);
    map.put(JsonKey.STATUS, 0);
    dto.addAdditionalProperty(JsonKey.FILTERS, map);
    return searchContent(dto);
  }

  public static List<Map<String, Object>> getFutureCourseBatches(
      String courseId, String today, String enrollmentType) {
    SearchDTO dto = new SearchDTO();
    Map<String, Object> map = new HashMap<>();
    Map<String, String> endDateRangeFilter = new HashMap<>();
    endDateRangeFilter.put(">", today);
    map.put(JsonKey.END_DATE, endDateRangeFilter);
    map.put(JsonKey.ENROLLMENT_TYPE, enrollmentType);
    map.put(JsonKey.COURSE_ID, courseId);
    dto.addAdditionalProperty(JsonKey.FILTERS, map);
    return searchContent(dto);
  }

  public static List<Map<String, Object>> getOngoingAndUpcomingCourseBatches(
      String courseId, String enrollmentType) {
    SearchDTO dto = new SearchDTO();
    Map<String, Object> map = new HashMap<>();
    map.put(
        JsonKey.STATUS,
        new ArrayList<String>(
            Arrays.asList("0", "1"))); // Set status to upcoming and ongoing batches
    map.put(JsonKey.ENROLLMENT_TYPE, enrollmentType);
    map.put(JsonKey.COURSE_ID, courseId);
    dto.addAdditionalProperty(JsonKey.FILTERS, map);
    return searchContent(dto);
  }

  public static Map<String, Object> getOpenForEnrollmentCourses(String countName, int offset) {
    Map<String, Object> dto = new HashMap<>();
    Map<String, Integer> countFilter = new HashMap<>();
    countFilter.put(">", 0);
    Map<String, Object> map = new HashMap<>();
    map.put(countName, countFilter);
    dto.put(JsonKey.FILTERS, map);
    dto.put(JsonKey.OFFSET, offset);
    dto.put(JsonKey.LIMIT, 100);
    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(JsonKey.REQUEST, dto);
    try {
      Map<String, Object> result =
          ContentSearchUtil.searchContentSync(
              null, new ObjectMapper().writeValueAsString(requestMap), getHeader());
      if (MapUtils.isNotEmpty(result)) {
        return result;
      }
    } catch (JsonProcessingException e) {
      ProjectLogger.log(
          "CourseBatchScheduleUtil:getOpenForEnrollmentCourses: Exception occurred with error message = "
              + e.getMessage(),
          LoggerEnum.INFO);
      return null;
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> searchContent(SearchDTO dto) {
    List<Map<String, Object>> listOfMap = new ArrayList<>();
    Future<Map<String, Object>> responseMapF =
        esService.search(dto, ProjectUtil.EsType.course.getTypeName());
    Map<String, Object> responseMap =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(responseMapF);
    if (responseMap != null && responseMap.size() > 0) {
      Object val = responseMap.get(JsonKey.CONTENT);
      if (val != null) {
        listOfMap = (List<Map<String, Object>>) val;
      } else {
        ProjectLogger.log(
            "CourseBatchSchedulerUtil: contentReadResponse: Empty records for "
                + dto.getAdditionalProperties().toString(),
            LoggerEnum.INFO.name());
      }
    } else {
      ProjectLogger.log(
          "CourseBatchSchedulerUtil: contentReadResponse: No response from ElasticSearch for "
              + dto.getAdditionalProperties().toString(),
          LoggerEnum.INFO.name());
    }
    return listOfMap;
  }

  public static String getCountName(String enrollmentType) {
    String name = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_INSTALLATION);
    String contentName = "";
    if (ProjectUtil.EnrolmentType.open.getVal().equals(enrollmentType)) {
      contentName = "c_" + name + "_open_batch_count";
    } else {
      contentName = "c_" + name + "_private_batch_count";
    }
    return contentName.toLowerCase();
  }

  public static int getUpdatedBatchCount(
      Map<String, Object> ekStepContent, String contentName, boolean increment) {
    int val = (int) ekStepContent.getOrDefault(contentName, 0);
    val = increment ? val + 1 : (val > 0) ? val - 1 : 0;
    return val;
  }

  public static boolean updateEkstepContent(String courseId, String contentName, int val) {
    String response = "";
    try {
      ProjectLogger.log("updating content details to Ekstep start", LoggerEnum.INFO.name());
      String contentUpdateBaseUrl = ProjectUtil.getConfigValue(JsonKey.EKSTEP_BASE_URL);
      response =
          HttpUtil.sendPatchRequest(
              contentUpdateBaseUrl
                  + PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_CONTENT_UPDATE_URL)
                  + courseId,
              "{\"request\": {\"content\": {\"" + contentName + "\": " + val + "}}}",
              getBasicHeader());
      ProjectLogger.log(
          "batch count update response==" + response + " " + courseId, LoggerEnum.INFO.name());
    } catch (IOException e) {
      ProjectLogger.log("Error while updating content value " + e.getMessage(), e);
    }
    return JsonKey.SUCCESS.equalsIgnoreCase(response);
  }

  public static Map<String, String> getHeader() {
    Map<String, String> headerMap = new HashMap<>();
    String header = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_AUTHORIZATION);
    header = JsonKey.BEARER + header;
    headerMap.put(JsonKey.AUTHORIZATION, header);
    headerMap.put("Content-Type", "application/json");
    return headerMap;
  }
}
