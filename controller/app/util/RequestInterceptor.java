package util;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.auth.verifier.AccessTokenValidator;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.HeaderParam;
import play.mvc.Http;

/**
 * Request interceptor responsible to authenticated HTTP requests
 *
 * @author Amit Kumar
 */
public class RequestInterceptor {

  private static final LoggerUtil logger = new LoggerUtil(RequestInterceptor.class);
  public static List<String> restrictedUriList = null;
  private static final ConcurrentHashMap<String, Short> apiHeaderIgnoreMap =
      new ConcurrentHashMap<>();

  private RequestInterceptor() {}

  static {
    restrictedUriList = new ArrayList<>();
    restrictedUriList.add("/v1/user/update");
    restrictedUriList.add("/v1/note/create");
    restrictedUriList.add("/v1/note/update");
    restrictedUriList.add("/v1/note/search");
    restrictedUriList.add("/v1/note/read");
    restrictedUriList.add("/v1/note/delete");
    restrictedUriList.add("/v1/user/feed");

    // ---------------------------
    short var = 1;
    apiHeaderIgnoreMap.put("/v1/user/create", var);
    apiHeaderIgnoreMap.put("/v2/user/create", var);
    apiHeaderIgnoreMap.put("/v2/org/search", var);
    apiHeaderIgnoreMap.put("/v2/org/preferences/read", var);
    apiHeaderIgnoreMap.put("/v3/user/create", var);
    apiHeaderIgnoreMap.put("/v1/user/signup", var);
    apiHeaderIgnoreMap.put("/v1/org/create", var);
    apiHeaderIgnoreMap.put("/v1/system/settings/set", var);
    apiHeaderIgnoreMap.put("/v1/org/update/encryptionkey", var);
    apiHeaderIgnoreMap.put("/v2/org/preferences/create", var);
    apiHeaderIgnoreMap.put("/v2/org/preferences/update", var);

    apiHeaderIgnoreMap.put("/v1/org/assign/key", var);
    apiHeaderIgnoreMap.put("/v2/user/signup", var);
    apiHeaderIgnoreMap.put("/v1/ssouser/create", var);
    apiHeaderIgnoreMap.put("/v1/org/search", var);
    apiHeaderIgnoreMap.put("/service/health", var);
    apiHeaderIgnoreMap.put("/health", var);
    apiHeaderIgnoreMap.put("/v1/notification/email", var);
    apiHeaderIgnoreMap.put("/v2/notification", var);
    apiHeaderIgnoreMap.put("/v1/data/sync", var);
    apiHeaderIgnoreMap.put("/v1/file/upload", var);
    apiHeaderIgnoreMap.put("/v1/user/getuser", var);
    // making org read as public access
    apiHeaderIgnoreMap.put("/v1/org/read", var);
    // making location APIs public access
    apiHeaderIgnoreMap.put("/v1/location/create", var);
    apiHeaderIgnoreMap.put("/v1/location/update", var);
    apiHeaderIgnoreMap.put("/v1/location/search", var);
    apiHeaderIgnoreMap.put("/v1/location/delete", var);
    apiHeaderIgnoreMap.put("/v1/otp/generate", var);
    apiHeaderIgnoreMap.put("/v1/otp/verify", var);
    apiHeaderIgnoreMap.put("/v2/otp/generate", var);
    apiHeaderIgnoreMap.put("/v2/otp/verify", var);
    apiHeaderIgnoreMap.put("/v1/user/get/email", var);
    apiHeaderIgnoreMap.put("/v1/user/get/phone", var);
    apiHeaderIgnoreMap.put("/v1/system/settings/get", var);
    apiHeaderIgnoreMap.put("/v1/system/settings/list", var);
    apiHeaderIgnoreMap.put("/private/user/v1/search", var);
    apiHeaderIgnoreMap.put("/private/user/v1/migrate", var);
    apiHeaderIgnoreMap.put("/private/user/v1/identifier/freeup", var);
    apiHeaderIgnoreMap.put("/private/user/v1/password/reset", var);
    apiHeaderIgnoreMap.put("/v1/user/exists/email", var);
    apiHeaderIgnoreMap.put("/v1/user/exists/phone", var);
    apiHeaderIgnoreMap.put("/v1/role/read", var);
    apiHeaderIgnoreMap.put("/v1/user/role/read", var);
    apiHeaderIgnoreMap.put("/private/user/v1/lookup", var);
    apiHeaderIgnoreMap.put("/private/user/feed/v1/create", var);
    apiHeaderIgnoreMap.put("/private/v2/org/search", var);
    apiHeaderIgnoreMap.put("/private/v2/org/preferences/read", var);
  }

  private static String getUserRequestedFor(Http.Request request) {
    String requestedForUserID = null;
    JsonNode jsonBody = request.body().asJson();
    try {
      if (!(jsonBody == null)
          && !(jsonBody.get(JsonKey.REQUEST)
              == null)) { // for search and update and create_mui api's
        if (!(jsonBody.get(JsonKey.REQUEST).get(JsonKey.USER_ID) == null)) {
          requestedForUserID = jsonBody.get(JsonKey.REQUEST).get(JsonKey.USER_ID).asText();
        }
      } else { // for read-api
        String uuidSegment = null;
        Path path = Paths.get(request.uri());
        if (request.queryString().isEmpty()) {
          uuidSegment = path.getFileName().toString();
        } else {
          String[] queryPath = path.getFileName().toString().split("\\?");
          uuidSegment = queryPath[0];
        }
        try {
          requestedForUserID = UUID.fromString(uuidSegment).toString();
        } catch (IllegalArgumentException iae) {
          logger.error("Perhaps this is another API, like search that doesn't carry user id.", iae);
        }
      }
    } catch (Exception e) {
      logger.error("Likely a possibility? " + request.uri(), e);
    }
    return requestedForUserID;
  }

  /**
   * Authenticates given HTTP request context
   *
   * @param request HTTP play request
   * @return User or Client ID for authenticated request. For unauthenticated requests, UNAUTHORIZED
   *     is returned release-3.0.0 on-wards validating managedBy token.
   */
  public static Map verifyRequestData(Http.Request request, Map<String, Object> requestContext) {
    Map userAuthentication = new HashMap<String, String>();
    userAuthentication.put(JsonKey.USER_ID, JsonKey.UNAUTHORIZED);
    userAuthentication.put(JsonKey.MANAGED_FOR, null);

    String clientId = JsonKey.UNAUTHORIZED;
    String managedForId = null;
    Optional<String> accessToken = request.header(HeaderParam.X_Authenticated_User_Token.getName());
    if (!isRequestInExcludeList(request.path()) && !isRequestPrivate(request.path())) {
      // The API must be invoked with either access token or client token.
      if (accessToken.isPresent()) {
        clientId = AccessTokenValidator.verifyUserToken(accessToken.get(), requestContext);
        if (!JsonKey.USER_UNAUTH_STATES.contains(clientId)) {
          // Now we have some valid token, next verify if the token is matching the request.
          String requestedForUserID = getUserRequestedFor(request);
          if (StringUtils.isNotEmpty(requestedForUserID) && !requestedForUserID.equals(clientId)) {
            // LUA - MUA user combo, check the 'for' token and its parent, child identifiers
            Optional<String> forTokenHeader =
                request.header(HeaderParam.X_Authenticated_For.getName());
            String managedAccessToken = forTokenHeader.isPresent() ? forTokenHeader.get() : "";
            if (StringUtils.isNotEmpty(managedAccessToken)) {
              String managedFor =
                  AccessTokenValidator.verifyManagedUserToken(
                      managedAccessToken, clientId, requestedForUserID, requestContext);
              if (!JsonKey.USER_UNAUTH_STATES.contains(managedFor)) {
                managedForId = managedFor;
              } else {
                clientId = JsonKey.UNAUTHORIZED;
              }
            }
          } else {
            logger.debug("Ignoring x-authenticated-for token...");
          }
        }
        userAuthentication.put(JsonKey.USER_ID, clientId);
        userAuthentication.put(JsonKey.MANAGED_FOR, managedForId);
      } else {
        logger.info("Token not present in request: " + request.getHeaders().toMap());
      }
    } else {
      if (accessToken.isPresent()) {
        String clientAccessTokenId = null;
        try {
          clientAccessTokenId =
              AccessTokenValidator.verifyUserToken(accessToken.get(), requestContext);
          if (JsonKey.UNAUTHORIZED.equalsIgnoreCase(clientAccessTokenId)) {
            clientAccessTokenId = null;
          }
        } catch (Exception ex) {
          logger.error(ex.getMessage(), ex);
          clientAccessTokenId = null;
        }
        userAuthentication.put(
            JsonKey.USER_ID,
            StringUtils.isNotBlank(clientAccessTokenId) ? clientAccessTokenId : JsonKey.ANONYMOUS);
      } else {
        userAuthentication.put(JsonKey.USER_ID, JsonKey.ANONYMOUS);
      }
    }
    return userAuthentication;
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
