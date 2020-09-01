package util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.common.util.Time;
import org.sunbird.auth.verifier.Base64Util;
import org.sunbird.auth.verifier.CryptoUtil;
import org.sunbird.auth.verifier.KeyManager;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.KeyCloakConnectionProvider;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;

/**
 * This class will handle all the method related to authentication. For example verifying user
 * access token, creating access token after success login.
 *
 * @author Manzarul
 */
public class AuthenticationHelper {

  private static CassandraOperation cassandraOperation = ServiceFactory.getInstance();

  public static String verifyUserAccessToken(String token) {
    String userId = JsonKey.UNAUTHORIZED;
    try {
      String[] tokenElements = token.split("\\.");
      String header = tokenElements[0];
      String body = tokenElements[1];
      String signature = tokenElements[2];
      String payLoad = header + JsonKey.DOT_SEPARATOR + body;
      ObjectMapper mapper = new ObjectMapper();
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

  @SuppressWarnings("unchecked")
  public static String verifyClientAccessToken(String clientId, String clientToken) {
    Util.DbInfo clientDbInfo = Util.dbInfoMap.get(JsonKey.CLIENT_INFO_DB);
    Map<String, Object> propertyMap = new HashMap<>();
    propertyMap.put(JsonKey.ID, clientId);
    propertyMap.put(JsonKey.MASTER_KEY, clientToken);
    String validClientId = JsonKey.UNAUTHORIZED;
    try {
      Response clientResponse =
          cassandraOperation.getRecordsByProperties(
              clientDbInfo.getKeySpace(), clientDbInfo.getTableName(), propertyMap);
      if (null != clientResponse && !clientResponse.getResult().isEmpty()) {
        List<Map<String, Object>> dataList =
            (List<Map<String, Object>>) clientResponse.getResult().get(JsonKey.RESPONSE);
        validClientId = (String) dataList.get(0).get(JsonKey.ID);
      }
    } catch (Exception e) {
      ProjectLogger.log("Validating client token failed due to : ", e);
    }
    return validClientId;
  }

  private static byte[] decodeFromBase64(String data) {
    return Base64Util.decode(data, 11);
  }
}
