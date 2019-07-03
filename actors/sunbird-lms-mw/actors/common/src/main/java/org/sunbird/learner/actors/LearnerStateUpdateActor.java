package org.sunbird.learner.actors;

import static org.sunbird.common.models.util.ProjectUtil.isNotNull;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.ProjectUtil.ProgressStatus;
import org.sunbird.common.models.util.TelemetryEnvKey;
import org.sunbird.common.models.util.datasecurity.OneWayHashing;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.telemetry.util.TelemetryUtil;

/**
 * This actor to handle learner's state update operation .
 *
 * @author Manzarul
 * @author Arvind
 */
@ActorConfig(
  tasks = {"addContent"},
  asyncTasks = {}
)
public class LearnerStateUpdateActor extends BaseActor {

  private static final String CONTENT_STATE_INFO = "contentStateInfo";

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();

  /**
   * Receives the actor message and perform the add content operation .
   *
   * @param request Request
   */
  @SuppressWarnings("unchecked")
  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    // set request id fto thread loacl...
    ExecutionContext.setRequestId(request.getRequestId());

    Response response = new Response();
    if (request.getOperation().equalsIgnoreCase(ActorOperations.ADD_CONTENT.getValue())) {
      Util.DbInfo dbInfo = Util.dbInfoMap.get(JsonKey.LEARNER_CONTENT_DB);
      Util.DbInfo batchdbInfo = Util.dbInfoMap.get(JsonKey.COURSE_BATCH_DB);
      // objects of telemetry event...
      Map<String, Object> targetObject = null;
      List<Map<String, Object>> correlatedObject = null;

      String userId = (String) request.getRequest().get(JsonKey.USER_ID);
      List<Map<String, Object>> requestedcontentList =
          (List<Map<String, Object>>) request.getRequest().get(JsonKey.CONTENTS);
      CopyOnWriteArrayList<Map<String, Object>> contentList =
          new CopyOnWriteArrayList<>(requestedcontentList);
      request.getRequest().put(JsonKey.CONTENTS, contentList);
      // map to hold the status of requested state of contents
      Map<String, Integer> contentStatusHolder = new HashMap<>();
      List<String> validBatchIds = new ArrayList<>();
      List<String> invalidBatchIds = new ArrayList<>();
      if (!(contentList.isEmpty())) {
        ProjectLogger.log(
            "LearnerStateUpdateActor:onReceive content state update method called for user and total content "
                + userId
                + " Contnet length="
                + contentList.size(),
            LoggerEnum.INFO.name());
        int count = 0;
        for (Map<String, Object> map : contentList) {
          // replace the course id (equivalent to Ekstep content id) with One way hashing
          // userId#courseId , bcoz in cassndra we are saving course id as userId#courseId
          String batchId = (String) map.get(JsonKey.BATCH_ID);
          boolean flag = true;

          // code to validate the whether request for valid batch range(start and end
          // date)
          if (!(StringUtils.isBlank(batchId)) && !validBatchIds.contains(batchId)) {
            if (invalidBatchIds.contains(batchId)) {
              contentList.remove(map);
              continue;
            }
            Response batchResponse =
                cassandraOperation.getRecordById(
                    batchdbInfo.getKeySpace(), batchdbInfo.getTableName(), batchId);
            List<Map<String, Object>> batches =
                (List<Map<String, Object>>) batchResponse.getResult().get(JsonKey.RESPONSE);
            if (batches.isEmpty()) {
              invalidBatchIds.add(batchId);
              flag = false;
            } else {
              Map<String, Object> batchInfo = batches.get(0);
              flag = validateBatchRange(batchInfo);
              if (flag) {
                validBatchIds.add(batchId);
              }
            }

            if (!flag) {
              response
                  .getResult()
                  .put((String) map.get(JsonKey.CONTENT_ID), "BATCH NOT STARTED OR BATCH CLOSED");
              contentList.remove(map);
              continue;
            }
          }
          map.putIfAbsent(JsonKey.COURSE_ID, JsonKey.NOT_AVAILABLE);
          preOperation(map, userId, contentStatusHolder);
          map.put(JsonKey.USER_ID, userId);
          map.put(JsonKey.DATE_TIME, new Timestamp(new Date().getTime()));
          try {
            ProjectLogger.log(
                "LearnerStateUpdateActor:onReceive: map  " + map, LoggerEnum.INFO.name());
            if (count == 0
                && map.get(JsonKey.COURSE_ID) != null
                && !(JsonKey.NOT_AVAILABLE.equalsIgnoreCase((String) map.get(JsonKey.COURSE_ID)))) {
              updateUserCourseStatus(
                  generateUserCoursesPrimaryKey(map),
                  ProjectUtil.BulkProcessStatus.IN_PROGRESS.name());
              count++;
            }
            cassandraOperation.upsertRecord(dbInfo.getKeySpace(), dbInfo.getTableName(), map);
            response.getResult().put((String) map.get(JsonKey.CONTENT_ID), JsonKey.SUCCESS);
          } catch (Exception e) {
            ProjectLogger.log(
                "LearnerStateUpdateActor:onReceive Error occured during db update:" + e,
                LoggerEnum.ERROR.name());
            response.getResult().put((String) map.get(JsonKey.CONTENT_ID), JsonKey.FAILED);
            contentList.remove(map);
            continue;
          }
          // create telemetry for user for each content ...
          try {
            targetObject =
                TelemetryUtil.generateTargetObject(
                    (String) map.get(JsonKey.CONTENT_ID),
                    StringUtils.capitalize(JsonKey.CONTENT),
                    JsonKey.CREATE,
                    null);
            // since this event will generate multiple times so nedd to recreate correlated
            // objects every time ...
            correlatedObject = new ArrayList<>();
            TelemetryUtil.generateCorrelatedObject(
                (String) map.get(JsonKey.COURSE_ID), JsonKey.COURSE, null, correlatedObject);
            TelemetryUtil.generateCorrelatedObject(
                (String) map.get(JsonKey.BATCH_ID), TelemetryEnvKey.BATCH, null, correlatedObject);
            Map<String, String> rollUp = new HashMap<>();
            rollUp.put("l1", (String) map.get(JsonKey.COURSE_ID));
            TelemetryUtil.addTargetObjectRollUp(rollUp, targetObject);
            TelemetryUtil.telemetryProcessingCall(
                request.getRequest(), targetObject, correlatedObject);
          } catch (Exception ex) {
            ProjectLogger.log(
                "LearnerStateUpdateActor:onReceive Error occured during telemetry:" + ex,
                LoggerEnum.ERROR.name());
          }
        }
      } else {
        ProjectLogger.log(
            "LearnerStateUpdateActor:onReceive content state update method called for user and total content "
                + userId
                + " Contnet length= 0 ",
            LoggerEnum.INFO.name());
      }
      request.getRequest().put(CONTENT_STATE_INFO, contentStatusHolder);
      updateUserCourses(request);
      sender().tell(response, self());
    } else {
      onReceiveUnsupportedOperation(request.getOperation());
    }
  }

  private void updateUserCourses(Request request) {
    // get the list of content objects
    List<Map<String, Object>> contentList =
        (List<Map<String, Object>>) request.getRequest().get(JsonKey.CONTENTS);
    // get the content state info
    Map<String, Integer> contentStateInfo =
        (Map<String, Integer>) request.get(this.CONTENT_STATE_INFO);
    Map<String, Object> temp = new HashMap<>();
    ProjectLogger.log(
        "LearnerStateUpdateActor:updateUserCourses method call started :", LoggerEnum.INFO.name());
    for (Map<String, Object> map : contentList) {
      String contentid = (String) map.get(JsonKey.ID);
      if (map.get(JsonKey.COURSE_ID) != null) {
        // generate course table primary key as hash of userid##courseid##batchId
        String primary = generateUserCoursesPrimaryKey(map);
        if (temp.containsKey(primary)) {
          Map<String, Object> innerMap = (Map<String, Object>) temp.get(primary);
          innerMap.put(
              JsonKey.CONTENT,
              getLatestContent(
                  (Map<String, Object>)
                      ((Map<String, Object>) temp.get(primary)).get(JsonKey.CONTENT),
                  map));
          if (((int) map.get(JsonKey.COMPLETED_COUNT)) == 1
              && contentStateInfo.get(contentid) == 2) {
            innerMap.put(JsonKey.PROGRESS, (Integer) innerMap.get(JsonKey.PROGRESS) + 1);
          }
        } else {
          Map<String, Object> innerMap = new HashMap<>();
          innerMap.put(JsonKey.CONTENT, map);
          if (((int) map.get(JsonKey.COMPLETED_COUNT)) == 1
              && contentStateInfo.get(contentid) == 2) {
            innerMap.put(JsonKey.PROGRESS, Integer.valueOf(1));
          } else {
            innerMap.put(JsonKey.PROGRESS, Integer.valueOf(0));
          }
          temp.put(primary, innerMap);
        }
      } else {
        ProjectLogger.log(
            "LearnerStateUpdateActor:updateUserCourses Courseid is not present in request: "
                + map.get(JsonKey.BATCH_ID)
                + " userId "
                + map.get(JsonKey.USER_ID),
            LoggerEnum.INFO.name());
      }
    }
    // logic to update the course
    updateCourse(temp, contentStateInfo);
  }

  /**
   * Method to update the course_enrollment with the latest content information
   *
   * @param temp Map<String, Object>
   * @param contentStateInfo Map<String, Integer>
   */
  @SuppressWarnings("unchecked")
  private void updateCourse(Map<String, Object> temp, Map<String, Integer> contentStateInfo) {
    Util.DbInfo dbInfo = Util.dbInfoMap.get(JsonKey.LEARNER_COURSE_DB);
    ProjectLogger.log(
        "LearnerStateUpdateActor:updateCourse method called started:", LoggerEnum.INFO.name());
    for (Map.Entry<String, Object> entry : temp.entrySet()) {
      String key = entry.getKey();
      Map<String, Object> value = (Map<String, Object>) entry.getValue();
      Response response =
          cassandraOperation.getRecordById(dbInfo.getKeySpace(), dbInfo.getTableName(), key);
      List<Map<String, Object>> courseList =
          (List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE);
      Map<String, Object> course = null;
      if (null != courseList && !courseList.isEmpty()) {
        Map<String, Object> updateDb = new HashMap<>();
        course = courseList.get(0);
        Integer courseProgress = 0;
        if (ProjectUtil.isNotNull(course.get(JsonKey.COURSE_PROGRESS))) {
          courseProgress = (Integer) course.get(JsonKey.COURSE_PROGRESS);
        }
        courseProgress = courseProgress + (Integer) value.get("progress");
        // update status on basis of leaf node count and progress ---
        if (course.containsKey(JsonKey.LEAF_NODE_COUNT)
            && ProjectUtil.isNotNull(course.get(JsonKey.LEAF_NODE_COUNT))) {
          Integer leafNodeCount = (Integer) course.get(JsonKey.LEAF_NODE_COUNT);
          courseProgress = courseProgress > leafNodeCount ? leafNodeCount : courseProgress;
          if (0 == leafNodeCount || (leafNodeCount > courseProgress)) {
            updateDb.put(JsonKey.STATUS, ProjectUtil.ProgressStatus.STARTED.getValue());
          } else {
            if (ProgressStatus.COMPLETED.getValue() != (Integer) course.get(JsonKey.STATUS)) {
              updateDb.put(JsonKey.COMPLETED_ON, new Timestamp(new Date().getTime()));
            }
            updateDb.put(JsonKey.STATUS, ProgressStatus.COMPLETED.getValue());
          }
        } else if (ProjectUtil.isNull(course.get(JsonKey.LEAF_NODE_COUNT))) {
          updateDb.put(JsonKey.STATUS, ProjectUtil.ProgressStatus.STARTED.getValue());
        }
        Timestamp ts = new Timestamp(new Date().getTime());
        updateDb.put(JsonKey.ID, course.get(JsonKey.ID));
        updateDb.put(JsonKey.COURSE_PROGRESS, courseProgress);
        updateDb.put(JsonKey.DATE_TIME, ts);
        updateDb.put(
            JsonKey.LAST_READ_CONTENTID,
            ((Map<String, Object>) value.get("content")).get(JsonKey.CONTENT_ID));
        updateDb.put(
            JsonKey.LAST_READ_CONTENT_STATUS,
            (contentStateInfo.get(((Map<String, Object>) value.get("content")).get(JsonKey.ID))));
        updateDb.put(JsonKey.PROCESSING_STATUS, ProjectUtil.BulkProcessStatus.COMPLETED.name());
        try {
          cassandraOperation.upsertRecord(dbInfo.getKeySpace(), dbInfo.getTableName(), updateDb);
          updateUserCourseStatus(
              (String) course.get(JsonKey.ID), ProjectUtil.BulkProcessStatus.COMPLETED.name());
          ProjectLogger.log(
              "LearnerStateUpdateActor:updateCourse user courses DB updated successfully : ",
              LoggerEnum.INFO.name());
          updateDb.put(JsonKey.BATCH_ID, course.get(JsonKey.BATCH_ID));
          updateDb.put(JsonKey.USER_ID, course.get(JsonKey.USER_ID));
          updateDb.put(JsonKey.DATE_TIME, ProjectUtil.formatDate(ts));
          if (updateDb.containsKey(JsonKey.COMPLETED_ON)) {
            updateDb.put(
                JsonKey.COMPLETED_ON,
                ProjectUtil.formatDate((Date) updateDb.get(JsonKey.COMPLETED_ON)));
          }
          updateUserCoursesToES(updateDb);
        } catch (Exception ex) {
          ProjectLogger.log(
              "LearnerStateUpdateActor:updateCourse exception occured: " + ex,
              LoggerEnum.ERROR.name());
        }
      } else {
        ProjectLogger.log(
            "LearnerStateUpdateActor:updateCourse CourseList is empty or null: ",
            LoggerEnum.ERROR.name());
      }
    }
  }

  private Map<String, Object> getLatestContent(
      Map<String, Object> current, Map<String, Object> next) {
    SimpleDateFormat simpleDateFormat = ProjectUtil.getDateFormatter();
    simpleDateFormat.setLenient(false);
    if (current.get(JsonKey.LAST_ACCESS_TIME) == null
        && next.get(JsonKey.LAST_ACCESS_TIME) == null) {
      return next;
    } else if (current.get(JsonKey.LAST_ACCESS_TIME) == null) {
      return next;
    } else if (next.get(JsonKey.LAST_ACCESS_TIME) == null) {
      return current;
    }
    try {
      Date currentUpdatedTime =
          simpleDateFormat.parse((String) current.get(JsonKey.LAST_ACCESS_TIME));
      Date nextUpdatedTime = simpleDateFormat.parse((String) next.get(JsonKey.LAST_ACCESS_TIME));
      if (currentUpdatedTime.after(nextUpdatedTime)) {
        return current;
      } else {
        return next;
      }
    } catch (ParseException e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    return null;
  }

  private boolean validateBatchRange(Map<String, Object> batchInfo) {

    String start = (String) batchInfo.get(JsonKey.START_DATE);
    String end = (String) batchInfo.get(JsonKey.END_DATE);

    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    Date todaydate = null;
    Date startDate = null;
    Date endDate = null;

    try {
      todaydate = format.parse(format.format(new Date()));
      startDate = format.parse(start);
      endDate = null;
      if (!(StringUtils.isBlank(end))) {
        endDate = format.parse(end);
      }
    } catch (ParseException e) {
      ProjectLogger.log("Date parse exception while parsing batch start and end date", e);
      return false;
    }

    if (todaydate.compareTo(startDate) < 0) {
      return false;
    }

    return (!(null != endDate && todaydate.compareTo(endDate) > 0));
  }

  /**
   * Method te perform the per operation on contents like setting the status , last completed and
   * access time etc.
   */
  @SuppressWarnings("unchecked")
  private void preOperation(
      Map<String, Object> req, String userId, Map<String, Integer> contentStateHolder)
      throws ParseException {
    ProjectLogger.log(
        "LearnerStateUpdateActor:preOperation method called.", LoggerEnum.INFO.name());
    SimpleDateFormat simpleDateFormat = ProjectUtil.getDateFormatter();
    simpleDateFormat.setLenient(false);

    Util.DbInfo dbInfo = Util.dbInfoMap.get(JsonKey.LEARNER_CONTENT_DB);
    req.put(JsonKey.ID, generatePrimaryKey(req, userId));
    contentStateHolder.put(
        (String) req.get(JsonKey.ID), ((BigInteger) req.get(JsonKey.STATUS)).intValue());
    Response response =
        cassandraOperation.getRecordById(
            dbInfo.getKeySpace(), dbInfo.getTableName(), (String) req.get(JsonKey.ID));

    List<Map<String, Object>> resultList =
        (List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE);

    if (!(resultList.isEmpty())) {
      Map<String, Object> result = resultList.get(0);
      int currentStatus = (int) result.get(JsonKey.STATUS);
      int requestedStatus = ((BigInteger) req.get(JsonKey.STATUS)).intValue();

      Integer currentProgressStatus = 0;
      if (isNotNull(result.get(JsonKey.CONTENT_PROGRESS))) {
        currentProgressStatus = (Integer) result.get(JsonKey.CONTENT_PROGRESS);
      }
      if (isNotNull(req.get(JsonKey.CONTENT_PROGRESS))) {
        Integer requestedProgressStatus =
            ((BigInteger) req.get(JsonKey.CONTENT_PROGRESS)).intValue();
        if (requestedProgressStatus > currentProgressStatus) {
          req.put(JsonKey.CONTENT_PROGRESS, requestedProgressStatus);
        } else {
          req.put(JsonKey.CONTENT_PROGRESS, currentProgressStatus);
        }
      } else {
        req.put(JsonKey.CONTENT_PROGRESS, currentProgressStatus);
      }

      Date accessTime = parseDate(result.get(JsonKey.LAST_ACCESS_TIME), simpleDateFormat);
      Date requestAccessTime = parseDate(req.get(JsonKey.LAST_ACCESS_TIME), simpleDateFormat);

      Date completedDate = parseDate(result.get(JsonKey.LAST_COMPLETED_TIME), simpleDateFormat);
      Date requestCompletedTime = parseDate(req.get(JsonKey.LAST_COMPLETED_TIME), simpleDateFormat);

      int completedCount;
      if (!(isNullCheck(result.get(JsonKey.COMPLETED_COUNT)))) {
        completedCount = (int) result.get(JsonKey.COMPLETED_COUNT);
      } else {
        completedCount = 0;
      }
      int viewCount;
      if (!(isNullCheck(result.get(JsonKey.VIEW_COUNT)))) {
        viewCount = (int) result.get(JsonKey.VIEW_COUNT);
      } else {
        viewCount = 0;
      }

      if (requestedStatus >= currentStatus) {
        req.put(JsonKey.STATUS, requestedStatus);
        if (requestedStatus == 2) {
          req.put(JsonKey.COMPLETED_COUNT, completedCount + 1);
          req.put(JsonKey.LAST_COMPLETED_TIME, compareTime(completedDate, requestCompletedTime));
        } else {
          req.put(JsonKey.COMPLETED_COUNT, completedCount);
        }
        req.put(JsonKey.VIEW_COUNT, viewCount + 1);
        req.put(JsonKey.LAST_ACCESS_TIME, compareTime(accessTime, requestAccessTime));
        req.put(JsonKey.LAST_UPDATED_TIME, ProjectUtil.getFormattedDate());

      } else {
        req.put(JsonKey.STATUS, currentStatus);
        req.put(JsonKey.VIEW_COUNT, viewCount + 1);
        req.put(JsonKey.LAST_ACCESS_TIME, compareTime(accessTime, requestAccessTime));
        req.put(JsonKey.LAST_UPDATED_TIME, ProjectUtil.getFormattedDate());
        req.put(JsonKey.COMPLETED_COUNT, completedCount);
      }
      ProjectLogger.log(
          "LearnerStateUpdateActor:preOperation User already read this content."
              + req.get(JsonKey.ID),
          LoggerEnum.INFO.name());
    } else {
      ProjectLogger.log(
          "LearnerStateUpdateActor:preOperation User is reading this content first time."
              + req.get(JsonKey.ID),
          LoggerEnum.INFO.name());
      // IT IS NEW CONTENT SIMPLY ADD IT
      Date requestCompletedTime = parseDate(req.get(JsonKey.LAST_COMPLETED_TIME), simpleDateFormat);
      if (null != req.get(JsonKey.STATUS)) {
        int requestedStatus = ((BigInteger) req.get(JsonKey.STATUS)).intValue();
        req.put(JsonKey.STATUS, requestedStatus);
        if (requestedStatus == 2) {
          req.put(JsonKey.COMPLETED_COUNT, 1);
          req.put(JsonKey.LAST_COMPLETED_TIME, compareTime(null, requestCompletedTime));
        } else {
          req.put(JsonKey.COMPLETED_COUNT, 0);
        }

      } else {
        req.put(JsonKey.STATUS, ProjectUtil.ProgressStatus.NOT_STARTED.getValue());
        req.put(JsonKey.COMPLETED_COUNT, 0);
      }

      int progressStatus = 0;
      if (isNotNull(req.get(JsonKey.CONTENT_PROGRESS))) {
        progressStatus = ((BigInteger) req.get(JsonKey.CONTENT_PROGRESS)).intValue();
      }
      req.put(JsonKey.CONTENT_PROGRESS, progressStatus);

      req.put(JsonKey.VIEW_COUNT, 1);
      Date requestAccessTime = parseDate(req.get(JsonKey.LAST_ACCESS_TIME), simpleDateFormat);

      req.put(JsonKey.LAST_UPDATED_TIME, ProjectUtil.getFormattedDate());

      if (requestAccessTime != null) {
        req.put(JsonKey.LAST_ACCESS_TIME, req.get(JsonKey.LAST_ACCESS_TIME));
      } else {
        req.put(JsonKey.LAST_ACCESS_TIME, ProjectUtil.getFormattedDate());
      }
    }
    ProjectLogger.log(
        "LearnerStateUpdateActor:preOperation  end for content ." + req.get(JsonKey.ID),
        LoggerEnum.INFO.name());
  }

  private Date parseDate(Object obj, SimpleDateFormat formatter) throws ParseException {
    if (null == obj || ((String) obj).equalsIgnoreCase(JsonKey.NULL)) {
      return null;
    }
    Date date;
    try {
      date = formatter.parse((String) obj);
    } catch (ParseException ex) {
      ProjectLogger.log(ex.getMessage(), ex);
      throw new ProjectCommonException(
          ResponseCode.invalidDateFormat.getErrorCode(),
          ResponseCode.invalidDateFormat.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    return date;
  }

  private String compareTime(Date currentValue, Date requestedValue) {
    SimpleDateFormat simpleDateFormat = ProjectUtil.getDateFormatter();
    simpleDateFormat.setLenient(false);
    if (currentValue == null && requestedValue == null) {
      return ProjectUtil.getFormattedDate();
    } else if (currentValue == null) {
      return simpleDateFormat.format(requestedValue);
    } else if (null == requestedValue) {
      return simpleDateFormat.format(currentValue);
    }
    return (requestedValue.after(currentValue)
        ? simpleDateFormat.format(requestedValue)
        : simpleDateFormat.format(currentValue));
  }

  private String generatePrimaryKey(Map<String, Object> req, String userId) {
    String contentId = (String) req.get(JsonKey.CONTENT_ID);
    String courseId = (String) req.get(JsonKey.COURSE_ID);
    String batchId = (String) req.get(JsonKey.BATCH_ID);
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

  private boolean isNullCheck(Object obj) {
    return null == obj;
  }

  /**
   * This method will combined map values with delimiter and create an encrypted key.
   *
   * @param req Map<String , Object>
   * @return String encrypted value
   */
  private String generateUserCoursesPrimaryKey(Map<String, Object> req) {
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

  private void updateUserCoursesToES(Map<String, Object> courseMap) {
    Request request = new Request();
    request.setOperation(ActorOperations.UPDATE_USR_COURSES_INFO_ELASTIC.getValue());
    request.getRequest().put(JsonKey.USER_COURSES, courseMap);
    try {
      ProjectLogger.log(
          "LearnerStateUpdateActor:updateUserCoursesToES call for background to save in ES:",
          LoggerEnum.INFO.name());
      tellToAnother(request);
    } catch (Exception ex) {
      ProjectLogger.log(
          "LearnerStateUpdateActor:updateUserCoursesToES Exception occurred during saving user count to Es : "
              + ex,
          LoggerEnum.ERROR.name());
    }
  }

  private void updateUserCourseStatus(String key, String status) {
    Util.DbInfo dbInfo = Util.dbInfoMap.get(JsonKey.LEARNER_COURSE_DB);
    Map<String, Object> request = new HashMap<>();
    request.put(JsonKey.PROCESSING_STATUS, status);
    request.put(JsonKey.ID, key);
    Response response =
        cassandraOperation.updateRecord(dbInfo.getKeySpace(), dbInfo.getTableName(), request);
    if (response != null) {
      ProjectLogger.log(
          "LearnerStateUpdateActor:updateUserCourseStatus course process status updated :"
              + "status= "
              + status
              + " "
              + response.get(JsonKey.RESPONSE),
          LoggerEnum.INFO.name());
    } else {
      ProjectLogger.log(
          "LearnerStateUpdateActor:updateUserCourseStatus course process status update fail :"
              + "status= "
              + status,
          LoggerEnum.INFO.name());
    }
  }
}
