package org.sunbird.actor.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.Util;
import scala.concurrent.Future;

/** Background sync of data between Cassandra and Elastic Search. */
@ActorConfig(
  tasks = {},
  asyncTasks = {"backgroundSync"}
)
public class EsSyncBackgroundActor extends BaseActor {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);

  @Override
  public void onReceive(Request request) throws Throwable {
    String operation = request.getOperation();
    if (ActorOperations.BACKGROUND_SYNC.getValue().equalsIgnoreCase(operation)) {
      sync(request);
    } else {
      onReceiveUnsupportedOperation("EsSyncBackgroundActor");
    }
  }

  private void sync(Request message) {
    logger.info(message.getRequestContext(), "EsSyncBackgroundActor: sync called");
    long startTime = System.currentTimeMillis();
    Map<String, Object> req = message.getRequest();
    Map<String, Object> dataMap = (Map<String, Object>) req.get(JsonKey.DATA);

    String objectType = (String) dataMap.get(JsonKey.OBJECT_TYPE);
    String operationType = (String) dataMap.get(JsonKey.OPERATION_TYPE);

    List<Object> objectIds = new ArrayList<>();
    if (null != dataMap.get(JsonKey.OBJECT_IDS)) {
      objectIds = (List<Object>) dataMap.get(JsonKey.OBJECT_IDS);
    }
    Util.DbInfo dbInfo = getDbInfoObj(objectType);
    if (null == dbInfo) {
      throw new ProjectCommonException(
          ResponseCode.invalidObjectType.getErrorCode(),
          ResponseCode.invalidObjectType.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    Response finalResponse = new Response();
    if (JsonKey.USER.equals(objectType)) {
      handleUserSyncRequest(objectIds, finalResponse, message.getRequestContext());
    } else {
      handleOrgAndLocationSync(objectIds, objectType, finalResponse, message.getRequestContext());
    }
    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;

    logger.info(
        message.getRequestContext(),
        "EsSyncBackgroundActor:sync: Total time taken to sync for type = "
            + objectType
            + " is "
            + elapsedTime
            + " ms");
    if (StringUtils.isNotBlank(operationType) && JsonKey.SYNC.equalsIgnoreCase(operationType)) {
      finalResponse.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
      sender().tell(finalResponse, self());
    }
  }

  private void handleOrgAndLocationSync(
      List<Object> objectIds,
      String objectType,
      Response finalResponse,
      RequestContext requestContext) {
    if (CollectionUtils.isNotEmpty(objectIds)) {
      String requestLogMsg =
          MessageFormat.format(
              "type = {0} and IDs = {1}", objectType, Arrays.toString(objectIds.toArray()));
      logger.info(
          requestContext,
          "EsSyncBackgroundActor:handleOrgAndLocationSync: Fetching data for "
              + requestLogMsg
              + " started");
      Map<String, Object> responseMap = new HashMap<>();
      Util.DbInfo dbInfo = getDbInfoObj(objectType);
      Response response =
          cassandraOperation.getRecordsByProperty(
              dbInfo.getKeySpace(), dbInfo.getTableName(), JsonKey.ID, objectIds, requestContext);
      List<Map<String, Object>> responseList =
          (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
      logger.info(
          requestContext,
          "EsSyncBackgroundActor:handleOrgAndLocationSync: Fetching data for "
              + requestLogMsg
              + " completed");
      if (CollectionUtils.isNotEmpty(responseList)) {
        for (Map<String, Object> map : responseList) {
          String objectId = (String) map.get(JsonKey.ID);
          try {
            logger.info(
                requestContext,
                "EsSyncBackgroundActor:handleOrgAndLocationSync for objectType :"
                    + objectType
                    + " for id : "
                    + map.get(JsonKey.ID));
            String esResponse = "";
            if (objectType.equals(JsonKey.ORGANISATION)) {
              esResponse =
                  saveDataToEs(
                      getType(objectType),
                      objectId,
                      getOrgDetails(map, requestContext),
                      requestContext);
            } else if (objectType.equalsIgnoreCase(JsonKey.LOCATION)) {
              esResponse = saveDataToEs(getType(objectType), objectId, map, requestContext);
            }
            if (StringUtils.isNotBlank(esResponse) && (esResponse).equalsIgnoreCase(objectId)) {
              responseMap.put(
                  objectId, ((objectId).equalsIgnoreCase((String) map.get(JsonKey.ID))));
            }
          } catch (Exception ex) {
            logger.error(
                requestContext,
                "Exception occurred while making sync call for "
                    + objectType
                    + ", id : "
                    + objectId,
                ex);
            responseMap.put(objectId, false);
          }
        }
      } else {
        logger.info(
            requestContext,
            "EsSyncBackgroundActor:handleOrgAndLocationSync invalid Ids " + objectIds);
      }
      finalResponse.getResult().put(JsonKey.ES_SYNC_RESPONSE, responseMap);
    }
  }

  private void handleUserSyncRequest(
      List<Object> objectIds, Response finalResponse, RequestContext context) {
    if (CollectionUtils.isNotEmpty(objectIds)) {
      Map<String, Object> esResponse = new HashMap<>();
      for (Object userId : objectIds) {
        try {
          logger.info(
              context,
              "EsSyncBackgroundActor:handleUserSyncRequest: Trigger sync of user details to ES");
          Map<String, Object> userDetails = Util.getUserDetails((String) userId, context);
          if (MapUtils.isNotEmpty(userDetails)) {
            logger.info(
                context,
                "EsSyncBackgroundActor:handleUserSyncRequest user rootOrgId :"
                    + userDetails.get(JsonKey.ROOT_ORG_ID)
                    + ", userId : "
                    + userDetails.get(JsonKey.ID));
            String response =
                saveDataToEs(
                    ProjectUtil.EsType.user.getTypeName(), (String) userId, userDetails, context);
            if (StringUtils.isNotBlank(response) && ((String) userId).equalsIgnoreCase(response)) {
              esResponse.put((String) userId, (((String) userId).equalsIgnoreCase(response)));
            }
          } else {
            logger.info(
                context, "EsSyncBackgroundActor:handleUserSyncRequest invalid userId " + userId);
          }
        } catch (Exception ex) {
          logger.error(
              context,
              "Exception occurred while making sync call for user with id : " + userId,
              ex);
          finalResponse.put((String) userId, false);
        }
      }
      finalResponse.getResult().put(JsonKey.ES_SYNC_RESPONSE, esResponse);
    }
  }

  private String saveDataToEs(
      String esType, String id, Map<String, Object> data, RequestContext context) {
    Future<String> responseF = esService.save(esType, id, data, context);
    return (String) ElasticSearchHelper.getResponseFromFuture(responseF);
  }

  private Map<String, Object> getOrgDetails(Map<String, Object> orgMap, RequestContext context) {
    logger.debug(context, "EsSyncBackgroundActor: getOrgDetails called");
    String orgLocation = (String) orgMap.get(JsonKey.ORG_LOCATION);
    List<Map<String, String>> orgLoc = new ArrayList<>();
    if (StringUtils.isNotBlank(orgLocation)) {
      try {
        ObjectMapper mapper = new ObjectMapper();
        orgLoc = mapper.readValue(orgLocation, List.class);
      } catch (Exception ex) {
        logger.error(context, "Exception occurred while parsing orgLocation", ex);
      }
    }
    orgMap.put(JsonKey.ORG_LOCATION, orgLoc);
    logger.debug(context, "EsSyncBackgroundActor: getOrgDetails returned");
    return orgMap;
  }

  private String getType(String objectType) {
    String type = "";
    if (objectType.equals(JsonKey.USER)) {
      type = ProjectUtil.EsType.user.getTypeName();
    } else if (objectType.equals(JsonKey.ORGANISATION)) {
      type = ProjectUtil.EsType.organisation.getTypeName();
    } else if (objectType.equals(JsonKey.LOCATION)) {
      type = ProjectUtil.EsType.location.getTypeName();
    }
    return type;
  }

  private org.sunbird.util.Util.DbInfo getDbInfoObj(String objectType) {
    if (objectType.equals(JsonKey.USER)) {
      return Util.dbInfoMap.get(JsonKey.USER_DB);
    } else if (objectType.equals(JsonKey.ORGANISATION)) {
      return Util.dbInfoMap.get(JsonKey.ORG_DB);
    } else if (objectType.equals(JsonKey.LOCATION)) {
      return Util.dbInfoMap.get(JsonKey.LOCATION);
    }
    return null;
  }
}
