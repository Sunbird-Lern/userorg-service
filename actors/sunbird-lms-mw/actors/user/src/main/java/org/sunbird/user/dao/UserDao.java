package org.sunbird.user.dao;

import java.util.List;
import java.util.Map;
import org.sunbird.common.models.response.Response;
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
   * @return User ID.
   */
  String createUser(User user);

  /**
   * This method will update existing user info or throw ProjectCommonException.
   *
   * @param user User Details.
   */
  Response updateUser(User user);

  /**
   * This method will update existing user info even if some fields need to be nullify or throw
   * ProjectCommonException.
   *
   * @param userMap User Details.
   */
  Response updateUser(Map<String, Object> userMap);

  /**
   * This method will search user from ES and return list of user details matching filter criteria.
   *
   * @param searchQueryMap search query for ES as a Map.
   * @return List<User> List of user.
   */
  List<User> searchUser(Map<String, Object> searchQueryMap);

  /**
   * This method will user based on userId and return user if found else throw
   * ProjectCommonException.
   *
   * @param userId User id.
   * @return User User Details.
   */
  User getUserById(String userId);

  /**
   * @param propertyMap Map of user property and its value
   * @return List<User> List of user.
   */
  List<User> getUsersByProperties(Map<String, Object> propertyMap);
}
