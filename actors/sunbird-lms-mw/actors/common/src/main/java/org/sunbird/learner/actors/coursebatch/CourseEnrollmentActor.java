package org.sunbird.learner.actors.coursebatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.ProjectUtil.EnrolmentType;
import org.sunbird.common.models.util.ProjectUtil.ProgressStatus;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.models.util.TelemetryEnvKey;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.learner.actors.coursebatch.dao.CourseBatchDao;
import org.sunbird.learner.actors.coursebatch.dao.UserCoursesDao;
import org.sunbird.learner.actors.coursebatch.dao.impl.CourseBatchDaoImpl;
import org.sunbird.learner.actors.coursebatch.dao.impl.UserCoursesDaoImpl;
import org.sunbird.learner.actors.coursebatch.service.UserCoursesService;
import org.sunbird.learner.util.EkStepRequestUtil;
import org.sunbird.learner.util.Util;
import org.sunbird.models.course.batch.CourseBatch;
import org.sunbird.models.user.courses.UserCourses;
import org.sunbird.telemetry.util.TelemetryUtil;

@ActorConfig(
  tasks = {"enrollCourse", "unenrollCourse"},
  asyncTasks = {}
)
public class CourseEnrollmentActor extends BaseActor {

  private static String EKSTEP_COURSE_SEARCH_QUERY =
      "{\"request\": {\"filters\":{\"contentType\": [\"Course\"], \"objectType\": [\"Content\"], \"identifier\": \"COURSE_ID_PLACEHOLDER\", \"status\": \"Live\"},\"limit\": 1}}";

  private CourseBatchDao courseBatchDao = new CourseBatchDaoImpl();
  private UserCoursesDao userCourseDao = UserCoursesDaoImpl.getInstance();
  private ObjectMapper mapper = new ObjectMapper();

  @Override
  public void onReceive(Request request) throws Throwable {

    ProjectLogger.log("CourseEnrollmentActor onReceive called");
    String operation = request.getOperation();

    Util.initializeContext(request, TelemetryEnvKey.BATCH);
    ExecutionContext.setRequestId(request.getRequestId());

    switch (operation) {
      case "enrollCourse":
        enrollCourseBatch(request);
        break;
      case "unenrollCourse":
        unenrollCourseBatch(request);
        break;
      default:
        onReceiveUnsupportedOperation("CourseEnrollmentActor");
    }
  }

  private void enrollCourseBatch(Request actorMessage) {
    ProjectLogger.log("enrollCourseClass called");
    Map<String, Object> courseMap = (Map<String, Object>) actorMessage.getRequest();
    courseMap.remove(JsonKey.ID);
    CourseBatch courseBatch =
        courseBatchDao.readById(
            (String) courseMap.get(JsonKey.COURSE_ID), (String) courseMap.get(JsonKey.BATCH_ID));
    String operation = "enrollCourse";
    validateCourseBatch(
        courseBatch,
        courseMap,
        (String) actorMessage.getContext().get(JsonKey.REQUESTED_BY),
        operation);

    UserCourses userCourseResult =
        userCourseDao.read(
            (String) courseMap.get(JsonKey.BATCH_ID), (String) courseMap.get(JsonKey.USER_ID));

    if (!ProjectUtil.isNull(userCourseResult) && userCourseResult.isActive()) {
      ProjectLogger.log("User Already Enrolled Course ");
      ProjectCommonException.throwClientErrorException(
          ResponseCode.userAlreadyEnrolledCourse,
          ResponseCode.userAlreadyEnrolledCourse.getErrorMessage());
    }
    courseMap = createUserCourseMap(courseMap, courseBatch, userCourseResult);
    Response result = new Response();
    if (userCourseResult == null) {
      // user is doing enrollment first time
      userCourseDao.insert(courseMap);
    } else {
      // second time user is doing enrollment for same course batch
      userCourseDao.update(userCourseResult.getBatchId(), userCourseResult.getUserId(), courseMap);
    }
    result.put("response", "SUCCESS");
    sender().tell(result, self());
    if (userCourseResult == null) {
      courseMap.put(JsonKey.DATE_TIME, ProjectUtil.formatDate(new Timestamp(new Date().getTime())));
      updateUserCoursesToES(courseMap);

    } else {
      ProjectLogger.log(
          "CourseEnrollmentActor:enrollCourseBatch user is enrolling second time.",
          LoggerEnum.INFO.name());
      UserCoursesService.sync(
          courseMap,
          (String) courseMap.get(JsonKey.BATCH_ID),
          (String) courseMap.get(JsonKey.USER_ID));
    }
    if (courseNotificationActive()) {
      batchOperationNotifier(courseMap, courseBatch, JsonKey.ADD);
    }
    generateAndProcessTelemetryEvent(courseMap, "user.batch.course", JsonKey.CREATE);
  }

  private boolean courseNotificationActive() {
    ProjectLogger.log(
        "CourseEnrollmentActor: courseNotificationActive: "
            + Boolean.parseBoolean(
                PropertiesCache.getInstance()
                    .getProperty(JsonKey.SUNBIRD_COURSE_BATCH_NOTIFICATIONS_ENABLED)),
        LoggerEnum.INFO.name());
    return Boolean.parseBoolean(
        PropertiesCache.getInstance()
            .getProperty(JsonKey.SUNBIRD_COURSE_BATCH_NOTIFICATIONS_ENABLED));
  }

  private Map<String, Object> createUserCourseMap(
      Map<String, Object> courseMap, CourseBatch courseBatchResult, UserCourses userCourseResult) {
    courseMap.put(JsonKey.ACTIVE, ProjectUtil.ActiveStatus.ACTIVE.getValue());
    if (userCourseResult == null) {
      // this will create user batch data for new user
      Timestamp ts = new Timestamp(new Date().getTime());
      String addedBy = (String) courseMap.get(JsonKey.REQUESTED_BY);
      courseMap.put(JsonKey.ADDED_BY, addedBy);
      courseMap.put(JsonKey.COURSE_ENROLL_DATE, ProjectUtil.getFormattedDate());
      courseMap.put(JsonKey.STATUS, ProjectUtil.ProgressStatus.NOT_STARTED.getValue());
      courseMap.put(JsonKey.DATE_TIME, ts);
      courseMap.put(JsonKey.COURSE_PROGRESS, 0);
    }
    return courseMap;
  }

  private void unenrollCourseBatch(Request actorMessage) {
    ProjectLogger.log("unenrollCourseClass called");
    // objects of telemetry event...
    Map<String, Object> request = actorMessage.getRequest();
    request.remove(JsonKey.ID);
    CourseBatch courseBatch =
        courseBatchDao.readById(
            (String) request.get(JsonKey.COURSE_ID), (String) request.get(JsonKey.BATCH_ID));
    String operation = "unenrollCourse";
    validateCourseBatch(
        courseBatch,
        request,
        (String) actorMessage.getContext().get(JsonKey.REQUESTED_BY),
        operation);
    UserCourses userCourseResult =
        userCourseDao.read(
            (String) request.get(JsonKey.BATCH_ID), (String) request.get(JsonKey.USER_ID));
    UserCoursesService.validateUserUnenroll(userCourseResult);
    Response result = updateUserCourses(userCourseResult);
    sender().tell(result, self());
    generateAndProcessTelemetryEvent(request, "user.batch.course.unenroll", JsonKey.UPDATE);

    if (courseNotificationActive()) {
      batchOperationNotifier(request, courseBatch, JsonKey.REMOVE);
    }
  }

  private void batchOperationNotifier(
      Map<String, Object> request, CourseBatch courseBatchResult, String operationType) {
    ProjectLogger.log("CourseBatchEnrollment: batchOperationNotifier: ", LoggerEnum.INFO.name());
    Request batchNotification = new Request();
    batchNotification.setOperation(ActorOperations.COURSE_BATCH_NOTIFICATION.getValue());
    Map<String, Object> batchNotificationMap = new HashMap<>();
    batchNotificationMap.put(JsonKey.USER_ID, request.get(JsonKey.USER_ID));
    batchNotificationMap.put(JsonKey.COURSE_BATCH, courseBatchResult);
    batchNotificationMap.put(JsonKey.OPERATION_TYPE, operationType);
    batchNotification.setRequest(batchNotificationMap);
    tellToAnother(batchNotification);
  }

  private void generateAndProcessTelemetryEvent(
      Map<String, Object> request, String corelation, String state) {
    Map<String, Object> targetObject = new HashMap<>();
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    targetObject =
        TelemetryUtil.generateTargetObject(
            (String) request.get(JsonKey.USER_ID), JsonKey.USER, state, null);
    TelemetryUtil.generateCorrelatedObject(
        (String) request.get(JsonKey.COURSE_ID), JsonKey.COURSE, corelation, correlatedObject);
    TelemetryUtil.generateCorrelatedObject(
        (String) request.get(JsonKey.BATCH_ID),
        TelemetryEnvKey.BATCH,
        "user.batch",
        correlatedObject);
    TelemetryUtil.telemetryProcessingCall(request, targetObject, correlatedObject);
  }

  private void updateUserCoursesToES(Map<String, Object> courseMap) {
    Request request = new Request();
    request.setOperation(ActorOperations.INSERT_USR_COURSES_INFO_ELASTIC.getValue());
    request.getRequest().put(JsonKey.USER_COURSES, courseMap);
    try {
      tellToAnother(request);
    } catch (Exception ex) {
      ProjectLogger.log("Exception Occurred during saving user count to Es : ", ex);
    }
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> getCourseObjectFromEkStep(
      String courseId, Map<String, String> headers) {
    ProjectLogger.log("Requested course id is ==" + courseId, LoggerEnum.INFO.name());
    if (!StringUtils.isBlank(courseId)) {
      try {
        String query = EKSTEP_COURSE_SEARCH_QUERY.replaceAll("COURSE_ID_PLACEHOLDER", courseId);
        Map<String, Object> result = EkStepRequestUtil.searchContent(query, headers);
        if (null != result && !result.isEmpty() && result.get(JsonKey.CONTENTS) != null) {
          return ((List<Map<String, Object>>) result.get(JsonKey.CONTENTS)).get(0);
          // return (Map<String, Object>) contentObject;
        } else {
          ProjectLogger.log(
              "CourseEnrollmentActor:getCourseObjectFromEkStep: Content not found for requested courseId "
                  + courseId,
              LoggerEnum.INFO.name());
        }
      } catch (Exception e) {
        ProjectLogger.log(e.getMessage(), e);
      }
    }
    return null;
  }

  /*
   * This method will validate courseBatch details before enrolling and
   * unenrolling
   *
   * @Params
   */
  private void validateCourseBatch(
      CourseBatch courseBatchDetails,
      Map<String, Object> request,
      String requestedBy,
      String operation) {

    if (ProjectUtil.isNull(courseBatchDetails)) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.invalidCourseBatchId, ResponseCode.invalidCourseBatchId.getErrorMessage());
    }
    verifyRequestedByAndThrowErrorIfNotMatch((String) request.get(JsonKey.USER_ID), requestedBy);
    if (EnrolmentType.inviteOnly.getVal().equals(courseBatchDetails.getEnrollmentType())) {
      ProjectLogger.log(
          "CourseEnrollmentActor validateCourseBatch self enrollment or unenrollment is not applicable for invite only batch.",
          LoggerEnum.INFO.name());
      ProjectCommonException.throwClientErrorException(
          ResponseCode.enrollmentTypeValidation,
          ResponseCode.enrollmentTypeValidation.getErrorMessage());
    }
    if (!((String) request.get(JsonKey.COURSE_ID)).equals(courseBatchDetails.getCourseId())) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.invalidCourseBatchId, ResponseCode.invalidCourseBatchId.getErrorMessage());
    }
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    try {
      Date todaydate = format.parse(format.format(new Date()));
      // there might be chance end date is not present
      Date courseBatchEndDate = null;
      Date courseBatchEnrollmentEndDate = null;
      if (StringUtils.isNotBlank(courseBatchDetails.getEndDate())) {
        courseBatchEndDate = format.parse(courseBatchDetails.getEndDate());
      }
      if (StringUtils.isNotBlank(courseBatchDetails.getEnrollmentEndDate())) {
        courseBatchEnrollmentEndDate = format.parse(courseBatchDetails.getEnrollmentEndDate());
      }
      if (operation.equals("enrollCourse")
          && courseBatchEnrollmentEndDate != null
          && courseBatchEnrollmentEndDate.before(todaydate)) {
        ProjectLogger.log(
            "CourseEnrollmentActor validateCourseBatch Enrollment Date has ended.",
            LoggerEnum.INFO.name());
        ProjectCommonException.throwClientErrorException(
            ResponseCode.courseBatchEnrollmentDateEnded,
            ResponseCode.courseBatchEnrollmentDateEnded.getErrorMessage());
      }
      if (ProgressStatus.COMPLETED.getValue() == courseBatchDetails.getStatus()
          || (courseBatchEndDate != null && courseBatchEndDate.before(todaydate))) {
        ProjectLogger.log(
            "CourseEnrollmentActor validateCourseBatch Course is completed already.",
            LoggerEnum.INFO.name());
        ProjectCommonException.throwClientErrorException(
            ResponseCode.courseBatchAlreadyCompleted,
            ResponseCode.courseBatchAlreadyCompleted.getErrorMessage());
      }
    } catch (ParseException e) {
      ProjectLogger.log("CourseEnrollmentActor validateCourseBatch ", e);
    }
  }

  private void verifyRequestedByAndThrowErrorIfNotMatch(String userId, String requestedBy) {
    if (!(userId.equals(requestedBy))) {
      ProjectCommonException.throwUnauthorizedErrorException();
    }
  }

  private Response updateUserCourses(UserCourses userCourses) {
    Map<String, Object> userCourseUpdateAttributes = new HashMap<>();
    userCourseUpdateAttributes.put(JsonKey.ACTIVE, false);
    Response result = new Response();
    userCourseDao.update(
        userCourses.getBatchId(), userCourses.getUserId(), userCourseUpdateAttributes);
    result.put("response", "SUCCESS");
    UserCoursesService.sync(
        userCourseUpdateAttributes, userCourses.getBatchId(), userCourses.getUserId());
    return result;
  }
}
