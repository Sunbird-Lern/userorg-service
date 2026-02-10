package modules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.ConfigFactory;

import controllers.BaseController;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.HeaderParam;
import org.sunbird.response.Response;
import org.sunbird.util.DataCacheHandler;
import org.sunbird.common.ProjectUtil;
import play.http.ActionCreator;
import play.libs.Json;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import util.Attrs;
import util.RequestInterceptor;

public class OnRequestHandler implements ActionCreator {

  private static final LoggerUtil logger = new LoggerUtil(OnRequestHandler.class);
  private final ObjectMapper mapper = new ObjectMapper();
  public static boolean isServiceHealthy = true;

  @Override
  public Action createAction(Http.Request request, Method method) {
    Optional<String> optionalMessageId = request.header(JsonKey.MESSAGE_ID);
    String requestId;
    if (optionalMessageId.isPresent()) {
      requestId = optionalMessageId.get();
    } else {
      UUID uuid = UUID.randomUUID();
      requestId = uuid.toString();
    }
    logger.debug("Original Url: " + request.uri());
    logger.debug("Original Captcha: " + request.getQueryString(JsonKey.CAPTCHA_RESPONSE));
    return new Action.Simple() {
      @Override
      public CompletionStage<Result> call(Http.Request request) {
        request.getHeaders();
        CompletionStage<Result> result = checkForServiceHealth(request);
        if (result != null) return result;
        // call method to set all the required params for the telemetry event(log)...
        Map<String, Object> requestContext = getRequestContext(request);
        request = updateRequestId(request, requestContext, requestId);
        // From 3.0.0 checking user access-token and managed-by from the request header
        Map userAuthentication = new HashMap<String, String>();
        //making user authentication optional based on the flag
        if(ConfigFactory.load().getBoolean(JsonKey.AUTH_ENABLED)){
        userAuthentication = RequestInterceptor.verifyRequestData(request, requestContext);
        }else{
          userAuthentication.put(JsonKey.USER_ID, JsonKey.ANONYMOUS);
          userAuthentication.put(JsonKey.MANAGED_FOR, null);
        }
        String message = (String) userAuthentication.get(JsonKey.USER_ID);
        updateActorIdAndType(requestContext, request, message);
        try {
          request = request.addAttr(Attrs.CONTEXT, mapper.writeValueAsString(requestContext));
        } catch (Exception e) {
          logger.error("Exception while parsing request context.", e);
          ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
        }
        if (userAuthentication.get(JsonKey.MANAGED_FOR) != null) {
          request =
              request.addAttr(
                  Attrs.MANAGED_FOR, (String) userAuthentication.get(JsonKey.MANAGED_FOR));
        }
        if (!JsonKey.USER_UNAUTH_STATES.contains(message)) {
          request = request.addAttr(Attrs.USER_ID, message);
          request = request.addAttr(Attrs.IS_AUTH_REQ, "false");
          for (String uri : RequestInterceptor.restrictedUriList) {
            if (request.path().contains(uri)) {
              request = request.addAttr(Attrs.IS_AUTH_REQ, "true");
              break;
            }
          }
          result = delegate.call(request);
        } else if (JsonKey.UNAUTHORIZED.equals(message)) {
          result = onDataValidationError(request, ResponseCode.UNAUTHORIZED.getResponseCode());
        } else {
          result = delegate.call(request);
        }
        return result.thenApply(res -> res.withHeader("Access-Control-Allow-Origin", "*"));
      }
    };
  }

  private void updateActorIdAndType(
      Map<String, Object> reqContext, Http.Request request, String userId) {
    if (!JsonKey.USER_UNAUTH_STATES.contains(userId)) {
      ((Map) reqContext.get(JsonKey.CONTEXT)).put(JsonKey.ACTOR_ID, userId);
      ((Map) reqContext.get(JsonKey.CONTEXT))
          .put(JsonKey.ACTOR_TYPE, StringUtils.capitalize(JsonKey.USER));
    } else {
      Optional<String> optionalConsumerId = request.header(HeaderParam.X_Consumer_ID.getName());
      String consumerId;
      if (optionalConsumerId.isPresent()) {
        consumerId = optionalConsumerId.get();
      } else {
        consumerId = JsonKey.DEFAULT_CONSUMER_ID;
      }
      ((Map) reqContext.get(JsonKey.CONTEXT)).put(JsonKey.ACTOR_ID, consumerId);
      ((Map) reqContext.get(JsonKey.CONTEXT))
          .put(JsonKey.ACTOR_TYPE, StringUtils.capitalize(JsonKey.CONSUMER));
    }
  }

  private Http.Request updateRequestId(
      Http.Request request, Map<String, Object> reqContext, String requestId) {
    Optional<String> optionalTraceId = request.header(HeaderParam.X_REQUEST_ID.getName());
    if (optionalTraceId.isPresent()) {
      ((Map) reqContext.get(JsonKey.CONTEXT)).put(JsonKey.X_REQUEST_ID, optionalTraceId.get());
      request = request.addAttr(Attrs.X_REQUEST_ID, optionalTraceId.get());
    } else {
      request = request.addAttr(Attrs.X_REQUEST_ID, requestId);
      ((Map) reqContext.get(JsonKey.CONTEXT)).put(JsonKey.X_REQUEST_ID, requestId);
    }
    return request;
  }

  public CompletionStage<Result> checkForServiceHealth(Http.Request request) {
    if (Boolean.parseBoolean((ProjectUtil.getConfigValue(JsonKey.SUNBIRD_HEALTH_CHECK_ENABLE)))
        && !request.path().endsWith(JsonKey.HEALTH)) {
      if (!isServiceHealthy) {
        ResponseCode headerCode = ResponseCode.SERVICE_UNAVAILABLE;
        Response resp = BaseController.createFailureResponse(request, headerCode, headerCode);
        return CompletableFuture.completedFuture(
            Results.status(ResponseCode.SERVICE_UNAVAILABLE.getResponseCode(), Json.toJson(resp)));
      }
    }
    return null;
  }

  /**
   * This method will do request data validation for GET method only. As a GET request user must
   * send some key in header.
   *
   * @param request Request
   * @return CompletionStage<Result>
   */
  public CompletionStage<Result> onDataValidationError(Http.Request request, int responseCode) {
    Response resp =
        BaseController.createFailureResponse(
            request, ResponseCode.unAuthorized, ResponseCode.UNAUTHORIZED);
    return CompletableFuture.completedFuture(Results.status(responseCode, Json.toJson(resp)));
  }

  private Map<String, Object> getRequestContext(Http.Request request) {
    Map<String, Object> requestContext = new HashMap<>();
    try {
      String actionMethod = request.method();
      String url = request.uri();
      String methodName = actionMethod;
      String signType = "";
      String source = "";
      if (request.body() != null && request.body().asJson() != null) {
        JsonNode requestNode =
            request.body().asJson().get("params"); // extracting signup type from request
        if (requestNode != null && requestNode.get(JsonKey.SIGNUP_TYPE) != null) {
          signType = requestNode.get(JsonKey.SIGNUP_TYPE).asText();
        }
        if (requestNode != null && requestNode.get(JsonKey.REQUEST_SOURCE) != null) {
          source = requestNode.get(JsonKey.REQUEST_SOURCE).asText();
        }
      }
      Map<String, Object> reqContext = new WeakHashMap<>();
      reqContext.put(JsonKey.SIGNUP_TYPE, signType);
      reqContext.put(JsonKey.REQUEST_SOURCE, source);
      Optional<String> optionalChannel = request.header(HeaderParam.CHANNEL_ID.getName());
      String channel;
      if (optionalChannel.isPresent()) {
        channel = optionalChannel.get();
      } else {
        String custodianOrgHashTagid =
            DataCacheHandler.getConfigSettings().get(JsonKey.CUSTODIAN_ORG_ID);
        channel =
            (StringUtils.isNotEmpty(custodianOrgHashTagid))
                ? custodianOrgHashTagid
                : JsonKey.DEFAULT_ROOT_ORG_ID;
      }
      reqContext.put(JsonKey.CHANNEL, channel);
      reqContext.put(JsonKey.ENV, getEnv(request));
      reqContext.putAll(DataCacheHandler.getTelemetryPdata());
      Optional<String> optionalAppId = request.header(HeaderParam.X_APP_ID.getName());
      if (optionalAppId.isPresent()) {
        reqContext.put(JsonKey.APP_ID, optionalAppId.get());
      }
      Optional<String> optionalDeviceId = request.header(HeaderParam.X_Device_ID.getName());
      if (optionalDeviceId.isPresent()) {
        reqContext.put(JsonKey.DEVICE_ID, optionalDeviceId.get());
      }

      Optional<String> optionalSessionId = request.header(HeaderParam.X_Session_ID.getName());
      if (optionalSessionId.isPresent()) {
        reqContext.put(JsonKey.X_Session_ID, optionalSessionId.get());
      }

      Optional<String> optionalAppVersion = request.header(HeaderParam.X_APP_VERSION.getName());
      if (optionalAppVersion.isPresent()) {
        reqContext.put(JsonKey.X_APP_VERSION, optionalAppVersion.get());
      }

      Optional<String> optionalTraceEnabled = request.header(HeaderParam.X_TRACE_ENABLED.getName());
      if (optionalTraceEnabled.isPresent()) {
        reqContext.put(JsonKey.X_TRACE_ENABLED, optionalTraceEnabled.get());
      }
      requestContext.put(JsonKey.CONTEXT, reqContext);
      Map<String, Object> additionalInfo = new HashMap<>();
      additionalInfo.put(JsonKey.URL, url);
      additionalInfo.put(JsonKey.METHOD, methodName);
      requestContext.put(JsonKey.ADDITIONAL_INFO, additionalInfo);
    } catch (Exception ex) {
      ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
    }
    return requestContext;
  }

  private String getEnv(Http.Request request) {

    String uri = request.uri();
    String env;
    if (uri.startsWith("/v1/user")
        || uri.startsWith("/v2/user")
        || uri.startsWith("/v3/user")
        || uri.startsWith("/v4/user")
        || uri.startsWith("/v5/user")
        || uri.startsWith("/v1/ssouser")
        || uri.startsWith("/v1/manageduser")
        || uri.startsWith("/v2/manageduser")
        || uri.startsWith("/v5/user")
        || uri.startsWith("/private/user")) {
      env = JsonKey.USER;
    } else if (uri.startsWith("/v1/org") || uri.startsWith("/v2/org")) {
      env = JsonKey.ORGANISATION;
    } else if (uri.startsWith("/v1/notification") || uri.startsWith("/v2/notification")) {
      env = JsonKey.NOTIFICATION;
    } else if (uri.startsWith("/v1/role")) {
      env = JsonKey.ROLE;
    } else if (uri.startsWith("/v1/note")) {
      env = JsonKey.NOTE;
    } else if (uri.startsWith("/v1/location")) {
      env = JsonKey.LOCATION;
    } else if (uri.startsWith("/v1/otp") || uri.startsWith("/v2/otp")) {
      env = "otp";
    } else {
      env = "miscellaneous";
    }
    return env;
  }
}
