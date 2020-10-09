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
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
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
    "insertOrgInfoToElastic",
    "updateOrgInfoToElastic",
    "updateUserOrgES",
    "removeUserOrgES",
    "insertUserNotesToElastic",
    "updateUserNotesToElastic",
  }
)
public class BackgroundJobManager extends BaseActor {

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

  @SuppressWarnings("unchecked")
  private void updateUserRoleToEs(Request actorMessage) {
    List<String> roles = (List<String>) actorMessage.getRequest().get(JsonKey.ROLES);
    String type = (String) actorMessage.get(JsonKey.TYPE);
    Map<String, Object> result = new HashMap<>();
    result.put(JsonKey.USER_ID, actorMessage.get(JsonKey.USER_ID));
    if (type.equals(JsonKey.USER)) {
      result.put(JsonKey.ROLES, roles);
    } else if (type.equals(JsonKey.ORGANISATION)) {
      Map<String, Object> searchMap = new LinkedHashMap<>(2);
      searchMap.put(JsonKey.USER_ID, actorMessage.get(JsonKey.USER_ID));
      Response res =
          cassandraOperation.getRecordsByCompositeKey(
              JsonKey.SUNBIRD, JsonKey.USER_ORG, searchMap, actorMessage.getRequestContext());
      List<Map<String, Object>> dataList = (List<Map<String, Object>>) res.get(JsonKey.RESPONSE);
      result.put(JsonKey.ORGANISATIONS, dataList);
    }

    updateDataToElastic(
        ProjectUtil.EsIndex.sunbird.getIndexName(),
        ProjectUtil.EsType.user.getTypeName(),
        (String) result.get(JsonKey.IDENTIFIER),
        result,
        actorMessage.getRequestContext());
  }

  @SuppressWarnings("unchecked")
  private void removeUserOrgInfoToEs(Request actorMessage) {
    Map<String, Object> orgMap = (Map<String, Object>) actorMessage.getRequest().get(JsonKey.USER);
    Future<Map<String, Object>> resultF =
        esService.getDataByIdentifier(
            ProjectUtil.EsType.user.getTypeName(),
            (String) orgMap.get(JsonKey.USER_ID),
            actorMessage.getRequestContext());
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
        result,
        actorMessage.getRequestContext());
  }

  @SuppressWarnings("unchecked")
  private void updateUserOrgInfoToEs(Request actorMessage) {
    Map<String, Object> orgMap = (Map<String, Object>) actorMessage.getRequest().get(JsonKey.USER);
    Future<Map<String, Object>> resultF =
        esService.getDataByIdentifier(
            ProjectUtil.EsType.user.getTypeName(),
            (String) orgMap.get(JsonKey.USER_ID),
            actorMessage.getRequestContext());
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
        result,
        actorMessage.getRequestContext());
  }

  @SuppressWarnings("unchecked")
  private void insertOrgInfoToEs(Request actorMessage) {
    Map<String, String> headerMap = new HashMap<>();
    String header = ProjectUtil.getConfigValue(JsonKey.EKSTEP_AUTHORIZATION);
    header = JsonKey.BEARER + header;
    headerMap.put(JsonKey.AUTHORIZATION, header);
    headerMap.put("Content-Type", "application/json");
    logger.info(actorMessage.getRequestContext(), "Calling method to save inside Es==");
    Map<String, Object> orgMap =
        (Map<String, Object>) actorMessage.getRequest().get(JsonKey.ORGANISATION);
    if (ProjectUtil.isNotNull(orgMap)) {
      Util.DbInfo orgDbInfo = Util.dbInfoMap.get(JsonKey.ORG_DB);
      String id = (String) orgMap.get(JsonKey.ID);
      Response orgResponse =
          cassandraOperation.getRecordById(
              orgDbInfo.getKeySpace(),
              orgDbInfo.getTableName(),
              id,
              actorMessage.getRequestContext());
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
      logger.info(actorMessage.getRequestContext(), "hashOrgId value is ==" + hashOrgId);
      // Just check it if hashOrgId is null or empty then replace with org id.
      if (StringUtils.isBlank(hashOrgId)) {
        hashOrgId = id;
      }
      // making call to register tag
      registertag(hashOrgId, "{}", headerMap, actorMessage.getRequestContext());
      insertDataToElastic(
          ProjectUtil.EsIndex.sunbird.getIndexName(),
          ProjectUtil.EsType.organisation.getTypeName(),
          id,
          esMap,
          actorMessage.getRequestContext());
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
        orgMap,
        actorMessage.getRequestContext());
  }

  private boolean updateDataToElastic(
      String indexName,
      String typeName,
      String identifier,
      Map<String, Object> data,
      RequestContext context) {
    Future<Boolean> responseF = esService.update(typeName, identifier, data, context);
    boolean response = (boolean) ElasticSearchHelper.getResponseFromFuture(responseF);
    if (response) {
      return true;
    }
    logger.info(context, "unbale to save the data inside ES with identifier " + identifier);
    return false;
  }

  private void updateUserInfoToEs(Request actorMessage) {
    String userId = (String) actorMessage.getRequest().get(JsonKey.ID);
    Map<String, Object> userDetails = Util.getUserDetails(userId, actorMessage.getRequestContext());
    logger.info(
        actorMessage.getRequestContext(),
        "BackGroundJobManager:updateUserInfoToEs userRootOrgId "
            + userDetails.get(JsonKey.ROOT_ORG_ID));
    insertDataToElastic(
        ProjectUtil.EsIndex.sunbird.getIndexName(),
        ProjectUtil.EsType.user.getTypeName(),
        userId,
        userDetails,
        actorMessage.getRequestContext());
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
      String index,
      String type,
      String identifier,
      Map<String, Object> data,
      RequestContext context) {
    logger.info(
        context,
        "BackgroundJobManager:insertDataToElastic: type = " + type + " identifier = " + identifier);
    /*
     * if (type.equalsIgnoreCase(ProjectUtil.EsType.user.getTypeName())) { // now
     * calculate profile completeness and error filed and store it in ES
     * ProfileCompletenessService service =
     * ProfileCompletenessFactory.getInstance(); Map<String, Object> responsemap =
     * service.computeProfile(data); data.putAll(responsemap); }
     */
    Future<String> responseF = esService.save(type, identifier, data, context);
    String response = (String) ElasticSearchHelper.getResponseFromFuture(responseF);
    logger.info(
        context,
        "Getting  ********** ES save response for type , identiofier=="
            + type
            + "  "
            + identifier
            + "  "
            + response);
    if (!StringUtils.isBlank(response)) {
      logger.info(context, "User Data is saved successfully ES ." + type + "  " + identifier);
      return true;
    }
    logger.info(context, "unbale to save the data inside ES with identifier " + identifier);
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
  private String registertag(
      String tagId, String body, Map<String, String> header, RequestContext context) {
    String tagStatus = "";
    try {
      logger.info(
          context,
          "BackgroundJobManager:registertag register tag call started with tagid = " + tagId);
      tagStatus = ProjectUtil.registertag(tagId, body, header, context);
      logger.info(
          context,
          "BackgroundJobManager:registertag  register tag call end with id and status = "
              + tagId
              + ", "
              + tagStatus);
    } catch (Exception e) {
      logger.error(
          context,
          "BackgroundJobManager:registertag register tag call failure with error message = "
              + e.getMessage(),
          e);
    }
    return tagStatus;
  }

  @SuppressWarnings("unchecked")
  private void insertUserNotesToEs(Request actorMessage) {
    logger.info(actorMessage.getRequestContext(), "Calling method to save inside Es==");
    Map<String, Object> noteMap = (Map<String, Object>) actorMessage.getRequest().get(JsonKey.NOTE);
    if (ProjectUtil.isNotNull(noteMap) && noteMap.size() > 0) {
      String id = (String) noteMap.get(JsonKey.ID);
      insertDataToElastic(
          ProjectUtil.EsIndex.sunbird.getIndexName(),
          ProjectUtil.EsType.usernotes.getTypeName(),
          id,
          noteMap,
          actorMessage.getRequestContext());
    } else {
      logger.info(actorMessage.getRequestContext(), "No data found to save inside Es for Notes--");
    }
  }

  @SuppressWarnings("unchecked")
  private void updateUserNotesToEs(Request actorMessage) {
    Map<String, Object> noteMap = (Map<String, Object>) actorMessage.getRequest().get(JsonKey.NOTE);
    updateDataToElastic(
        ProjectUtil.EsIndex.sunbird.getIndexName(),
        ProjectUtil.EsType.usernotes.getTypeName(),
        (String) noteMap.get(JsonKey.ID),
        noteMap,
        actorMessage.getRequestContext());
  }

  private void mergeUserDetailsToEs(Request mergeRequest) {
    String mergeeId = (String) mergeRequest.get(JsonKey.FROM_ACCOUNT_ID);
    Map<String, Object> mergeeMap =
        (Map<String, Object>) mergeRequest.get(JsonKey.USER_MERGEE_ACCOUNT);
    updateDataToElastic(
        ProjectUtil.EsIndex.sunbird.getIndexName(),
        ProjectUtil.EsType.user.getTypeName(),
        mergeeId,
        mergeeMap,
        mergeRequest.getRequestContext());
    logger.info(
        mergeRequest.getRequestContext(),
        "user details for updated for user in ES with id:" + mergeeId);
  }
}
