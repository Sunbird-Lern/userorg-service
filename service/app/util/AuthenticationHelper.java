package util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.models.util.datasecurity.EncryptionService;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.learner.util.Util.DbInfo;
import org.sunbird.services.sso.SSOManager;
import org.sunbird.services.sso.SSOServiceFactory;

/**
 * This class will handle all the method related to authentication. For example verifying user
 * access token, creating access token after success login.
 *
 * @author Manzarul
 */
public class AuthenticationHelper {

  private static boolean ssoEnabled =
      ((PropertiesCache.getInstance().getProperty(JsonKey.SSO_PUBLIC_KEY) != null)
          && (Boolean.parseBoolean(
              PropertiesCache.getInstance().getProperty(JsonKey.IS_SSO_ENABLED))));
  private static CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private static DbInfo userAuth = Util.dbInfoMap.get(JsonKey.USER_AUTH_DB);
  public static final String KEY_SPACE_NAME = "sunbird";
  private static EncryptionService encryptionService =
      org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getEncryptionServiceInstance(
          null);

  /**
   * This method will verify the incoming user access token against store data base /cache. If token
   * is valid then it would be associated with some user id. In case of token matched it will
   * provide user id. else will provide empty string.
   *
   * @param token String
   * @return String
   */
  @SuppressWarnings("unchecked")
  public static String verifyUserAccesToken(String token) {
    SSOManager ssoManager = SSOServiceFactory.getInstance();
    String userId = JsonKey.UNAUTHORIZED;
    try {
      if (ssoEnabled) {
        userId = ssoManager.verifyToken(token);
      } else {
        Response authResponse =
            cassandraOperation.getRecordById(
                userAuth.getKeySpace(), userAuth.getTableName(), token, null);
        if (authResponse != null && authResponse.get(JsonKey.RESPONSE) != null) {
          List<Map<String, Object>> authList =
              (List<Map<String, Object>>) authResponse.get(JsonKey.RESPONSE);
          if (authList != null && !authList.isEmpty()) {
            Map<String, Object> authMap = authList.get(0);
            userId = (String) authMap.get(JsonKey.USER_ID);
          }
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
              clientDbInfo.getKeySpace(), clientDbInfo.getTableName(), propertyMap, null);
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
}
