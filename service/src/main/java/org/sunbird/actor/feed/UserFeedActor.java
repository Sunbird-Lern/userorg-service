package org.sunbird.actor.feed;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.Feed;
import org.sunbird.model.user.FeedStatus;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.feed.FeedFactory;
import org.sunbird.service.feed.IFeedService;
import org.sunbird.telemetry.dto.TelemetryEnvKey;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.Util;

public class UserFeedActor extends BaseActor {

  IFeedService feedService = FeedFactory.getInstance();
  ObjectMapper mapper = new ObjectMapper();

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    RequestContext context = request.getRequestContext();
    String operation = request.getOperation();
    logger.debug(context, "UserFeedActor:onReceive called for operation : " + operation);
    switch (operation) {
      case "getUserFeedById":
        String userId = (String) request.getRequest().get(JsonKey.USER_ID);
        getUserFeed(userId, context);
        break;
      case "createUserFeed":
        createUserFeed(request, context);
        break;
      case "deleteUserFeed":
        deleteUserFeed(request, context);
        break;
      case "updateUserFeed":
        updateUserFeed(request, context);
        break;
      default:
        onReceiveUnsupportedOperation();
    }
  }

  private void getUserFeed(String userId, RequestContext context) {
    Map<String, Object> reqMap = new WeakHashMap<>(2);
    reqMap.put(JsonKey.USER_ID, userId);
    List<Feed> feedList = feedService.getFeedsByProperties(reqMap, context);
    Map<String, Object> result = new HashMap<>();
    result.put(JsonKey.USER_FEED, feedList);
    Response response = new Response();
    response.put(JsonKey.RESPONSE, result);
    sender().tell(response, self());
  }

  private void createUserFeed(Request request, RequestContext context) {
    Feed feed = mapper.convertValue(request.getRequest(), Feed.class);
    feed.setStatus(FeedStatus.UNREAD.getfeedStatus());
    feed.setCreatedBy((String) request.getContext().get(JsonKey.REQUESTED_BY));
    Response feedCreateResponse = feedService.insert(feed, context);
    sender().tell(feedCreateResponse, self());
    // Delete the old user feed
    Map<String, Object> reqMap = new WeakHashMap<>(2);
    reqMap.put(JsonKey.USER_ID, feed.getUserId());
    List<Feed> feedList = feedService.getFeedsByProperties(reqMap, context);
    if (feedList.size() >= Integer.parseInt(ProjectUtil.getConfigValue(JsonKey.FEED_LIMIT))) {
      feedList.sort(Comparator.comparing(Feed::getCreatedOn));
      Feed delRecord = feedList.get(0);
      feedService.delete(
          delRecord.getId(), delRecord.getUserId(), delRecord.getCategory(), context);
    }
  }

  private void deleteUserFeed(Request request, RequestContext context) {
    Response feedDeleteResponse = new Response();
    Map<String, Object> deleteRequest = request.getRequest();
    feedService.delete(
        (String) deleteRequest.get(JsonKey.FEED_ID),
        (String) deleteRequest.get(JsonKey.USER_ID),
        (String) deleteRequest.get(JsonKey.CATEGORY),
        context);
    feedDeleteResponse.getResult().put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    sender().tell(feedDeleteResponse, self());
  }

  private void updateUserFeed(Request request, RequestContext context) {
    Map<String, Object> updateRequest = request.getRequest();
    String feedId = (String) updateRequest.get(JsonKey.FEED_ID);
    Feed feed = mapper.convertValue(updateRequest, Feed.class);
    feed.setId(feedId);
    feed.setStatus(FeedStatus.READ.getfeedStatus());
    feed.setUpdatedBy((String) request.getContext().get(JsonKey.REQUESTED_BY));
    Response feedUpdateResponse = feedService.update(feed, context);
    sender().tell(feedUpdateResponse, self());
  }
}
