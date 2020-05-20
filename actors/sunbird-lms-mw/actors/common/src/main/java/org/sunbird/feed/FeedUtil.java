package org.sunbird.feed;

import java.util.*;
import java.util.stream.IntStream;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.bean.ShadowUser;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.feed.impl.FeedFactory;
import org.sunbird.models.user.Feed;
import org.sunbird.models.user.FeedAction;
import org.sunbird.models.user.FeedStatus;

/** this class will be used as a Util for inserting Feed in table */
public class FeedUtil {
  private static IFeedService feedService = FeedFactory.getInstance();

  public static Response saveFeed(ShadowUser shadowUser, List<String> userIds) {
    return saveFeed(shadowUser, userIds.get(0));
  }

  public static Response saveFeed(ShadowUser shadowUser, String userId) {
    ProjectLogger.log("FeedUtil:saveFeed method called.", LoggerEnum.INFO.name());
    Response response = null;
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.USER_ID, userId);
    reqMap.put(JsonKey.CATEGORY, FeedAction.ORG_MIGRATION_ACTION.getfeedAction());
    ProjectLogger.log(
        "FeedUtil:saveFeed:fetching feed for userId ." + userId, LoggerEnum.INFO.name());
    List<Feed> feedList = feedService.getRecordsByProperties(reqMap);
    ProjectLogger.log(
        "FeedUtil:saveFeed total no. of feed fetched for user id ."
            + userId
            + " ,feed count = "
            + feedList.size(),
        LoggerEnum.INFO.name());
    int index = getIndexOfMatchingFeed(feedList);
    if (index == -1) {
      response = feedService.insert(createFeedObj(shadowUser, userId));
    } else {
      Map<String, Object> data = feedList.get(index).getData();
      List<String> channelList = (List<String>) data.get(JsonKey.PROSPECT_CHANNELS);
      if (!channelList.contains(shadowUser.getChannel())) {
        channelList.add(shadowUser.getChannel());
      }
      response = feedService.update(feedList.get(index));
    }
    return response;
  }

  private static Feed createFeedObj(ShadowUser shadowUser, String userId) {
    Feed feed = new Feed();
    feed.setPriority(1);
    feed.setCreatedBy(shadowUser.getAddedBy());
    feed.setStatus(FeedStatus.UNREAD.getfeedStatus());
    feed.setCategory(FeedAction.ORG_MIGRATION_ACTION.getfeedAction());
    Map<String, Object> prospectsChannel = new HashMap<>();
    List<String> channelList = new ArrayList<>();
    channelList.add(shadowUser.getChannel());
    prospectsChannel.put(JsonKey.PROSPECT_CHANNELS, channelList);
    feed.setData(prospectsChannel);
    feed.setUserId(userId);
    return feed;
  }

  private static int getIndexOfMatchingFeed(List<Feed> feedList) {
    int index =
        IntStream.range(0, feedList.size())
            .filter(i -> Objects.nonNull(feedList.get(i)))
            .filter(
                i ->
                    StringUtils.equalsIgnoreCase(
                        FeedAction.ORG_MIGRATION_ACTION.getfeedAction(),
                        feedList.get(i).getCategory()))
            .findFirst()
            .orElse(-1);
    return index;
  }
}
