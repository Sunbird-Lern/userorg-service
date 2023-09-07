package org.sunbird.dao.user;

import java.util.Map;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

/**
 * This interface will have all methods required for user service api.
 *
 * @author Amit Kumar
 */
public interface UserOwnershipTransferDao {

  /**
   * This method will create user and return userId as success response or throw
   * ProjectCommonException.
   *
   * @param user User Details.
   * @param context
   * @return User ID.
   */
  Response createUserOwnershipTransfer(Map<String, Object> user, RequestContext context);

  /**
   * This method will update existing user info even if some fields need to be nullify or throw
   * ProjectCommonException.
   *
   * @param userMap User Details.
   * @param context
   */
  Response updateUserOwnershipTransfer(Map<String, Object> userMap, RequestContext context);

  /**
   * This method will user based on userId and return user if found else throw
   * ProjectCommonException.
   *
   * @param userId User id.
   * @param context
   * @return User User Details.
   */
  Map<String, Object> getUserOwnershipTransferDetailsById(
      String userId, String organisationId, RequestContext context);
}
