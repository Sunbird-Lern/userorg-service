package org.sunbird.learner.actors.coursebatch.service;

import java.util.*;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.datasecurity.OneWayHashing;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.dto.SearchDTO;
import org.sunbird.learner.actors.coursebatch.dao.UserCoursesDao;
import org.sunbird.learner.actors.coursebatch.dao.impl.UserCoursesDaoImpl;
import org.sunbird.models.user.courses.UserCourses;
import scala.concurrent.Future;

public class UserCoursesService {
  private UserCoursesDao userCourseDao = UserCoursesDaoImpl.getInstance();
  private static ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);
  public static final String UNDERSCORE = "_";

  protected Integer CASSANDRA_BATCH_SIZE = getBatchSize(JsonKey.CASSANDRA_WRITE_BATCH_SIZE);

  public static String generateUserCourseESId(String batchId, String userId) {
    return batchId + UNDERSCORE + userId;
  }

  public static void validateUserUnenroll(UserCourses userCourseResult) {
    if (userCourseResult == null || !userCourseResult.isActive()) {
      ProjectLogger.log(
          "UserCoursesService:validateUserUnenroll: User is not enrolled yet",
          LoggerEnum.INFO.name());
      throw new ProjectCommonException(
          ResponseCode.userNotEnrolledCourse.getErrorCode(),
          ResponseCode.userNotEnrolledCourse.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    if (userCourseResult.getStatus() == ProjectUtil.ProgressStatus.COMPLETED.getValue()) {
      ProjectLogger.log(
          "UserCoursesService:validateUserUnenroll: User already completed the course");
      throw new ProjectCommonException(
          ResponseCode.userAlreadyCompletedCourse.getErrorCode(),
          ResponseCode.userAlreadyCompletedCourse.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  public static String getPrimaryKey(Map<String, Object> userCourseMap) {
    String userId = (String) userCourseMap.get(JsonKey.USER_ID);
    String courseId = (String) userCourseMap.get(JsonKey.COURSE_ID);
    String batchId = (String) userCourseMap.get(JsonKey.BATCH_ID);
    return getPrimaryKey(userId, courseId, batchId);
  }

  public static String getPrimaryKey(String userId, String courseId, String batchId) {
    return OneWayHashing.encryptVal(
        userId
            + JsonKey.PRIMARY_KEY_DELIMETER
            + courseId
            + JsonKey.PRIMARY_KEY_DELIMETER
            + batchId);
  }

  public void enroll(String batchId, String courseId, List<String> userIds) {
    Integer count = 0;

    List<Map<String, Object>> records = new ArrayList<>();
    Map<String, Object> userCoursesCommon = new HashMap<>();
    userCoursesCommon.put(JsonKey.BATCH_ID, batchId);
    userCoursesCommon.put(JsonKey.COURSE_ID, courseId);
    userCoursesCommon.put(JsonKey.COURSE_ENROLL_DATE, ProjectUtil.getFormattedDate());
    userCoursesCommon.put(JsonKey.ACTIVE, ProjectUtil.ActiveStatus.ACTIVE.getValue());
    userCoursesCommon.put(JsonKey.STATUS, ProjectUtil.ProgressStatus.NOT_STARTED.getValue());
    userCoursesCommon.put(JsonKey.COURSE_PROGRESS, 0);

    for (String userId : userIds) {
      Map<String, Object> userCourses = new HashMap<>();
      userCourses.put(JsonKey.USER_ID, userId);
      userCourses.putAll(userCoursesCommon);

      count++;
      records.add(userCourses);
      if (count > CASSANDRA_BATCH_SIZE) {
        performBatchInsert(records);
        syncUsersToES(records);
        records.clear();
        count = 0;
      }
      if (count != 0) {
        performBatchInsert(records);
        syncUsersToES(records);
        records.clear();
        count = 0;
      }
    }
  }

  private void syncUsersToES(List<Map<String, Object>> records) {

    for (Map<String, Object> userCourses : records) {
      sync(
          userCourses,
          (String) userCourses.get(JsonKey.BATCH_ID),
          (String) userCourses.get(JsonKey.USER_ID));
    }
  }

  protected void performBatchInsert(List<Map<String, Object>> records) {
    try {
      userCourseDao.batchInsert(records);
    } catch (Exception ex) {
      ProjectLogger.log(
          "UserCoursesService:performBatchInsert: Performing retry due to exception = "
              + ex.getMessage(),
          LoggerEnum.ERROR);
      for (Map<String, Object> task : records) {
        try {
          userCourseDao.insert(task);
        } catch (Exception exception) {
          ProjectLogger.log(
              "UserCoursesService:performBatchInsert: Exception occurred with error message = "
                  + ex.getMessage()
                  + " for ID = "
                  + task.get(JsonKey.ID),
              exception);
        }
      }
    }
  }

  public void unenroll(String batchId, String userId) {
    UserCourses userCourses = userCourseDao.read(batchId, userId);
    validateUserUnenroll(userCourses);
    Map<String, Object> updateAttributes = new HashMap<>();
    updateAttributes.put(JsonKey.ACTIVE, ProjectUtil.ActiveStatus.INACTIVE.getValue());
    userCourseDao.update(userCourses.getBatchId(), userCourses.getUserId(), updateAttributes);
    sync(updateAttributes, userCourses.getBatchId(), userCourses.getUserId());
  }

  public Map<String, Object> getActiveUserCourses(String userId) {
    Map<String, Object> filter = new HashMap<>();
    filter.put(JsonKey.USER_ID, userId);
    filter.put(JsonKey.ACTIVE, ProjectUtil.ActiveStatus.ACTIVE.getValue());
    SearchDTO searchDto = new SearchDTO();
    searchDto.getAdditionalProperties().put(JsonKey.FILTERS, filter);
    Future<Map<String, Object>> resultF =
        esService.search(searchDto, ProjectUtil.EsType.usercourses.getTypeName());
    Map<String, Object> result =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
    return result;
  }

  public static void sync(Map<String, Object> courseMap, String batchId, String userId) {
    String id = generateUserCourseESId(batchId, userId);
    Future<Boolean> responseF =
        esService.upsert(ProjectUtil.EsType.usercourses.getTypeName(), id, courseMap);
    boolean response = (boolean) ElasticSearchHelper.getResponseFromFuture(responseF);
    ProjectLogger.log(
        "UserCoursesService:sync: Sync user courses for ID = " + id + " response = " + response,
        LoggerEnum.INFO.name());
  }

  public List<String> getEnrolledUserFromBatch(String batchId) {

    return userCourseDao.getAllActiveUserOfBatch(batchId);
  }

  public Integer getBatchSize(String key) {
    Integer batchSize = ProjectUtil.DEFAULT_BATCH_SIZE;
    try {
      batchSize = Integer.parseInt(ProjectUtil.getConfigValue(key));
    } catch (Exception ex) {
      ProjectLogger.log(
          "UserCoursesService:getBatchSize: Failed to read cassandra batch size for " + key, ex);
    }
    return batchSize;
  }

  public List<String> getParticipantsList(String batchId, boolean active) {
    return userCourseDao.getBatchParticipants(batchId, active);
  }
}
