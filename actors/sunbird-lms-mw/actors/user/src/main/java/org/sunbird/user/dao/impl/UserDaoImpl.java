package org.sunbird.user.dao.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.learner.util.Util;
import org.sunbird.models.user.User;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.user.dao.UserDao;

/**
 * Implementation class of UserDao interface.
 *
 * @author Amit Kumar
 */
public class UserDaoImpl implements UserDao {

  private static final String TABLE_NAME = "user";
  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private ObjectMapper mapper = new ObjectMapper();
  private static UserDao userDao = null;
  private ElasticSearchService esUtil = EsClientFactory.getInstance(JsonKey.REST);

  public static UserDao getInstance() {
    if (userDao == null) {
      userDao = new UserDaoImpl();
    }
    return userDao;
  }

  @Override
  public Response createUser(Map<String, Object> user, RequestContext context) {
    return cassandraOperation.insertRecord(Util.KEY_SPACE_NAME, TABLE_NAME, user, context);
  }

  @Override
  public Response updateUser(User user, RequestContext context) {
    Map<String, Object> map = mapper.convertValue(user, Map.class);
    return cassandraOperation.updateRecord(Util.KEY_SPACE_NAME, TABLE_NAME, map, context);
  }

  @Override
  public Response updateUser(Map<String, Object> userMap, RequestContext context) {
    return cassandraOperation.updateRecord(Util.KEY_SPACE_NAME, TABLE_NAME, userMap, context);
  }

  @Override
  public User getUserById(String userId, RequestContext context) {
    Map<String, Object> user = getUserDetailsById(userId, context);
    if (MapUtils.isNotEmpty(user)) {
      return mapper.convertValue(user, User.class);
    }
    return null;
  }

  @Override
  public Map<String, Object> getUserDetailsById(String userId, RequestContext context) {
    Response response =
        cassandraOperation.getRecordById(Util.KEY_SPACE_NAME, TABLE_NAME, userId, context);
    List<Map<String, Object>> responseList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (CollectionUtils.isNotEmpty(responseList)) {
      return responseList.get(0);
    }
    return null;
  }

  @Override
  public Response getUserPropertiesById(
      List<String> userIds, List<String> properties, RequestContext context) {
    return cassandraOperation.getPropertiesValueById(
        Util.KEY_SPACE_NAME, TABLE_NAME, userIds, properties, context);
  }
}
