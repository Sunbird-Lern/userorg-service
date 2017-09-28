package util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.HeaderParam;
import org.sunbird.common.responsecode.ResponseCode;
import play.mvc.Http.Request;

/**
 * This class will do the request header validation
 * 
 * @author Amit Kumar
 *
 */
public class RequestInterceptor {

  private RequestInterceptor() {}
  public static List<String> restrictedUriList = null;
  private static ConcurrentHashMap<String, Short> apiHeaderIgnoreMap = new ConcurrentHashMap<>();
  static {
    restrictedUriList = new ArrayList<>();
    restrictedUriList.add("/v1/user/update");
    restrictedUriList.add("/v1/note/create");
    restrictedUriList.add("/v1/note/update");
    restrictedUriList.add("/v1/note/search");
    restrictedUriList.add("/v1/note/read");
    restrictedUriList.add("/v1/note/delete");
    restrictedUriList.add("/v1/content/state/update");
    
    //---------------------------
    short var = 1;
    apiHeaderIgnoreMap.put("/v1/user/create", var);
    apiHeaderIgnoreMap.put("/v1/org/search", var);
    apiHeaderIgnoreMap.put("/v1/health", var);
    apiHeaderIgnoreMap.put("/v1/page/assemble", var);
    apiHeaderIgnoreMap.put("/health", var);
  }

  /**
   * This Method will do the request header validation.
   * 
   * @param request Request
   * @return String
   */
  public static String verifyRequestData(Request request) {
    String response = "{userId}";
    if (!isRequestInExcludeList(request.path())) {
      if (ProjectUtil
          .isStringNullOREmpty(request.getHeader(HeaderParam.X_Access_TokenId.getName()))) {
        return ResponseCode.authTokenRequired.getErrorCode();
      }
      String userId = AuthenticationHelper
          .verifyUserAccesToken(request.getHeader(HeaderParam.X_Access_TokenId.getName()));
      if (ProjectUtil.isStringNullOREmpty(userId)) {
        return ResponseCode.invalidAuthToken.getErrorCode();
      }
      response = "{userId}" + userId;
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
    if (!ProjectUtil.isStringNullOREmpty(request)) {
      if (apiHeaderIgnoreMap.containsKey(request)) {
        resp = true;
      } else {
        String[] splitedpath = request.split("[/]");
        request = removeLastValue(splitedpath);
        if (apiHeaderIgnoreMap.containsKey(request)) {
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
