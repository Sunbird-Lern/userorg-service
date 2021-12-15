package org.sunbird.actor.feed;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.client.NotificationServiceClient;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.Feed;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.feed.FeedFactory;
import org.sunbird.service.feed.IFeedService;
import org.sunbird.telemetry.dto.TelemetryEnvKey;
import org.sunbird.util.Util;

public class UserFeedActor extends BaseActor {

  private IFeedService feedService;


  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    RequestContext context = request.getRequestContext();
    String operation = request.getOperation();
    logger.debug(context, "UserFeedActor:onReceive called for operation : " + operation);
    NotificationServiceClient serviceClient = new NotificationServiceClient();
    feedService = FeedFactory.getInstance(serviceClient);
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
    request
        .getRequest()
        .put(JsonKey.CREATED_BY, (String) request.getContext().get(JsonKey.REQUESTED_BY));
    Response feedCreateResponse = feedService.insert(request, context);
    sender().tell(feedCreateResponse, self());
  }

  private void deleteUserFeed(Request request, RequestContext context) {
    Response feedDeleteResponse = new Response();
    feedService.delete(request, context);
    feedDeleteResponse.getResult().put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    sender().tell(feedDeleteResponse, self());
  }

  private void updateUserFeed(Request request, RequestContext context) {
    Map<String, Object> updateRequest = request.getRequest();
    String feedId = (String) updateRequest.get(JsonKey.FEED_ID);
    updateRequest.put(JsonKey.IDS, Arrays.asList(feedId));
    updateRequest.put(JsonKey.UPDATED_BY, request.getContext().get(JsonKey.REQUESTED_BY));
    Response feedUpdateResponse = feedService.update(request, context);
    sender().tell(feedUpdateResponse, self());
  }
}
