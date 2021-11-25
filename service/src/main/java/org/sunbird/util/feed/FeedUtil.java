package org.sunbird.util.feed;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.model.ShadowUser;
import org.sunbird.model.user.Feed;
import org.sunbird.model.user.FeedAction;
import org.sunbird.model.user.FeedStatus;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.feed.FeedFactory;
import org.sunbird.service.feed.IFeedService;
import org.sunbird.service.organisation.OrgService;
import org.sunbird.service.organisation.impl.OrgServiceImpl;

/** this class will be used as a Util for inserting Feed in table */
public class FeedUtil {
  private static final LoggerUtil logger = new LoggerUtil(FeedUtil.class);

  private static final IFeedService feedService = FeedFactory.getInstance();
  private static final OrgService orgService = OrgServiceImpl.getInstance();
  private static Map<String, Object> orgIdMap = new HashMap<>();

  public static Response saveFeed(
      ShadowUser shadowUser, List<String> userIds, RequestContext context) {
    return saveFeed(shadowUser, userIds.get(0), context);
  }

  public static Response saveFeed(ShadowUser shadowUser, String userId, RequestContext context) {
    Response response = null;
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.USER_ID, userId);
    reqMap.put(JsonKey.CATEGORY, FeedAction.ORG_MIGRATION_ACTION.getfeedAction());
    logger.info(context, "FeedUtil:saveFeed:fetching feed for userId ." + userId);
    List<Feed> feedList = feedService.getFeedsByProperties(reqMap, context);
    logger.info(
        context,
        "FeedUtil:saveFeed total no. of feed fetched for user id ."
            + userId
            + " ,feed count = "
            + feedList.size());
    int index = getIndexOfMatchingFeed(feedList);
    if (index == -1) {
      response = feedService.insert(createFeedObj(shadowUser, userId, context), context);
    } else {
      Map<String, Object> data = feedList.get(index).getData();
      List<String> channelList = (List<String>) data.get(JsonKey.PROSPECT_CHANNELS);
      if (!channelList.contains(shadowUser.getChannel())) {
        channelList.add(shadowUser.getChannel());
        List<Map<String, String>> orgList =
            (ArrayList<Map<String, String>>) data.get(JsonKey.PROSPECT_CHANNELS_IDS);
        orgList.addAll(getOrgDetails(shadowUser.getChannel(), context));
      }
      response = feedService.update(feedList.get(index), context);
    }
    return response;
  }

  private static Feed createFeedObj(ShadowUser shadowUser, String userId, RequestContext context) {
    Feed feed = new Feed();
    feed.setPriority(1);
    feed.setCreatedBy(shadowUser.getAddedBy());
    feed.setStatus(FeedStatus.UNREAD.getfeedStatus());
    feed.setCategory(FeedAction.ORG_MIGRATION_ACTION.getfeedAction());
    Map<String, Object> prospectsChannel = new HashMap<>();
    List<String> channelList = new ArrayList<>();
    channelList.add(shadowUser.getChannel());
    prospectsChannel.put(JsonKey.PROSPECT_CHANNELS, channelList);
    prospectsChannel.put(
        JsonKey.PROSPECT_CHANNELS_IDS, getOrgDetails(shadowUser.getChannel(), context));
    feed.setData(prospectsChannel);
    feed.setUserId(userId);
    return feed;
  }

  private static List<Map<String, String>> getOrgDetails(String channel, RequestContext context) {
    List<Map<String, String>> orgList = new CopyOnWriteArrayList<>();
    Map<String, String> orgMap = new HashMap<>();
    if (!orgIdMap.isEmpty() && orgIdMap.containsKey(channel)) {
      orgMap = (Map<String, String>) orgIdMap.get(channel);
    } else {
      Map<String, Object> org = orgService.getRootOrgFromChannel(channel, context);
      orgMap.put("id", (String) org.get(JsonKey.ID));
      orgMap.put("name", channel);
      orgIdMap.put(channel, orgMap);
    }
    orgList.add(orgMap);
    return orgList;
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
