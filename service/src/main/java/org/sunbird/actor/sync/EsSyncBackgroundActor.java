package org.sunbird.actor.sync;

import java.text.MessageFormat;
import java.util.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.organisation.validator.OrgTypeValidator;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.userorg.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.location.LocationService;
import org.sunbird.service.location.LocationServiceImpl;
import org.sunbird.service.organisation.OrgService;
import org.sunbird.service.organisation.impl.OrgServiceImpl;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.common.ProjectUtil;

public class EsSyncBackgroundActor extends BaseActor {

  private final OrgService orgService = OrgServiceImpl.getInstance();
  private final LocationService locationService = LocationServiceImpl.getInstance();
  private final UserService userService = UserServiceImpl.getInstance();

  @Override
  public void onReceive(Request request) throws Throwable {
    String operation = request.getOperation();
    if (ActorOperations.BACKGROUND_SYNC.getValue().equalsIgnoreCase(operation)) {
      sync(request);
    } else {
      onReceiveUnsupportedOperation();
    }
  }

  private void sync(Request message) {
    logger.info(message.getRequestContext(), "EsSyncBackgroundActor: sync called");
    long startTime = System.currentTimeMillis();
    Map<String, Object> req = message.getRequest();
    Map<String, Object> dataMap = (Map<String, Object>) req.get(JsonKey.DATA);

    String objectType = (String) dataMap.get(JsonKey.OBJECT_TYPE);
    String operationType = (String) dataMap.get(JsonKey.OPERATION_TYPE);

    List<String> objectIds = new ArrayList<>();
    if (null != dataMap.get(JsonKey.OBJECT_IDS)) {
      objectIds = (List<String>) dataMap.get(JsonKey.OBJECT_IDS);
    }
    Response finalResponse = new Response();
    if (JsonKey.USER.equalsIgnoreCase(objectType)) {
      handleUserSyncRequest(objectIds, finalResponse, message.getRequestContext());
    } else if (JsonKey.ORGANISATION.equalsIgnoreCase(objectType)) {
      handleOrgAndLocationSync(objectIds, objectType, finalResponse, message.getRequestContext());
    } else if (JsonKey.LOCATION.equalsIgnoreCase(objectType)) {
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
      List<String> objectIds,
      String objectType,
      Response finalResponse,
      RequestContext requestContext) {
    if (CollectionUtils.isNotEmpty(objectIds)) {
      List<Map<String, Object>> responseList = getObjectData(objectIds, objectType, requestContext);
      Map<String, Object> responseMap = new HashMap<>();
      if (CollectionUtils.isNotEmpty(responseList)) {
        for (Map<String, Object> map : responseList) {
          String objectId = (String) map.get(JsonKey.ID);
          try {
            logger.info(
                requestContext,
                "EsSyncBackgroundActor:handleOrgAndLocationSync for objectType :"
                    + objectType
                    + " for id : "
                    + objectId);
            String esResponse = "";
            if (JsonKey.ORGANISATION.equals(objectType)
                || (JsonKey.LOCATION.equalsIgnoreCase(objectType))) {
              OrgTypeValidator.getInstance().updateOrganisationTypeFlags(map);
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

  private List<Map<String, Object>> getObjectData(
      List<String> objectIds, String objectType, RequestContext requestContext) {
    String requestLogMsg =
        MessageFormat.format(
            "type = {0} and IDs = {1}", objectType, Arrays.toString(objectIds.toArray()));
    logger.info(
        requestContext,
        "EsSyncBackgroundActor:handleOrgAndLocationSync: Fetching data for "
            + requestLogMsg
            + " started");
    List<Map<String, Object>> responseList;
    if (JsonKey.ORGANISATION.equalsIgnoreCase(objectType)) {
      responseList = orgService.getOrgByIds(objectIds, requestContext);
    } else {
      responseList =
          locationService.getLocationsByIds(objectIds, Collections.emptyList(), requestContext);
    }
    logger.info(
        requestContext,
        "EsSyncBackgroundActor:handleOrgAndLocationSync: Fetching data for "
            + requestLogMsg
            + " completed");
    return responseList;
  }

  private void handleUserSyncRequest(
      List<String> objectIds, Response finalResponse, RequestContext context) {
    if (CollectionUtils.isNotEmpty(objectIds)) {
      Map<String, Object> esResponse = new HashMap<>();
      for (String userId : objectIds) {
        try {
          logger.info(
              context,
              "EsSyncBackgroundActor:handleUserSyncRequest: Trigger sync of user details to ES");
          Map<String, Object> userDetails = userService.getUserDetailsForES(userId, context);
          if (MapUtils.isNotEmpty(userDetails)) {
            logger.info(
                context,
                "EsSyncBackgroundActor:handleUserSyncRequest user rootOrgId :"
                    + userDetails.get(JsonKey.ROOT_ORG_ID)
                    + ", userId : "
                    + userDetails.get(JsonKey.ID));
            String response =
                saveDataToEs(ProjectUtil.EsType.user.getTypeName(), userId, userDetails, context);
            if (StringUtils.isNotBlank(response) && userId.equalsIgnoreCase(response)) {
              esResponse.put(userId, userId.equalsIgnoreCase(response));
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
          finalResponse.put(userId, false);
        }
      }
      finalResponse.getResult().put(JsonKey.ES_SYNC_RESPONSE, esResponse);
    }
  }

  private String saveDataToEs(
      String esType, String id, Map<String, Object> data, RequestContext context) {
    if (ProjectUtil.EsType.organisation.getTypeName().equalsIgnoreCase(esType)) {
      return orgService.saveOrgToEs(id, data, context);
    } else if (ProjectUtil.EsType.location.getTypeName().equalsIgnoreCase(esType)) {
      return locationService.saveLocationToEs(id, data, context);
    } else if (ProjectUtil.EsType.user.getTypeName().equalsIgnoreCase(esType)) {
      return userService.saveUserToES(id, data, context);
    }
    return "";
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
}
