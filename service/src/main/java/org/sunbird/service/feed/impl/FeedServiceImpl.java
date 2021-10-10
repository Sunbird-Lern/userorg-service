package org.sunbird.service.feed.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.model.user.Feed;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.feed.IFeedService;
import org.sunbird.util.ProjectUtil;

public class FeedServiceImpl implements IFeedService {
  private final LoggerUtil logger = new LoggerUtil(FeedServiceImpl.class);
  private final String notification_service_base_url =
      System.getenv("notification_service_base_url");
  private final ObjectMapper mapper = new ObjectMapper();

  @Override
  public Response insert(Request request, RequestContext context) {
    String NOTIFICATION_SERVICE_URL =
        notification_service_base_url + "/private/v2/notification/send";
    logger.debug(
        context,
        "FeedServiceImpl:insert :: calling notification service URL :" + NOTIFICATION_SERVICE_URL);
    Response response = new Response();
    Request req = new Request();
    Map<String, Object> reqObj = new HashMap<>();
    reqObj.put(JsonKey.NOTIFICATIONS, Arrays.asList(request.getRequest()));
    req.setRequest(reqObj);
    try {
      String json = mapper.writeValueAsString(req);
      json = new String(json.getBytes(), StandardCharsets.UTF_8);
      String responseStr =
          HttpClientUtil.post(NOTIFICATION_SERVICE_URL, json, getHeaders(context), context);
      logger.debug(context, "FeedServiceImpl:insert :: Response =" + response);
      response = mapper.readValue(responseStr, Response.class);
    } catch (Exception ex) {
      logger.error(context, "FeedServiceImpl:insert Exception occurred while mapping.", ex);
      ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
    }
    return response;
  }

  @Override
  public Response update(Request request, RequestContext context) {
    String NOTIFICATION_SERVICE_URL =
        notification_service_base_url + "/private/v1/notification/feed/update";
    Response response = new Response();
    try {
      String json = mapper.writeValueAsString(request);
      json = new String(json.getBytes(), StandardCharsets.UTF_8);
      String responseStr =
          HttpClientUtil.patch(NOTIFICATION_SERVICE_URL, json, getHeaders(context), context);
      logger.debug(context, "FeedServiceImpl:insert :: Response =" + response);
      response = mapper.readValue(responseStr, Response.class);
    } catch (Exception ex) {
      logger.error(context, "FeedServiceImpl:update Exception occurred while mapping.", ex);
      ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
    }
    return response;
  }

  @Override
  public List<Feed> getFeedsByProperties(Map<String, Object> properties, RequestContext context) {
    String NOTIFICATION_SERVICE_URL =
        notification_service_base_url
            + "/private/v1/notification/feed/read/"
            + properties.get(JsonKey.USER_ID);
    logger.debug(
        context,
        "FeedServiceImpl:insert :: calling notification service URL :" + NOTIFICATION_SERVICE_URL);
    List<Feed> feedList = new ArrayList<>();
    try {
      String response = HttpClientUtil.get(NOTIFICATION_SERVICE_URL, getHeaders(context), context);
      logger.debug(context, "FeedServiceImpl:callNotificationService :: Response =" + response);
      if (!StringUtils.isBlank(response)) {
        Response notificationRes = mapper.readValue(response, Response.class);
        List<Map<String, Object>> feeds =
            (List<Map<String, Object>>) notificationRes.getResult().get(JsonKey.FEEDS);
        if (CollectionUtils.isNotEmpty(feeds)) {
          for (Map<String, Object> feed : feeds) {
            feedList.add(mapper.convertValue(feed, Feed.class));
          }
        }
      }
    } catch (Exception ex) {
      logger.error(context, "FeedServiceImpl:read Exception occurred while mapping.", ex);
      ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
    }
    return feedList;
  }

  @Override
  public Response delete(Request request, RequestContext context) {
    String NOTIFICATION_SERVICE_URL =
        notification_service_base_url + "/private/v1/notification/feed/delete";
    Response response = new Response();
    request.getRequest().put(JsonKey.IDS, Arrays.asList(request.getRequest().get(JsonKey.FEED_ID)));
    try {
      String json = mapper.writeValueAsString(request);
      json = new String(json.getBytes(), StandardCharsets.UTF_8);
      String responseStr =
          HttpClientUtil.post(NOTIFICATION_SERVICE_URL, json, getHeaders(context), context);
      logger.debug(context, "FeedServiceImpl:insert :: Response =" + response);
      response = mapper.readValue(responseStr, Response.class);
    } catch (Exception ex) {
      logger.error(context, "FeedServiceImpl:read Exception occurred while mapping.", ex);
      ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
    }
    return response;
  }

  private Map<String, String> getHeaders(RequestContext context) {
    Map<String, String> headers = new HashMap<>();
    headers.put("Accept", "application/json");
    headers.put("Content-type", "application/json");
    headers.put("requestId", context.getReqId());
    ProjectUtil.setTraceIdInHeader(headers, context);
    return headers;
  }
}
