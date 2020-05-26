package modules;

import akka.actor.ActorRef;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.BaseController;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.sunbird.actor.router.RequestRouter;
import org.sunbird.actorutil.org.OrganisationClient;
import org.sunbird.actorutil.org.impl.OrganisationClientImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.*;
import org.sunbird.common.request.HeaderParam;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.learner.util.DataCacheHandler;
import play.http.ActionCreator;
import play.libs.Json;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import util.RequestInterceptor;

public class OnRequestHandler implements ActionCreator {

  private ObjectMapper mapper = new ObjectMapper();
  private static String custodianOrgHashTagId;
  public static boolean isServiceHealthy = true;
  private final List<String> USER_UNAUTH_STATES =
      Arrays.asList(JsonKey.UNAUTHORIZED, JsonKey.ANONYMOUS);

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
    return new Action.Simple() {
      @Override
      public CompletionStage<Result> call(Http.Request request) {
        request.getHeaders();
        CompletionStage<Result> result = checkForServiceHealth(request);
        if (result != null) return result;
        String message = RequestInterceptor.verifyRequestData(request);
        // call method to set all the required params for the telemetry event(log)...
        initializeRequestInfo(request, message, requestId);
        if (!USER_UNAUTH_STATES.contains(message)) {
          request.flash().put(JsonKey.USER_ID, message);
          request.flash().put(JsonKey.IS_AUTH_REQ, "false");
          for (String uri : RequestInterceptor.restrictedUriList) {
            if (request.path().contains(uri)) {
              request.flash().put(JsonKey.IS_AUTH_REQ, "true");
              break;
            }
          }
          result = delegate.call(request);
        } else if (JsonKey.UNAUTHORIZED.equals(message)) {
          result =
              onDataValidationError(request, message, ResponseCode.UNAUTHORIZED.getResponseCode());
        } else {
          result = delegate.call(request);
        }
        return result.thenApply(res -> res.withHeader("Access-Control-Allow-Origin", "*"));
      }
    };
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
   * @param errorMessage String
   * @return CompletionStage<Result>
   */
  public CompletionStage<Result> onDataValidationError(
      Http.Request request, String errorMessage, int responseCode) {
    ProjectLogger.log("Data error found--" + errorMessage);
    ResponseCode code = ResponseCode.getResponse(errorMessage);
    ResponseCode headerCode = ResponseCode.CLIENT_ERROR;
    Response resp = BaseController.createFailureResponse(request, code, headerCode);
    return CompletableFuture.completedFuture(Results.status(responseCode, Json.toJson(resp)));
  }

  public void initializeRequestInfo(Http.Request request, String userId, String requestId) {
    try {
      String actionMethod = request.method();
      String url = request.uri();
      String methodName = actionMethod;
      long startTime = System.currentTimeMillis();
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
        String custodianOrgHashTagid = getCustodianOrgHashTagId();
        channel =
            (StringUtils.isNotEmpty(custodianOrgHashTagid))
                ? custodianOrgHashTagid
                : JsonKey.DEFAULT_ROOT_ORG_ID;
      }
      reqContext.put(JsonKey.CHANNEL, channel);
      reqContext.put(JsonKey.ENV, getEnv(request));
      reqContext.put(JsonKey.REQUEST_ID, requestId);
      reqContext.putAll(DataCacheHandler.getTelemetryPdata());
      Optional<String> optionalAppId = request.header(HeaderParam.X_APP_ID.getName());
      if (optionalAppId.isPresent()) {
        reqContext.put(JsonKey.APP_ID, optionalAppId.get());
      }
      Optional<String> optionalDeviceId = request.header(HeaderParam.X_Device_ID.getName());
      if (optionalDeviceId.isPresent()) {
        reqContext.put(JsonKey.DEVICE_ID, optionalDeviceId.get());
      }
      if (!USER_UNAUTH_STATES.contains(userId)) {
        reqContext.put(JsonKey.ACTOR_ID, userId);
        reqContext.put(JsonKey.ACTOR_TYPE, StringUtils.capitalize(JsonKey.USER));
        ;
      } else {
        Optional<String> optionalConsumerId = request.header(HeaderParam.X_Consumer_ID.getName());
        String consumerId;
        if (optionalConsumerId.isPresent()) {
          consumerId = optionalConsumerId.get();
        } else {
          consumerId = JsonKey.DEFAULT_CONSUMER_ID;
        }
        reqContext.put(JsonKey.ACTOR_ID, consumerId);
        reqContext.put(JsonKey.ACTOR_TYPE, StringUtils.capitalize(JsonKey.CONSUMER));
      }
      Map<String, Object> map = new WeakHashMap<>();
      map.put(JsonKey.CONTEXT, reqContext);
      Map<String, Object> additionalInfo = new WeakHashMap<>();
      additionalInfo.put(JsonKey.URL, url);
      additionalInfo.put(JsonKey.METHOD, methodName);
      additionalInfo.put(JsonKey.START_TIME, startTime);
      map.put(JsonKey.ADDITIONAL_INFO, additionalInfo);
      request.flash().put(JsonKey.REQUEST_ID, requestId);
      request.flash().put(JsonKey.CONTEXT, mapper.writeValueAsString(map));
    } catch (Exception ex) {
      ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
    }
  }

  private static String getCustodianOrgHashTagId() {
    if (custodianOrgHashTagId != null) {
      return custodianOrgHashTagId;
    }
    synchronized (OnRequestHandler.class) {
      if (custodianOrgHashTagId == null) {
        try {
          // Get hash tag ID of custodian org
          OrganisationClient orgClient = new OrganisationClientImpl();
          ActorRef orgActorRef = RequestRouter.getActor(ActorOperations.GET_ORG_DETAILS.getValue());
          custodianOrgHashTagId =
              orgClient
                  .getOrgById(
                      orgActorRef,
                      DataCacheHandler.getConfigSettings().get(JsonKey.CUSTODIAN_ORG_ID))
                  .getHashTagId();
        } catch (ProjectCommonException e) {
          if (e.getResponseCode() == HttpStatus.SC_NOT_FOUND) custodianOrgHashTagId = "";
          else throw e;
        }
      }
    }
    return custodianOrgHashTagId;
  }

  private String getEnv(Http.Request request) {

    String uri = request.uri();
    String env;
    if (uri.startsWith("/v1/user") || uri.startsWith("/v2/user")) {
      env = JsonKey.USER;
    } else if (uri.startsWith("/v1/org")) {
      env = JsonKey.ORGANISATION;
    } else if (uri.startsWith("/v1/object")) {
      env = JsonKey.ANNOUNCEMENT;
    } else if (uri.startsWith("/v1/page")) {
      env = JsonKey.PAGE;
    } else if (uri.startsWith("/v1/notification")) {
      env = JsonKey.NOTIFICATION;
    } else if (uri.startsWith("/v1/dashboard")) {
      env = JsonKey.DASHBOARD;
    } else if (uri.startsWith("/v1/badges")) {
      env = JsonKey.BADGES;
    } else if (uri.startsWith("/v1/issuer")) {
      env = BadgingJsonKey.BADGES;
    } else if (uri.startsWith("/v1/role")) {
      env = JsonKey.ROLE;
    } else if (uri.startsWith("/v1/note")) {
      env = JsonKey.NOTE;
    } else if (uri.startsWith("/v1/location")) {
      env = JsonKey.LOCATION;
    } else if (uri.startsWith("/v1/otp")) {
      env = "otp";
    } else if (uri.startsWith("/private/user/v1/password/reset")) {
      env = JsonKey.USER;
    } else {
      env = "miscellaneous";
    }
    return env;
  }
}
