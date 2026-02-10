package org.sunbird.keycloak;

import java.util.Map;
import org.sunbird.request.RequestContext;

/**
 * Interface defining operations for Single Sign-On (SSO) management.
 * Handles user token verification, password updates, and user lifecycle management in Keycloak.
 */
public interface SSOManager {

  /**
   * Verifies the user access token and returns the user ID if valid.
   * Throws ProjectCommonException with 401 Unauthorized if the token is invalid.
   *
   * @param token The JWT access token to verify.
   * @param context The request context.
   * @return The user ID extracted from the token.
   */
  String verifyToken(String token, RequestContext context);

  /**
   * Updates the user's password in the SSO provider (Keycloak).
   *
   * @param userId The ID of the user.
   * @param password The new password.
   * @param context The request context.
   * @return true if the password update was successful, false otherwise.
   */
  boolean updatePassword(String userId, String password, RequestContext context);

  /**
   * Removes Personally Identifiable Information (PII) for a user.
   *
   * @param userId The ID of the user.
   * @param context The request context.
   * @return true if the operation was successful, false otherwise.
   */
  boolean removePII(String userId, RequestContext context);

  /**
   * Removes a user from Keycloak based on the provided request details (typically userId).
   *
   * @param request A map containing user identification details.
   * @param context The request context.
   * @return The result of the removal operation (e.g., success message or status).
   */
  String removeUser(Map<String, Object> request, RequestContext context);

  /**
   * Deactivates a user in Keycloak (soft delete).
   *
   * @param request A map containing user identification details.
   * @param context The request context.
   * @return The result of the deactivation operation.
   */
  String deactivateUser(Map<String, Object> request, RequestContext context);

  /**
   * Activates a user in Keycloak.
   *
   * @param request A map containing user identification details.
   * @param context The request context.
   * @return The result of the activation operation.
   */
  String activateUser(Map<String, Object> request, RequestContext context);

  /**
   * Sets a required action for a user in Keycloak (e.g., Update Password, Verify Email).
   *
   * @param userId The ID of the user.
   * @param requiredAction The action to valid.
   */
  void setRequiredAction(String userId, String requiredAction);

  /**
   * Verifies the user access token against a specific URL and returns the user ID.
   * Throws ProjectCommonException with 401 Unauthorized if the token is invalid.
   *
   * @param token The JWT access token to verify.
   * @param url The URL to validate the token against.
   * @param context The request context.
   * @return The user ID extracted from the token.
   */
  String verifyToken(String token, String url, RequestContext context);
}
