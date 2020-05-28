/** */
package org.sunbird.services.sso;

import java.util.Map;

/** @author Manzarul This interface will handle all call related to single sign out. */
public interface SSOManager {

  /**
   * This method will verify user access token and provide userId if token is valid. in case of
   * invalid access token it will throw ProjectCommon exception with 401.
   *
   * @param token String JWT access token
   * @return String
   */
  String verifyToken(String token);

  /** Update password in SSO server (keycloak). */
  boolean updatePassword(String userId, String password);

  /**
   * Method to update user account in keycloak on basis of userId.
   *
   * @param request
   * @return
   */
  String updateUser(Map<String, Object> request);

  /**
   * Method to remove user from keycloak account on basis of userId .
   *
   * @param request
   * @return
   */
  String removeUser(Map<String, Object> request);

  /**
   * This method will check email is verified by user or not.
   *
   * @param userId String
   * @return boolean
   */
  boolean isEmailVerified(String userId);

  /**
   * Method to deactivate user from keycloak , it is like soft delete .
   *
   * @param request
   * @return
   */
  String deactivateUser(Map<String, Object> request);

  /**
   * Method to activate user from keycloak , it is like soft delete .
   *
   * @param request
   * @return
   */
  String activateUser(Map<String, Object> request);

  /**
   * This method will read user last login time from key claok.
   *
   * @param userId String
   * @return String (as epoch value or null)
   */
  String getLastLoginTime(String userId);

  /**
   * This method will add user current login time to keycloak.
   *
   * @param userId String
   * @return boolean
   */
  boolean addUserLoginTime(String userId);

  /**
   * this method will set emailVerified flag of keycloak as false.
   *
   * @param userId
   */
  String setEmailVerifiedAsFalse(String userId);

  /**
   * This method will set email verified flag on keycloak.
   *
   * @param userId String
   * @param flag boolean (true/false)
   */
  void setEmailVerifiedUpdatedFlag(String userId, String flag);

  /**
   * This method will provide the user already set attribute under keycloak.
   *
   * @param userId String
   * @return String
   */
  String getEmailVerifiedUpdatedFlag(String userId);

  /**
   * This method will do the data sync from cassandra db to keyclaok.
   *
   * @param request Map<String, Object>
   * @return String
   */
  String syncUserData(Map<String, Object> request);

  /**
   * This method will do the user password update.
   *
   * @param userId String
   * @param password String
   * @return boolean true/false
   */
  boolean doPasswordUpdate(String userId, String password);

  String setEmailVerifiedTrue(String userId);

  void setRequiredAction(String userId, String requiredAction);

  String getUsernameById(String userId);
  /**
   * This method will verify user access token and provide userId if token is valid. in case of
   * invalid access token it will throw ProjectCommon exception with 401.
   *
   * @param token String JWT access token
   * @param url token will be validated against this url
   * @return String
   */
  String verifyToken(String token, String url);
}
