package org.sunbird.user.dao.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.models.user.User;
import org.sunbird.user.dao.UserDao;
import scala.concurrent.Future;

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
  public String createUser(User user) {
    Map<String, Object> map = mapper.convertValue(user, Map.class);
    cassandraOperation.insertRecord(Util.KEY_SPACE_NAME, TABLE_NAME, map);
    return (String) map.get(JsonKey.ID);
  }

  @Override
  public Response updateUser(User user) {
    Map<String, Object> map = mapper.convertValue(user, Map.class);
    return cassandraOperation.updateRecord(Util.KEY_SPACE_NAME, TABLE_NAME, map);
  }

  @Override
  public Response updateUser(Map<String, Object> userMap) {
    return cassandraOperation.updateRecord(Util.KEY_SPACE_NAME, TABLE_NAME, userMap);
  }

  @Override
  public List<User> searchUser(Map<String, Object> searchQueryMap) {
    List<User> userList = new ArrayList<>();
    Map<String, Object> searchRequestMap = new HashMap<>();
    SearchDTO searchDto = Util.createSearchDto(searchRequestMap);
    searchRequestMap.put(JsonKey.FILTERS, searchQueryMap);
    String type = ProjectUtil.EsType.user.getTypeName();
    Future<Map<String, Object>> resultF = esUtil.search(searchDto, type);
    Map<String, Object> result =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);

    if (MapUtils.isNotEmpty(result)) {
      List<Map<String, Object>> searchResult =
          (List<Map<String, Object>>) result.get(JsonKey.CONTENT);
      if (CollectionUtils.isNotEmpty(searchResult)) {
        userList =
            searchResult
                .stream()
                .map(s -> mapper.convertValue(s, User.class))
                .collect(Collectors.toList());
      }
    }
    return userList;
  }

  @Override
  public User getUserById(String userId) {
    Response response = cassandraOperation.getRecordById(Util.KEY_SPACE_NAME, TABLE_NAME, userId);
    List<Map<String, Object>> responseList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (CollectionUtils.isNotEmpty(responseList)) {
      Map<String, Object> userMap = responseList.get(0);
      return mapper.convertValue(userMap, User.class);
    }
    return null;
  }

  @Override
  public List<User> getUsersByProperties(Map<String, Object> propertyMap) {
    List<User> userList = new ArrayList<>();
    Response response =
        cassandraOperation.getRecordsByProperties(Util.KEY_SPACE_NAME, TABLE_NAME, propertyMap);
    List<Map<String, Object>> responseList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (CollectionUtils.isNotEmpty(responseList)) {
      userList =
          responseList
              .stream()
              .map(s -> mapper.convertValue(s, User.class))
              .collect(Collectors.toList());
    }
    return userList;
  }
}
