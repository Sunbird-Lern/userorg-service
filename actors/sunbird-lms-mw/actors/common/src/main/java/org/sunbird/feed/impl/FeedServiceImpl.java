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
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.dto.SearchDTO;
import org.sunbird.feed.IFeedService;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.models.user.Feed;
import scala.concurrent.Future;

public class FeedServiceImpl implements IFeedService {
  private Util.DbInfo usrFeedDbInfo = Util.dbInfoMap.get(JsonKey.USER_FEED_DB);
  private ObjectMapper mapper = new ObjectMapper();

  public static CassandraOperation getCassandraInstance() {
    return ServiceFactory.getInstance();
  }

  public static ElasticSearchService getESInstance() {
    return EsClientFactory.getInstance(JsonKey.REST);
  }

  @Override
  public Response insert(Feed feed) {
    ProjectLogger.log("FeedServiceImpl:insert method called : ", LoggerEnum.INFO.name());
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
      ProjectLogger.log("FeedServiceImpl:insert Exception occurred while mapping.", ex);
    }
    Response response = saveFeed(dbMap);
    // save data to ES
    dbMap.put(JsonKey.FEED_DATA, feedData);
    dbMap.put(JsonKey.CREATED_ON, Calendar.getInstance().getTimeInMillis());
    getESInstance().save(ProjectUtil.EsType.userfeed.getTypeName(), feedId, dbMap);
    return response;
  }

  @Override
  public Response update(Feed feed) {
    ProjectLogger.log("FeedServiceImpl:update method called : ", LoggerEnum.INFO.name());
    Map<String, Object> feedData = feed.getData();
    Map<String, Object> dbMap = mapper.convertValue(feed, Map.class);
    try {
      if (MapUtils.isNotEmpty(feed.getData())) {
        dbMap.put(JsonKey.FEED_DATA, mapper.writeValueAsString(feed.getData()));
      }
    } catch (Exception ex) {
      ProjectLogger.log("FeedServiceImpl:update Exception occurred while mapping.", ex);
    }
    dbMap.remove(JsonKey.CREATED_ON);
    dbMap.put(JsonKey.UPDATED_ON, new Timestamp(Calendar.getInstance().getTimeInMillis()));
    Response response = saveFeed(dbMap);
    // update data to ES
    dbMap.put(JsonKey.FEED_DATA, feedData);
    dbMap.put(JsonKey.UPDATED_ON, Calendar.getInstance().getTimeInMillis());
    getESInstance().update(ProjectUtil.EsType.userfeed.getTypeName(), feed.getId(), dbMap);
    return response;
  }

  @Override
  public List<Feed> getRecordsByProperties(Map<String, Object> properties) {
    ProjectLogger.log(
        "FeedServiceImpl:getRecordsByProperties method called : ", LoggerEnum.INFO.name());
    Response dbResponse =
        getCassandraInstance()
            .getRecordsByProperties(
                usrFeedDbInfo.getKeySpace(), usrFeedDbInfo.getTableName(), properties);
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
                ProjectLogger.log(
                    "FeedServiceImpl:getRecordsByProperties :Exception occurred while mapping feed data.",
                    ex);
              }
            });
      }
    }
    return feedList;
  }

  @Override
  public Response search(SearchDTO searchDTO) {
    ProjectLogger.log("FeedServiceImpl:search method called : ", LoggerEnum.INFO.name());
    Future<Map<String, Object>> resultF =
        getESInstance().search(searchDTO, ProjectUtil.EsType.userfeed.getTypeName());
    Map<String, Object> result =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
    Response response = new Response();
    response.put(JsonKey.RESPONSE, result);
    return response;
  }

  @Override
  public void delete(String id) {
    ProjectLogger.log(
        "FeedServiceImpl:delete method called for feedId : " + id, LoggerEnum.INFO.name());
    getCassandraInstance()
        .deleteRecord(usrFeedDbInfo.getKeySpace(), usrFeedDbInfo.getTableName(), id);
    getESInstance().delete(ProjectUtil.EsType.userfeed.getTypeName(), id);
  }

  private Response saveFeed(Map<String, Object> feed) {
    return getCassandraInstance()
        .upsertRecord(usrFeedDbInfo.getKeySpace(), usrFeedDbInfo.getTableName(), feed);
  }
}
