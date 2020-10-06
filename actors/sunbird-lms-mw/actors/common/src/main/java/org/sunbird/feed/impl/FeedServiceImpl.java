package org.sunbird.feed.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.*;
import org.sunbird.common.request.RequestContext;
import org.sunbird.dto.SearchDTO;
import org.sunbird.feed.IFeedService;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.models.user.Feed;
import scala.concurrent.Future;

public class FeedServiceImpl implements IFeedService {
  private static LoggerUtil logger = new LoggerUtil(FeedServiceImpl.class);

  private Util.DbInfo usrFeedDbInfo = Util.dbInfoMap.get(JsonKey.USER_FEED_DB);
  private ObjectMapper mapper = new ObjectMapper();

  public static CassandraOperation getCassandraInstance() {
    return ServiceFactory.getInstance();
  }

  public static ElasticSearchService getESInstance() {
    return EsClientFactory.getInstance(JsonKey.REST);
  }

  @Override
  public Response insert(Feed feed, RequestContext context) {
    logger.info(context, "FeedServiceImpl:insert method called : ");
    Map<String, Object> feedData = feed.getData();
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
    }
    Response response = saveFeed(dbMap, context);
    // save data to ES
    dbMap.put(JsonKey.FEED_DATA, feedData);
    dbMap.put(JsonKey.CREATED_ON, Calendar.getInstance().getTimeInMillis());
    getESInstance().save(ProjectUtil.EsType.userfeed.getTypeName(), feedId, dbMap, context);
    return response;
  }

  @Override
  public Response update(Feed feed, RequestContext context) {
    logger.info(context, "FeedServiceImpl:update method called : ");
    Map<String, Object> feedData = feed.getData();
    Map<String, Object> dbMap = mapper.convertValue(feed, Map.class);
    try {
      if (MapUtils.isNotEmpty(feed.getData())) {
        dbMap.put(JsonKey.FEED_DATA, mapper.writeValueAsString(feed.getData()));
      }
    } catch (Exception ex) {
      logger.error(context, "FeedServiceImpl:update Exception occurred while mapping.", ex);
    }
    dbMap.remove(JsonKey.CREATED_ON);
    dbMap.put(JsonKey.UPDATED_ON, new Timestamp(Calendar.getInstance().getTimeInMillis()));
    Response response = saveFeed(dbMap, context);
    // update data to ES
    dbMap.put(JsonKey.FEED_DATA, feedData);
    dbMap.put(JsonKey.UPDATED_ON, Calendar.getInstance().getTimeInMillis());
    getESInstance().update(ProjectUtil.EsType.userfeed.getTypeName(), feed.getId(), dbMap, context);
    return response;
  }

  @Override
  public List<Feed> getRecordsByUserId(Map<String, Object> properties, RequestContext context) {
    logger.info(context, "FeedServiceImpl:getRecordsByUserId method called : ");
    Response dbResponse =
        getCassandraInstance()
            .getRecordById(
                usrFeedDbInfo.getKeySpace(), usrFeedDbInfo.getTableName(), properties, context);
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
  public Response search(SearchDTO searchDTO, RequestContext context) {
    logger.info(context, "FeedServiceImpl:search method called : ");
    Future<Map<String, Object>> resultF =
        getESInstance().search(searchDTO, ProjectUtil.EsType.userfeed.getTypeName(), context);
    Map<String, Object> result =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
    Response response = new Response();
    response.put(JsonKey.RESPONSE, result);
    return response;
  }

  @Override
  public void delete(String id, String userId, String category, RequestContext context) {
    logger.info(context, "FeedServiceImpl:delete method called for feedId : " + id);
    Map<String, String> compositeKey = new HashMap();
    compositeKey.put("userid", userId);
    compositeKey.put("id", id);
    compositeKey.put("category", category);
    getCassandraInstance()
        .deleteRecord(
            usrFeedDbInfo.getKeySpace(), usrFeedDbInfo.getTableName(), compositeKey, context);
    getESInstance().delete(ProjectUtil.EsType.userfeed.getTypeName(), id, context);
  }

  private Response saveFeed(Map<String, Object> feed, RequestContext context) {
    return getCassandraInstance()
        .upsertRecord(usrFeedDbInfo.getKeySpace(), usrFeedDbInfo.getTableName(), feed, context);
  }
}
