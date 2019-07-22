package org.sunbird.learner.actors;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.*;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.HttpUtil;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.CourseBatchSchedulerUtil;
import org.sunbird.learner.util.Util;
import org.sunbird.learner.util.Util.DbInfo;
import scala.concurrent.Future;

/**
 * This class will handle all the background job. Example when ever course is published then this
 * job will collect course related data from EKStep and update with Sunbird.
 *
 * @author Manzarul
 * @author Amit Kumar
 */
@ActorConfig(
  tasks = {},
  asyncTasks = {
    "updateUserInfoToElastic",
    "updateUserRoles",
    "addUserBadgebackground",
    "updateUserCoursesInfoToElastic",
    "updateUserCoursesInfoToElastic",
    "insertOrgInfoToElastic",
    "updateOrgInfoToElastic",
    "updateUserOrgES",
    "removeUserOrgES",
    "insertUserNotesToElastic",
    "updateUserNotesToElastic",
    "insertUserCoursesInfoToElastic",
    "updateCourseBatchToEs",
    "insertCourseBatchToEs"
  }
)
public class BackgroundJobManager extends BaseActor {

  private static Map<String, String> headerMap = new HashMap<>();
  private static Util.DbInfo dbInfo = null;
  private ObjectMapper mapper = new ObjectMapper();

  static {
    headerMap.put("content-type", "application/json");
    headerMap.put("accept", "application/json");
  }

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);

  @Override
  public void onReceive(Request request) throws Throwable {
    ProjectLogger.log(
        "BackgroundJobManager received action: " + request.getOperation(), LoggerEnum.INFO.name());
    ProjectLogger.log("BackgroundJobManager  onReceive called");
    if (dbInfo == null) {
      dbInfo = Util.dbInfoMap.get(JsonKey.COURSE_MANAGEMENT_DB);
    }
    String operation = request.getOperation();
    ProjectLogger.log("Operation name is ==" + operation);
    if (operation.equalsIgnoreCase(ActorOperations.PUBLISH_COURSE.getValue())) {
      // manageBackgroundJob(request);
    } else if (operation.equalsIgnoreCase(ActorOperations.UPDATE_USER_INFO_ELASTIC.getValue())) {
      ProjectLogger.log("Update user info to ES called.", LoggerEnum.INFO.name());
      updateUserInfoToEs(request);
    } else if (operation.equalsIgnoreCase(
        ActorOperations.INSERT_USR_COURSES_INFO_ELASTIC.getValue())) {
      insertUserCourseInfoToEs(request);
    } else if (operation.equalsIgnoreCase(ActorOperations.UPDATE_USER_COUNT.getValue())) {
      updateUserCount(request);
    } else if (operation.equalsIgnoreCase(ActorOperations.UPDATE_ORG_INFO_ELASTIC.getValue())) {
      updateOrgInfoToEs(request);
    } else if (operation.equalsIgnoreCase(ActorOperations.INSERT_ORG_INFO_ELASTIC.getValue())) {
      insertOrgInfoToEs(request);
    } else if (operation.equalsIgnoreCase(ActorOperations.INSERT_COURSE_BATCH_ES.getValue())) {
      insertCourseBatchInfoToEs(request);
    } else if (operation.equalsIgnoreCase(ActorOperations.UPDATE_COURSE_BATCH_ES.getValue())) {
      updateCourseBatchInfoToEs(request);
    } else if (operation.equalsIgnoreCase(ActorOperations.UPDATE_USER_ORG_ES.getValue())) {
      updateUserOrgInfoToEs(request);
    } else if (operation.equalsIgnoreCase(ActorOperations.REMOVE_USER_ORG_ES.getValue())) {
      removeUserOrgInfoToEs(request);
    } else if (operation.equalsIgnoreCase(ActorOperations.UPDATE_USER_ROLES_ES.getValue())) {
      updateUserRoleToEs(request);
    } else if (operation.equalsIgnoreCase(
        ActorOperations.UPDATE_USR_COURSES_INFO_ELASTIC.getValue())) {
      updateUserCourseInfoToEs(request);
    } else if (operation.equalsIgnoreCase(ActorOperations.ADD_USER_BADGE_BKG.getValue())) {
      addBadgeToUserprofile(request);
    } else if (operation.equalsIgnoreCase(ActorOperations.INSERT_USER_NOTES_ES.getValue())) {
      insertUserNotesToEs(request);
    } else if (operation.equalsIgnoreCase(ActorOperations.UPDATE_USER_NOTES_ES.getValue())) {
      updateUserNotesToEs(request);
    } else {
      ProjectLogger.log("UNSUPPORTED OPERATION");
      ProjectCommonException exception =
          new ProjectCommonException(
              ResponseCode.invalidOperationName.getErrorCode(),
              ResponseCode.invalidOperationName.getErrorMessage(),
              ResponseCode.CLIENT_ERROR.getResponseCode());
      ProjectLogger.log("UnSupported operation in Background Job Manager", exception);
    }
  }

  /** @param actorMessage */
  private void addBadgeToUserprofile(Request actorMessage) {
    Map<String, Object> userBadgeMap = actorMessage.getRequest();
    userBadgeMap.remove(JsonKey.OPERATION);
    DbInfo userbadge = Util.dbInfoMap.get(JsonKey.USER_BADGES_DB);
    Response response =
        cassandraOperation.getRecordsByProperties(
            userbadge.getKeySpace(), userbadge.getTableName(), userBadgeMap);
    if (response != null && response.get(JsonKey.RESPONSE) != null) {
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> badgesList =
          (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
      if (badgesList != null && !badgesList.isEmpty()) {
        badgesList = removeDataFromMap(badgesList);
        Map<String, Object> map = new HashMap<>();
        map.put(JsonKey.BADGES, badgesList);
        boolean updateResponse =
            updateDataToElastic(
                ProjectUtil.EsIndex.sunbird.getIndexName(),
                ProjectUtil.EsType.user.getTypeName(),
                (String) userBadgeMap.get(JsonKey.RECEIVER_ID),
                map);
        ProjectLogger.log("User badge update response==" + updateResponse);
      }
    } else {
      ProjectLogger.log("No data found user badges to sync with user===", LoggerEnum.INFO.name());
    }
  }

  public static List<Map<String, Object>> removeDataFromMap(List<Map<String, Object>> listOfMap) {
    List<Map<String, Object>> list = new ArrayList<>();
    for (Map<String, Object> map : listOfMap) {
      Map<String, Object> innermap = new HashMap<>();
      innermap.put(JsonKey.ID, map.get(JsonKey.ID));
      innermap.put(JsonKey.BADGE_TYPE_ID, map.get(JsonKey.BADGE_TYPE_ID));
      innermap.put(JsonKey.RECEIVER_ID, map.get(JsonKey.RECEIVER_ID));
      innermap.put(JsonKey.CREATED_DATE, map.get(JsonKey.CREATED_DATE));
      innermap.put(JsonKey.CREATED_BY, map.get(JsonKey.CREATED_BY));
      list.add(innermap);
    }
    return list;
  }

  @SuppressWarnings("unchecked")
  private void updateUserRoleToEs(Request actorMessage) {
    List<String> roles = (List<String>) actorMessage.getRequest().get(JsonKey.ROLES);
    String type = (String) actorMessage.get(JsonKey.TYPE);
    String orgId = (String) actorMessage.get(JsonKey.ORGANISATION_ID);
    Future<Map<String, Object>> resultF =
        esService.getDataByIdentifier(
            ProjectUtil.EsType.user.getTypeName(), (String) actorMessage.get(JsonKey.USER_ID));
    Map<String, Object> result =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
    if (type.equals(JsonKey.USER)) {
      result.put(JsonKey.ROLES, roles);
    } else if (type.equals(JsonKey.ORGANISATION)) {
      List<Map<String, Object>> roleMapList =
          (List<Map<String, Object>>) result.get(JsonKey.ORGANISATIONS);
      if (null != roleMapList) {
        for (Map<String, Object> map : roleMapList) {
          if ((orgId.equalsIgnoreCase((String) map.get(JsonKey.ORGANISATION_ID)))) {
            map.put(JsonKey.ROLES, roles);
          }
        }
      }
    }
    updateDataToElastic(
        ProjectUtil.EsIndex.sunbird.getIndexName(),
        ProjectUtil.EsType.user.getTypeName(),
        (String) result.get(JsonKey.IDENTIFIER),
        result);
  }

  @SuppressWarnings("unchecked")
  private void updateUserCourseInfoToEs(Request actorMessage) {

    Map<String, Object> batch =
        (Map<String, Object>) actorMessage.getRequest().get(JsonKey.USER_COURSES);
    updateDataToElastic(
        ProjectUtil.EsIndex.sunbird.getIndexName(),
        ProjectUtil.EsType.usercourses.getTypeName(),
        (String) batch.get(JsonKey.ID),
        batch);
  }

  @SuppressWarnings("unchecked")
  private void insertUserCourseInfoToEs(Request actorMessage) {

    Map<String, Object> batch =
        (Map<String, Object>) actorMessage.getRequest().get(JsonKey.USER_COURSES);
    insertDataToElastic(
        ProjectUtil.EsIndex.sunbird.getIndexName(),
        ProjectUtil.EsType.usercourses.getTypeName(),
        (String) batch.get(JsonKey.ID),
        batch);
  }

  @SuppressWarnings("unchecked")
  private void removeUserOrgInfoToEs(Request actorMessage) {
    Map<String, Object> orgMap = (Map<String, Object>) actorMessage.getRequest().get(JsonKey.USER);
    Future<Map<String, Object>> resultF =
        esService.getDataByIdentifier(
            ProjectUtil.EsType.user.getTypeName(), (String) orgMap.get(JsonKey.USER_ID));
    Map<String, Object> result =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
    if (result.containsKey(JsonKey.ORGANISATIONS) && null != result.get(JsonKey.ORGANISATIONS)) {
      List<Map<String, Object>> orgMapList =
          (List<Map<String, Object>>) result.get(JsonKey.ORGANISATIONS);
      if (null != orgMapList) {
        Iterator<Map<String, Object>> itr = orgMapList.iterator();
        while (itr.hasNext()) {
          Map<String, Object> map = itr.next();
          if ((((String) map.get(JsonKey.USER_ID))
                  .equalsIgnoreCase((String) orgMap.get(JsonKey.USER_ID)))
              && (((String) map.get(JsonKey.ORGANISATION_ID))
                  .equalsIgnoreCase((String) orgMap.get(JsonKey.ORGANISATION_ID)))) {
            itr.remove();
          }
        }
      }
    }
    updateDataToElastic(
        ProjectUtil.EsIndex.sunbird.getIndexName(),
        ProjectUtil.EsType.user.getTypeName(),
        (String) result.get(JsonKey.IDENTIFIER),
        result);
  }

  @SuppressWarnings("unchecked")
  private void updateUserOrgInfoToEs(Request actorMessage) {
    Map<String, Object> orgMap = (Map<String, Object>) actorMessage.getRequest().get(JsonKey.USER);
    Future<Map<String, Object>> resultF =
        esService.getDataByIdentifier(
            ProjectUtil.EsType.user.getTypeName(), (String) orgMap.get(JsonKey.USER_ID));
    Map<String, Object> result =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
    if (result.containsKey(JsonKey.ORGANISATIONS) && null != result.get(JsonKey.ORGANISATIONS)) {
      List<Map<String, Object>> orgMapList =
          (List<Map<String, Object>>) result.get(JsonKey.ORGANISATIONS);
      orgMapList.add(orgMap);
    } else {
      List<Map<String, Object>> mapList = new ArrayList<>();
      mapList.add(orgMap);
      result.put(JsonKey.ORGANISATIONS, mapList);
    }
    updateDataToElastic(
        ProjectUtil.EsIndex.sunbird.getIndexName(),
        ProjectUtil.EsType.user.getTypeName(),
        (String) result.get(JsonKey.IDENTIFIER),
        result);
  }

  @SuppressWarnings("unchecked")
  private void updateCourseBatchInfoToEs(Request actorMessage) {
    Map<String, Object> batch = (Map<String, Object>) actorMessage.getRequest().get(JsonKey.BATCH);
    updateDataToElastic(
        ProjectUtil.EsIndex.sunbird.getIndexName(),
        ProjectUtil.EsType.course.getTypeName(),
        (String) batch.get(JsonKey.ID),
        batch);
  }

  @SuppressWarnings("unchecked")
  private void insertCourseBatchInfoToEs(Request actorMessage) {
    Map<String, Object> batch = (Map<String, Object>) actorMessage.getRequest().get(JsonKey.BATCH);
    // making call to register tag
    registertag(
        (String) batch.getOrDefault(JsonKey.HASH_TAG_ID, batch.get(JsonKey.ID)),
        "{}",
        CourseBatchSchedulerUtil.headerMap);
    // register tag for course
    registertag(
        (String) batch.getOrDefault(JsonKey.COURSE_ID, batch.get(JsonKey.COURSE_ID)),
        "{}",
        CourseBatchSchedulerUtil.headerMap);
  }

  @SuppressWarnings("unchecked")
  private void insertOrgInfoToEs(Request actorMessage) {
    ProjectLogger.log("Calling method to save inside Es==");
    Map<String, Object> orgMap =
        (Map<String, Object>) actorMessage.getRequest().get(JsonKey.ORGANISATION);
    if (ProjectUtil.isNotNull(orgMap)) {
      Util.DbInfo orgDbInfo = Util.dbInfoMap.get(JsonKey.ORG_DB);
      String id = (String) orgMap.get(JsonKey.ID);
      Response orgResponse =
          cassandraOperation.getRecordById(orgDbInfo.getKeySpace(), orgDbInfo.getTableName(), id);
      List<Map<String, Object>> orgList =
          (List<Map<String, Object>>) orgResponse.getResult().get(JsonKey.RESPONSE);
      Map<String, Object> esMap = new HashMap<>();
      if (!(orgList.isEmpty())) {
        esMap = orgList.get(0);
        esMap.remove(JsonKey.CONTACT_DETAILS);

        if (MapUtils.isNotEmpty((Map<String, Object>) orgMap.get(JsonKey.ADDRESS))) {
          esMap.put(JsonKey.ADDRESS, orgMap.get(JsonKey.ADDRESS));
        } else {
          esMap.put(JsonKey.ADDRESS, new HashMap<>());
        }
      }
      // Register the org into EKStep.
      String hashOrgId = (String) esMap.getOrDefault(JsonKey.HASH_TAG_ID, "");
      ProjectLogger.log("hashOrgId value is ==" + hashOrgId);
      // Just check it if hashOrgId is null or empty then replace with org id.
      if (StringUtils.isBlank(hashOrgId)) {
        hashOrgId = id;
      }
      // making call to register tag
      registertag(hashOrgId, "{}", CourseBatchSchedulerUtil.headerMap);
      insertDataToElastic(
          ProjectUtil.EsIndex.sunbird.getIndexName(),
          ProjectUtil.EsType.organisation.getTypeName(),
          id,
          esMap);
    }
  }

  @SuppressWarnings("unchecked")
  private void updateOrgInfoToEs(Request actorMessage) {
    Map<String, Object> orgMap =
        (Map<String, Object>) actorMessage.getRequest().get(JsonKey.ORGANISATION);
    updateDataToElastic(
        ProjectUtil.EsIndex.sunbird.getIndexName(),
        ProjectUtil.EsType.organisation.getTypeName(),
        (String) orgMap.get(JsonKey.ID),
        orgMap);
  }

  private boolean updateDataToElastic(
      String indexName, String typeName, String identifier, Map<String, Object> data) {
    Future<Boolean> responseF = esService.update(typeName, identifier, data);
    boolean response = (boolean) ElasticSearchHelper.getResponseFromFuture(responseF);
    if (response) {
      return true;
    }
    ProjectLogger.log(
        "unbale to save the data inside ES with identifier " + identifier, LoggerEnum.INFO.name());
    return false;
  }

  private void updateUserInfoToEs(Request actorMessage) {
    String userId = (String) actorMessage.getRequest().get(JsonKey.ID);
    Map<String, Object> userDetails =
        Util.getUserDetails(userId, getActorRef(ActorOperations.GET_SYSTEM_SETTING.getValue()));
    ProjectLogger.log(
        "BackGroundJobManager:updateUserInfoToEs userRootOrgId "
            + userDetails.get(JsonKey.ROOT_ORG_ID),
        LoggerEnum.INFO.name());
    insertDataToElastic(
        ProjectUtil.EsIndex.sunbird.getIndexName(),
        ProjectUtil.EsType.user.getTypeName(),
        userId,
        userDetails);
  }

  /** Method to update the user count . */
  @SuppressWarnings("unchecked")
  private void updateUserCount(Request actorMessage) {
    String courseId = (String) actorMessage.get(JsonKey.COURSE_ID);
    Map<String, Object> updateRequestMap = actorMessage.getRequest();

    Response result =
        cassandraOperation.getPropertiesValueById(
            dbInfo.getKeySpace(), dbInfo.getTableName(), courseId, JsonKey.USER_COUNT);
    Map<String, Object> responseMap = null;
    if (null != (result.get(JsonKey.RESPONSE))
        && (!((List<Map<String, Object>>) result.get(JsonKey.RESPONSE)).isEmpty())) {
      responseMap = ((List<Map<String, Object>>) result.get(JsonKey.RESPONSE)).get(0);
    }
    int userCount =
        (int)
            (responseMap.get(JsonKey.USER_COUNT) != null ? responseMap.get(JsonKey.USER_COUNT) : 0);
    updateRequestMap.put(JsonKey.USER_COUNT, userCount + 1);
    updateRequestMap.put(JsonKey.ID, courseId);
    updateRequestMap.remove(JsonKey.OPERATION);
    updateRequestMap.remove(JsonKey.COURSE_ID);
    Response resposne =
        cassandraOperation.updateRecord(
            dbInfo.getKeySpace(), dbInfo.getTableName(), updateRequestMap);
    if (resposne.get(JsonKey.RESPONSE).equals(JsonKey.SUCCESS)) {
      ProjectLogger.log("USER COUNT UPDATED SUCCESSFULLY IN COURSE MGMT TABLE");
    } else {
      ProjectLogger.log("USER COUNT NOT UPDATED SUCCESSFULLY IN COURSE MGMT TABLE");
    }
  }

  /**
   * @param request
   * @return boolean
   */
  @SuppressWarnings("unchecked")
  private boolean manageBackgroundJob(Request request) {
    Map<String, Object> data = null;
    if (request.getRequest() == null) {
      return false;
    } else {
      data = request.getRequest();
    }

    List<Map<String, Object>> list = (List<Map<String, Object>>) data.get(JsonKey.RESPONSE);
    Map<String, Object> content = list.get(0);
    String contentId = (String) content.get(JsonKey.CONTENT_ID);
    if (!StringUtils.isBlank(contentId)) {
      String contentData = getCourseData(contentId);
      if (!StringUtils.isBlank(contentData)) {
        Map<String, Object> map = getContentDetails(contentData);
        map.put(JsonKey.ID, content.get(JsonKey.COURSE_ID));
        updateCourseManagement(map);
        List<String> createdForValue = null;
        Object obj = content.get(JsonKey.COURSE_CREATED_FOR);
        if (obj != null) {
          createdForValue = (List<String>) obj;
        }
        content.remove(JsonKey.COURSE_CREATED_FOR);
        content.put(JsonKey.APPLICABLE_FOR, createdForValue);
        Map<String, Object> finalResponseMap = (Map<String, Object>) map.get(JsonKey.RESULT);
        finalResponseMap.putAll(content);
        finalResponseMap.put(JsonKey.OBJECT_TYPE, ProjectUtil.EsType.course.getTypeName());
        insertDataToElastic(
            ProjectUtil.EsIndex.sunbird.getIndexName(),
            ProjectUtil.EsType.course.getTypeName(),
            (String) map.get(JsonKey.ID),
            finalResponseMap);
      }
    }
    return true;
  }

  /**
   * Method to get the course data.
   *
   * @param contnetId String
   * @return String
   */
  private String getCourseData(String contnetId) {
    String responseData = null;
    try {
      String ekStepBaseUrl = System.getenv(JsonKey.EKSTEP_BASE_URL);
      if (StringUtils.isBlank(ekStepBaseUrl)) {
        ekStepBaseUrl = PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_BASE_URL);
      }

      responseData =
          HttpUtil.sendGetRequest(
              ekStepBaseUrl
                  + PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_CONTENT_URL)
                  + contnetId,
              headerMap);
    } catch (IOException e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    return responseData;
  }

  /**
   * Method to get the content details of the given content id.
   *
   * @param content String
   * @return Map<String , Object>
   */
  @SuppressWarnings("unchecked")
  private Map<String, Object> getContentDetails(String content) {
    Map<String, Object> map = new HashMap<>();
    try {
      JSONObject object = new JSONObject(content);
      JSONObject resultObj = object.getJSONObject(JsonKey.RESULT);
      HashMap<String, Map<String, Object>> result =
          mapper.readValue(resultObj.toString(), HashMap.class);
      Map<String, Object> contentMap = result.get(JsonKey.CONTENT);
      map.put(JsonKey.APPICON, contentMap.get(JsonKey.APPICON));
      try {
        map.put(JsonKey.TOC_URL, contentMap.get(JsonKey.TOC_URL));
      } catch (Exception e) {
        ProjectLogger.log(e.getMessage(), e);
      }
      map.put(JsonKey.COUNT, contentMap.get(JsonKey.LEAF_NODE_COUNT));
      map.put(JsonKey.RESULT, contentMap);

    } catch (JSONException | IOException e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    return map;
  }

  /**
   * Method to update the course management data on basis of course id.
   *
   * @param data Map<String, Object>
   * @return boolean
   */
  private boolean updateCourseManagement(Map<String, Object> data) {
    Map<String, Object> updateRequestMap = new HashMap<>();
    updateRequestMap.put(
        JsonKey.NO_OF_LECTURES, data.get(JsonKey.COUNT) != null ? data.get(JsonKey.COUNT) : 0);
    updateRequestMap.put(
        JsonKey.COURSE_LOGO_URL,
        data.get(JsonKey.APPICON) != null ? data.get(JsonKey.APPICON) : "");
    updateRequestMap.put(
        JsonKey.TOC_URL, data.get(JsonKey.TOC_URL) != null ? data.get(JsonKey.TOC_URL) : "");
    updateRequestMap.put(JsonKey.ID, data.get(JsonKey.ID));
    Response resposne =
        cassandraOperation.updateRecord(
            dbInfo.getKeySpace(), dbInfo.getTableName(), updateRequestMap);
    ProjectLogger.log(resposne.toString());

    return (!(resposne.get(JsonKey.RESPONSE) instanceof ProjectCommonException));
  }

  /**
   * Method to cache the course data .
   *
   * @param index String
   * @param type String
   * @param identifier String
   * @param data Map<String,Object>
   * @return boolean
   */
  private boolean insertDataToElastic(
      String index, String type, String identifier, Map<String, Object> data) {
    ProjectLogger.log(
        "BackgroundJobManager:insertDataToElastic: type = " + type + " identifier = " + identifier,
        LoggerEnum.INFO.name());
    /*
     * if (type.equalsIgnoreCase(ProjectUtil.EsType.user.getTypeName())) { // now
     * calculate profile completeness and error filed and store it in ES
     * ProfileCompletenessService service =
     * ProfileCompletenessFactory.getInstance(); Map<String, Object> responsemap =
     * service.computeProfile(data); data.putAll(responsemap); }
     */

    Future<String> responseF = esService.save(type, identifier, data);
    String response = (String) ElasticSearchHelper.getResponseFromFuture(responseF);
    ProjectLogger.log(
        "Getting  ********** ES save response for type , identiofier=="
            + type
            + "  "
            + identifier
            + "  "
            + response,
        LoggerEnum.INFO.name());
    if (!StringUtils.isBlank(response)) {
      ProjectLogger.log("User Data is saved successfully ES ." + type + "  " + identifier);
      return true;
    }
    ProjectLogger.log(
        "unbale to save the data inside ES with identifier " + identifier, LoggerEnum.INFO.name());
    return false;
  }

  /**
   * This method will make EkStep api call register the tag.
   *
   * @param tagId String unique tag id.
   * @param body String requested body
   * @param header Map<String,String>
   * @return String
   */
  private String registertag(String tagId, String body, Map<String, String> header) {
    String tagStatus = "";
    try {
      ProjectLogger.log(
          "BackgroundJobManager:registertag register tag call started with tagid = " + tagId,
          LoggerEnum.INFO.name());
      tagStatus = ProjectUtil.registertag(tagId, body, header);
      ProjectLogger.log(
          "BackgroundJobManager:registertag  register tag call end with id and status = "
              + tagId
              + ", "
              + tagStatus,
          LoggerEnum.INFO.name());
    } catch (Exception e) {
      ProjectLogger.log(
          "BackgroundJobManager:registertag register tag call failure with error message = "
              + e.getMessage(),
          e);
    }
    return tagStatus;
  }

  @SuppressWarnings("unchecked")
  private void insertUserNotesToEs(Request actorMessage) {
    ProjectLogger.log("Calling method to save inside Es==");
    Map<String, Object> noteMap = (Map<String, Object>) actorMessage.getRequest().get(JsonKey.NOTE);
    if (ProjectUtil.isNotNull(noteMap) && noteMap.size() > 0) {
      String id = (String) noteMap.get(JsonKey.ID);
      insertDataToElastic(
          ProjectUtil.EsIndex.sunbird.getIndexName(),
          ProjectUtil.EsType.usernotes.getTypeName(),
          id,
          noteMap);
    } else {
      ProjectLogger.log("No data found to save inside Es for Notes--", LoggerEnum.INFO.name());
    }
  }

  @SuppressWarnings("unchecked")
  private void updateUserNotesToEs(Request actorMessage) {
    Map<String, Object> noteMap = (Map<String, Object>) actorMessage.getRequest().get(JsonKey.NOTE);
    updateDataToElastic(
        ProjectUtil.EsIndex.sunbird.getIndexName(),
        ProjectUtil.EsType.usernotes.getTypeName(),
        (String) noteMap.get(JsonKey.ID),
        noteMap);
  }
}
