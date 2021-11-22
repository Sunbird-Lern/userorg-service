package org.sunbird.dao.feed.impl;

import java.util.LinkedHashMap;
import java.util.Map;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.dao.feed.IFeedDao;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.feed.impl.FeedServiceImpl;
import org.sunbird.util.Util;

public class FeedDaoImpl implements IFeedDao {

  private final LoggerUtil logger = new LoggerUtil(FeedServiceImpl.class);
  private final Util.DbInfo usrFeedDbInfo = Util.dbInfoMap.get(JsonKey.USER_FEED_DB);
  private static IFeedDao iFeedDao = null;

  public static IFeedDao getInstance() {
    if (iFeedDao == null) {
      iFeedDao = new FeedDaoImpl();
    }
    return iFeedDao;
  }

  public static CassandraOperation getCassandraInstance() {
    return ServiceFactory.getInstance();
  }

  public Response insert(Map<String, Object> feedMap, RequestContext context) {
    logger.debug(
        context,
        "FeedDaoImpl: insert called for feedId : "
            + feedMap.get(JsonKey.ID)
            + " and userId:"
            + feedMap.get(JsonKey.USER_ID));
    return getCassandraInstance()
        .insertRecord(usrFeedDbInfo.getKeySpace(), usrFeedDbInfo.getTableName(), feedMap, context);
  }

  public Response update(Map<String, Object> feedMap, RequestContext context) {
    logger.debug(
        context,
        "FeedDaoImpl: update called for feedId : "
            + feedMap.get(JsonKey.ID)
            + " and userId:"
            + feedMap.get(JsonKey.USER_ID));
    Map<String, Object> compositeKey = new LinkedHashMap<>(3);
    compositeKey.put(JsonKey.USER_ID, feedMap.remove(JsonKey.USER_ID));
    compositeKey.put(JsonKey.CATEGORY, feedMap.remove(JsonKey.CATEGORY));
    compositeKey.put(JsonKey.ID, feedMap.remove(JsonKey.ID));
    return getCassandraInstance()
        .updateRecord(
            usrFeedDbInfo.getKeySpace(),
            usrFeedDbInfo.getTableName(),
            feedMap,
            compositeKey,
            context);
  }

  public Response getFeedsByProperties(Map<String, Object> properties, RequestContext context) {
    logger.debug(
        context,
        "FeedDaoImpl: getFeedsByProperties called for userId : " + properties.get(JsonKey.USER_ID));
    return getCassandraInstance()
        .getRecordById(
            usrFeedDbInfo.getKeySpace(), usrFeedDbInfo.getTableName(), properties, context);
  }

  public void delete(String id, String userId, String category, RequestContext context) {
    logger.debug(
        context, "FeedDaoImpl: delete method called for feedId : " + id + " and userId:" + userId);
    Map<String, String> compositeKey = new LinkedHashMap<>(3);
    compositeKey.put("userid", userId);
    compositeKey.put("category", category);
    compositeKey.put("id", id);
    getCassandraInstance()
        .deleteRecord(
            usrFeedDbInfo.getKeySpace(), usrFeedDbInfo.getTableName(), compositeKey, context);
  }
}
