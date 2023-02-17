package org.sunbird.auth.verifier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Map;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.common.util.Time;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;

public class AccessTokenValidator {
  private static final LoggerUtil logger = new LoggerUtil(AccessTokenValidator.class);
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String sso_url = System.getenv(JsonKey.SUNBIRD_SSO_URL);
  private static final String realm = System.getenv(JsonKey.SUNBIRD_SSO_RELAM);

  private static Map<String, Object> validateToken(String token, Map<String, Object> requestContext)
      throws JsonProcessingException {
    String[] tokenElements = token.split("\\.");
    String header = tokenElements[0];
    String body = tokenElements[1];
    String signature = tokenElements[2];
    String payLoad = header + JsonKey.DOT_SEPARATOR + body;
    Map<Object, Object> headerData =
        mapper.readValue(new String(decodeFromBase64(header)), Map.class);
    String keyId = headerData.get("kid").toString();
    boolean isValid =
        CryptoUtil.verifyRSASign(
            payLoad,
            decodeFromBase64(signature),
            KeyManager.getPublicKey(keyId).getPublicKey(),
            JsonKey.SHA_256_WITH_RSA,
            requestContext);
    if (isValid) {
      Map<String, Object> tokenBody =
          mapper.readValue(new String(decodeFromBase64(body)), Map.class);
      boolean isExp = isExpired((Integer) tokenBody.get("exp"));
      if (isExp) {
        logger.info("Token is expired " + token + ", request context data :" + requestContext);
        return Collections.EMPTY_MAP;
      }
      return tokenBody;
    }
    return Collections.EMPTY_MAP;
  }

  /**
   * managedtoken is validated and requestedByUserID, requestedForUserID values are validated
   * aganist the managedEncToken
   *
   * @param managedEncToken
   * @param requestedByUserId
   * @param requestedForUserId
   * @return
   */
  public static String verifyManagedUserToken(
      String managedEncToken,
      String requestedByUserId,
      String requestedForUserId,
      Map<String, Object> requestContext) {
    String managedFor = JsonKey.UNAUTHORIZED;
    try {
      Map<String, Object> payload = validateToken(managedEncToken, requestContext);
      if (MapUtils.isNotEmpty(payload)) {
        String parentId = (String) payload.get(JsonKey.PARENT_ID);
        String muaId = (String) payload.get(JsonKey.SUB);
        logger.info(
            "AccessTokenValidator: parent uuid: "
                + parentId
                + " managedBy uuid: "
                + muaId
                + " requestedByUserID: "
                + requestedByUserId
                + " requestedForUserId: "
                + requestedForUserId
                + " request context data : "
                + requestContext);
        boolean isValid =
            parentId.equalsIgnoreCase(requestedByUserId)
                && muaId.equalsIgnoreCase(requestedForUserId);
        if (isValid) {
          managedFor = muaId;
        }
      }
    } catch (Exception ex) {
      logger.error(
          "Exception in verifyManagedUserToken: Token : "
              + managedEncToken
              + ", request context data :"
              + requestContext,
          ex);
    }
    return managedFor;
  }

  public static String verifyUserToken(String token, Map<String, Object> requestContext) {
    String userId = JsonKey.UNAUTHORIZED;
    try {
      Map<String, Object> payload = validateToken(token, requestContext);
      logger.debug(
          "user org access token validateToken() :"
              + payload.toString()
              + ", request context data : "
              + requestContext);
      if (MapUtils.isNotEmpty(payload) && checkIss((String) payload.get("iss"))) {
        userId = (String) payload.get(JsonKey.SUB);
        if (StringUtils.isNotBlank(userId)) {
          int pos = userId.lastIndexOf(":");
          userId = userId.substring(pos + 1);
        }
      }
    } catch (Exception ex) {
      logger.error(
          "Exception in verifyUserAccessToken: Token : "
              + token
              + ", request context data : "
              + requestContext,
          ex);
    }
    if (JsonKey.UNAUTHORIZED.equalsIgnoreCase(userId)) {
      logger.info(
          "verifyUserAccessToken: Invalid User Token: "
              + token
              + ", request context data : "
              + requestContext);
    }
    return userId;
  }

  public static String verifySourceUserToken(
      String token, String url, Map<String, Object> requestContext) {
    String userId = JsonKey.UNAUTHORIZED;
    try {
      Map<String, Object> payload = validateToken(token, requestContext);
      logger.debug(
          "user org source access token validateToken() :"
              + payload.toString()
              + ", request context data : "
              + requestContext);
      if (MapUtils.isNotEmpty(payload) && checkSourceIss((String) payload.get("iss"), url)) {
        userId = (String) payload.get(JsonKey.SUB);
        if (StringUtils.isNotBlank(userId)) {
          int pos = userId.lastIndexOf(":");
          userId = userId.substring(pos + 1);
        }
      }
    } catch (Exception ex) {
      logger.error(
          "Exception in verifySourceUserToken: Token : "
              + token
              + ", request context data : "
              + requestContext,
          ex);
    }
    if (JsonKey.UNAUTHORIZED.equalsIgnoreCase(userId)) {
      logger.info(
          "verifySourceUserToken: Invalid source user Token: "
              + token
              + ", request context data : "
              + requestContext);
    }
    return userId;
  }

  private static boolean checkIss(String iss) {
    String realmUrl = sso_url + "realms/" + realm;
    return (realmUrl.equalsIgnoreCase(iss));
  }

  private static boolean checkSourceIss(String iss, String url) {
    String ssoUrl = (url != null ? url : sso_url);
    String realmUrl = ssoUrl + "realms/" + realm;
    return (realmUrl.equalsIgnoreCase(iss));
  }

  private static boolean isExpired(Integer expiration) {
    return (Time.currentTime() > expiration);
  }

  private static byte[] decodeFromBase64(String data) {
    return Base64Util.decode(data, 11);
  }
}
