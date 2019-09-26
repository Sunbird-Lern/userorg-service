package util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.HeaderParam;
import play.mvc.Http;
import play.mvc.Http.Request;

/**
 * Request interceptor responsible to authenticated HTTP requests
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

    // ---------------------------
    short var = 1;
    apiHeaderIgnoreMap.put("/v1/user/create", var);
    apiHeaderIgnoreMap.put("/v2/user/create", var);
    apiHeaderIgnoreMap.put("/v1/org/search", var);
    apiHeaderIgnoreMap.put("/service/health", var);
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
    apiHeaderIgnoreMap.put("/v1/otp/generate", var);
    apiHeaderIgnoreMap.put("/v1/otp/verify", var);
    apiHeaderIgnoreMap.put("/v1/user/get/email", var);
    apiHeaderIgnoreMap.put("/v1/user/get/phone", var);
    apiHeaderIgnoreMap.put("/v1/user/get/loginId", var);
    apiHeaderIgnoreMap.put("/v1/user/get/loginid", var);
    apiHeaderIgnoreMap.put("/v1/system/settings/get", var);
    apiHeaderIgnoreMap.put("/v1/system/settings/list", var);
    apiHeaderIgnoreMap.put("/v1/user/mock/read", var);
    apiHeaderIgnoreMap.put("/v1/cache/clear", var);
    apiHeaderIgnoreMap.put("/private/user/v1/search", var);
    apiHeaderIgnoreMap.put("/private/user/v1/migrate", var);
    apiHeaderIgnoreMap.put("/private/user/v1/identifier/freeup", var);
    apiHeaderIgnoreMap.put("/private/user/v1/password/reset", var);
    apiHeaderIgnoreMap.put("/private/user/v1/certs/add", var);
  }

  /**
   * Authenticates given HTTP request context
   *
   * @param ctx HTTP play request context
   * @return User or Client ID for authenticated request. For unauthenticated requests, UNAUTHORIZED
   *     is returned
   */
  public static String verifyRequestData(Http.Context ctx) {
    Request request = ctx.request();
    String clientId = JsonKey.UNAUTHORIZED;
    String accessToken = request.getHeader(HeaderParam.X_Authenticated_User_Token.getName());
    String authClientToken = request.getHeader(HeaderParam.X_Authenticated_Client_Token.getName());
    String authClientId = request.getHeader(HeaderParam.X_Authenticated_Client_Id.getName());
    if (!isRequestInExcludeList(request.path()) && !isRequestPrivate(request.path())) {
      if (StringUtils.isNotBlank(accessToken)) {
        clientId = AuthenticationHelper.verifyUserAccesToken(accessToken);
      } else if (StringUtils.isNotBlank(authClientToken) && StringUtils.isNotBlank(authClientId)) {
        clientId = AuthenticationHelper.verifyClientAccessToken(authClientId, authClientToken);
        if (!JsonKey.UNAUTHORIZED.equals(clientId)) {
          ctx.flash().put(JsonKey.AUTH_WITH_MASTER_KEY, Boolean.toString(true));
        }
      }
      return clientId;
    } else {
      if (StringUtils.isNotBlank(accessToken)) {
        String clientAccessTokenId = null;
        try {
          clientAccessTokenId = AuthenticationHelper.verifyUserAccesToken(accessToken);
          if (JsonKey.UNAUTHORIZED.equalsIgnoreCase(clientAccessTokenId)) {
            clientAccessTokenId = null;
          }
        } catch (Exception ex) {
          ProjectLogger.log(ex.getMessage(), ex);
          clientAccessTokenId = null;
        }
        return StringUtils.isNotBlank(clientAccessTokenId)
            ? clientAccessTokenId
            : JsonKey.ANONYMOUS;
      }
      return JsonKey.ANONYMOUS;
    }
  }

  private static boolean isRequestPrivate(String path) {
    return path.contains(JsonKey.PRIVATE);
  }

  /**
   * Checks if request URL is in excluded (i.e. public) URL list or not
   *
   * @param requestUrl Request URL
   * @return True if URL is in excluded (public) URLs. Otherwise, returns false
   */
  public static boolean isRequestInExcludeList(String requestUrl) {
    boolean resp = false;
    if (!StringUtils.isBlank(requestUrl)) {
      if (apiHeaderIgnoreMap.containsKey(requestUrl)) {
        resp = true;
      } else {
        String[] splitPath = requestUrl.split("[/]");
        String urlWithoutPathParam = removeLastValue(splitPath);
        if (apiHeaderIgnoreMap.containsKey(urlWithoutPathParam)) {
          resp = true;
        }
      }
    }
    return resp;
  }

  /**
   * Returns URL without path and query parameters.
   *
   * @param splitPath URL path split on slash (i.e. /)
   * @return URL without path and query parameters
   */
  private static String removeLastValue(String splitPath[]) {

    StringBuilder builder = new StringBuilder();
    if (splitPath != null && splitPath.length > 0) {
      for (int i = 1; i < splitPath.length - 1; i++) {
        builder.append("/" + splitPath[i]);
      }
    }
    return builder.toString();
  }
}
