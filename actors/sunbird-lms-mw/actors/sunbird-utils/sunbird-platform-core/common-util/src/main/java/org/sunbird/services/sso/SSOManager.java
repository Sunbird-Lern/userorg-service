/** */
package org.sunbird.services.sso;

import java.util.Map;
import org.sunbird.common.request.RequestContext;

/** @author Manzarul This interface will handle all call related to single sign out. */
public interface SSOManager {

  /**
   * This method will verify user access token and provide userId if token is valid. in case of
   * invalid access token it will throw ProjectCommon exception with 401.
   *
   * @param token String JWT access token
   * @param context
   * @return String
   */
  String verifyToken(String token, RequestContext context);

  /** Update password in SSO server (keycloak). */
  boolean updatePassword(String userId, String password, RequestContext context);

  /**
   * Method to remove user from keycloak account on basis of userId .
   *
   * @param request
   * @param context
   * @return
   */
  String removeUser(Map<String, Object> request, RequestContext context);

  /**
   * Method to deactivate user from keycloak , it is like soft delete .
   *
   * @param request
   * @param context
   * @return
   */
  String deactivateUser(Map<String, Object> request, RequestContext context);

  /**
   * Method to activate user from keycloak , it is like soft delete .
   *
   * @param request
   * @param context
   * @return
   */
  String activateUser(Map<String, Object> request, RequestContext context);

  void setRequiredAction(String userId, String requiredAction);

  /**
   * This method will verify user access token and provide userId if token is valid. in case of
   * invalid access token it will throw ProjectCommon exception with 401.
   *
   * @param token String JWT access token
   * @param url token will be validated against this url
   * @param context
   * @return String
   */
  String verifyToken(String token, String url, RequestContext context);
}
