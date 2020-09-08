package util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sunbird.auth.verifier.Base64Util;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
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
  private static Util.DbInfo userAuth = Util.dbInfoMap.get(JsonKey.USER_AUTH_DB);

  public static String verifyUserAccessToken(String token) {
    String userId = JsonKey.UNAUTHORIZED;
    try {
      Response authResponse =
          cassandraOperation.getRecordById(userAuth.getKeySpace(), userAuth.getTableName(), token);
      if (authResponse != null && authResponse.get(JsonKey.RESPONSE) != null) {
        List<Map<String, Object>> authList =
            (List<Map<String, Object>>) authResponse.get(JsonKey.RESPONSE);
        if (authList != null && !authList.isEmpty()) {
          Map<String, Object> authMap = authList.get(0);
          userId = (String) authMap.get(JsonKey.USER_ID);
        }
      }
    } catch (Exception e) {
      ProjectLogger.log("invalid auth token =" + token, e);
    }
    return userId;
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
