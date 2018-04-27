package util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.request.HeaderParam;
import org.sunbird.common.responsecode.ResponseCode;
import play.mvc.Http;
import play.mvc.Http.Request;

/**
 * This class will do the request header validation
 *
 * @author Amit Kumar
 */
public class RequestInterceptor {

  protected static List<String> restrictedUriList = null;
  private static ConcurrentHashMap<String, Short> apiHeaderIgnoreMap = new ConcurrentHashMap<>();

  private RequestInterceptor() {}

  static {
    restrictedUriList = new ArrayList<>();
    restrictedUriList.add("/v1/user/update");
    restrictedUriList.add("/v1/note/create");
    restrictedUriList.add("/v1/note/update");
    restrictedUriList.add("/v1/note/search");
    restrictedUriList.add("/v1/note/read");
    restrictedUriList.add("/v1/note/delete");
    restrictedUriList.add("/v1/content/state/update");

    // ---------------------------
    short var = 1;
    apiHeaderIgnoreMap.put("/v1/user/create", var);
    apiHeaderIgnoreMap.put("/v1/org/search", var);
    apiHeaderIgnoreMap.put("/v1/health", var);
    apiHeaderIgnoreMap.put("/v1/page/assemble", var);
    apiHeaderIgnoreMap.put("/health", var);
    apiHeaderIgnoreMap.put("/v1/notification/email", var);
    apiHeaderIgnoreMap.put("/v1/data/sync", var);
    apiHeaderIgnoreMap.put("/v1/user/data/encrypt", var);
    apiHeaderIgnoreMap.put("/v1/user/data/decrypt", var);
    apiHeaderIgnoreMap.put("/v1/file/upload", var);
    apiHeaderIgnoreMap.put("/v1/user/forgotpassword", var);
    apiHeaderIgnoreMap.put("/v1/user/login", var);
    apiHeaderIgnoreMap.put("/v1/user/logout", var);
    apiHeaderIgnoreMap.put("/v1/object/read/list", var);
    apiHeaderIgnoreMap.put("/v1/object/read", var);
    apiHeaderIgnoreMap.put("/v1/object/create", var);
    apiHeaderIgnoreMap.put("/v1/object/update", var);
    apiHeaderIgnoreMap.put("/v1/object/delete", var);
    apiHeaderIgnoreMap.put("/v1/object/search", var);
    apiHeaderIgnoreMap.put("/v1/object/metrics", var);
    apiHeaderIgnoreMap.put("/v1/client/register", var);
    apiHeaderIgnoreMap.put("/v1/client/key/read", var);
    apiHeaderIgnoreMap.put("/v1/notification/send", var);
    apiHeaderIgnoreMap.put("/v1/user/getuser", var);
    apiHeaderIgnoreMap.put("/v1/notification/audience", var);
    apiHeaderIgnoreMap.put("/v1/org/preferences/read", var);
    apiHeaderIgnoreMap.put("/v1/org/preferences/create", var);
    apiHeaderIgnoreMap.put("/v1/org/preferences/update", var);
    apiHeaderIgnoreMap.put("/v1/telemetry", var);
    // making badging api's as public access
    apiHeaderIgnoreMap.put("/v1/issuer/create", var);
    apiHeaderIgnoreMap.put("/v1/issuer/read", var);
    apiHeaderIgnoreMap.put("/v1/issuer/list", var);
    apiHeaderIgnoreMap.put("/v1/issuer/delete", var);
    apiHeaderIgnoreMap.put("/v1/issuer/badge/create", var);
    apiHeaderIgnoreMap.put("/v1/issuer/badge/read", var);
    apiHeaderIgnoreMap.put("/v1/issuer/badge/search", var);
    apiHeaderIgnoreMap.put("/v1/issuer/badge/delete", var);
    apiHeaderIgnoreMap.put("/v1/issuer/badge/assertion/create", var);
    apiHeaderIgnoreMap.put("/v1/issuer/badge/assertion/read", var);
    apiHeaderIgnoreMap.put("/v1/issuer/badge/assertion/search", var);
    apiHeaderIgnoreMap.put("/v1/issuer/badge/assertion/delete", var);
    // making org read as public access
    apiHeaderIgnoreMap.put("/v1/org/read", var);
    // making location APIs public access
    apiHeaderIgnoreMap.put("/v1/location/create", var);
    apiHeaderIgnoreMap.put("/v1/location/update", var);
    apiHeaderIgnoreMap.put("/v1/location/search", var);
    apiHeaderIgnoreMap.put("/v1/location/delete", var);
    apiHeaderIgnoreMap.put("/v1/bulk/location/upload", var);
  }

  /**
   * This Method will do the request header validation.
   *
   * @param request Request
   * @return String
   */
  public static String verifyRequestData(Http.Context ctx) {
    Request request = ctx.request();
    String response = "{userId}";
    if (!isRequestInExcludeList(request.path())) {
      String accessToken = request.getHeader(HeaderParam.X_Access_TokenId.getName());
      String authClientToken =
          request.getHeader(HeaderParam.X_Authenticated_Client_Token.getName());
      String authClientId = request.getHeader(HeaderParam.X_Authenticated_Client_Id.getName());
      if (StringUtils.isBlank(accessToken)
          && StringUtils.isBlank(authClientToken)
          && StringUtils.isBlank(authClientId)) {
        return ResponseCode.unAuthorised.getErrorCode();
      }
      if (StringUtils.isBlank(System.getenv(JsonKey.SSO_PUBLIC_KEY))
          && Boolean.parseBoolean(
              PropertiesCache.getInstance().getProperty(JsonKey.IS_SSO_ENABLED))) {
        ProjectLogger.log(
            "SSO public key is not set by environment variable==", LoggerEnum.INFO.name());
        response = "{userId}" + JsonKey.NOT_AVAILABLE;
      } else if (!StringUtils.isBlank(authClientToken) && !StringUtils.isBlank(authClientId)) {
        String clientId =
            AuthenticationHelper.verifyClientAccessToken(authClientId, authClientToken);
        if (StringUtils.isBlank(clientId)) {
          return ResponseCode.unAuthorised.getErrorCode();
        }
        response = "{userId}" + clientId;
        ctx.flash().put(JsonKey.AUTH_WITH_MASTER_KEY, Boolean.toString(true));
      } else {
        String userId = AuthenticationHelper.verifyUserAccesToken(accessToken);
        if (StringUtils.isBlank(userId)) {
          return ResponseCode.unAuthorised.getErrorCode();
        }
        response = "{userId}" + userId;
      }
    } else {
      AuthenticationHelper.invalidateToken("");
    }
    return response;
  }

  /**
   * this method will check incoming request required validation or not. if this method return true
   * it means no need of validation other wise validation is required.
   *
   * @param request Stirng URI
   * @return boolean
   */
  public static boolean isRequestInExcludeList(String request) {
    boolean resp = false;
    if (!StringUtils.isBlank(request)) {
      if (apiHeaderIgnoreMap.containsKey(request)) {
        resp = true;
      } else {
        String[] splitedpath = request.split("[/]");
        String tempRequest = removeLastValue(splitedpath);
        if (apiHeaderIgnoreMap.containsKey(tempRequest)) {
          resp = true;
        }
      }
    }
    return resp;
  }

  /**
   * Method to remove last value
   *
   * @param splited String []
   * @return String
   */
  private static String removeLastValue(String splited[]) {

    StringBuilder builder = new StringBuilder();
    if (splited != null && splited.length > 0) {
      for (int i = 1; i < splited.length - 1; i++) {
        builder.append("/" + splited[i]);
      }
    }
    return builder.toString();
  }
}
