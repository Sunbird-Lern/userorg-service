package org.sunbird.keycloak.impl;

import static java.util.Arrays.asList;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.RSATokenVerifier;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.response.ResponseMessage;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.keycloak.KeyCloakConnectionProvider;
import org.sunbird.keycloak.SSOManager;
import org.sunbird.common.ProjectUtil;

/**
 * Implementation of SSOManager using Keycloak as the Identity Provider.
 * Handles user management operations like password updates, account lock/unlock,
 * PII removal, and token verification.
 */
public class KeyCloakServiceImpl implements SSOManager {
  private final LoggerUtil logger = new LoggerUtil(KeyCloakServiceImpl.class);
  private final Keycloak keycloak = KeyCloakConnectionProvider.getConnection();

  private static PublicKey SSO_PUBLIC_KEY = null;

  /**
   * Retrieves the SSO public key from the environment variable.
   *
   * @return The PublicKey used for token verification.
   */
  public PublicKey getPublicKey() {
    if (null == SSO_PUBLIC_KEY) {
      SSO_PUBLIC_KEY = toPublicKey(System.getenv(JsonKey.SSO_PUBLIC_KEY));
    }
    return SSO_PUBLIC_KEY;
  }

  @Override
  public String verifyToken(String accessToken, RequestContext context) {
    return verifyToken(accessToken, null, context);
  }

  /**
   * Converts a Base64 encoded public key string to a PublicKey object.
   *
   * @param publicKeyString The Base64 encoded public key string.
   * @return The PublicKey object, or null if conversion fails.
   */
  private PublicKey toPublicKey(String publicKeyString) {
    try {
      byte[] publicBytes = Base64.getDecoder().decode(publicKeyString);
      X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      return keyFactory.generatePublic(keySpec);
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public boolean updatePassword(String userId, String password, RequestContext context) {
    try {
      String fedUserId = getFederatedUserId(userId);
      UserResource ur = keycloak.realm(KeyCloakConnectionProvider.SSO_REALM).users().get(fedUserId);
      CredentialRepresentation cr = new CredentialRepresentation();
      cr.setType(CredentialRepresentation.PASSWORD);
      cr.setValue(password);
      ur.resetPassword(cr);
      return true;
    } catch (Exception e) {
      logger.error(context, "KeyCloakServiceImpl:updatePassword: Exception occurred: ", e);
    }
    return false;
  }

  @Override
  public boolean removePII(String userId, RequestContext context) {
    try {
      String fedUserId = getFederatedUserId(userId);
      UserResource userResource =
          keycloak.realm(KeyCloakConnectionProvider.SSO_REALM).users().get(fedUserId);
      UserRepresentation user = userResource.toRepresentation();
      user.setEmailVerified(false);
      user.setEmail("");
      user.setFirstName("");
      user.setLastName("");
      user.setEnabled(false);
      logger.info(context, "KeyCloakServiceImpl:removePII: Removing PII for userId: " + fedUserId);
      userResource.update(user);
      List userSessions = userResource.getUserSessions();
      for (Object userSession : userSessions)
        userSessions.remove(userSession);
      return true;
    } catch (Exception e) {
      logger.error(context, "KeyCloakServiceImpl:removePII: Exception occurred: ", e);
    }
    return false;
  }

  /**
   * Removes a user from Keycloak based on the provided request map.
   *
   * @param request Map containing user details, specifically {@link JsonKey#USER_ID}.
   * @param context The request context.
   * @return {@link JsonKey#SUCCESS} on success.
   * @throws ProjectCommonException If the user ID parameter is invalid.
   */
  @Override
  public String removeUser(Map<String, Object> request, RequestContext context) {
    Keycloak keycloak = KeyCloakConnectionProvider.getConnection();
    String userId = (String) request.get(JsonKey.USER_ID);
    logger.info(context, "KeyCloakServiceImpl:removeUser: Removing user with userId: " + userId);
    try {
      String fedUserId = getFederatedUserId(userId);
      logger.info(context, "KeyCloakServiceImpl:removeUser: Federated userId: " + fedUserId);
      UserResource resource =
          keycloak.realm(KeyCloakConnectionProvider.SSO_REALM).users().get(fedUserId);
      if (null != (resource)) {
        logger.info(context, "KeyCloakServiceImpl:removeUser: Resource found: " + resource.toRepresentation());
        resource.remove();
      }
    } catch (Exception ex) {
      logger.error(context, "KeyCloakServiceImpl:removeUser: Error occurred: ", ex);
      String exMsg =
          String.format(ResponseMessage.Message.INVALID_PARAMETER_VALUE, userId, JsonKey.USER_ID);
      ProjectCommonException.throwClientErrorException(ResponseCode.invalidParameterValue, exMsg);
    }
    return JsonKey.SUCCESS;
  }

  /**
   * Deactivates a user (sets enabled=false).
   *
   * @param request Map containing user details.
   * @param context The request context.
   * @return {@link JsonKey#SUCCESS} on success.
   */
  @Override
  public String deactivateUser(Map<String, Object> request, RequestContext context) {
    String userId = (String) request.get(JsonKey.USER_ID);
    makeUserActiveOrInactive(userId, false, context);
    return JsonKey.SUCCESS;
  }

  /**
   * Activates a user (sets enabled=true).
   *
   * @param request Map containing user details.
   * @param context The request context.
   * @return {@link JsonKey#SUCCESS} on success.
   */
  @Override
  public String activateUser(Map<String, Object> request, RequestContext context) {
    String userId = (String) request.get(JsonKey.USER_ID);
    makeUserActiveOrInactive(userId, true, context);
    return JsonKey.SUCCESS;
  }

  /**
   * Helper method to update the user's enabled status in Keycloak.
   *
   * @param userId The user ID.
   * @param status The target status (true for active, false for inactive).
   * @param context The request context.
   * @throws ProjectCommonException If the user ID is invalid/missing.
   */
  private void makeUserActiveOrInactive(String userId, boolean status, RequestContext context) {
    try {
      String fedUserId = getFederatedUserId(userId);
      logger.info(context, "KeyCloakServiceImpl:makeUserActiveOrInactive: Federated ID: " + fedUserId);
      validateUserId(fedUserId);
      logger.info(context, "KeyCloakServiceImpl:makeUserActiveOrInactive: User validated.");
      Keycloak keycloak = KeyCloakConnectionProvider.getConnection();

      logger.info(
          context,
          "KeyCloakServiceImpl:makeUserActiveOrInactive: Keycloak instance info: "
              + keycloak.toString()
              + " || "
              + keycloak.serverInfo());
      UserResource resource =
          keycloak.realm(KeyCloakConnectionProvider.SSO_REALM).users().get(fedUserId);
      logger.info(context, "KeyCloakServiceImpl:makeUserActiveOrInactive: Resource: " + resource.toString());
      UserRepresentation ur = resource.toRepresentation();
      logger.info(context, "KeyCloakServiceImpl:makeUserActiveOrInactive: Current status: " + ur.isEnabled());
      ur.setEnabled(status);
      resource.update(ur);
    } catch (Exception e) {
      logger.error(
          context,
          "KeyCloakServiceImpl:makeUserActiveOrInactive: Error occurred while updating user status: " + e.getMessage(),
          e);
      String exMsg =
          String.format(ResponseMessage.Message.INVALID_PARAMETER_VALUE, userId, JsonKey.USER_ID);
      ProjectCommonException.throwClientErrorException(ResponseCode.invalidParameterValue, exMsg);
    }
  }

  /**
   * Validates if the user ID is present.
   *
   * @param userId The user ID string.
   * @throws ProjectCommonException If the user ID is blank.
   */
  private void validateUserId(String userId) {
    if (StringUtils.isBlank(userId)) {
      String exMsg =
          String.format(ResponseMessage.Message.INVALID_PARAMETER_VALUE, userId, JsonKey.USER_ID);
      ProjectCommonException.throwClientErrorException(ResponseCode.invalidParameterValue, exMsg);
    }
  }

  private String getFederatedUserId(String userId) {
    return String.join(
        ":",
        "f",
        ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYCLOAK_USER_FEDERATION_PROVIDER_ID),
        userId);
  }

  @Override
  public void setRequiredAction(String userId, String requiredAction) {
    String fedUserId = getFederatedUserId(userId);
    UserResource resource =
        keycloak.realm(KeyCloakConnectionProvider.SSO_REALM).users().get(fedUserId);

    UserRepresentation userRepresentation = resource.toRepresentation();
    userRepresentation.setRequiredActions(asList(requiredAction));
    resource.update(userRepresentation);
  }

  @Override
  public String verifyToken(String accessToken, String url, RequestContext context) {

    try {
      PublicKey publicKey = getPublicKey();
      if (publicKey != null) {
        String ssoUrl = (url != null ? url : KeyCloakConnectionProvider.SSO_URL);
        AccessToken token =
            RSATokenVerifier.verifyToken(
                accessToken,
                publicKey,
                ssoUrl + "realms/" + KeyCloakConnectionProvider.SSO_REALM,
                true,
                true);
        logger.debug(
            context,
            "KeyCloakServiceImpl:verifyToken: Token Details - ID: " + token.getId()
                + ", IssuedFor: " + token.issuedFor
                + ", Profile: " + token.getProfile()
                + ", Subject: " + token.getSubject()
                + ", Active: " + token.isActive()
                + ", IsExpired: " + token.isExpired()
                + ", Expiration: " + token.issuedNow().getExpiration());
        
        String tokenSubject = token.getSubject();
        if (StringUtils.isNotBlank(tokenSubject)) {
          int pos = tokenSubject.lastIndexOf(":");
          return tokenSubject.substring(pos + 1);
        }
        return token.getSubject();
      } else {
        logger.info(context, "KeyCloakServiceImpl:verifyToken: SSO_PUBLIC_KEY is NULL.");
        throw new ProjectCommonException(
            ResponseCode.serverError,
            ResponseCode.serverError.getErrorMessage(),
            ResponseCode.SERVER_ERROR.getResponseCode());
      }
    } catch (Exception e) {
      logger.error(context, "KeyCloakServiceImpl:verifyToken: Exception occurred during token verification: ", e);
      throw new ProjectCommonException(
          ResponseCode.unAuthorized,
          ResponseCode.unAuthorized.getErrorMessage(),
          ResponseCode.UNAUTHORIZED.getResponseCode());
    }
  }
}
