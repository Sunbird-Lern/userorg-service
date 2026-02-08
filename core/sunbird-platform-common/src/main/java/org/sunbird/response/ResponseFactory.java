package org.sunbird.response;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.exception.BaseException;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;

/**
 * Factory class for creating standardized response objects.
 */
public class ResponseFactory {

  /**
   * Generates a failure response object based on the provided exception and request context.
   *
   * @param exception The exception causing the failure.
   * @param request The original request object, used for context.
   * @return A Response object populated with failure details.
   */
  public static Response getFailureMessage(Object exception, Request request) {
    Response response = new Response();
    if (request != null) {
      response.setId(getApiId(request.getPath()));
      response.setVer(request.getVer());
      response.setTs(getCurrentDate());
    } else {
      response.setTs(getCurrentDate());
    }

    if (exception instanceof BaseException) {
      BaseException ex = (BaseException) exception;
      ResponseCode code = ResponseCode.getResponseCodeByCode(ex.getResponseCode());
      if (code == null) {
        code = ResponseCode.SERVER_ERROR;
      }
      response.setParams(createResponseParamObj(request, code, ex.getMessage()));
      if (response.getParams() != null) {
        if (StringUtils.isNotBlank(response.getParams().getErrmsg())
            && response.getParams().getErrmsg().contains("{0}")) {
          response.getParams().setErrmsg(ex.getMessage());
        }
        response.getParams().setStatus(ResponseParams.StatusType.FAILED.name().toLowerCase());
        response.getParams().setResmsgid(UUID.randomUUID().toString());
      }
      response.setResponseCode(ResponseCode.getHeaderResponseCode(ex.getResponseCode()));
    } else {
      // Handle generic exceptions
      ResponseParams params = new ResponseParams();
      params.setErr(ResponseCode.SERVER_ERROR.getErrorCode());
      params.setErrmsg(ResponseCode.SERVER_ERROR.getErrorMessage());
      params.setStatus(ResponseParams.StatusType.FAILED.name().toLowerCase());
      params.setResmsgid(UUID.randomUUID().toString());
      setResponseParams(request, params);
      response.setParams(params);
      response.setResponseCode(ResponseCode.SERVER_ERROR);
    }
    return response;
  }

  /**
   * Creates a ResponseParams object with error details if the response code is not OK.
   *
   * @param request The original request object.
   * @param code The response code indicating success or failure.
   * @param customMessage A custom error message to override the default.
   * @return A populated ResponseParams object.
   */
  public static ResponseParams createResponseParamObj(
      Request request, ResponseCode code, String customMessage) {
    ResponseParams params = new ResponseParams();
    if (code.getCode() != 200) {
      params.setErr(code.getErrorCode());
      params.setErrmsg(StringUtils.isNotBlank(customMessage) ? customMessage : code.name());
    }
    setResponseParams(request, params);
    return params;
  }

  /**
   * Extracts and sets response parameters (like message ID) from the request context.
   *
   * @param request The original request object.
   * @param params The ResponseParams object to populate.
   */
  private static void setResponseParams(Request request, ResponseParams params) {
    if (request != null && request.getContext() != null) {
      if (request.getContext().containsKey(JsonKey.X_REQUEST_ID)) {
        params.setMsgid((String) request.getContext().get(JsonKey.X_REQUEST_ID));
      } else if (request.getContext().containsKey(JsonKey.REQUEST_MESSAGE_ID)) {
        params.setMsgid((String) request.getContext().get(JsonKey.REQUEST_MESSAGE_ID));
      }
    }
  }

  /**
   * Generates a success response object based on the provided request context.
   *
   * @param request The original request object.
   * @return A Response object populated with success details.
   */
  public static Response getSuccessMessage(Request request) {
    Response response = new Response();
    if (request != null) {
      response.setId(request.getId());
      response.setVer(request.getVer());
    }
    response.setTs(System.currentTimeMillis() + StringUtils.EMPTY);
    ResponseParams params = new ResponseParams();
    params.setResmsgid(UUID.randomUUID().toString());
    params.setStatus(ResponseParams.StatusType.SUCCESSFUL.name().toLowerCase());
    setResponseParams(request, params);
    response.setParams(params);
    response.setResponseCode(ResponseCode.OK);
    return response;
  }

  /**
   * Normalizes a URI into a standardized API identifier (e.g., /v1/user/read -> api.user.read).
   *
   * @param uri The request URI.
   * @return A formatted API ID string.
   */
  public static String getApiId(String uri) {
    final String ver = "/" + JsonKey.API_VERSION;
    StringBuilder builder = new StringBuilder();
    if (StringUtils.isNotBlank(uri)) {
      if (uri.contains(ver)) {
        uri = uri.replaceFirst(ver, "api");
      }
      String temVal[] = uri.split("/");
      for (String str : temVal) {
        if (str.matches("[A-Za-z]+")) {
          builder.append(str + ".");
        }
      }
      if (builder.length() > 0) {
        builder.deleteCharAt(builder.length() - 1);
      }
    }
    return builder.toString();
  }

  /**
   * Gets the current system date and time formatted as yyyy-MM-dd HH:mm:ss:SSSZ.
   *
   * @return A formatted timestamp string.
   */
  public static String getCurrentDate() {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSSZ");
    simpleDateFormat.setLenient(false);
    return simpleDateFormat.format(new Date());
  }
}
