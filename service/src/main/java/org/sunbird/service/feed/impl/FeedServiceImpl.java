package org.sunbird.service.feed.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.dao.feed.IFeedDao;
import org.sunbird.dao.feed.impl.FeedDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.model.user.Feed;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.feed.IFeedService;
import org.sunbird.util.ProjectUtil;

public class FeedServiceImpl implements IFeedService {
  private final LoggerUtil logger = new LoggerUtil(FeedServiceImpl.class);
  private static IFeedDao iFeedDao = FeedDaoImpl.getInstance();
  private final ObjectMapper mapper = new ObjectMapper();

  @Override
  public Response insert(Feed feed, RequestContext context) {

    logger.debug(context, "FeedServiceImpl:insert method called : ");
    Map<String, Object> dbMap = mapper.convertValue(feed, Map.class);
    String feedId = ProjectUtil.generateUniqueId();
    dbMap.put(JsonKey.ID, feedId);
    dbMap.put(JsonKey.CREATED_ON, new Timestamp(Calendar.getInstance().getTimeInMillis()));
    try {
      if (MapUtils.isNotEmpty(feed.getData())) {
        dbMap.put(JsonKey.FEED_DATA, mapper.writeValueAsString(feed.getData()));
      }
    } catch (Exception ex) {
      logger.error(context, "FeedServiceImpl:insert Exception occurred while mapping.", ex);
      ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
    }
    return iFeedDao.insert(dbMap, context);
  }

  @Override
  public Response update(Feed feed, RequestContext context) {
    logger.debug(context, "FeedServiceImpl:update method called : ");
    Map<String, Object> dbMap = mapper.convertValue(feed, Map.class);
    try {
      if (MapUtils.isNotEmpty(feed.getData())) {
        dbMap.put(JsonKey.FEED_DATA, mapper.writeValueAsString(feed.getData()));
      }
    } catch (Exception ex) {
      logger.error(context, "FeedServiceImpl:update Exception occurred while mapping.", ex);
      ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
    }
    dbMap.remove(JsonKey.CREATED_ON);
    dbMap.put(JsonKey.UPDATED_ON, new Timestamp(Calendar.getInstance().getTimeInMillis()));
    return iFeedDao.update(dbMap, context);
  }

  @Override
  public List<Feed> getFeedsByProperties(Map<String, Object> properties, RequestContext context) {
    logger.debug(context, "FeedServiceImpl:getFeedsByUserId method called : ");
    Response dbResponse = iFeedDao.getFeedsByProperties(properties, context);
    List<Map<String, Object>> responseList = null;
    List<Feed> feedList = new ArrayList<>();
    if (null != dbResponse && null != dbResponse.getResult()) {
      responseList = (List<Map<String, Object>>) dbResponse.getResult().get(JsonKey.RESPONSE);
      if (CollectionUtils.isNotEmpty(responseList)) {
        responseList.forEach(
            s -> {
              try {
                String data = (String) s.get(JsonKey.FEED_DATA);
                if (StringUtils.isNotBlank(data)) {
                  s.put(
                      JsonKey.FEED_DATA,
                      mapper.readValue(data, new TypeReference<Map<String, Object>>() {}));
                } else {
                  s.put(JsonKey.FEED_DATA, Collections.emptyMap());
                }
                feedList.add(mapper.convertValue(s, Feed.class));
              } catch (Exception ex) {
                logger.error(
                    context,
                    "FeedServiceImpl:getRecordsByUserId :Exception occurred while mapping feed data.",
                    ex);
              }
            });
      }
    }
    return feedList;
  }

  @Override
  public void delete(String id, String userId, String category, RequestContext context) {
    logger.debug(
        context, "FeedServiceImpl:delete method called for feedId : " + id + "user-id:" + userId);
    iFeedDao.delete(id, userId, category, context);
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
