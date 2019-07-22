package org.sunbird.learner.actors.bulkupload;

import static org.sunbird.learner.util.Util.isNotNull;
import static org.sunbird.learner.util.Util.isNull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.*;
import org.sunbird.common.models.util.ProjectUtil.EsIndex;
import org.sunbird.common.models.util.ProjectUtil.EsType;
import org.sunbird.common.models.util.ProjectUtil.Status;
import org.sunbird.common.models.util.datasecurity.DecryptionService;
import org.sunbird.common.models.util.datasecurity.EncryptionService;
import org.sunbird.common.models.util.datasecurity.OneWayHashing;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.UserRequestValidator;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.common.responsecode.ResponseMessage;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.UserUtility;
import org.sunbird.learner.util.Util;
import org.sunbird.models.user.User;
import org.sunbird.services.sso.SSOManager;
import org.sunbird.services.sso.SSOServiceFactory;
import org.sunbird.telemetry.util.TelemetryUtil;
import scala.concurrent.Future;

/**
 * This actor will handle bulk upload operation .
 *
 * @author Amit Kumar
 */
@ActorConfig(
  tasks = {},
  asyncTasks = {"processBulkUpload"}
)
public class BulkUploadBackGroundJobActor extends BaseActor {

  private String processId = "";
  private final Util.DbInfo bulkDb = Util.dbInfoMap.get(JsonKey.BULK_OP_DB);
  private final EncryptionService encryptionService =
      org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getEncryptionServiceInstance(
          null);
  private final DecryptionService decryptionService =
      org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getDecryptionServiceInstance(
          null);
  private final PropertiesCache propertiesCache = PropertiesCache.getInstance();
  private final List<String> locnIdList = new ArrayList<>();
  private final CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private final SSOManager ssoManager = SSOServiceFactory.getInstance();
  private ObjectMapper mapper = new ObjectMapper();
  private static ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    ExecutionContext.setRequestId(request.getRequestId());
    if (request.getOperation().equalsIgnoreCase(ActorOperations.PROCESS_BULK_UPLOAD.getValue())) {
      process(request);
    } else {
      onReceiveUnsupportedOperation(request.getOperation());
    }
  }

  private void process(Request actorMessage) {
    processId = (String) actorMessage.get(JsonKey.PROCESS_ID);
    Map<String, Object> dataMap = getBulkData(processId);
    int status = (int) dataMap.get(JsonKey.STATUS);
    if (!(status == (ProjectUtil.BulkProcessStatus.COMPLETED.getValue())
        || status == (ProjectUtil.BulkProcessStatus.INTERRUPT.getValue()))) {
      TypeReference<List<Map<String, Object>>> mapType =
          new TypeReference<List<Map<String, Object>>>() {};
      List<Map<String, Object>> jsonList = null;
      try {
        jsonList = mapper.readValue((String) dataMap.get(JsonKey.DATA), mapType);
      } catch (IOException e) {
        ProjectLogger.log(
            "Exception occurred while converting json String to List in BulkUploadBackGroundJobActor : ",
            e);
      }
      if (((String) dataMap.get(JsonKey.OBJECT_TYPE)).equalsIgnoreCase(JsonKey.USER)) {
        long startTime = System.currentTimeMillis();
        ProjectLogger.log(
            "BulkUploadBackGroundJobActor:processUserInfo start at : " + startTime,
            LoggerEnum.INFO.name());
        processUserInfo(jsonList, processId, (String) dataMap.get(JsonKey.UPLOADED_BY));
        ProjectLogger.log(
            "BulkUploadBackGroundJobActor:processUserInfo Total time taken : for processId  : "
                + processId
                + " : "
                + (System.currentTimeMillis() - startTime),
            LoggerEnum.INFO.name());
      } else if (((String) dataMap.get(JsonKey.OBJECT_TYPE)).equalsIgnoreCase(JsonKey.BATCH)) {
        processBatchEnrollment(jsonList, processId);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void processBatchEnrollment(List<Map<String, Object>> jsonList, String processId) {
    // update status from NEW to INProgress
    updateStatusForProcessing(processId);
    Util.DbInfo dbInfo = Util.dbInfoMap.get(JsonKey.COURSE_BATCH_DB);
    List<Map<String, Object>> successResultList = new ArrayList<>();
    List<Map<String, Object>> failureResultList = new ArrayList<>();

    Map<String, Object> successListMap = null;
    Map<String, Object> failureListMap = null;
    for (Map<String, Object> batchMap : jsonList) {
      successListMap = new HashMap<>();
      failureListMap = new HashMap<>();
      Map<String, Object> tempFailList = new HashMap<>();
      Map<String, Object> tempSuccessList = new HashMap<>();

      String batchId = (String) batchMap.get(JsonKey.BATCH_ID);
      Response courseBatchResult =
          cassandraOperation.getRecordById(dbInfo.getKeySpace(), dbInfo.getTableName(), batchId);
      String msg = validateBatchInfo(courseBatchResult);
      if (msg.equals(JsonKey.SUCCESS)) {
        List<Map<String, Object>> courseList =
            (List<Map<String, Object>>) courseBatchResult.get(JsonKey.RESPONSE);
        List<String> userList =
            new ArrayList<>(Arrays.asList((((String) batchMap.get(JsonKey.USER_IDs)).split(","))));
        validateBatchUserListAndAdd(
            courseList.get(0), batchId, userList, tempFailList, tempSuccessList);
        failureListMap.put(batchId, tempFailList.get(JsonKey.FAILURE_RESULT));
        successListMap.put(batchId, tempSuccessList.get(JsonKey.SUCCESS_RESULT));
      } else {
        batchMap.put(JsonKey.ERROR_MSG, msg);
        failureResultList.add(batchMap);
      }
      if (!successListMap.isEmpty()) {
        successResultList.add(successListMap);
      }
      if (!failureListMap.isEmpty()) {
        failureResultList.add(failureListMap);
      }
    }

    // Insert record to BulkDb table
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ID, processId);
    map.put(JsonKey.SUCCESS_RESULT, ProjectUtil.convertMapToJsonString(successResultList));
    map.put(JsonKey.FAILURE_RESULT, ProjectUtil.convertMapToJsonString(failureResultList));
    map.put(JsonKey.PROCESS_END_TIME, ProjectUtil.getFormattedDate());
    map.put(JsonKey.STATUS, ProjectUtil.BulkProcessStatus.COMPLETED.getValue());
    try {
      cassandraOperation.updateRecord(bulkDb.getKeySpace(), bulkDb.getTableName(), map);
    } catch (Exception e) {
      ProjectLogger.log(
          "Exception Occurred while updating bulk_upload_process in BulkUploadBackGroundJobActor : ",
          e);
    }
  }

  @SuppressWarnings("unchecked")
  private void validateBatchUserListAndAdd(
      Map<String, Object> courseBatchObject,
      String batchId,
      List<String> userIds,
      Map<String, Object> failList,
      Map<String, Object> successList) {
    Util.DbInfo dbInfo = Util.dbInfoMap.get(JsonKey.COURSE_BATCH_DB);
    Util.DbInfo userOrgdbInfo = Util.dbInfoMap.get(JsonKey.USR_ORG_DB);
    List<Map<String, Object>> failedUserList = new ArrayList<>();
    List<Map<String, Object>> passedUserList = new ArrayList<>();

    Map<String, Object> map = null;
    List<String> createdFor = (List<String>) courseBatchObject.get(JsonKey.COURSE_CREATED_FOR);
    Map<String, Boolean> participants =
        (Map<String, Boolean>) courseBatchObject.get(JsonKey.PARTICIPANT);
    if (participants == null) {
      participants = new HashMap<>();
    }
    // check whether can update user or not
    for (String userId : userIds) {
      if (!(participants.containsKey(userId))) {
        Response dbResponse =
            cassandraOperation.getRecordsByProperty(
                userOrgdbInfo.getKeySpace(), userOrgdbInfo.getTableName(), JsonKey.USER_ID, userId);
        List<Map<String, Object>> userOrgResult =
            (List<Map<String, Object>>) dbResponse.get(JsonKey.RESPONSE);

        if (userOrgResult.isEmpty()) {
          map = new HashMap<>();
          map.put(userId, ResponseCode.userNotAssociatedToOrg.getErrorMessage());
          failedUserList.add(map);
          continue;
        }
        boolean flag = false;
        for (int i = 0; i < userOrgResult.size() && !flag; i++) {
          Map<String, Object> usrOrgDetail = userOrgResult.get(i);
          if (createdFor.contains(usrOrgDetail.get(JsonKey.ORGANISATION_ID))) {
            participants.put(
                userId,
                addUserCourses(
                    batchId,
                    (String) courseBatchObject.get(JsonKey.COURSE_ID),
                    userId,
                    (Map<String, String>) (courseBatchObject.get(JsonKey.COURSE_ADDITIONAL_INFO))));
            flag = true;
          }
        }
        if (flag) {
          map = new HashMap<>();
          map.put(userId, JsonKey.SUCCESS);
          passedUserList.add(map);
        } else {
          map = new HashMap<>();
          map.put(userId, ResponseCode.userNotAssociatedToOrg.getErrorMessage());
          failedUserList.add(map);
        }

      } else {
        map = new HashMap<>();
        map.put(userId, JsonKey.SUCCESS);
        passedUserList.add(map);
      }
    }
    courseBatchObject.put(JsonKey.PARTICIPANT, participants);
    cassandraOperation.updateRecord(dbInfo.getKeySpace(), dbInfo.getTableName(), courseBatchObject);
    successList.put(JsonKey.SUCCESS_RESULT, passedUserList);
    failList.put(JsonKey.FAILURE_RESULT, failedUserList);
    // process Audit Log
    ProjectLogger.log("method call going to satrt for ES--.....");
    Request request = new Request();
    request.setOperation(ActorOperations.UPDATE_COURSE_BATCH_ES.getValue());
    request.getRequest().put(JsonKey.BATCH, courseBatchObject);
    ProjectLogger.log("making a call to save Course Batch data to ES");
    try {
      tellToAnother(request);
    } catch (Exception ex) {
      ProjectLogger.log(
          "Exception Occurred during saving Course Batch to Es while updating Course Batch : ", ex);
    }
  }

  private Boolean addUserCourses(
      String batchId, String courseId, String userId, Map<String, String> additionalCourseInfo) {

    Util.DbInfo courseEnrollmentdbInfo = Util.dbInfoMap.get(JsonKey.LEARNER_COURSE_DB);
    Util.DbInfo coursePublishdbInfo = Util.dbInfoMap.get(JsonKey.COURSE_PUBLISHED_STATUS);
    Response response =
        cassandraOperation.getRecordById(
            coursePublishdbInfo.getKeySpace(), coursePublishdbInfo.getTableName(), courseId);
    List<Map<String, Object>> resultList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (!ProjectUtil.CourseMgmtStatus.LIVE
        .getValue()
        .equalsIgnoreCase(additionalCourseInfo.get(JsonKey.STATUS))) {
      if (resultList.isEmpty()) {
        return false;
      }
      Map<String, Object> publishStatus = resultList.get(0);

      if (Status.ACTIVE.getValue() != (Integer) publishStatus.get(JsonKey.STATUS)) {
        return false;
      }
    }
    Boolean flag = false;
    Timestamp ts = new Timestamp(new Date().getTime());
    Map<String, Object> userCourses = new HashMap<>();
    userCourses.put(JsonKey.USER_ID, userId);
    userCourses.put(JsonKey.BATCH_ID, batchId);
    userCourses.put(JsonKey.COURSE_ID, courseId);
    userCourses.put(JsonKey.ID, generatePrimaryKey(userCourses));
    userCourses.put(JsonKey.CONTENT_ID, courseId);
    userCourses.put(JsonKey.COURSE_ENROLL_DATE, ProjectUtil.getFormattedDate());
    userCourses.put(JsonKey.ACTIVE, ProjectUtil.ActiveStatus.ACTIVE.getValue());
    userCourses.put(JsonKey.STATUS, ProjectUtil.ProgressStatus.NOT_STARTED.getValue());
    userCourses.put(JsonKey.DATE_TIME, ts);
    userCourses.put(JsonKey.COURSE_PROGRESS, 0);
    userCourses.put(JsonKey.COURSE_LOGO_URL, additionalCourseInfo.get(JsonKey.COURSE_LOGO_URL));
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
          courseEnrollmentdbInfo.getKeySpace(), courseEnrollmentdbInfo.getTableName(), userCourses);
      // TODO: for some reason, ES indexing is failing with TimestelemetryProcessingCalltamp value.
      // need to
      // check and
      // correct it.
      userCourses.put(JsonKey.DATE_TIME, ProjectUtil.formatDate(ts));
      insertUserCoursesToES(userCourses);
      flag = true;
      Map<String, Object> targetObject =
          TelemetryUtil.generateTargetObject(userId, JsonKey.USER, JsonKey.UPDATE, null);
      List<Map<String, Object>> correlatedObject = new ArrayList<>();
      TelemetryUtil.generateCorrelatedObject(batchId, JsonKey.BATCH, null, correlatedObject);
      TelemetryUtil.telemetryProcessingCall(userCourses, targetObject, correlatedObject);
    } catch (Exception ex) {
      ProjectLogger.log("INSERT RECORD TO USER COURSES EXCEPTION ", ex);
      flag = false;
    }
    return flag;
  }

  private void insertUserCoursesToES(Map<String, Object> courseMap) {
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
  private String validateBatchInfo(Response courseBatchResult) {
    // check batch exist in db or not
    List<Map<String, Object>> courseList =
        (List<Map<String, Object>>) courseBatchResult.get(JsonKey.RESPONSE);
    if ((courseList.isEmpty())) {
      return ResponseCode.invalidCourseBatchId.getErrorMessage();
    }
    Map<String, Object> courseBatchObject = courseList.get(0);
    // check whether coursebbatch type is invite only or not ...
    if (ProjectUtil.isNull(courseBatchObject.get(JsonKey.ENROLLMENT_TYPE))
        || !((String) courseBatchObject.get(JsonKey.ENROLLMENT_TYPE))
            .equalsIgnoreCase(JsonKey.INVITE_ONLY)) {
      return ResponseCode.enrollmentTypeValidation.getErrorMessage();
    }
    if (ProjectUtil.isNull(courseBatchObject.get(JsonKey.COURSE_CREATED_FOR))
        || ((List) courseBatchObject.get(JsonKey.COURSE_CREATED_FOR)).isEmpty()) {
      return ResponseCode.courseCreatedForIsNull.getErrorMessage();
    }
    return JsonKey.SUCCESS;
  }

  private List<Map<String, Object>> searchOrgByChannel(String channel, Util.DbInfo orgDbInfo) {
    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.CHANNEL, channel);
    filters.put(JsonKey.IS_ROOT_ORG, true);

    Response orgResponse =
        cassandraOperation.getRecordsByProperties(
            orgDbInfo.getKeySpace(), orgDbInfo.getTableName(), filters);
    List<Map<String, Object>> rootOrgListRes =
        (List<Map<String, Object>>) orgResponse.get(JsonKey.RESPONSE);
    return rootOrgListRes;
  }

  private void generateTelemetryForOrganisation(
      Map<String, Object> map, String id, boolean isOrgUpdated) {

    String orgState = JsonKey.CREATE;
    if (isOrgUpdated) {
      orgState = JsonKey.UPDATE;
    }
    Map<String, Object> targetObject =
        TelemetryUtil.generateTargetObject(id, JsonKey.ORGANISATION, orgState, null);
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    TelemetryUtil.generateCorrelatedObject(id, JsonKey.ORGANISATION, null, correlatedObject);
    TelemetryUtil.telemetryProcessingCall(map, targetObject, correlatedObject);
  }

  private String validateLocationId(String locId) {
    String locnId = null;
    try {
      if (locnIdList.isEmpty()) {
        Util.DbInfo geoLocDbInfo = Util.dbInfoMap.get(JsonKey.GEO_LOCATION_DB);
        Response response =
            cassandraOperation.getAllRecords(
                geoLocDbInfo.getKeySpace(), geoLocDbInfo.getTableName());
        List<Map<String, Object>> list = (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
        if (!list.isEmpty()) {
          for (Map<String, Object> map : list) {
            locnIdList.add(((String) map.get(JsonKey.ID)));
          }
        }
      }
      if (locnIdList.contains(locId)) {
        return locId;
      } else {
        return null;
      }
    } catch (Exception ex) {
      ProjectLogger.log("Exception occurred while validating location id ", ex);
    }
    return locnId;
  }

  private String validateOrgType(String orgType) {
    String orgTypeId = null;
    try {
      if (!StringUtils.isBlank(DataCacheHandler.getOrgTypeMap().get(orgType.toLowerCase()))) {
        orgTypeId = DataCacheHandler.getOrgTypeMap().get(orgType.toLowerCase());
      } else {
        Util.DbInfo orgTypeDbInfo = Util.dbInfoMap.get(JsonKey.ORG_TYPE_DB);
        Response response =
            cassandraOperation.getAllRecords(
                orgTypeDbInfo.getKeySpace(), orgTypeDbInfo.getTableName());
        List<Map<String, Object>> list = (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
        if (!list.isEmpty()) {
          for (Map<String, Object> map : list) {
            if ((((String) map.get(JsonKey.NAME)).toLowerCase())
                .equalsIgnoreCase(orgType.toLowerCase())) {
              orgTypeId = (String) map.get(JsonKey.ID);
              DataCacheHandler.getOrgTypeMap()
                  .put(
                      ((String) map.get(JsonKey.NAME)).toLowerCase(), (String) map.get(JsonKey.ID));
            }
          }
        }
      }
    } catch (Exception ex) {
      ProjectLogger.log("Exception occurred while getting orgTypeId from OrgType", ex);
    }
    return orgTypeId;
  }

  private Map<String, Object> elasticSearchComplexSearch(
      Map<String, Object> filters, String index, String type) {

    SearchDTO searchDTO = new SearchDTO();
    searchDTO.getAdditionalProperties().put(JsonKey.FILTERS, filters);

    Future<Map<String, Object>> resultF = esService.search(searchDTO, type);
    return (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
  }

  private void processUserInfo(
      List<Map<String, Object>> dataMapList, String processId, String updatedBy) {
    // update status from NEW to INProgress
    updateStatusForProcessing(processId);
    Util.DbInfo usrDbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
    List<Map<String, Object>> failureUserReq = new ArrayList<>();
    List<Map<String, Object>> successUserReq = new ArrayList<>();
    Map<String, Object> userMap = null;
    /*
     * To store hashTagId inside user_org table, first we need to get hashTagId from
     * provided organisation ID. Currently in bulk user upload, we are passing only
     * one organisation, so we can get the details before for loop and reuse it.
     */
    String hashTagId = null;
    if (dataMapList != null && dataMapList.size() > 0) {
      String orgId = (String) dataMapList.get(0).get(JsonKey.ORGANISATION_ID);
      hashTagId = Util.getHashTagIdFromOrgId(orgId);
    }
    for (int i = 0; i < dataMapList.size(); i++) {
      userMap = dataMapList.get(i);
      Map<String, Object> welcomeMailTemplateMap = new HashMap<>();
      String errMsg = validateUser(userMap);
      if (JsonKey.SUCCESS.equalsIgnoreCase(errMsg)) {
        try {

          // convert userName,provide,loginId,externalId.. value to lowercase
          updateMapSomeValueTOLowerCase(userMap);
          Map<String, Object> foundUserMap = findUser(userMap);
          foundUserMap = insertRecordToKeyCloak(userMap, foundUserMap, updatedBy);
          Map<String, Object> tempMap = new HashMap<>();
          tempMap.putAll(userMap);
          tempMap.remove(JsonKey.EMAIL_VERIFIED);
          tempMap.remove(JsonKey.POSITION);
          // remove externalID and Provider as we are not saving these to user table
          tempMap.remove(JsonKey.EXTERNAL_ID);
          tempMap.remove(JsonKey.EXTERNAL_ID_PROVIDER);
          tempMap.remove(JsonKey.EXTERNAL_ID_TYPE);
          tempMap.remove(JsonKey.ORGANISATION_ID);
          tempMap.put(JsonKey.EMAIL_VERIFIED, false);
          Response response = null;
          if (null == tempMap.get(JsonKey.OPERATION)) {
            // will allowing only PUBLIC role at user level.
            tempMap.remove(JsonKey.ROLES);
            // insert user record
            // Add only PUBLIC role to user
            List<String> list = new ArrayList<>();
            list.add(ProjectUtil.UserRole.PUBLIC.getValue());
            tempMap.put(JsonKey.ROLES, list);
            try {
              UserUtility.encryptUserData(tempMap);
            } catch (Exception ex) {
              ProjectLogger.log(
                  "Exception occurred while bulk user upload in BulkUploadBackGroundJobActor during data encryption :",
                  ex);
              throw new ProjectCommonException(
                  ResponseCode.userDataEncryptionError.getErrorCode(),
                  ResponseCode.userDataEncryptionError.getErrorMessage(),
                  ResponseCode.SERVER_ERROR.getResponseCode());
            }
            tempMap.put(JsonKey.CREATED_BY, updatedBy);
            tempMap.put(JsonKey.IS_DELETED, false);
            tempMap.remove(JsonKey.EXTERNAL_IDS);
            try {
              response =
                  cassandraOperation.insertRecord(
                      usrDbInfo.getKeySpace(), usrDbInfo.getTableName(), tempMap);
              // insert details to user_org table

              userMap.put(JsonKey.HASHTAGID, hashTagId);
              registerUserToOrg(userMap, JsonKey.CREATE);
              // removing added hashTagId
              userMap.remove(JsonKey.HASHTAGID);
            } catch (Exception ex) {
              // incase of exception also removing added hashTagId
              userMap.remove(JsonKey.HASHTAGID);
              ProjectLogger.log(
                  "Exception occurred while bulk user upload in BulkUploadBackGroundJobActor:", ex);
              userMap.remove(JsonKey.ID);
              userMap.remove(JsonKey.PASSWORD);
              userMap.put(JsonKey.ERROR_MSG, ex.getMessage() + " ,user insertion failed.");
              removeOriginalExternalIds(userMap.get(JsonKey.EXTERNAL_IDS));
              failureUserReq.add(userMap);
              continue;
            } finally {
              if (null == response) {
                ssoManager.removeUser(userMap);
              }
            }
            sendEmailAndSms(userMap, welcomeMailTemplateMap);
            // object of telemetry event...
            Map<String, Object> targetObject = null;
            List<Map<String, Object>> correlatedObject = new ArrayList<>();

            targetObject =
                TelemetryUtil.generateTargetObject(
                    (String) userMap.get(JsonKey.ID), JsonKey.USER, JsonKey.CREATE, null);
            TelemetryUtil.telemetryProcessingCall(userMap, targetObject, correlatedObject);
          } else {
            // update user record
            tempMap.put(JsonKey.UPDATED_BY, updatedBy);
            tempMap.put(JsonKey.UPDATED_DATE, ProjectUtil.getFormattedDate());
            try {
              UserUtility.encryptUserData(tempMap);
            } catch (Exception ex) {
              ProjectLogger.log(
                  "Exception occurred while bulk user upload in BulkUploadBackGroundJobActor during data encryption :",
                  ex);
              throw new ProjectCommonException(
                  ResponseCode.userDataEncryptionError.getErrorCode(),
                  ResponseCode.userDataEncryptionError.getErrorMessage(),
                  ResponseCode.SERVER_ERROR.getResponseCode());
            }
            try {
              removeFieldsFrmUpdateReq(tempMap);
              response =
                  cassandraOperation.updateRecord(
                      usrDbInfo.getKeySpace(), usrDbInfo.getTableName(), tempMap);
              // update user-org table(role update)
              userMap.put(JsonKey.UPDATED_BY, updatedBy);
              userMap.put(JsonKey.HASHTAGID, hashTagId);
              registerUserToOrg(userMap, JsonKey.UPDATE);
              userMap.remove(JsonKey.HASHTAGID);
            } catch (Exception ex) {
              userMap.remove(JsonKey.HASHTAGID);
              ProjectLogger.log(
                  "Exception occurred while bulk user upload in BulkUploadBackGroundJobActor:", ex);
              userMap.remove(JsonKey.ID);
              userMap.remove(JsonKey.PASSWORD);
              userMap.put(JsonKey.ERROR_MSG, ex.getMessage() + " ,user updation failed.");
              removeOriginalExternalIds(userMap.get(JsonKey.EXTERNAL_IDS));
              failureUserReq.add(userMap);
              continue;
            }
          }

          // update the user external identity data
          try {
            if (null != userMap.get(JsonKey.EXTERNAL_IDS)) {
              Util.updateUserExtId(userMap);
              removeOriginalExternalIds(userMap.get(JsonKey.EXTERNAL_IDS));
            }
          } catch (Exception ex) {
            removeOriginalExternalIds(userMap.get(JsonKey.EXTERNAL_IDS));
            userMap.put(
                JsonKey.ERROR_MSG, "Update of user external IDs failed. " + ex.getMessage());
          }
          // save successfully created user data
          tempMap.putAll(userMap);
          tempMap.remove(JsonKey.STATUS);
          tempMap.remove(JsonKey.CREATED_DATE);
          tempMap.remove(JsonKey.CREATED_BY);
          tempMap.remove(JsonKey.ID);
          tempMap.remove(JsonKey.LOGIN_ID);
          tempMap.put(JsonKey.PASSWORD, "*****");
          successUserReq.add(tempMap);

          // update elastic search
          ProjectLogger.log(
              "making a call to save user data to ES in BulkUploadBackGroundJobActor");
          Request request = new Request();
          request.setOperation(ActorOperations.UPDATE_USER_INFO_ELASTIC.getValue());
          request.getRequest().put(JsonKey.ID, userMap.get(JsonKey.ID));
          tellToAnother(request);
          // object of telemetry event...
          Map<String, Object> targetObject = null;
          List<Map<String, Object>> correlatedObject = new ArrayList<>();
          targetObject =
              TelemetryUtil.generateTargetObject(
                  (String) userMap.get(JsonKey.ID), JsonKey.USER, JsonKey.UPDATE, null);
          TelemetryUtil.telemetryProcessingCall(userMap, targetObject, correlatedObject);
        } catch (Exception ex) {
          ProjectLogger.log(
              "Exception occurred while bulk user upload in BulkUploadBackGroundJobActor:", ex);
          userMap.remove(JsonKey.ID);
          userMap.remove(JsonKey.PASSWORD);
          userMap.put(JsonKey.ERROR_MSG, ex.getMessage());
          removeOriginalExternalIds(userMap.get(JsonKey.EXTERNAL_IDS));
          failureUserReq.add(userMap);
        }
      } else {
        userMap.put(JsonKey.ERROR_MSG, errMsg);
        removeOriginalExternalIds(userMap.get(JsonKey.EXTERNAL_IDS));
        failureUserReq.add(userMap);
      }
    }
    updateSuccessAndFailureResultToDb(processId, failureUserReq, successUserReq);
  }

  private void updateSuccessAndFailureResultToDb(
      String processId,
      List<Map<String, Object>> failureUserReq,
      List<Map<String, Object>> successUserReq) {
    // Insert record to BulkDb table
    // After Successful completion of bulk upload process , encrypt the success and
    // failure result
    // and delete the user data(csv file data)
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ID, processId);
    try {
      map.put(
          JsonKey.SUCCESS_RESULT,
          UserUtility.encryptData(ProjectUtil.convertMapToJsonString(successUserReq)));
      map.put(
          JsonKey.FAILURE_RESULT,
          UserUtility.encryptData(ProjectUtil.convertMapToJsonString(failureUserReq)));
    } catch (Exception e1) {
      ProjectLogger.log(
          "Exception occurred while encrypting success and failure result in bulk upload process : ",
          e1);
    }
    map.put(JsonKey.PROCESS_END_TIME, ProjectUtil.getFormattedDate());
    map.put(JsonKey.STATUS, ProjectUtil.BulkProcessStatus.COMPLETED.getValue());
    map.put(JsonKey.DATA, "");
    try {
      cassandraOperation.updateRecord(bulkDb.getKeySpace(), bulkDb.getTableName(), map);
    } catch (Exception e) {
      ProjectLogger.log(
          "Exception Occurred while updating bulk_upload_process in BulkUploadBackGroundJobActor : ",
          e);
    }
  }

  private void sendEmailAndSms(
      Map<String, Object> userMap, Map<String, Object> welcomeMailTemplateMap) {
    Map<String, Object> usrMap = new HashMap<>(userMap);
    usrMap.put(JsonKey.REDIRECT_URI, Util.getSunbirdWebUrlPerTenent(userMap));
    usrMap.put(JsonKey.USERNAME, userMap.get(JsonKey.LOGIN_ID));
    // generate required action link and shorten the url
    Util.getUserRequiredActionLink(usrMap);
    // send the welcome mail to user
    welcomeMailTemplateMap.putAll(usrMap);
    Request welcomeMailReqObj = Util.sendOnboardingMail(welcomeMailTemplateMap);
    if (null != welcomeMailReqObj) {
      tellToAnother(welcomeMailReqObj);
    }

    if (StringUtils.isNotBlank((String) usrMap.get(JsonKey.PHONE))) {
      Util.sendSMS(usrMap);
    }
  }

  private void registerUserToOrg(Map<String, Object> userMap, String operation) {
    if (((String) userMap.get(JsonKey.ORGANISATION_ID))
        .equalsIgnoreCase((String) userMap.get(JsonKey.ROOT_ORG_ID))) {
      if (operation.equalsIgnoreCase(JsonKey.CREATE)) {
        Util.registerUserToOrg(userMap);
      } else {
        // for update
        Util.upsertUserOrgData(userMap);
      }
    } else {
      Map<String, Object> map = new HashMap<>(userMap);
      if (operation.equalsIgnoreCase(JsonKey.CREATE)) {
        // first register for subOrg then root org
        Util.registerUserToOrg(map);
        prepareRequestForAddingUserToRootOrg(userMap, map);
        // register user to root org
        Util.registerUserToOrg(map);
      } else {
        // for update
        // first register for subOrg then root org
        Util.upsertUserOrgData(map);
        prepareRequestForAddingUserToRootOrg(userMap, map);
        // register user to root org
        Util.upsertUserOrgData(userMap);
      }
    }
  }

  private void prepareRequestForAddingUserToRootOrg(
      Map<String, Object> userMap, Map<String, Object> map) {
    map.put(JsonKey.ORGANISATION_ID, userMap.get(JsonKey.ROOT_ORG_ID));
    List<String> roles = Arrays.asList(ProjectUtil.UserRole.PUBLIC.getValue());
    map.put(JsonKey.ROLES, roles);
  }

  private void removeOriginalExternalIds(Object externalIds) {
    if (externalIds instanceof List) {
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> request = (List<Map<String, Object>>) externalIds;
      try {
        request.forEach(
            externalId -> {
              externalId.put(JsonKey.ID, externalId.get(JsonKey.ORIGINAL_EXTERNAL_ID));
              externalId.remove(JsonKey.ORIGINAL_EXTERNAL_ID);
              externalId.put(JsonKey.PROVIDER, externalId.get(JsonKey.ORIGINAL_PROVIDER));
              externalId.remove(JsonKey.ORIGINAL_PROVIDER);
              externalId.put(JsonKey.ID_TYPE, externalId.get(JsonKey.ORIGINAL_ID_TYPE));
              externalId.remove(JsonKey.ORIGINAL_ID_TYPE);
            });
      } catch (Exception ex) {
        ProjectLogger.log(
            "BulkUploadBackGroundJobActor:removeOriginalExternalIds : exception msg: " + ex);
      }
    }
  }

  private void parseExternalIds(Map<String, Object> userMap) throws IOException {
    if (userMap.containsKey(JsonKey.EXTERNAL_IDS)
        && StringUtils.isNotBlank((String) userMap.get(JsonKey.EXTERNAL_IDS))) {
      String externalIds = (String) userMap.get(JsonKey.EXTERNAL_IDS);
      List<Map<String, String>> externalIdList = new ArrayList<>();
      externalIdList = mapper.readValue(externalIds, List.class);
      userMap.put(JsonKey.EXTERNAL_IDS, externalIdList);
    }
  }

  private void convertCommaSepStringToList(Map<String, Object> map, String property) {
    String[] props = ((String) map.get(property)).split(",");
    List<String> list = new ArrayList<>(Arrays.asList(props));
    map.put(property, list);
  }

  private void updateStatusForProcessing(String processId) {
    // Update status to BulkDb table
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ID, processId);
    map.put(JsonKey.STATUS, ProjectUtil.BulkProcessStatus.IN_PROGRESS.getValue());
    try {
      cassandraOperation.updateRecord(bulkDb.getKeySpace(), bulkDb.getTableName(), map);
    } catch (Exception e) {
      ProjectLogger.log(
          "Exception Occurred while updating bulk_upload_process in BulkUploadBackGroundJobActor : ",
          e);
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> getBulkData(String processId) {
    try {
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.ID, processId);
      map.put(JsonKey.PROCESS_START_TIME, ProjectUtil.getFormattedDate());
      map.put(JsonKey.STATUS, ProjectUtil.BulkProcessStatus.IN_PROGRESS.getValue());
      cassandraOperation.updateRecord(bulkDb.getKeySpace(), bulkDb.getTableName(), map);
    } catch (Exception ex) {
      ProjectLogger.log(
          "Exception occurred while updating status to bulk_upload_process "
              + "table in BulkUploadBackGroundJobActor.",
          ex);
    }
    Response res =
        cassandraOperation.getRecordById(bulkDb.getKeySpace(), bulkDb.getTableName(), processId);
    return (((List<Map<String, Object>>) res.get(JsonKey.RESPONSE)).get(0));
  }

  private void updateRecordToUserOrgTable(Map<String, Object> map, String updatedBy) {
    Util.DbInfo usrOrgDb = Util.dbInfoMap.get(JsonKey.USR_ORG_DB);
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.ID, map.get(JsonKey.ID));
    reqMap.put(JsonKey.IS_DELETED, false);
    reqMap.put(JsonKey.UPDATED_BY, updatedBy);
    reqMap.put(JsonKey.UPDATED_DATE, ProjectUtil.getFormattedDate());
    try {
      cassandraOperation.updateRecord(usrOrgDb.getKeySpace(), usrOrgDb.getTableName(), reqMap);
    } catch (Exception e) {
      ProjectLogger.log(e.getMessage(), e);
    }
  }

  private Map<String, Object> findUser(Map<String, Object> requestedUserMap) {
    Map<String, Object> foundUserMap = null;
    String extId = (String) requestedUserMap.get(JsonKey.EXTERNAL_ID);
    String provider = (String) requestedUserMap.get(JsonKey.EXTERNAL_ID_PROVIDER);
    String idType = (String) requestedUserMap.get(JsonKey.EXTERNAL_ID_TYPE);
    String userName = (String) requestedUserMap.get(JsonKey.USERNAME);
    if (StringUtils.isNotBlank(extId)
        && StringUtils.isNotBlank(provider)
        && StringUtils.isNotBlank(idType)
        && StringUtils.isBlank(userName)) {
      foundUserMap = Util.getUserFromExternalId(requestedUserMap);
      if (MapUtils.isEmpty(foundUserMap)) {
        throw new ProjectCommonException(
            ResponseCode.externalIdNotFound.getErrorCode(),
            ProjectUtil.formatMessage(
                ResponseCode.externalIdNotFound.getErrorMessage(), extId, idType, provider),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
    } else {
      foundUserMap = getRecordByLoginId(requestedUserMap);
    }
    return foundUserMap;
  }

  private Map<String, Object> insertRecordToKeyCloak(
      Map<String, Object> requestedUserMap, Map<String, Object> foundUserMap, String updatedBy)
      throws Exception {
    requestedUserMap.put(JsonKey.UPDATED_BY, updatedBy);
    if (MapUtils.isNotEmpty(foundUserMap)) {
      updateUser(requestedUserMap, foundUserMap);
    } else {
      createUser(requestedUserMap);
    }
    return requestedUserMap;
  }

  private Map<String, Object> getRecordByLoginId(Map<String, Object> userMap) {
    Util.DbInfo usrDbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
    Map<String, Object> user = null;
    userMap.put(JsonKey.LOGIN_ID, Util.getLoginId(userMap));
    String loginId = Util.getEncryptedData((String) userMap.get(JsonKey.LOGIN_ID));
    Response resultFrUserName =
        cassandraOperation.getRecordsByProperty(
            usrDbInfo.getKeySpace(), usrDbInfo.getTableName(), JsonKey.LOGIN_ID, loginId);
    if (CollectionUtils.isNotEmpty(
        (List<Map<String, Object>>) resultFrUserName.get(JsonKey.RESPONSE))) {
      user = ((List<Map<String, Object>>) resultFrUserName.get(JsonKey.RESPONSE)).get(0);
    }
    return user;
  }

  private void createUser(Map<String, Object> userMap) throws Exception {
    // user doesn't exist
    validateExternalIds(userMap, JsonKey.CREATE);

    try {
      String userId = "";
      userMap.put(JsonKey.BULK_USER_UPLOAD, true);
      Util.checkEmailUniqueness(userMap, JsonKey.CREATE);
      Util.checkPhoneUniqueness(userMap, JsonKey.CREATE);
      ssoManager.updatePassword(userId, (String) userMap.get(JsonKey.PASSWORD));
      userMap.remove(JsonKey.BULK_USER_UPLOAD);

      if (!StringUtils.isBlank(userId)) {
        userMap.put(JsonKey.USER_ID, userId);
        userMap.put(JsonKey.ID, userId);
      } else {
        throw new ProjectCommonException(
            ResponseCode.userRegUnSuccessfull.getErrorCode(),
            ResponseCode.userRegUnSuccessfull.getErrorMessage(),
            ResponseCode.SERVER_ERROR.getResponseCode());
      }
    } catch (Exception exception) {
      ProjectLogger.log("Exception occurred while creating user in keycloak ", exception);
      throw exception;
    }
    userMap.put(JsonKey.CREATED_DATE, ProjectUtil.getFormattedDate());
    userMap.put(JsonKey.STATUS, ProjectUtil.Status.ACTIVE.getValue());
    userMap.put(JsonKey.IS_DELETED, false);
    if (!StringUtils.isBlank((String) userMap.get(JsonKey.COUNTRY_CODE))) {
      userMap.put(
          JsonKey.COUNTRY_CODE, propertiesCache.getProperty("sunbird_default_country_code"));
    }
    /**
     * set role as PUBLIC by default if role is empty in request body. And if roles are coming in
     * request body, then check for PUBLIC role , if not present then add PUBLIC role to the list
     */
    if (null == userMap.get(JsonKey.ROLES)) {
      userMap.put(JsonKey.ROLES, new ArrayList<String>());
    }
    List<String> roles = (List<String>) userMap.get(JsonKey.ROLES);
    if (!roles.contains(ProjectUtil.UserRole.PUBLIC.getValue())) {
      roles.add(ProjectUtil.UserRole.PUBLIC.getValue());
      userMap.put(JsonKey.ROLES, roles);
    }
  }

  private void updateUser(Map<String, Object> userMap, Map<String, Object> userDbRecord) {
    // user exist
    if (null != userDbRecord.get(JsonKey.IS_DELETED)
        && (boolean) userDbRecord.get(JsonKey.IS_DELETED)) {
      throw new ProjectCommonException(
          ResponseCode.inactiveUser.getErrorCode(),
          ResponseCode.inactiveUser.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    userMap.put(JsonKey.ID, userDbRecord.get(JsonKey.ID));
    userMap.put(JsonKey.USER_ID, userDbRecord.get(JsonKey.ID));
    userMap.put(JsonKey.OPERATION, JsonKey.UPDATE);

    validateExternalIds(userMap, JsonKey.UPDATE);

    Util.checkEmailUniqueness(userMap, JsonKey.UPDATE);
    Util.checkPhoneUniqueness(userMap, JsonKey.UPDATE);
    String email = "";
    try {
      email = encryptionService.encryptData((String) userMap.get(JsonKey.EMAIL));
    } catch (Exception ex) {
      ProjectLogger.log(
          "Exception occurred while bulk user upload in BulkUploadBackGroundJobActor during encryption of loginId:",
          ex);
      throw new ProjectCommonException(
          ResponseCode.userDataEncryptionError.getErrorCode(),
          ResponseCode.userDataEncryptionError.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    if (null != (String) userDbRecord.get(JsonKey.EMAIL)
        && ((String) userDbRecord.get(JsonKey.EMAIL)).equalsIgnoreCase(email)) {
      // DB email value and req email value both are same , no need to update
      email = (String) userMap.get(JsonKey.EMAIL);
      userMap.remove(JsonKey.EMAIL);
    }
    // check user is active for this Organisation or not
    isUserDeletedFromOrg(userMap, (String) userMap.get(JsonKey.UPDATED_BY));
    updateKeyCloakUserBase(userMap);
    email = decryptionService.decryptData(email);
    userMap.put(JsonKey.EMAIL, email);
  }

  private void validateExternalIds(Map<String, Object> userMap, String operation) {
    if (CollectionUtils.isNotEmpty((List<Map<String, String>>) userMap.get(JsonKey.EXTERNAL_IDS))) {
      List<Map<String, String>> list =
          Util.copyAndConvertExternalIdsToLower(
              (List<Map<String, String>>) userMap.get(JsonKey.EXTERNAL_IDS));
      userMap.put(JsonKey.EXTERNAL_IDS, list);
    }
    User user = mapper.convertValue(userMap, User.class);
    Util.checkExternalIdUniqueness(user, operation);
    if (JsonKey.UPDATE.equalsIgnoreCase(operation)) {
      Util.validateUserExternalIds(userMap);
    }
  }

  private boolean isUserDeletedFromOrg(Map<String, Object> userMap, String updatedBy) {
    Util.DbInfo usrOrgDbInfo = Util.dbInfoMap.get(JsonKey.USER_ORG_DB);
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.USER_ID, userMap.get(JsonKey.ID));
    map.put(JsonKey.ORGANISATION_ID, userMap.get(JsonKey.ORGANISATION_ID));
    Response response =
        cassandraOperation.getRecordsByProperties(
            usrOrgDbInfo.getKeySpace(), usrOrgDbInfo.getTableName(), map);
    List<Map<String, Object>> resList = (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (!resList.isEmpty()) {
      Map<String, Object> res = resList.get(0);
      if (null != res.get(JsonKey.IS_DELETED)) {
        boolean bool = (boolean) (res.get(JsonKey.IS_DELETED));
        // if deleted then add this user to org and proceed
        try {
          if (bool) {
            updateRecordToUserOrgTable(res, updatedBy);
          }
        } catch (Exception ex) {
          throw new ProjectCommonException(
              ResponseCode.userUpdateToOrgFailed.getErrorCode(),
              ResponseCode.userUpdateToOrgFailed.getErrorMessage(),
              ResponseCode.CLIENT_ERROR.getResponseCode());
        }
        return bool;
      } else {
        return false;
      }
    }
    return false;
  }

  private String validateUser(Map<String, Object> userMap) {
    if (null != userMap.get(JsonKey.EXTERNAL_IDS)) {
      try {
        parseExternalIds(userMap);
      } catch (Exception ex) {
        return ProjectUtil.formatMessage(
            ResponseMessage.Message.PARSING_FAILED, JsonKey.EXTERNAL_IDS);
      }
    }
    userMap.put(JsonKey.EMAIL_VERIFIED, false);
    if (!StringUtils.isBlank((String) userMap.get(JsonKey.PHONE_VERIFIED))) {
      try {
        userMap.put(
            JsonKey.PHONE_VERIFIED,
            Boolean.parseBoolean((String) userMap.get(JsonKey.PHONE_VERIFIED)));
      } catch (Exception ex) {
        return ProjectUtil.formatMessage(
            ResponseMessage.Message.DATA_TYPE_ERROR, JsonKey.PHONE_VERIFIED, "Boolean");
      }
    }
    if (null != userMap.get(JsonKey.ROLES)) {
      convertCommaSepStringToList(userMap, JsonKey.ROLES);
    }
    if (null != userMap.get(JsonKey.GRADE)) {
      convertCommaSepStringToList(userMap, JsonKey.GRADE);
    }

    if (null != userMap.get(JsonKey.SUBJECT)) {
      convertCommaSepStringToList(userMap, JsonKey.SUBJECT);
    }

    if (null != userMap.get(JsonKey.LANGUAGE)) {
      convertCommaSepStringToList(userMap, JsonKey.LANGUAGE);
    }
    Request request = new Request();
    request.getRequest().putAll(userMap);
    try {
      new UserRequestValidator().validateBulkUserData(request);
      List<String> roles = (List<String>) userMap.get(JsonKey.ROLES);
      if (CollectionUtils.isNotEmpty(roles)) {
        String response = Util.validateRoles(roles);
        if (!JsonKey.SUCCESS.equalsIgnoreCase(response)) {
          return ResponseMessage.Message.INVALID_ROLE;
        }
        roles = roles.stream().map(s -> s.trim()).collect(Collectors.toList());
        userMap.put(JsonKey.ROLES, roles);
      }
    } catch (Exception ex) {
      return ex.getMessage();
    }

    return JsonKey.SUCCESS;
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

  /**
   * This method will make some requested key value as lower case.
   *
   * @param map Request
   */
  public static void updateMapSomeValueTOLowerCase(Map<String, Object> map) {
    if (map.get(JsonKey.SOURCE) != null) {
      map.put(JsonKey.SOURCE, ((String) map.get(JsonKey.SOURCE)).toLowerCase());
    }
    if (map.get(JsonKey.EXTERNAL_ID) != null) {
      map.put(JsonKey.EXTERNAL_ID, ((String) map.get(JsonKey.EXTERNAL_ID)).toLowerCase());
    }
    if (map.get(JsonKey.USERNAME) != null) {
      map.put(JsonKey.USERNAME, ((String) map.get(JsonKey.USERNAME)).toLowerCase());
    }
    if (map.get(JsonKey.USER_NAME) != null) {
      map.put(JsonKey.USER_NAME, ((String) map.get(JsonKey.USER_NAME)).toLowerCase());
    }
    if (map.get(JsonKey.PROVIDER) != null) {
      map.put(JsonKey.PROVIDER, ((String) map.get(JsonKey.PROVIDER)).toLowerCase());
    }
    if (map.get(JsonKey.LOGIN_ID) != null) {
      map.put(JsonKey.LOGIN_ID, ((String) map.get(JsonKey.LOGIN_ID)).toLowerCase());
    }
  }

  private void updateKeyCloakUserBase(Map<String, Object> userMap) {
    try {
      String userId = ssoManager.updateUser(userMap);
      if (!(!StringUtils.isBlank(userId) && userId.equalsIgnoreCase(JsonKey.SUCCESS))) {
        throw new ProjectCommonException(
            ResponseCode.userUpdationUnSuccessfull.getErrorCode(),
            ResponseCode.userUpdationUnSuccessfull.getErrorMessage(),
            ResponseCode.SERVER_ERROR.getResponseCode());
      }
    } catch (Exception e) {
      ProjectLogger.log(e.getMessage(), e);
      throw new ProjectCommonException(
          ResponseCode.userUpdationUnSuccessfull.getErrorCode(),
          ResponseCode.userUpdationUnSuccessfull.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
  }

  // method will compare two strings and return true id both are same otherwise
  // false ...
  private boolean compareStrings(String first, String second) {

    if (isNull(first) && isNull(second)) {
      return true;
    }
    if ((isNull(first) && isNotNull(second)) || (isNull(second) && isNotNull(first))) {
      return false;
    }
    return first.equalsIgnoreCase(second);
  }

  /**
   * Fields which are not allowed to update while updating user info.
   *
   * @param userMap
   */
  private void removeFieldsFrmUpdateReq(Map<String, Object> userMap) {
    userMap.remove(JsonKey.OPERATION);
    userMap.remove(JsonKey.EXTERNAL_IDS);
    userMap.remove(JsonKey.ENC_EMAIL);
    userMap.remove(JsonKey.ENC_PHONE);
    userMap.remove(JsonKey.EMAIL_VERIFIED);
    userMap.remove(JsonKey.STATUS);
    userMap.remove(JsonKey.USERNAME);
    userMap.remove(JsonKey.ROOT_ORG_ID);
    userMap.remove(JsonKey.LOGIN_ID);
    userMap.remove(JsonKey.ROLES);
    userMap.remove(JsonKey.CHANNEL);
  }

  private boolean isSlugUnique(String slug) {
    if (!StringUtils.isBlank(slug)) {
      Map<String, Object> filters = new HashMap<>();
      filters.put(JsonKey.SLUG, slug);
      filters.put(JsonKey.IS_ROOT_ORG, true);
      Map<String, Object> esResult =
          elasticSearchComplexSearch(
              filters, EsIndex.sunbird.getIndexName(), EsType.organisation.getTypeName());
      if (isNotNull(esResult)
          && esResult.containsKey(JsonKey.CONTENT)
          && isNotNull(esResult.get(JsonKey.CONTENT))) {
        return (((List) esResult.get(JsonKey.CONTENT)).isEmpty());
      }
    }
    return false;
  }
}
