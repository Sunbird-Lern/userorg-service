package org.sunbird.keycloak;

import java.util.Map;
import org.sunbird.request.RequestContext;

/**
 * Interface defining operations for Single Sign-On (SSO) management.
 */
public interface SSOManager {

  /**
   * Verifies the user access token and returns the userId if valid.
   * Throws ProjectCommonException with 401 if the token is invalid.
   *
   * @param token   String JWT access token.
   * @param context The request context.
   * @return The user ID associated with the token.
   */
  String verifyToken(String token, RequestContext context);

  /**
   * Updates the password in the SSO server (Keycloak).
   *
   * @param userId   The user ID.
   * @param password The new password.
   * @param context  The request context.
   * @return True if the password update was successful, false otherwise.
   */
  boolean updatePassword(String userId, String password, RequestContext context);

  /**
   * Cleans up User PII (Personally Identifiable Information).
   *
   * @param userId  The user ID.
   * @param context The request context.
   * @return True if cleanup was successful, false otherwise.
   */
  boolean removePII(String userId, RequestContext context);

  /**
   * Removes a user from the Keycloak account based on userId.
   *
   * @param request Map containing request details.
   * @param context The request context.
   * @return A status message or identifier related to the removal.
   */
  String removeUser(Map<String, Object> request, RequestContext context);

  /**
   * Deactivates a user in Keycloak (soft delete).
   *
   * @param request Map containing request details.
   * @param context The request context.
   * @return A status message or identifier related to the deactivation.
   */
  String deactivateUser(Map<String, Object> request, RequestContext context);

  /**
   * Activates a user in Keycloak.
   *
   * @param request Map containing request details.
   * @param context The request context.
   * @return A status message or identifier related to the activation.
   */
  String activateUser(Map<String, Object> request, RequestContext context);

  /**
   * Sets a required action for a user (e.g., UPDATE_PASSWORD, VERIFY_EMAIL).
   *
   * @param userId         The user ID.
   * @param requiredAction The action to be required.
   */
  void setRequiredAction(String userId, String requiredAction);

  /**
   * Verifies the user access token against a specific URL and returns the userId if valid.
   * Throws ProjectCommonException with 401 if the token is invalid.
   *
   * @param token   String JWT access token.
   * @param url     The URL to validate the token against.
   * @param context The request context.
   * @return The user ID associated with the token.
   */
  String verifyToken(String token, String url, RequestContext context);
}
