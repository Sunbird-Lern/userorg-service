package org.sunbird.auth.verifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.common.util.Time;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.KeyCloakConnectionProvider;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;

public class AccessTokenValidator {

  private static ObjectMapper mapper = new ObjectMapper();

  /**
   * managedtoken is validated and requestedByUserID, requestedForUserID values are validated
   * aganist the managedEncToken
   *
   * @param managedEncToken
   * @param requestedByUserId
   * @param requestedForUserId
   * @return
   */
  public static String verify(
      String managedEncToken, String requestedByUserId, String requestedForUserId) {
    boolean isValid = false;
    String managedFor = JsonKey.UNAUTHORIZED;
    try {
      String[] tokenElements = managedEncToken.split("\\.");
      String header = tokenElements[0];
      String body = tokenElements[1];
      String signature = tokenElements[2];
      String payLoad = header + JsonKey.DOT_SEPARATOR + body;
      Map<Object, Object> headerData =
          mapper.readValue(new String(decodeFromBase64(header)), Map.class);
      String keyId = headerData.get("kid").toString();
      ProjectLogger.log("AccessTokenValidator:verify: keyId: " + keyId, LoggerEnum.INFO.name());
      Map<String, String> tokenBody =
          mapper.readValue(new String(decodeFromBase64(body)), Map.class);
      String parentId = tokenBody.get(JsonKey.PARENT_ID);
      String muaId = tokenBody.get(JsonKey.SUB);
      ProjectLogger.log(
          "AccessTokenValidator: parent uuid: "
              + parentId
              + " managedBy uuid: "
              + muaId
              + " requestedByUserID: "
              + requestedByUserId
              + " requestedForUserId: "
              + requestedForUserId,
          LoggerEnum.INFO.name());
      ProjectLogger.log(
          "AccessTokenValidator: key modified value: " + keyId, LoggerEnum.INFO.name());
      isValid =
          CryptoUtil.verifyRSASign(
              payLoad,
              decodeFromBase64(signature),
              KeyManager.getPublicKey(keyId).getPublicKey(),
              JsonKey.SHA_256_WITH_RSA);
      isValid &=
          parentId.equalsIgnoreCase(requestedByUserId)
              && muaId.equalsIgnoreCase(requestedForUserId);
      if (isValid) {
        managedFor = muaId;
      }
    } catch (Exception ex) {
      ProjectLogger.log("Exception in AccessTokenValidator: verify ", LoggerEnum.ERROR);
      ex.printStackTrace();
    }

    return managedFor;
  }

  public static String verifyToken(String token) {
    String userId = JsonKey.UNAUTHORIZED;
    try {
      String[] tokenElements = token.split("\\.");
      String header = tokenElements[0];
      String body = tokenElements[1];
      String signature = tokenElements[2];
      String payLoad = header + JsonKey.DOT_SEPARATOR + body;
      Map<Object, Object> headerData =
          mapper.readValue(new String(decodeFromBase64(header)), Map.class);
      String keyId = headerData.get("kid").toString();
      Map<String, Object> tokenBody =
          mapper.readValue(new String(decodeFromBase64(body)), Map.class);
      userId = (String) tokenBody.get(JsonKey.SUB);
      if (StringUtils.isNotBlank(userId)) {
        int pos = userId.lastIndexOf(":");
        userId = userId.substring(pos + 1);
      }
      boolean isExp = isExpired((Integer) tokenBody.get("exp"));
      boolean issChecked = checkIss((String) tokenBody.get("iss"));
      boolean isValid =
          CryptoUtil.verifyRSASign(
              payLoad,
              decodeFromBase64(signature),
              KeyManager.getPublicKey(keyId).getPublicKey(),
              JsonKey.SHA_256_WITH_RSA);
      if (!isExp && issChecked && isValid) {
        return userId;
      } else {
        return JsonKey.UNAUTHORIZED;
      }
    } catch (Exception ex) {
      ProjectLogger.log("Exception in verifyUserAccessToken: verify ", ex);
    }
    return userId;
  }

  private static boolean checkIss(String iss) {
    String realmUrl =
        KeyCloakConnectionProvider.SSO_URL + "realms/" + KeyCloakConnectionProvider.SSO_REALM;
    return (realmUrl.equalsIgnoreCase(iss));
  }

  private static boolean isExpired(Integer expiration) {
    return (Time.currentTime() > expiration);
  }

  private static byte[] decodeFromBase64(String data) {
    return Base64Util.decode(data, 11);
  }
}
