package org.sunbird.actor;

import java.util.*;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.Util;
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
    "updateUserOrgES",
    "insertUserNotesToElastic",
    "updateUserNotesToElastic",
  }
)
public class BackgroundJobManager extends BaseActor {
  private ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);

  @Override
  public void onReceive(Request request) throws Throwable {
    String operation = request.getOperation();
    if (operation.equalsIgnoreCase(ActorOperations.UPDATE_USER_INFO_ELASTIC.getValue())) {
      updateUserInfoToEs(request);
    } else if (operation.equalsIgnoreCase(ActorOperations.UPDATE_USER_ORG_ES.getValue())) {
      updateUserOrgInfoToEs(request);
    } else if (operation.equalsIgnoreCase(ActorOperations.INSERT_USER_NOTES_ES.getValue())) {
      insertUserNotesToEs(request);
    } else if (operation.equalsIgnoreCase(ActorOperations.UPDATE_USER_NOTES_ES.getValue())) {
      updateUserNotesToEs(request);
    } else if (operation.equalsIgnoreCase(ActorOperations.MERGE_USER_TO_ELASTIC.getValue())) {
      mergeUserDetailsToEs(request);
    } else {
      onReceiveUnsupportedOperation(request.getOperation());
    }
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
    logger.info(context, "unable to save the data to ES with identifier " + identifier);
    return false;
  }

  private void updateUserInfoToEs(Request actorMessage) {
    String userId = (String) actorMessage.getRequest().get(JsonKey.ID);
    Map<String, Object> userDetails = Util.getUserDetails(userId, actorMessage.getRequestContext());
    if (MapUtils.isNotEmpty(userDetails)) {
      insertDataToElastic(
          ProjectUtil.EsIndex.sunbird.getIndexName(),
          ProjectUtil.EsType.user.getTypeName(),
          userId,
          userDetails,
          actorMessage.getRequestContext());
    } else {
      logger.info(
          actorMessage.getRequestContext(),
          "BackGroundJobManager:updateUserInfoToEs invalid userId " + userId);
    }
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
    Future<String> responseF = esService.save(type, identifier, data, context);
    String response = (String) ElasticSearchHelper.getResponseFromFuture(responseF);
    logger.info(
        context,
        "ES save response for type , identifier == " + type + "  " + identifier + "  " + response);
    if (!StringUtils.isBlank(response)) {
      logger.info(context, "Data saved successfully to ES ." + type + "  " + identifier);
      return true;
    }
    logger.info(context, "unable to save the data inside ES with identifier " + identifier);
    return false;
  }


  @SuppressWarnings("unchecked")
  private void insertUserNotesToEs(Request actorMessage) {
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
        "user details updated for user in ES with id:" + mergeeId);
  }
}
