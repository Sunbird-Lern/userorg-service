package org.sunbird.user.actors;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.*;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.dto.SearchDTO;
import org.sunbird.feed.IFeedService;
import org.sunbird.feed.impl.FeedFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.models.user.Feed;
import org.sunbird.models.user.FeedStatus;

/** This class contains API related to user feed. */
@ActorConfig(
  tasks = {"getUserFeedById", "createUserFeed"},
  asyncTasks = {},
  dispatcher = "most-used-two-dispatcher"
)
public class UserFeedActor extends BaseActor {

  IFeedService feedService = FeedFactory.getInstance();
  ObjectMapper mapper = new ObjectMapper();

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    RequestContext context = request.getRequestContext();
    String operation = request.getOperation();
    if (ActorOperations.GET_USER_FEED_BY_ID.getValue().equalsIgnoreCase(operation)) {
      logger.info(context, "UserFeedActor:onReceive getUserFeed method called");
      String userId = (String) request.getRequest().get(JsonKey.USER_ID);
      getUserFeed(userId, context);
    } else if (ActorOperations.CREATE_USER_FEED.getValue().equalsIgnoreCase(operation)) {
      logger.info(context, "UserFeedActor:onReceive createUserFeed method called");
      createUserFeed(request, context);
    } else if (ActorOperations.DELETE_USER_FEED.getValue().equalsIgnoreCase(operation)) {
      logger.info(context, "UserFeedActor:onReceive deleteUserFeed method called");
      deleteUserFeed(request, context);
    } else {
      onReceiveUnsupportedOperation("UserFeedActor");
    }
  }
  
  private void deleteUserFeed(Request request, RequestContext context) {
    Response feedDeleteResponse = new Response();
    Map<String, Object> deleteRequest = request.getRequest();
    feedService.delete((String) deleteRequest.get(JsonKey.FEED_ID), (String) deleteRequest.get(JsonKey.USER_ID),
      (String)deleteRequest.get(JsonKey.CATEGORY), context);
    feedDeleteResponse.getResult().put("")
    sender().tell(feedDeleteResponse, self());
  }
  
  private void createUserFeed(Request request, RequestContext context) {
    Feed feed = mapper.convertValue(request.getRequest(), Feed.class);
    Response feedCreateResponse = feedService.insert(feed, context);
    sender().tell(feedCreateResponse, self());
  }

  private void getUserFeed(String userId, RequestContext context) {
    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.USER_ID, userId);
    SearchDTO search = new SearchDTO();
    search.getAdditionalProperties().put(JsonKey.FILTERS, filters);
    Response userFeedResponse = feedService.search(search, context);
    Map<String, Object> result =
        (Map<String, Object>) userFeedResponse.getResult().get(JsonKey.RESPONSE);
    result.put(
        JsonKey.USER_FEED,
        result.get(JsonKey.CONTENT) == null ? new ArrayList<>() : result.get(JsonKey.CONTENT));
    result.remove(JsonKey.COUNT);
    result.remove(JsonKey.CONTENT);
    sender().tell(userFeedResponse, self());
  }
}
