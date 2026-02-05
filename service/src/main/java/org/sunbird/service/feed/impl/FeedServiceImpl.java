package org.sunbird.service.feed.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.apache.commons.collections.CollectionUtils;

import org.sunbird.client.NotificationServiceClient;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.model.user.Feed;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.feed.IFeedService;

public class FeedServiceImpl implements IFeedService {
  private final LoggerUtil logger = new LoggerUtil(FeedServiceImpl.class);
  private final ObjectMapper mapper = new ObjectMapper();
  private NotificationServiceClient serviceClient;
  public FeedServiceImpl(NotificationServiceClient serviceClient){
     this.serviceClient= serviceClient;
  }
  @Override
  public Response insert(Request request, RequestContext context) {

    logger.debug(context, "FeedServiceImpl:insert method called : ");

    Request req = new Request();
    Map<String, Object> reqObj = new HashMap<>();
    reqObj.put(JsonKey.NOTIFICATIONS, Arrays.asList(request.getRequest()));
    req.setRequest(reqObj);
    return serviceClient.sendSyncV2Notification(req,context);
  }

  @Override
  public Response update(Request request, RequestContext context) {
    logger.debug(context, "FeedServiceImpl:update method called : ");
    return serviceClient.updateV1Notification(request,context);
  }

  @Override
  public List<Feed> getFeedsByProperties(Map<String, Object> request, RequestContext context) {

    List<Feed> feedList = new ArrayList<>();
    Request req = new Request();
    req.setRequest(request);
    Response response = serviceClient.readV1Notification(req,context);
    if (null != response) {
      List<Map<String, Object>> feeds =
          (List<Map<String, Object>>) response.getResult().get(JsonKey.FEEDS);
      if (CollectionUtils.isNotEmpty(feeds)) {
         for (Map<String, Object> feed : feeds) {
           feedList.add(mapper.convertValue(feed, Feed.class));
         }
       }
     }

    return feedList;
  }

  @Override
  public void delete(Request request, RequestContext context) {
    Request req = new Request();
    Map<String,Object> reqObj = new HashMap<>();
    reqObj.put(JsonKey.IDS,Arrays.asList(request.getRequest().get(JsonKey.FEED_ID)));
    reqObj.put(JsonKey.USER_ID,request.getRequest().get(JsonKey.USER_ID));
    reqObj.put(JsonKey.CATEGORY,request.getRequest().get(JsonKey.CATEGORY));
    req.setRequest(reqObj);
    Response response = serviceClient.deleteV1Notification(req,context);
    if(null == response){
      ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
    }
  }

}
