package org.sunbird.sso.impl;

import static java.util.Arrays.asList;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.RSATokenVerifier;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.exception.ResponseMessage;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.sso.KeyCloakConnectionProvider;
import org.sunbird.sso.SSOManager;
import org.sunbird.util.ProjectUtil;

/**
 * Single sign out service implementation with Key Cloak.
 *
 * @author Manzarul
 */
public class KeyCloakServiceImpl implements SSOManager {
  private final LoggerUtil logger = new LoggerUtil(KeyCloakServiceImpl.class);
  private final Keycloak keycloak = KeyCloakConnectionProvider.getConnection();

  private static PublicKey SSO_PUBLIC_KEY = null;

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
   * This method will generate Public key form keycloak realm publickey String
   *
   * @param publicKeyString String
   * @return PublicKey
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
      logger.error(context, "updatePassword: Exception occurred: ", e);
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
      System.out.println("KeyCloakServiceImpl::removePII:: userId:: " + fedUserId);
      userResource.update(user);
      return true;
    } catch (Exception e) {
      logger.error(context, "removePII: Exception occurred: ", e);
    }
    return false;
  }

  /**
   * Method to remove the user on basis of user id.
   *
   * @param request Map
   * @param context RequestContext
   * @return boolean true if success otherwise false .
   */
  @Override
  public String removeUser(Map<String, Object> request, RequestContext context) {
    Keycloak keycloak = KeyCloakConnectionProvider.getConnection();
    String userId = (String) request.get(JsonKey.USER_ID);
    logger.info("KeycloakServiceImpl:: removeUser:: userId:: " + userId);
    try {
      String fedUserId = getFederatedUserId(userId);
      logger.info("KeycloakServiceImpl:: removeUser:: fedUserId:: " + fedUserId);
      UserResource resource =
          keycloak.realm(KeyCloakConnectionProvider.SSO_REALM).users().get(fedUserId);
      if (null != (resource)) {
        logger.info("KeycloakServiceImpl:: removeUser:: resource:: " + resource.toRepresentation());
        resource.remove();
      }
    } catch (Exception ex) {
      logger.error(context, "Error occurred : ", ex);
      String exMsg =
          String.format(ResponseMessage.Message.INVALID_PARAMETER_VALUE, userId, JsonKey.USER_ID);
      ProjectCommonException.throwClientErrorException(ResponseCode.invalidParameterValue, exMsg);
    }
    return JsonKey.SUCCESS;
  }

  /**
   * Method to deactivate the user on basis of user id.
   *
   * @param request Map
   * @param context
   * @return boolean true if success otherwise false .
   */
  @Override
  public String deactivateUser(Map<String, Object> request, RequestContext context) {
    String userId = (String) request.get(JsonKey.USER_ID);
    makeUserActiveOrInactive(userId, false, context);
    return JsonKey.SUCCESS;
  }

  /**
   * Method to activate the user on basis of user id.
   *
   * @param request Map
   * @param context
   * @return boolean true if success otherwise false .
   */
  @Override
  public String activateUser(Map<String, Object> request, RequestContext context) {
    String userId = (String) request.get(JsonKey.USER_ID);
    makeUserActiveOrInactive(userId, true, context);
    return JsonKey.SUCCESS;
  }

  /**
   * This method will take userid and boolean status to update user status
   *
   * @param userId String
   * @param status boolean
   * @throws ProjectCommonException
   */
  private void makeUserActiveOrInactive(String userId, boolean status, RequestContext context) {
    try {
      String fedUserId = getFederatedUserId(userId);
      logger.info(context, "makeUserActiveOrInactive: federation id formed: " + fedUserId);
      validateUserId(fedUserId);
      logger.info(context, "makeUserActiveOrInactive: user validated: ");
      Keycloak keycloak = KeyCloakConnectionProvider.getConnection();

      logger.info(
          context,
          "makeUserActiveOrInactive: keycloak: "
              + keycloak.toString()
              + " || "
              + keycloak.serverInfo());
      UserResource resource =
          keycloak.realm(KeyCloakConnectionProvider.SSO_REALM).users().get(fedUserId);
      logger.info("makeUserActiveOrInactive: resource: " + resource.toString());
      UserRepresentation ur = resource.toRepresentation();
      logger.info("makeUserActiveOrInactive: ur: " + ur.isEnabled());
      ur.setEnabled(status);
      resource.update(ur);
    } catch (Exception e) {
      logger.info(
          "makeUserActiveOrInactive:error occurred while blocking or unblocking user: "
              + e.getCause()
              + " || "
              + e.getMessage());
      logger.error(
          context,
          "makeUserActiveOrInactive:error occurred while blocking or unblocking user: ",
          e);
      String exMsg =
          String.format(ResponseMessage.Message.INVALID_PARAMETER_VALUE, userId, JsonKey.USER_ID);
      ProjectCommonException.throwClientErrorException(ResponseCode.invalidParameterValue, exMsg);
    }
  }

  /**
   * This method will check userId value, if value is null or empty then it will throw
   * ProjectCommonException
   *
   * @param userId String
   * @throws ProjectCommonException
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
        logger.info(
            context,
            token.getId()
                + " "
                + token.issuedFor
                + " "
                + token.getProfile()
                + " "
                + token.getSubject()
                + " Active: "
                + token.isActive()
                + "  isExpired: "
                + token.isExpired()
                + " "
                + token.issuedNow().getExpiration());
        String tokenSubject = token.getSubject();
        if (StringUtils.isNotBlank(tokenSubject)) {
          int pos = tokenSubject.lastIndexOf(":");
          return tokenSubject.substring(pos + 1);
        }
        return token.getSubject();
      } else {
        logger.info(context, "verifyToken: SSO_PUBLIC_KEY is NULL.");
        throw new ProjectCommonException(
            ResponseCode.serverError,
            ResponseCode.serverError.getErrorMessage(),
            ResponseCode.SERVER_ERROR.getResponseCode());
      }
    } catch (Exception e) {
      logger.error(context, "verifyToken: Exception occurred: ", e);
      throw new ProjectCommonException(
          ResponseCode.unAuthorized,
          ResponseCode.unAuthorized.getErrorMessage(),
          ResponseCode.UNAUTHORIZED.getResponseCode());
    }
  }
}
