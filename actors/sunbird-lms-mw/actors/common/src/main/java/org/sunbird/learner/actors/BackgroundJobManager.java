package org.sunbird.learner.actors;

import java.util.*;
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
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.learner.util.Util.DbInfo;
import scala.concurrent.Future;

/**
 * This class will handle all the background job.
 *
 * @author Manzarul
 * @author Amit Kumar
 */
@ActorConfig(
  tasks = {},
  asyncTasks = {
    "mergeUserToElastic",
    "updateUserInfoToElastic",
    "updateUserRoles",
    "addUserBadgebackground",
    "insertOrgInfoToElastic",
    "updateOrgInfoToElastic",
    "updateUserOrgES",
    "removeUserOrgES",
    "insertUserNotesToElastic",
    "updateUserNotesToElastic",
  }
)
public class BackgroundJobManager extends BaseActor {

  private static Map<String, String> headerMap = new HashMap<>();
  private static Util.DbInfo dbInfo = null;

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
    String operation = request.getOperation();
    ProjectLogger.log("Operation name is ==" + operation);
    if (operation.equalsIgnoreCase(ActorOperations.UPDATE_USER_INFO_ELASTIC.getValue())) {
      ProjectLogger.log("Update user info to ES called.", LoggerEnum.INFO.name());
      updateUserInfoToEs(request);
    } else if (operation.equalsIgnoreCase(ActorOperations.UPDATE_ORG_INFO_ELASTIC.getValue())) {
      updateOrgInfoToEs(request);
    } else if (operation.equalsIgnoreCase(ActorOperations.INSERT_ORG_INFO_ELASTIC.getValue())) {
      insertOrgInfoToEs(request);
    } else if (operation.equalsIgnoreCase(ActorOperations.UPDATE_USER_ORG_ES.getValue())) {
      updateUserOrgInfoToEs(request);
    } else if (operation.equalsIgnoreCase(ActorOperations.REMOVE_USER_ORG_ES.getValue())) {
      removeUserOrgInfoToEs(request);
    } else if (operation.equalsIgnoreCase(ActorOperations.UPDATE_USER_ROLES_ES.getValue())) {
      updateUserRoleToEs(request);
    } else if (operation.equalsIgnoreCase(ActorOperations.ADD_USER_BADGE_BKG.getValue())) {
      addBadgeToUserprofile(request);
    } else if (operation.equalsIgnoreCase(ActorOperations.INSERT_USER_NOTES_ES.getValue())) {
      insertUserNotesToEs(request);
    } else if (operation.equalsIgnoreCase(ActorOperations.UPDATE_USER_NOTES_ES.getValue())) {
      updateUserNotesToEs(request);
    } else if (operation.equalsIgnoreCase(ActorOperations.MERGE_USER_TO_ELASTIC.getValue())) {
      mergeUserDetailsToEs(request);
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
  private void insertOrgInfoToEs(Request actorMessage) {
    Map<String, String> headerMap = new HashMap<>();
    String header = ProjectUtil.getConfigValue(JsonKey.EKSTEP_AUTHORIZATION);
    header = JsonKey.BEARER + header;
    headerMap.put(JsonKey.AUTHORIZATION, header);
    headerMap.put("Content-Type", "application/json");
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
      registertag(hashOrgId, "{}", headerMap);
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

  private void mergeUserDetailsToEs(Request mergeRequest) {
    String mergeeId = (String) mergeRequest.get(JsonKey.FROM_ACCOUNT_ID);
    Map<String, Object> mergeeMap =
        (Map<String, Object>) mergeRequest.get(JsonKey.USER_MERGEE_ACCOUNT);
    updateDataToElastic(
        ProjectUtil.EsIndex.sunbird.getIndexName(),
        ProjectUtil.EsType.user.getTypeName(),
        mergeeId,
        mergeeMap);
    ProjectLogger.log(
        "user details for updated for user in ES with id:" + mergeeId, LoggerEnum.INFO.name());
  }
}
