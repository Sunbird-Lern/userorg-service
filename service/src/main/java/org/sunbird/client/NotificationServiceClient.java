package org.sunbird.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.PropertiesCache;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class NotificationServiceClient {

    private final LoggerUtil logger = new LoggerUtil(NotificationServiceClient.class);
    private ObjectMapper mapper = new ObjectMapper();


    private Map<String,String> getHeader(RequestContext context){
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        headers.put("Content-type", "application/json");
        ProjectUtil.setTraceIdInHeader(headers, context);
        return headers;
    }

    private String getJsonString(Request req) throws JsonProcessingException {
        String json = mapper.writeValueAsString(req);
        json = new String(json.getBytes(), StandardCharsets.UTF_8);
        return json;
    }

    /**
     * Call Sync V2 send notification API to send notification feed
     * @param reqObj
     * @param context
     * @return
     */
    public Response  sendSyncV2Notification(Request reqObj, RequestContext context) {
        logger.debug(context, "NotificationServiceClient:sendSyncV2Notification method called : ");

        String serviceUrl = getServiceApiUrl(JsonKey.NOTIFICATION_SERVICE_V2_SEND_URL);
        logger.debug(
                context,
                "NotificationServiceClient:sendSyncV2Notification :: calling notification service URL :" + serviceUrl);
        try {
            Response response = callCreateOrDeleteNotificationService(reqObj, context, serviceUrl);
            return response;
        }  catch (Exception ex) {
            logger.error(context, "FeedServiceImpl:sendSyncV2Notification Exception occurred while mapping.", ex);
            ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
        }

        return null;
    }

    /**
     * Call v1 Update notification service api to update feeds
     * @param reqObj
     * @param context
     * @return
     */
    public Response  updateV1Notification(Request reqObj, RequestContext context) {
        logger.debug(context, "NotificationServiceClient:updateV1Notification method called : ");

        String serviceUrl = getServiceApiUrl(JsonKey.NOTIFICATION_SERVICE_V1_UPDATE_URL);
        logger.debug(
                context,
                "NotificationServiceClient:updateV1Notification :: calling notification service URL :" + serviceUrl);
        try {
            String json = getJsonString(reqObj);
            String responseStr = HttpClientUtil.patch(serviceUrl, json,  getHeader(context), context);
            Response response = mapper.readValue(responseStr, Response.class);
            return response;
        }  catch (Exception ex) {
            logger.error(context, "FeedServiceImpl:updateV1Notification Exception occurred while mapping.", ex);
            ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
        }

        return null;

    }

    /**
     *  Call V1 read notification to read notifications
     * @param reqObj
     * @param context
     * @return
     */
    public Response  readV1Notification(Request reqObj, RequestContext context) {
        logger.debug(context, "NotificationServiceClient:readV1Notification method called : ");
        String serviceUrl = getServiceApiUrl(JsonKey.NOTIFICATION_SERVICE_V1_READ_URL);
        logger.debug(
                context,
                "NotificationServiceClient:readV1Notification :: calling notification service URL :" + serviceUrl);
        try {
            String responseStr = HttpClientUtil.get(serviceUrl+ "/" +reqObj.getRequest().get(JsonKey.USER_ID), getHeader(context), context);
            Response response = mapper.readValue(responseStr, Response.class);
            return response;
        }  catch (Exception ex) {
            logger.error(context, "FeedServiceImpl:insert Exception occurred while mapping.", ex);
            ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
        }

        return null;
    }

    /**
     * Call v1 delete notification service api
     * @param reqObj
     * @param context
     * @return
     */
    public Response  deleteV1Notification(Request reqObj, RequestContext context) {
        logger.debug(context, "NotificationServiceClient:readV1Notification method called : ");

        String serviceUrl = getServiceApiUrl(JsonKey.NOTIFICATION_SERVICE_V1_DELETE_URL);
        logger.debug(
                context,
                "NotificationServiceClient:readV1Notification :: calling notification service URL :" + serviceUrl);
        try {
            Response response = callCreateOrDeleteNotificationService(reqObj, context, serviceUrl);
            return response;
        }  catch (Exception ex) {
            logger.error(context, "FeedServiceImpl:insert Exception occurred while mapping.", ex);
            ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
        }

        return null;
    }

    private String getServiceApiUrl(String serviceUrlKey){
        String NOTIFICATION_SERVICE_BASE_URL = PropertiesCache.getInstance().getProperty(JsonKey.NOTIFICATION_SERVICE_BASE_URL);
        String NOTIFICATION_SERVICE_URL = PropertiesCache.getInstance().getProperty(serviceUrlKey);
        return NOTIFICATION_SERVICE_BASE_URL+NOTIFICATION_SERVICE_URL;
    }
    private Response callCreateOrDeleteNotificationService(Request reqObj, RequestContext context, String serviceUrl) throws JsonProcessingException {
        String json = getJsonString(reqObj);
        String responseStr = HttpClientUtil.post(serviceUrl, json, getHeader(context), context);
        return mapper.readValue(responseStr, Response.class);
    }

}
