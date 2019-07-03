package org.sunbird.learner.actors;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.ProjectUtil.ProgressStatus;
import org.sunbird.common.models.util.datasecurity.OneWayHashing;
import org.sunbird.common.request.Request;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;

/**
 * This will updated user learner state activity. example what was the last accessed content. how
 * much percentage is completed, what is the state of content.
 *
 * @author arvind
 */
@ActorConfig(
  tasks = {},
  asyncTasks = {"updateLearnerState"}
)
public class LeanerStateUpdateBackGroundActor extends BaseActor {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private static final String CONTENT_STATE_INFO = "contentStateInfo";

  @Override
  public void onReceive(Request request) throws Throwable {
    ProjectLogger.log("LeanerStateUpdateBackGroundActor  onReceive called", LoggerEnum.INFO.name());
    String operation = request.getOperation();
    switch (operation) {
      case "updateLearnerState":
        updateLearnerState(request);
        break;
      default:
        onReceiveUnsupportedOperation("updateLearnerState");
    }
  }

  private void updateLearnerState(Object message) throws Throwable {
    if (message instanceof Request) {
      Request req = (Request) message;
      // get the list of content objects

      List<Map<String, Object>> contentList =
          (List<Map<String, Object>>) req.getRequest().get(JsonKey.CONTENTS);
      // get the content state info
      Map<String, Integer> contentStateInfo =
          (Map<String, Integer>) req.get(this.CONTENT_STATE_INFO);
      Map<String, Object> temp = new HashMap<>();
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
        }
      }
      // logic to update the course
      updateCourse(temp, contentStateInfo);
    }
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
        try {
          cassandraOperation.upsertRecord(dbInfo.getKeySpace(), dbInfo.getTableName(), updateDb);
          updateDb.put(JsonKey.BATCH_ID, course.get(JsonKey.BATCH_ID));
          updateDb.put(JsonKey.USER_ID, course.get(JsonKey.USER_ID));
          updateDb.put(JsonKey.DATE_TIME, ProjectUtil.formatDate(ts));
          updateUserCoursesToES(updateDb);
        } catch (Exception ex) {
          ProjectLogger.log(ex.getMessage(), ex);
        }
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
      tellToAnother(request);
    } catch (Exception ex) {
      ProjectLogger.log("Exception Occurred during saving user count to Es : ", ex);
    }
  }
}
