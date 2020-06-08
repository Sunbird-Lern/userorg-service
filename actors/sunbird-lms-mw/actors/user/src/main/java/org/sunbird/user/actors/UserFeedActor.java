package org.sunbird.user.actors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.*;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.dto.SearchDTO;
import org.sunbird.feed.IFeedService;
import org.sunbird.feed.impl.FeedFactory;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.user.service.UserService;
import org.sunbird.user.service.impl.UserServiceImpl;
import org.sunbird.user.util.UserUtil;

/** This class contains API related to user feed. */
@ActorConfig(
  tasks = {"getUserFeedById"},
  asyncTasks = {}
)
public class UserFeedActor extends BaseActor {
  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private UserService userService = UserServiceImpl.getInstance();

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    String operation = request.getOperation();
    if (ActorOperations.GET_USER_FEED_BY_ID.getValue().equalsIgnoreCase(operation)) {
      ProjectLogger.log(
          "UserFeedActor:onReceive getUserFeed method called", LoggerEnum.INFO.name());
      getUserFeed(request);
    } else {
      onReceiveUnsupportedOperation("UserFeedActor");
    }
  }

  private void getUserFeed(Request request) {
    String userId = (String) request.getRequest().get(JsonKey.USER_ID);
    String contextUserId = (String) request.getContext().get(JsonKey.REQUESTED_BY);
    Map<String, Object> userDbRecord = UserUtil.getUserFromES(userId);
    if (userDbRecord == null || userDbRecord.size() == 0) {
      throw new ProjectCommonException(
              ResponseCode.userNotFound.getErrorCode(),
              ResponseCode.userNotFound.getErrorMessage(),
              ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }
    String managedById = (String) userDbRecord.get(JsonKey.MANAGED_BY);

    // If user account isManagedUser (managedBy passed in request) should be same as context user_id
    if ((StringUtils.isEmpty(managedById)
            && (!StringUtils.isBlank(userId) && !userId.equals(contextUserId)))
            || (StringUtils.isNotEmpty(managedById) && !contextUserId.equals(managedById))) {
      throw new ProjectCommonException(
              ResponseCode.unAuthorized.getErrorCode(),
              ResponseCode.unAuthorized.getErrorMessage(),
              ResponseCode.UNAUTHORIZED.getResponseCode());
    }
    IFeedService feedService = FeedFactory.getInstance();
    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.USER_ID, userId);
    SearchDTO search = new SearchDTO();
    search.getAdditionalProperties().put(JsonKey.FILTERS, filters);
    Response userFeedResponse = feedService.search(search);
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
