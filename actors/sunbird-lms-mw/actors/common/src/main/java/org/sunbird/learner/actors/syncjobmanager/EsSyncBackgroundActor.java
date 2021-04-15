package org.sunbird.learner.actors.syncjobmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.learner.util.Util.DbInfo;
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
    Map<String, Object> responseMap = new HashMap<>();
    List<Map<String, Object>> reponseList = null;
    List<Map<String, Object>> result = new ArrayList<>();
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

    String requestLogMsg = "";
    Response finalResponse = new Response();
    if (JsonKey.USER.equals(objectType)) {
      handleUserSyncRequest(objectIds, finalResponse, message.getRequestContext());
    } else {
      if (CollectionUtils.isNotEmpty(objectIds)) {
        requestLogMsg =
            MessageFormat.format(
                "type = {0} and IDs = {1}", objectType, Arrays.toString(objectIds.toArray()));

        logger.info(
            message.getRequestContext(),
            "EsSyncBackgroundActor:sync: Fetching data for " + requestLogMsg + " started");
        Response response =
            cassandraOperation.getRecordsByProperty(
                dbInfo.getKeySpace(),
                dbInfo.getTableName(),
                JsonKey.ID,
                objectIds,
                message.getRequestContext());
        reponseList = (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
        logger.info(
            message.getRequestContext(),
            "EsSyncBackgroundActor:sync: Fetching data for " + requestLogMsg + " completed");
        if (CollectionUtils.isNotEmpty(reponseList)) {
          for (Map<String, Object> map : reponseList) {
            responseMap.put((String) map.get(JsonKey.ID), map);
          }
          Iterator<Entry<String, Object>> itr = responseMap.entrySet().iterator();
          if (objectType.equals(JsonKey.ORGANISATION)) {
            while (itr.hasNext()) {
              result.add(getOrgDetails(itr.next(), message.getRequestContext()));
            }
          } else if (objectType.equalsIgnoreCase(JsonKey.LOCATION)) {
            while (itr.hasNext()) {
              Entry<String, Object> entry = itr.next();
              result.add((Map<String, Object>) entry.getValue());
            }
          }
          if (CollectionUtils.isNotEmpty(result)) {
            Future<Boolean> fBoolean =
                esService.bulkInsert(getType(objectType), result, message.getRequestContext());
            Boolean fResult = (Boolean) ElasticSearchHelper.getResponseFromFuture(fBoolean);
            finalResponse.getResult().put(JsonKey.ES_SYNC_RESPONSE, fResult);
          }
        }
      }
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

  private void handleUserSyncRequest(
      List<Object> objectIds, Response finalResponse, RequestContext context) {
    if (CollectionUtils.isNotEmpty(objectIds)) {
      Map<String, Object> esResponse = new HashMap<>();
      for (Object userId : objectIds) {
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
          Future<String> responseF =
              esService.save(
                  ProjectUtil.EsType.user.getTypeName(), (String) userId, userDetails, context);
          String response = (String) ElasticSearchHelper.getResponseFromFuture(responseF);
          if (StringUtils.isNotBlank(response) && ((String) userId).equalsIgnoreCase(response)) {
            esResponse.put((String) userId, (((String) userId).equalsIgnoreCase(response)));
          }
        } else {
          logger.info(
              context, "EsSyncBackgroundActor:handleUserSyncRequest invalid userId " + userId);
        }
      }
      finalResponse.getResult().put(JsonKey.ES_SYNC_RESPONSE, esResponse);
    }
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

  private Map<String, Object> getOrgDetails(Entry<String, Object> entry, RequestContext context) {
    logger.debug(context, "EsSyncBackgroundActor: getOrgDetails called");
    Map<String, Object> orgMap = (Map<String, Object>) entry.getValue();
    String orgLocation = (String) orgMap.get(JsonKey.ORG_LOCATION);
    if (StringUtils.isNotBlank(orgLocation)) {
      try {
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, String>> orgLoc = mapper.readValue(orgLocation, List.class);
        orgMap.put(JsonKey.ORG_LOCATION, orgLoc);
      } catch (Exception ex) {
        logger.error(context, "Exception occurred while parsing orgLocation", ex);
      }
    }
    logger.debug(context, "EsSyncBackgroundActor: getOrgDetails returned");
    return orgMap;
  }

  private DbInfo getDbInfoObj(String objectType) {
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
