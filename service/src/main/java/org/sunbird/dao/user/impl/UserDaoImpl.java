package org.sunbird.dao.user.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.dao.user.UserDao;
import org.sunbird.dto.SearchDTO;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.Util;
import org.sunbird.model.user.User;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import scala.concurrent.Future;

/**
 * Implementation class of UserDao interface.
 *
 * @author Amit Kumar
 */
public class UserDaoImpl implements UserDao {

  private LoggerUtil logger = new LoggerUtil(UserDaoImpl.class);
  private static final String TABLE_NAME = "user";
  private ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);
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

  @Override
  public Map<String, Object> search(SearchDTO searchDTO, RequestContext context) {
    Future<Map<String, Object>> esResultF =
      esUtil.search(
        searchDTO,
        ProjectUtil.EsType.user.getTypeName(),
        context);
    return  (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(esResultF);
  }

  @Override
  public Map<String, Object> getEsUserById(String userId, RequestContext context) {
    Future<Map<String, Object>> esResultF =
      esUtil.getDataByIdentifier(ProjectUtil.EsType.user.getTypeName(), userId, context);
    Map<String, Object> esResult =
      (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(esResultF);
    if (esResult == null || esResult.size() == 0) {
      throw new ProjectCommonException(
        ResponseCode.userNotFound.getErrorCode(),
        ResponseCode.userNotFound.getErrorMessage(),
        ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }
    return esResult;
  }

  @Override
  public boolean updateUserDataToES(String identifier, Map<String, Object> data, RequestContext context) {
    Future<Boolean> responseF = esService.update(ProjectUtil.EsType.user.getTypeName(), identifier, data, context);
    if ((boolean) ElasticSearchHelper.getResponseFromFuture(responseF)) {
      return true;
    }
    logger.info(context, "UserRoleDaoImpl:updateUserRoleToES:unable to save the user role data to ES with identifier " + identifier);
    return false;
  }
}
