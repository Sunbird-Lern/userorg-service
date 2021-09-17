package org.sunbird.dao.user;

import java.util.List;
import java.util.Map;
import org.sunbird.dto.SearchDTO;
import org.sunbird.model.user.User;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

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
  Response createUser(Map<String, Object> user, RequestContext context);

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
   * This method will user based on userId and return user if found else throw
   * ProjectCommonException.
   *
   * @param userId User id.
   * @param context
   * @return User User Details.
   */
  User getUserById(String userId, RequestContext context);

  /**
   * This method will user based on userId and return user if found else throw
   * ProjectCommonException.
   *
   * @param userId User id.
   * @param context
   * @return User User Details.
   */
  Map<String, Object> getUserDetailsById(String userId, RequestContext context);

  /**
   * This method will user based on userId and return user if found else throw
   * ProjectCommonException.
   *
   * @param userId User id.
   * @param properties list of properties
   * @param context
   * @return response
   */
  Response getUserPropertiesById(
      List<String> userId, List<String> properties, RequestContext context);

  Map<String, Object> search(SearchDTO searchDTO, RequestContext context);

  Map<String, Object> getEsUserById(String userId, RequestContext context);

  boolean updateUserDataToES(String identifier, Map<String, Object> data, RequestContext context);

  String saveUserToES(String identifier, Map<String, Object> data, RequestContext context);
}
