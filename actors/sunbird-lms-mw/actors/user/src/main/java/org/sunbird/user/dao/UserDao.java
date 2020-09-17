package org.sunbird.user.dao;

import java.util.List;
import java.util.Map;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.request.RequestContext;
import org.sunbird.models.user.User;

/**
 * This interface will have all methods required for user service api.
 *
 * @author Amit Kumar
 */
public interface UserDao {

  /**
   * This method will create user and return userId as success response or throw
   * ProjectCommonException.
   *
   * @param user User Details.
   * @param context
   * @return User ID.
   */
  String createUser(User user, RequestContext context);

  /**
   * This method will update existing user info or throw ProjectCommonException.
   *
   * @param user User Details.
   * @param context
   */
  Response updateUser(User user, RequestContext context);

  /**
   * This method will update existing user info even if some fields need to be nullify or throw
   * ProjectCommonException.
   *
   * @param userMap User Details.
   * @param context
   */
  Response updateUser(Map<String, Object> userMap, RequestContext context);

  /**
   * This method will search user from ES and return list of user details matching filter criteria.
   *
   * @param searchQueryMap search query for ES as a Map.
   * @param context
   * @return List<User> List of user.
   */
  List<User> searchUser(Map<String, Object> searchQueryMap, RequestContext context);

  /**
   * This method will user based on userId and return user if found else throw
   * ProjectCommonException.
   *
   * @param userId User id.
   * @param context
   * @return User User Details.
   */
  User getUserById(String userId, RequestContext context);
}
