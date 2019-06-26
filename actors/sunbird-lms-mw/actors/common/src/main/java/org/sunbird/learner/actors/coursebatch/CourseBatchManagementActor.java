package org.sunbird.learner.actors.coursebatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
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
import org.sunbird.common.models.util.ProjectUtil.EsType;
import org.sunbird.common.models.util.ProjectUtil.ProgressStatus;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.models.util.TelemetryEnvKey;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.dto.SearchDTO;
import org.sunbird.learner.actors.coursebatch.dao.CourseBatchDao;
import org.sunbird.learner.actors.coursebatch.dao.impl.CourseBatchDaoImpl;
import org.sunbird.learner.actors.coursebatch.service.UserCoursesService;
import org.sunbird.learner.util.CourseBatchSchedulerUtil;
import org.sunbird.learner.util.CourseBatchUtil;
import org.sunbird.learner.util.Util;
import org.sunbird.models.course.batch.CourseBatch;
import org.sunbird.telemetry.util.TelemetryUtil;
import scala.concurrent.Future;

@ActorConfig(
  tasks = {
    "createBatch",
    "updateBatch",
    "addUserBatch",
    "removeUserFromBatch",
    "getBatch",
    "getCourseBatchDetail"
  },
  asyncTasks = {}
)
public class CourseBatchManagementActor extends BaseActor {

  private CourseBatchDao courseBatchDao = new CourseBatchDaoImpl();
  private UserCoursesService userCoursesService = new UserCoursesService();
  private ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  static {
    DATE_FORMAT.setTimeZone(
        TimeZone.getTimeZone(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_TIMEZONE)));
  }

  @Override
  public void onReceive(Request request) throws Throwable {

    Util.initializeContext(request, TelemetryEnvKey.BATCH);
    ExecutionContext.setRequestId(request.getRequestId());

    String requestedOperation = request.getOperation();
    switch (requestedOperation) {
      case "createBatch":
        createCourseBatch(request);
        break;
      case "updateBatch":
        updateCourseBatch(request);
        break;
      case "getBatch":
        getCourseBatch(request);
        break;
      case "addUserBatch":
        addUserCourseBatch(request);
        break;
      case "getCourseBatchDetail":
        getCourseBatchDetails(request);
        break;
      default:
        onReceiveUnsupportedOperation(request.getOperation());
        break;
    }
  }

  @SuppressWarnings("unchecked")
  private void createCourseBatch(Request actorMessage) {
    Map<String, Object> request = actorMessage.getRequest();
    Map<String, Object> targetObject;
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    String courseBatchId = ProjectUtil.getUniqueIdFromTimestamp(actorMessage.getEnv());
    Map<String, String> headers =
        (Map<String, String>) actorMessage.getContext().get(JsonKey.HEADER);
    String requestedBy = (String) actorMessage.getContext().get(JsonKey.REQUESTED_BY);

    List<String> participants = (List<String>) request.get(JsonKey.PARTICIPANTS);
    request.remove(JsonKey.PARTICIPANTS);
    request.remove(JsonKey.PARTICIPANT);
    CourseBatch courseBatch = new ObjectMapper().convertValue(request, CourseBatch.class);
    courseBatch.initCount();
    courseBatch.setId(courseBatchId);
    courseBatch.setStatus(setCourseBatchStatus((String) request.get(JsonKey.START_DATE)));
    courseBatch.setHashTagId(
        getHashTagId((String) request.get(JsonKey.HASH_TAG_ID), JsonKey.CREATE, "", courseBatchId));
    String courseId = (String) request.get(JsonKey.COURSE_ID);
    Map<String, Object> contentDetails = getContentDetails(courseId, headers);
    courseBatch.setContentDetails(contentDetails, requestedBy);

    validateContentOrg(courseBatch.getCreatedFor());
    validateMentors(courseBatch);
    Map<String, Object> participantsMap = null;
    if (participants != null) {
      validateParticipants(participants, courseBatch);
      participantsMap = getParticipantsMap(participants, courseBatch);
    }

    Response result = courseBatchDao.create(courseBatch);
    result.put(JsonKey.BATCH_ID, courseBatchId);

    CourseBatchUtil.syncCourseBatchForeground(
        courseBatchId, new ObjectMapper().convertValue(courseBatch, Map.class));
    sender().tell(result, self());

    targetObject =
        TelemetryUtil.generateTargetObject(
            courseBatchId, TelemetryEnvKey.BATCH, JsonKey.CREATE, null);
    TelemetryUtil.generateCorrelatedObject(
        (String) request.get(JsonKey.COURSE_ID), JsonKey.COURSE, null, correlatedObject);

    Map<String, String> rollUp = new HashMap<>();
    rollUp.put("l1", (String) request.get(JsonKey.COURSE_ID));
    TelemetryUtil.addTargetObjectRollUp(rollUp, targetObject);
    TelemetryUtil.telemetryProcessingCall(request, targetObject, correlatedObject);

    if (courseNotificationActive()) {
      if (participantsMap != null)
        courseBatch.setParticipant((List<String>) participantsMap.get(JsonKey.PARTICIPANTS));
      batchOperationNotifier(courseBatch, null);
    }
    updateBatchCount(courseBatch);
  }

  private boolean courseNotificationActive() {
    ProjectLogger.log(
        "CourseBatchManagementActor: courseNotificationActive: "
            + Boolean.parseBoolean(
                PropertiesCache.getInstance()
                    .getProperty(JsonKey.SUNBIRD_COURSE_BATCH_NOTIFICATIONS_ENABLED)),
        LoggerEnum.INFO.name());
    return Boolean.parseBoolean(
        PropertiesCache.getInstance()
            .getProperty(JsonKey.SUNBIRD_COURSE_BATCH_NOTIFICATIONS_ENABLED));
  }

  private void batchOperationNotifier(
      CourseBatch courseBatch, Map<String, Object> participantMentorMap) {
    ProjectLogger.log(
        "CourseBatchManagementActor: batchoperationNotifier called", LoggerEnum.INFO.name());
    Request batchNotification = new Request();
    batchNotification.setOperation(ActorOperations.COURSE_BATCH_NOTIFICATION.getValue());
    Map<String, Object> batchNotificationMap = new HashMap<>();
    if (participantMentorMap != null) {
      batchNotificationMap.put(JsonKey.UPDATE, true);
      batchNotificationMap.put(
          JsonKey.ADDED_MENTORS, participantMentorMap.get(JsonKey.ADDED_MENTORS));
      batchNotificationMap.put(
          JsonKey.REMOVED_MENTORS, participantMentorMap.get(JsonKey.REMOVED_MENTORS));
      batchNotificationMap.put(
          JsonKey.ADDED_PARTICIPANTS, participantMentorMap.get(JsonKey.ADDED_PARTICIPANTS));
      batchNotificationMap.put(
          JsonKey.REMOVED_PARTICIPANTS, participantMentorMap.get(JsonKey.REMOVED_PARTICIPANTS));

    } else {
      batchNotificationMap.put(JsonKey.OPERATION_TYPE, JsonKey.ADD);
      batchNotificationMap.put(JsonKey.ADDED_MENTORS, courseBatch.getMentors());
      batchNotificationMap.put(JsonKey.ADDED_PARTICIPANTS, courseBatch.getParticipant());
    }
    batchNotificationMap.put(JsonKey.COURSE_BATCH, courseBatch);
    batchNotification.setRequest(batchNotificationMap);
    tellToAnother(batchNotification);
  }

  @SuppressWarnings("unchecked")
  private void updateCourseBatch(Request actorMessage) {
    Map<String, Object> targetObject = null;
    Map<String, Object> participantsMap = null;

    List<Map<String, Object>> correlatedObject = new ArrayList<>();

    Map<String, Object> request = actorMessage.getRequest();
    List<String> participants = (List<String>) request.get(JsonKey.PARTICIPANTS);
    String requestedBy = (String) actorMessage.getContext().get(JsonKey.REQUESTED_BY);

    CourseBatch oldBatch = courseBatchDao.readById((String) request.get(JsonKey.ID));
    CourseBatch courseBatch = getUpdateCourseBatch(request);
    courseBatch.setUpdatedDate(ProjectUtil.getFormattedDate());
    checkBatchStatus(courseBatch);
    validateUserPermission(courseBatch, requestedBy);
    validateContentOrg(courseBatch.getCreatedFor());
    validateMentors(courseBatch);
    if (participants != null
        && JsonKey.INVITE_ONLY.equalsIgnoreCase(courseBatch.getEnrollmentType())) {
      validateParticipants(participants, courseBatch);
      participantsMap = getParticipantsMap(participants, courseBatch);
    } else {
      participantsMap = new HashMap<>();
    }
    participantsMap = getMentorLists(participantsMap, oldBatch, courseBatch);
    Map<String, Object> courseBatchMap = new ObjectMapper().convertValue(courseBatch, Map.class);
    Response result = courseBatchDao.update(courseBatchMap);
    sender().tell(result, self());

    if (((String) result.get(JsonKey.RESPONSE)).equalsIgnoreCase(JsonKey.SUCCESS)) {
      CourseBatchUtil.syncCourseBatchForeground(
          (String) courseBatchMap.get(JsonKey.ID), courseBatchMap);
    } else {
      ProjectLogger.log(
          "CourseBatchManagementActor:updateCourseBatch: Course batch not synced to ES as response is not successful",
          LoggerEnum.INFO.name());
    }
    targetObject =
        TelemetryUtil.generateTargetObject(
            (String) request.get(JsonKey.ID), TelemetryEnvKey.BATCH, JsonKey.UPDATE, null);

    Map<String, String> rollUp = new HashMap<>();
    rollUp.put("l1", courseBatch.getCourseId());
    TelemetryUtil.addTargetObjectRollUp(rollUp, targetObject);
    TelemetryUtil.telemetryProcessingCall(courseBatchMap, targetObject, correlatedObject);

    if (courseNotificationActive()) {
      if (participants != null
          && JsonKey.INVITE_ONLY.equalsIgnoreCase(courseBatch.getEnrollmentType())) {
        courseBatch.setParticipant((List<String>) participantsMap.get(JsonKey.PARTICIPANTS));
      }
      batchOperationNotifier(courseBatch, participantsMap);
    }
  }

  private Map<String, Object> getMentorLists(
      Map<String, Object> participantsMap, CourseBatch prevBatch, CourseBatch newBatch) {
    List<String> prevMentors = prevBatch.getMentors();
    List<String> removedMentors = prevBatch.getMentors();
    List<String> addedMentors = newBatch.getMentors();

    if (addedMentors == null) {
      addedMentors = new ArrayList<>();
    }
    if (prevMentors == null) {
      prevMentors = new ArrayList<>();
      removedMentors = new ArrayList<>();
    }

    removedMentors.removeAll(addedMentors);
    addedMentors.removeAll(prevMentors);

    participantsMap.put(JsonKey.REMOVED_MENTORS, removedMentors);
    participantsMap.put(JsonKey.ADDED_MENTORS, addedMentors);

    return participantsMap;
  }

  private void checkBatchStatus(CourseBatch courseBatch) {
    ProjectLogger.log(
        "CourseBatchManagementActor:checkBatchStatus batch staus is :" + courseBatch.getStatus(),
        LoggerEnum.INFO.name());
    if (ProjectUtil.ProgressStatus.COMPLETED.getValue() == courseBatch.getStatus()) {
      throw new ProjectCommonException(
          ResponseCode.courseBatchEndDateError.getErrorCode(),
          ResponseCode.courseBatchEndDateError.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  @SuppressWarnings("unchecked")
  private CourseBatch getUpdateCourseBatch(Map<String, Object> request) {
    CourseBatch courseBatch = courseBatchDao.readById((String) request.get(JsonKey.ID));

    courseBatch.setEnrollmentType(
        getEnrollmentType(
            (String) request.get(JsonKey.ENROLLMENT_TYPE), courseBatch.getEnrollmentType()));
    courseBatch.setCreatedFor(
        getUpdatedCreatedFor(
            (List<String>) request.get(JsonKey.COURSE_CREATED_FOR),
            courseBatch.getEnrollmentType(),
            courseBatch.getCreatedFor()));
    courseBatch.setHashTagId(
        getHashTagId(
            (String) request.get(JsonKey.HASHTAGID),
            JsonKey.UPDATE,
            (String) request.get(JsonKey.ID),
            ""));

    if (request.containsKey(JsonKey.NAME)) courseBatch.setName((String) request.get(JsonKey.NAME));

    if (request.containsKey(JsonKey.DESCRIPTION))
      courseBatch.setDescription((String) request.get(JsonKey.DESCRIPTION));

    if (request.containsKey(JsonKey.MENTORS))
      courseBatch.setMentors((List<String>) request.get(JsonKey.MENTORS));

    updateCourseBatchDate(courseBatch, request);

    return courseBatch;
  }

  private String getEnrollmentType(String requestEnrollmentType, String dbEnrollmentType) {
    if (requestEnrollmentType != null) return requestEnrollmentType;
    return dbEnrollmentType;
  }

  @SuppressWarnings("unchecked")
  private void addUserCourseBatch(Request actorMessage) {
    Map<String, Object> req = actorMessage.getRequest();
    Response response = new Response();

    Map<String, Object> targetObject = null;
    List<Map<String, Object>> correlatedObject = new ArrayList<>();

    String batchId = (String) req.get(JsonKey.BATCH_ID);
    TelemetryUtil.generateCorrelatedObject(batchId, TelemetryEnvKey.BATCH, null, correlatedObject);

    CourseBatch courseBatch = courseBatchDao.readById(batchId);
    Map<String, Object> courseBatchObject = new ObjectMapper().convertValue(courseBatch, Map.class);

    if (ProjectUtil.isNull(courseBatchObject.get(JsonKey.ENROLLMENT_TYPE))
        || !((String) courseBatchObject.get(JsonKey.ENROLLMENT_TYPE))
            .equalsIgnoreCase(JsonKey.INVITE_ONLY)) {
      throw new ProjectCommonException(
          ResponseCode.enrollmentTypeValidation.getErrorCode(),
          ResponseCode.enrollmentTypeValidation.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    if (ProjectUtil.isNull(courseBatchObject.get(JsonKey.COURSE_CREATED_FOR))
        || ((List) courseBatchObject.get(JsonKey.COURSE_CREATED_FOR)).isEmpty()) {
      throw new ProjectCommonException(
          ResponseCode.courseCreatedForIsNull.getErrorCode(),
          ResponseCode.courseCreatedForIsNull.getErrorMessage(),
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }
    String batchCreator = (String) courseBatchObject.get(JsonKey.CREATED_BY);
    if (StringUtils.isBlank(batchCreator)) {
      throw new ProjectCommonException(
          ResponseCode.invalidCourseCreatorId.getErrorCode(),
          ResponseCode.invalidCourseCreatorId.getErrorMessage(),
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }
    String batchCreatorRootOrgId = getRootOrg(batchCreator);
    List<String> participants = userCoursesService.getEnrolledUserFromBatch(courseBatch.getId());
    courseBatch.setParticipant(participants);
    List<String> userIds = (List<String>) req.get(JsonKey.USER_IDs);
    if (participants == null) {
      participants = new ArrayList<>();
    }
    Map<String, String> participantWithRootOrgIds = getRootOrgForMultipleUsers(userIds);
    List<String> addedParticipants = new ArrayList<>();
    for (String userId : userIds) {
      if (!(participants.contains(userId))) {
        if (!participantWithRootOrgIds.containsKey(userId)
            || (!batchCreatorRootOrgId.equals(participantWithRootOrgIds.get(userId)))) {
          response.put(userId, ResponseCode.userNotAssociatedToRootOrg.getErrorMessage());
          continue;
        }
        addedParticipants.add(userId);

      } else {
        response.getResult().put(userId, JsonKey.SUCCESS);
      }
    }

    userCoursesService.enroll(
        batchId,
        (String) courseBatchObject.get(JsonKey.COURSE_ID),
        addedParticipants,
        (Map<String, String>) (courseBatchObject.get(JsonKey.COURSE_ADDITIONAL_INFO)));
    for (String userId : addedParticipants) {
      participants.add(userId);
      response.getResult().put(userId, JsonKey.SUCCESS);

      targetObject =
          TelemetryUtil.generateTargetObject(userId, TelemetryEnvKey.USER, JsonKey.UPDATE, null);
      correlatedObject = new ArrayList<>();
      TelemetryUtil.generateCorrelatedObject(
          batchId, TelemetryEnvKey.BATCH, null, correlatedObject);
      TelemetryUtil.telemetryProcessingCall(req, targetObject, correlatedObject);
    }

    sender().tell(response, self());
    Request request = new Request();
    request.setOperation(ActorOperations.UPDATE_COURSE_BATCH_ES.getValue());
    request.getRequest().put(JsonKey.BATCH, courseBatchObject);
    if (courseNotificationActive()) {
      courseBatch.setParticipant(getAddedParticipants(participants, courseBatch));
      batchOperationNotifier(courseBatch, null);
    }
    try {
      ProjectLogger.log(
          "CourseBatchManagementActor:addUserCourseBatch: Sync course batch details to ES called");
      tellToAnother(request);
    } catch (Exception ex) {
      ProjectLogger.log(
          "CourseBatchManagementActor:addUserCourseBatch: Exception occurred with error message = "
              + ex.getMessage(),
          ex);
    }
  }

  private List<String> getAddedParticipants(List<String> participants, CourseBatch courseBatch) {
    List<String> currentParticipants = courseBatch.getParticipant();
    if (participants == null) return courseBatch.getParticipant();
    else if (currentParticipants == null || currentParticipants.isEmpty()) {
      return participants;
    } else {

      for (String key : currentParticipants) {
        if (participants.contains(key)) {
          participants.remove(key);
        }
      }
    }
    return participants;
  }

  private void getCourseBatch(Request actorMessage) {
    Future<Map<String, Object>> resultF =
        esService.getDataByIdentifier(
            ProjectUtil.EsType.course.getTypeName(),
            (String) actorMessage.getContext().get(JsonKey.BATCH_ID));
    Map<String, Object> result =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
    Response response = new Response();
    if (null != result) {
      if (JsonKey.INVITE_ONLY.equalsIgnoreCase((String) result.get(JsonKey.ENROLLMENT_TYPE))) {
        List<String> participants =
            userCoursesService.getEnrolledUserFromBatch(
                (String) actorMessage.getContext().get(JsonKey.BATCH_ID));
        result.put(JsonKey.PARTICIPANTS, participants);
      }
      response.put(JsonKey.RESPONSE, result);
    } else {
      result = new HashMap<>();
      response.put(JsonKey.RESPONSE, result);
    }
    sender().tell(response, self());
  }

  @SuppressWarnings("unchecked")
  private void getCourseBatchDetails(Request actorMessage) {
    String batchId = (String) actorMessage.getContext().get(JsonKey.BATCH_ID);
    CourseBatch courseBatch = courseBatchDao.readById(batchId);
    List<Map<String, Object>> courseBatchList = new ArrayList<>();
    ObjectMapper mapper = new ObjectMapper();
    courseBatchList.add(mapper.convertValue(courseBatch, Map.class));
    Response result = new Response();
    result.getResult().put(JsonKey.RESPONSE, courseBatchList);

    sender().tell(result, self());
  }

  private int setCourseBatchStatus(String startDate) {
    try {
      Date todayDate = DATE_FORMAT.parse(DATE_FORMAT.format(new Date()));
      Date requestedStartDate = DATE_FORMAT.parse(startDate);
      ProjectLogger.log(
          "CourseBatchManagementActor:setCourseBatchStatus: todayDate="
              + todayDate
              + ", requestedStartDate="
              + requestedStartDate,
          LoggerEnum.INFO);
      if (todayDate.compareTo(requestedStartDate) == 0) {
        return ProgressStatus.STARTED.getValue();
      } else {
        return ProgressStatus.NOT_STARTED.getValue();
      }
    } catch (ParseException e) {
      ProjectLogger.log(
          "CourseBatchManagementActor:setCourseBatchStatus: Exception occurred with error message = "
              + e.getMessage(),
          e);
    }
    return ProgressStatus.NOT_STARTED.getValue();
  }

  private void validateMentors(CourseBatch courseBatch) {
    List<String> mentors = courseBatch.getMentors();
    if (mentors != null) {
      String batchCreatorRootOrgId = getRootOrg(courseBatch.getCreatedBy());

      for (String userId : mentors) {
        Future<Map<String, Object>> resultF =
            esService.getDataByIdentifier(ProjectUtil.EsType.user.getTypeName(), userId);
        Map<String, Object> result =
            (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);

        String mentorRootOrgId = getRootOrg(userId);
        if (!batchCreatorRootOrgId.equals(mentorRootOrgId)) {
          throw new ProjectCommonException(
              ResponseCode.userNotAssociatedToRootOrg.getErrorCode(),
              ResponseCode.userNotAssociatedToRootOrg.getErrorMessage(),
              ResponseCode.CLIENT_ERROR.getResponseCode(),
              userId);
        }
        if ((ProjectUtil.isNull(result))
            || (ProjectUtil.isNotNull(result) && result.isEmpty())
            || (ProjectUtil.isNotNull(result)
                && result.containsKey(JsonKey.IS_DELETED)
                && ProjectUtil.isNotNull(result.get(JsonKey.IS_DELETED))
                && (Boolean) result.get(JsonKey.IS_DELETED))) {
          throw new ProjectCommonException(
              ResponseCode.invalidUserId.getErrorCode(),
              ResponseCode.invalidUserId.getErrorMessage(),
              ResponseCode.CLIENT_ERROR.getResponseCode());
        }
      }
    }
  }

  private Map<String, Object> getParticipantsMap(
      List<String> participants, CourseBatch courseBatchObject) {
    Map<String, Object> participantsList = new HashMap<>();
    String batchId = courseBatchObject.getId();
    List<String> dbParticipants = getParticipantsFromUserCourses(courseBatchObject.getId());
    if (CollectionUtils.isEmpty(dbParticipants)) {
      dbParticipants = new ArrayList<>();
    }
    List<String> finalParticipants = new ArrayList<>();
    List<String> addedParticipants = new ArrayList<>();
    for (String userId : participants) {
      if (!(dbParticipants.contains(userId))) {
        finalParticipants.add(userId);
        addedParticipants.add(userId);
      } else {
        finalParticipants.add(userId);
        dbParticipants.remove(userId);
      }
    }
    userCoursesService.enroll(
        batchId,
        courseBatchObject.getCourseId(),
        addedParticipants,
        (courseBatchObject.getCourseAdditionalInfo()));
    if (!dbParticipants.isEmpty()) {
      removeParticipants(dbParticipants, batchId, courseBatchObject.getCourseId());
    }
    participantsList.put(JsonKey.REMOVED_PARTICIPANTS, dbParticipants);
    participantsList.put(JsonKey.ADDED_PARTICIPANTS, addedParticipants);
    participantsList.put(JsonKey.PARTICIPANTS, finalParticipants);
    return participantsList;
  }

  private List<String> getParticipantsFromUserCourses(String id) {
    return userCoursesService.getEnrolledUserFromBatch(id);
  }

  private List<String> getParticipantsList(Map<String, Boolean> participantsMap) {
    List<String> participantsList = null;

    if (participantsMap != null) {
      participantsList = new ArrayList<>();
      Set<String> keys = participantsMap.keySet();
      for (String user : keys) {
        if (participantsMap.get(user)) {
          participantsList.add(user);
        }
      }
    }

    return participantsList;
  }

  private void validateParticipants(List<String> participants, CourseBatch courseBatch) {

    String batchCreator = courseBatch.getCreatedBy();
    if (StringUtils.isBlank(batchCreator)) {
      throw new ProjectCommonException(
          ResponseCode.invalidCourseCreatorId.getErrorCode(),
          ResponseCode.invalidCourseCreatorId.getErrorMessage(),
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }
    validateCourseBatchData(courseBatch);
    String batchCreatorRootOrgId = getRootOrg(batchCreator);
    Map<String, String> participantWithRootOrgIds = getRootOrgForMultipleUsers(participants);
    for (String userId : participants) {
      if (!participantWithRootOrgIds.containsKey(userId)
          || (!batchCreatorRootOrgId.equals(participantWithRootOrgIds.get(userId)))) {
        throw new ProjectCommonException(
            ResponseCode.userNotAssociatedToRootOrg.getErrorCode(),
            ResponseCode.userNotAssociatedToRootOrg.getErrorMessage(),
            ResponseCode.RESOURCE_NOT_FOUND.getResponseCode(),
            userId);
      }
    }
  }

  private void removeParticipants(
      List<String> removedParticipant, String batchId, String courseId) {
    removedParticipant.forEach(
        (userId) -> {
          new UserCoursesService().unenroll(userId, courseId, batchId);
        });
  }

  private void validateCourseBatchData(CourseBatch courseBatchObject) {
    if (ProjectUtil.isNull(courseBatchObject.getCreatedFor())
        || courseBatchObject.getCreatedFor().isEmpty()) {
      throw new ProjectCommonException(
          ResponseCode.courseCreatedForIsNull.getErrorCode(),
          ResponseCode.courseCreatedForIsNull.getErrorMessage(),
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }
    if (ProjectUtil.isNull(courseBatchObject.getEnrollmentType())
        || !(courseBatchObject.getEnrollmentType()).equalsIgnoreCase(JsonKey.INVITE_ONLY)) {
      throw new ProjectCommonException(
          ResponseCode.enrollmentTypeValidation.getErrorCode(),
          ResponseCode.enrollmentTypeValidation.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  private List<String> getUpdatedCreatedFor(
      List<String> createdFor, String enrolmentType, List<String> dbValueCreatedFor) {
    if (createdFor != null) {
      for (String orgId : createdFor) {
        if (!dbValueCreatedFor.contains(orgId) && !isOrgValid(orgId)) {
          throw new ProjectCommonException(
              ResponseCode.invalidOrgId.getErrorCode(),
              ResponseCode.invalidOrgId.getErrorMessage(),
              ResponseCode.CLIENT_ERROR.getResponseCode());
        }
      }
      return createdFor;
    }
    return dbValueCreatedFor;
  }

  @SuppressWarnings("unchecked")
  private void updateCourseBatchDate(CourseBatch courseBatch, Map<String, Object> req) {
    Map<String, Object> courseBatchMap = new ObjectMapper().convertValue(courseBatch, Map.class);
    Date todayDate = getDate(null, DATE_FORMAT, null);
    Date dbBatchStartDate = getDate(JsonKey.START_DATE, DATE_FORMAT, courseBatchMap);
    Date dbBatchEndDate = getDate(JsonKey.END_DATE, DATE_FORMAT, courseBatchMap);
    Date requestedStartDate = getDate(JsonKey.START_DATE, DATE_FORMAT, req);
    Date requestedEndDate = getDate(JsonKey.END_DATE, DATE_FORMAT, req);

    validateUpdateBatchStartDate(requestedStartDate);
    validateBatchStartAndEndDate(
        dbBatchStartDate, dbBatchEndDate, requestedStartDate, requestedEndDate, todayDate);
    if (null != requestedStartDate && todayDate.equals(requestedStartDate)) {
      courseBatch.setStatus(ProgressStatus.STARTED.getValue());
      CourseBatchSchedulerUtil.updateCourseBatchDbStatus(req, true);
    }
    courseBatch.setStartDate(
        requestedStartDate != null
            ? (String) req.get(JsonKey.START_DATE)
            : courseBatch.getStartDate());
    courseBatch.setEndDate(
        requestedEndDate != null ? (String) req.get(JsonKey.END_DATE) : courseBatch.getEndDate());
  }

  private void validateUserPermission(CourseBatch courseBatch, String requestedBy) {
    List<String> canUpdateList = new ArrayList<>();
    canUpdateList.add(courseBatch.getCreatedBy());
    if (CollectionUtils.isNotEmpty(courseBatch.getMentors())) {
      canUpdateList.addAll(courseBatch.getMentors());
    }
    if (!canUpdateList.contains(requestedBy)) {
      throw new ProjectCommonException(
          ResponseCode.unAuthorized.getErrorCode(),
          ResponseCode.unAuthorized.getErrorMessage(),
          ResponseCode.UNAUTHORIZED.getResponseCode());
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> getRootOrgForMultipleUsers(List<String> userIds) {

    Map<String, String> userWithRootOrgs = new HashMap<>();
    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.ID, userIds);

    List<String> fields = new ArrayList<>();
    fields.add(JsonKey.ROOT_ORG_ID);
    fields.add(JsonKey.ID);
    fields.add(JsonKey.REGISTERED_ORG);

    SearchDTO searchDTO = new SearchDTO();
    if (CollectionUtils.isNotEmpty(userIds)) {
      searchDTO.setLimit(userIds.size());
      searchDTO.setOffset(0);
    }
    searchDTO.getAdditionalProperties().put(JsonKey.FILTERS, filters);
    searchDTO.setFields(fields);

    Future<Map<String, Object>> resultF = esService.search(searchDTO, EsType.user.getTypeName());
    Map<String, Object> result =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);

    List<Map<String, Object>> esContent = (List<Map<String, Object>>) result.get(JsonKey.CONTENT);
    for (Map<String, Object> user : esContent) {
      String rootOrg = getRootOrgFromUserMap(user);
      userWithRootOrgs.put((String) user.get(JsonKey.ID), rootOrg);
    }
    return userWithRootOrgs;
  }

  private String getRootOrg(String batchCreator) {
    Future<Map<String, Object>> userInfoF =
        esService.getDataByIdentifier(EsType.user.getTypeName(), batchCreator);
    Map<String, Object> userInfo =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(userInfoF);
    return getRootOrgFromUserMap(userInfo);
  }

  @SuppressWarnings("unchecked")
  private String getRootOrgFromUserMap(Map<String, Object> userInfo) {
    String rootOrg = (String) userInfo.get(JsonKey.ROOT_ORG_ID);
    Map<String, Object> registeredOrgInfo =
        (Map<String, Object>) userInfo.get(JsonKey.REGISTERED_ORG);
    if (registeredOrgInfo != null && !registeredOrgInfo.isEmpty()) {
      if (null != registeredOrgInfo.get(JsonKey.IS_ROOT_ORG)
          && (Boolean) registeredOrgInfo.get(JsonKey.IS_ROOT_ORG)) {
        rootOrg = (String) registeredOrgInfo.get(JsonKey.ID);
      }
    }
    return rootOrg;
  }

  private String getHashTagId(String hasTagId, String operation, String id, String uniqueId) {
    if (hasTagId != null) return validateHashTagId(hasTagId, operation, id);
    return uniqueId;
  }

  @SuppressWarnings("unchecked")
  private String validateHashTagId(String hashTagId, String opType, String id) {
    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.HASHTAGID, hashTagId);
    SearchDTO searchDto = new SearchDTO();
    searchDto.getAdditionalProperties().put(JsonKey.FILTERS, filters);
    Future<Map<String, Object>> resultF =
        esService.search(searchDto, ProjectUtil.EsType.course.getTypeName());
    Map<String, Object> result =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
    List<Map<String, Object>> dataMapList = (List<Map<String, Object>>) result.get(JsonKey.CONTENT);
    if (opType.equalsIgnoreCase(JsonKey.CREATE)) {
      if (!dataMapList.isEmpty()) {
        throw new ProjectCommonException(
            ResponseCode.invalidHashTagId.getErrorCode(),
            ResponseCode.invalidHashTagId.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
    } else if (opType.equalsIgnoreCase(JsonKey.UPDATE) && !dataMapList.isEmpty()) {
      Map<String, Object> batchMap = dataMapList.get(0);
      if (!(((String) batchMap.get(JsonKey.ID)).equalsIgnoreCase(id))) {
        throw new ProjectCommonException(
            ResponseCode.invalidHashTagId.getErrorCode(),
            ResponseCode.invalidHashTagId.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
    }
    return hashTagId;
  }

  private void validateUpdateBatchStartDate(Date startDate) {
    if (startDate != null) {
      try {
        DATE_FORMAT.format(startDate);
      } catch (Exception e) {
        throw new ProjectCommonException(
            ResponseCode.dateFormatError.getErrorCode(),
            ResponseCode.dateFormatError.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
    } else {
      throw new ProjectCommonException(
          ResponseCode.courseBatchStartDateRequired.getErrorCode(),
          ResponseCode.courseBatchStartDateRequired.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  private void validateBatchStartAndEndDate(
      Date existingStartDate,
      Date existingEndDate,
      Date requestedStartDate,
      Date requestedEndDate,
      Date todayDate) {
    Date startDate = requestedStartDate != null ? requestedStartDate : existingStartDate;
    Date endDate = requestedEndDate != null ? requestedEndDate : existingEndDate;
    ProjectLogger.log(
        "existingStartDate, existingEndDate, requestedStartDate, requestedEndDate, todaydate"
            + existingStartDate
            + ","
            + existingEndDate
            + ","
            + requestedStartDate
            + ","
            + requestedEndDate
            + ","
            + todayDate,
        LoggerEnum.INFO.name());

    if ((existingStartDate.before(todayDate) || existingStartDate.equals(todayDate))
        && !(existingStartDate.equals(requestedStartDate))) {
      throw new ProjectCommonException(
          ResponseCode.invalidBatchStartDateError.getErrorCode(),
          ResponseCode.invalidBatchStartDateError.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    if ((requestedStartDate.before(todayDate)) && !requestedStartDate.equals(existingStartDate)) {
      throw new ProjectCommonException(
          ResponseCode.invalidBatchStartDateError.getErrorCode(),
          ResponseCode.invalidBatchStartDateError.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    if (endDate != null && startDate.after(endDate)) {
      throw new ProjectCommonException(
          ResponseCode.invalidBatchEndDateError.getErrorCode(),
          ResponseCode.invalidBatchEndDateError.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    if ((endDate != null && !endDate.after(todayDate))
        || (existingEndDate != null && !existingEndDate.after(todayDate))) {
      throw new ProjectCommonException(
          ResponseCode.courseBatchEndDateError.getErrorCode(),
          ResponseCode.courseBatchEndDateError.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  private Date getDate(String key, SimpleDateFormat format, Map<String, Object> map) {
    try {
      if (MapUtils.isEmpty(map)) {
        return format.parse(format.format(new Date()));
      } else {
        if (StringUtils.isNotBlank((String) map.get(key))) {
          return format.parse((String) map.get(key));
        } else {
          return null;
        }
      }
    } catch (ParseException e) {

      ProjectLogger.log(
          "CourseBatchManagementActor:getDate: Exception occurred with message = " + e.getMessage(),
          e);
    }
    return null;
  }

  private boolean isOrgValid(String orgId) {
    Future<Map<String, Object>> respF =
        esService.getDataByIdentifier(ProjectUtil.EsType.organisation.getTypeName(), orgId);
    Map<String, Object> resp =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(respF);
    if (resp != null && resp.size() > 0) {
      ProjectLogger.log(
          "CourseBatchManagementActor:isOrgValid: Organisation found in ES with id = " + orgId);
      return true;
    }
    ProjectLogger.log(
        "CourseBatchManagementActor:isOrgValid: Organisation NOT found in ES with id = " + orgId);
    return false;
  }

  private Map<String, Object> getContentDetails(String courseId, Map<String, String> headers) {
    Map<String, Object> ekStepContent =
        CourseEnrollmentActor.getCourseObjectFromEkStep(courseId, headers);
    if (null == ekStepContent || ekStepContent.size() == 0) {
      ProjectLogger.log(
          "CourseBatchManagementActor:getEkStepContent: Not found course for ID = " + courseId,
          LoggerEnum.INFO.name());
      throw new ProjectCommonException(
          ResponseCode.invalidCourseId.getErrorCode(),
          ResponseCode.invalidCourseId.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    return ekStepContent;
  }

  private void validateContentOrg(List<String> createdFor) {
    if (createdFor != null) {
      for (String orgId : createdFor) {
        if (!isOrgValid(orgId)) {
          throw new ProjectCommonException(
              ResponseCode.invalidOrgId.getErrorCode(),
              ResponseCode.invalidOrgId.getErrorMessage(),
              ResponseCode.CLIENT_ERROR.getResponseCode());
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void updateBatchCount(CourseBatch courseBatch) {
    if (CourseBatchSchedulerUtil.doOperationInEkStepCourse(
        courseBatch.getCourseId(), true, courseBatch.getEnrollmentType())) {
      courseBatch.setCountIncrementStatus(true);
      courseBatch.setCountIncrementDate(ProjectUtil.getFormattedDate());
      Map<String, Object> courseBatchMap = new ObjectMapper().convertValue(courseBatch, Map.class);
      Response response = courseBatchDao.update(courseBatchMap);

      if (JsonKey.SUCCESS.equalsIgnoreCase((String) response.get(JsonKey.RESPONSE))) {
        CourseBatchUtil.syncCourseBatchForeground(
            (String) courseBatchMap.get(JsonKey.ID), courseBatchMap);
      } else {
        ProjectLogger.log(
            "CourseBatchManagementActor:updateBatchCount: Course batch not synced to ES as response is not successful",
            LoggerEnum.INFO.name());
      }
    }
  }
}
