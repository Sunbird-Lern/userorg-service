/** */
package org.sunbird.common.quartz.scheduler;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.datasecurity.OneWayHashing;
import org.sunbird.common.request.Request;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.CourseBatchSchedulerUtil;
import org.sunbird.learner.util.EkStepRequestUtil;
import org.sunbird.learner.util.Util;
import org.sunbird.telemetry.util.TelemetryEvents;
import org.sunbird.telemetry.util.TelemetryUtil;

/**
 * This class will call the EKstep get content api to know the status of course published. once
 * course status becomes live then it will update status under course published table and collect
 * all participant from course-batch table and register all those participant under user_course
 * table and push the data to ES.
 *
 * @author Manzarul
 */
public class CoursePublishedUpdate extends BaseJob {

  private static Util.DbInfo coursePublishDBInfo =
      Util.dbInfoMap.get(JsonKey.COURSE_PUBLISHED_STATUS);
  private Util.DbInfo courseBatchDBInfo = Util.dbInfoMap.get(JsonKey.COURSE_BATCH_DB);
  private static CassandraOperation cassandraOperation = ServiceFactory.getInstance();

  private static String requestData =
      "{\"request\":{\"filters\":{\"identifier\":dataVal},\"fields\":[\"status\"]}}";

  @SuppressWarnings("unchecked")
  public void execute(JobExecutionContext ctx) throws JobExecutionException {
    ProjectLogger.log("Fetching All unpublished course status.", LoggerEnum.INFO.name());

    Util.initializeContextForSchedulerJob(
        JsonKey.SYSTEM, ctx.getFireInstanceId(), JsonKey.SCHEDULER_JOB);
    Map<String, Object> logInfo =
        genarateLogInfo(JsonKey.SYSTEM, ctx.getJobDetail().getDescription());
    logInfo.put(JsonKey.LOG_LEVEL, JsonKey.INFO);

    List<String> courseListWithStatusAsDraft = getAllUnPublishedCourseStatusId();
    if (null != courseListWithStatusAsDraft && !courseListWithStatusAsDraft.isEmpty()) {
      ProjectLogger.log("Fetching All course details from ekstep.", LoggerEnum.INFO.name());
      List<String> ekStepResult = getAllPublishedCourseListFromEKStep(courseListWithStatusAsDraft);
      if (null != ekStepResult && !ekStepResult.isEmpty()) {
        ProjectLogger.log("update course status table.", LoggerEnum.INFO.name());
        updateCourseStatusTable(ekStepResult);
        for (String courseId : ekStepResult) {
          try {
            Map<String, Object> map = new HashMap<>();
            map.put(JsonKey.COURSE_ID, courseId);
            map.put(JsonKey.STATUS, ProjectUtil.ProgressStatus.NOT_STARTED.getValue());
            ProjectLogger.log("Fetching participants list from Db", LoggerEnum.INFO.name());
            Response response =
                cassandraOperation.getRecordsByProperty(
                    courseBatchDBInfo.getKeySpace(),
                    courseBatchDBInfo.getTableName(),
                    JsonKey.COURSE_ID,
                    courseId);
            List<Map<String, Object>> batchList =
                (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
            ProjectLogger.log("Add participants to user course table", LoggerEnum.INFO.name());
            addUserToUserCourseTable(batchList);
          } catch (Exception ex) {
            ProjectLogger.log(
                ex.getMessage(), ex, genarateTelemetryInfoForError(JsonKey.SCHEDULER_JOB));
            logInfo.put(JsonKey.LOG_LEVEL, "error");
          }
        }
      }
    }

    TelemetryUtil.telemetryProcessingCall(logInfo, null, null, TelemetryEvents.LOG.getName());
  }

  @SuppressWarnings("unchecked")
  private void addUserToUserCourseTable(List<Map<String, Object>> batchList) {
    ProjectLogger.log("Adding participants to user course table started", LoggerEnum.INFO.name());
    Util.DbInfo courseEnrollmentdbInfo = Util.dbInfoMap.get(JsonKey.LEARNER_COURSE_DB);
    for (Map<String, Object> batch : batchList) {
      Map<String, String> additionalCourseInfo =
          (Map<String, String>) batch.get(JsonKey.COURSE_ADDITIONAL_INFO);
      if ((int) batch.get(JsonKey.STATUS) != ProjectUtil.ProgressStatus.COMPLETED.getValue()) {
        Map<String, Boolean> participants = (Map<String, Boolean>) batch.get(JsonKey.PARTICIPANT);
        if (participants == null) {
          participants = new HashMap<>();
        }
        for (Map.Entry<String, Boolean> entry : participants.entrySet()) {
          if (!entry.getValue()) {
            Timestamp ts = new Timestamp(new Date().getTime());
            Map<String, Object> userCourses = new HashMap<>();
            userCourses.put(JsonKey.USER_ID, entry.getKey());
            userCourses.put(JsonKey.BATCH_ID, batch.get(JsonKey.ID));
            userCourses.put(JsonKey.COURSE_ID, batch.get(JsonKey.COURSE_ID));
            userCourses.put(JsonKey.ID, generatePrimaryKey(userCourses));
            userCourses.put(JsonKey.CONTENT_ID, batch.get(JsonKey.COURSE_ID));
            userCourses.put(JsonKey.COURSE_ENROLL_DATE, ProjectUtil.getFormattedDate());
            userCourses.put(JsonKey.ACTIVE, ProjectUtil.ActiveStatus.ACTIVE.getValue());
            userCourses.put(JsonKey.STATUS, ProjectUtil.ProgressStatus.NOT_STARTED.getValue());
            userCourses.put(JsonKey.DATE_TIME, ts);
            userCourses.put(JsonKey.COURSE_PROGRESS, 0);
            userCourses.put(
                JsonKey.COURSE_LOGO_URL, additionalCourseInfo.get(JsonKey.COURSE_LOGO_URL));
            userCourses.put(JsonKey.COURSE_NAME, additionalCourseInfo.get(JsonKey.COURSE_NAME));
            userCourses.put(JsonKey.DESCRIPTION, additionalCourseInfo.get(JsonKey.DESCRIPTION));
            if (!StringUtils.isBlank(additionalCourseInfo.get(JsonKey.LEAF_NODE_COUNT))) {
              userCourses.put(
                  JsonKey.LEAF_NODE_COUNT,
                  Integer.parseInt("" + additionalCourseInfo.get(JsonKey.LEAF_NODE_COUNT)));
            }
            userCourses.put(JsonKey.TOC_URL, additionalCourseInfo.get(JsonKey.TOC_URL));
            try {
              cassandraOperation.insertRecord(
                  courseEnrollmentdbInfo.getKeySpace(),
                  courseEnrollmentdbInfo.getTableName(),
                  userCourses);
              // TODO: for some reason, ES indexing is failing with Timestamp value. need to
              // check
              // and correct it.

              // put logic here to generate the telemetry event for user update as batch ...

              // object of telemetry event...
              Map<String, Object> targetObject = new HashMap<>();
              List<Map<String, Object>> correlatedObject = new ArrayList<>();

              targetObject =
                  TelemetryUtil.generateTargetObject(
                      entry.getKey(), JsonKey.USER, JsonKey.CREATE, null);
              TelemetryUtil.generateCorrelatedObject(
                  entry.getKey(), JsonKey.USER, null, correlatedObject);
              TelemetryUtil.generateCorrelatedObject(
                  (String) batch.get(JsonKey.ID), JsonKey.BATCH, null, correlatedObject);
              TelemetryUtil.generateCorrelatedObject(
                  (String) batch.get(JsonKey.COURSE_ID), JsonKey.COURSE, null, correlatedObject);
              TelemetryUtil.telemetryProcessingCall(
                  userCourses, targetObject, correlatedObject, "AUDIT");

              userCourses.put(JsonKey.DATE_TIME, ProjectUtil.formatDate(ts));
              insertUserCoursesToES(userCourses);
              // update participant map value as true
              entry.setValue(true);
            } catch (Exception ex) {
              ProjectLogger.log("INSERT RECORD TO USER COURSES EXCEPTION ", ex);
            }
          }
        }
        ProjectLogger.log(
            "Adding participants to user course table completed", LoggerEnum.INFO.name());
        Map<String, Object> updatedBatch = new HashMap<>();
        updatedBatch.put(JsonKey.ID, batch.get(JsonKey.ID));
        updatedBatch.put(JsonKey.PARTICIPANT, participants);
        ProjectLogger.log(
            "Updating participants to batch course table started", LoggerEnum.INFO.name());
        cassandraOperation.updateRecord(
            courseBatchDBInfo.getKeySpace(), courseBatchDBInfo.getTableName(), updatedBatch);
        ProjectLogger.log(
            "Updating participants to batch course table completed", LoggerEnum.INFO.name());
      }
    }
  }

  private String generatePrimaryKey(Map<String, Object> req) {
    String userId = (String) req.get(JsonKey.USER_ID);
    String courseId = (String) req.get(JsonKey.COURSE_ID);
    String batchId = (String) req.get(JsonKey.BATCH_ID);
    return OneWayHashing.encryptVal(
        userId
            + JsonKey.PRIMARY_KEY_DELIMETER
            + courseId
            + JsonKey.PRIMARY_KEY_DELIMETER
            + batchId);
  }

  private void updateCourseStatusTable(List<String> ekStepResult) {
    ProjectLogger.log(
        "Updating Course status to course status table started", LoggerEnum.INFO.name());
    Map<String, Object> map = null;
    for (String courseId : ekStepResult) {
      map = new HashMap<>();
      map.put(JsonKey.ID, courseId);
      map.put(JsonKey.STATUS, ProjectUtil.CourseMgmtStatus.LIVE.ordinal());
      try {
        cassandraOperation.updateRecord(
            coursePublishDBInfo.getKeySpace(), coursePublishDBInfo.getTableName(), map);
        ProjectLogger.log(
            "Updating Course status to course status table completed", LoggerEnum.INFO.name());
      } catch (Exception ex) {
        ProjectLogger.log(ex.getMessage(), ex);
      }
    }
  }

  /**
   * This method will provide list of all those course ids , which we did the published but haven't
   * got the status back as live.
   *
   * @return List<String>
   */
  @SuppressWarnings("unchecked")
  private List<String> getAllUnPublishedCourseStatusId() {
    ProjectLogger.log("start of calling get unpublished course status==", LoggerEnum.INFO.name());
    List<String> ids = new ArrayList<>();
    Response response =
        cassandraOperation.getRecordsByProperty(
            coursePublishDBInfo.getKeySpace(),
            coursePublishDBInfo.getTableName(),
            JsonKey.STATUS,
            ProjectUtil.CourseMgmtStatus.DRAFT.ordinal());
    if (response != null && response.get(JsonKey.RESPONSE) != null) {
      Object obj = response.get(JsonKey.RESPONSE);
      if (obj != null && obj instanceof List) {
        List<Map<String, Object>> listOfMap = (List<Map<String, Object>>) obj;
        if (listOfMap != null) {
          for (Map<String, Object> map : listOfMap) {
            ids.add((String) map.get(JsonKey.ID));
          }
        }
      }
    }
    ProjectLogger.log(
        "end of calling get unpublished course status==" + ids, LoggerEnum.INFO.name());
    return ids;
  }

  /**
   * This method will verify call the EKStep to know the course published status
   *
   * @param ids List<String>
   * @return List<String>
   */
  @SuppressWarnings("unchecked")
  private List<String> getAllPublishedCourseListFromEKStep(List<String> ids) {
    ProjectLogger.log("fetching course details from Ekstep start");
    List<String> liveCourseIds = new ArrayList<>();
    StringBuilder identifier = new StringBuilder("[ ");
    for (int i = 0; i < ids.size(); i++) {
      if (i == 0) {
        identifier.append(" \"" + ids.get(i) + "\"");
      } else {
        identifier.append(" ,\"" + ids.get(i) + "\"");
      }
    }
    identifier.append(" ] ");
    Map<String, Object> result =
        EkStepRequestUtil.searchContent(
            requestData.replace("dataVal", identifier.toString()),
            CourseBatchSchedulerUtil.headerMap);
    List<Map<String, Object>> reslt = (List<Map<String, Object>>) result.get(JsonKey.CONTENTS);
    if (reslt != null)
      for (Map<String, Object> map : reslt) {
        String status = (String) map.get(JsonKey.STATUS);
        if (ProjectUtil.CourseMgmtStatus.LIVE.getValue().equalsIgnoreCase(status)) {
          liveCourseIds.add((String) map.get(JsonKey.IDENTIFIER));
        }
      }
    ProjectLogger.log("fetching course details from Ekstep completed", LoggerEnum.INFO.name());
    return liveCourseIds;
  }

  private void insertUserCoursesToES(Map<String, Object> courseMap) {
    Request request = new Request();
    request.setOperation(ActorOperations.INSERT_USR_COURSES_INFO_ELASTIC.getValue());
    request.getRequest().put(JsonKey.USER_COURSES, courseMap);
    tellToBGRouter(request);
  }

  public static void main(String[] args) {
    CoursePublishedUpdate coursePublishedUpdate = new CoursePublishedUpdate();
    try {
      coursePublishedUpdate.execute(null);
    } catch (JobExecutionException e) {
      ProjectLogger.log(e.getMessage(), e);
    }
  }

  private static Map<String, Object> genarateTelemetryInfoForError(String errType) {

    Map<String, Object> map = new HashMap<>();
    Map<String, Object> contextInfo = TelemetryUtil.getTelemetryContext();
    Map<String, Object> params = new HashMap<>();
    params.put("errtype", errType);

    map.put(JsonKey.CONTEXT, contextInfo);
    map.put(JsonKey.PARAMS, params);
    return map;
  }

  private Map<String, Object> genarateLogInfo(String logType, String message) {
    Map<String, Object> info = new HashMap<>();
    info.put(JsonKey.LOG_TYPE, logType);
    long startTime = System.currentTimeMillis();
    info.put(JsonKey.START_TIME, startTime);
    info.put(JsonKey.MESSAGE, message);
    info.put(JsonKey.LOG_LEVEL, JsonKey.INFO);
    return info;
  }
}
