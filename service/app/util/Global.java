package util;

import akka.actor.ActorRef;
import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.sunbird.actor.router.RequestRouter;
import org.sunbird.actor.service.SunbirdMWService;
import org.sunbird.actorutil.org.OrganisationClient;
import org.sunbird.actorutil.org.impl.OrganisationClientImpl;
import org.sunbird.actorutil.systemsettings.SystemSettingClient;
import org.sunbird.actorutil.systemsettings.impl.SystemSettingClientImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.BadgingJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.ProjectUtil.Environment;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.HeaderParam;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.learner.util.SchedulerManager;
import org.sunbird.learner.util.Util;
import org.sunbird.models.systemsetting.SystemSetting;
import org.sunbird.telemetry.util.TelemetryUtil;
import play.Application;
import play.GlobalSettings;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Results;

/**
 * This class will work as a filter.
 *
 * @author Manzarul
 */
public class Global extends GlobalSettings {
  public static ProjectUtil.Environment env;

  public static Map<String, Map<String, Object>> requestInfo = new HashMap<>();

  public static String ssoPublicKey = "";
  private static final String version = "v1";
  private final List<String> USER_UNAUTH_STATES =
      Arrays.asList(JsonKey.UNAUTHORIZED, JsonKey.ANONYMOUS);
  private static String custodianOrgHashTagId;
  public static boolean isServiceHealthy = true;

  static {
    init();
  }

  private class ActionWrapper extends Action.Simple {
    public ActionWrapper(Action<?> action) {
      this.delegate = action;
    }

    @Override
    public Promise<Result> call(Http.Context ctx) throws java.lang.Throwable {
      ctx.request().headers();
      Promise<Result> result = checkForServiceHealth(ctx);
      if (result != null) return result;
      ctx.response().setHeader("Access-Control-Allow-Origin", "*");

      // Unauthorized, Anonymous, UserID
      String message = RequestInterceptor.verifyRequestData(ctx);
      // call method to set all the required params for the telemetry event(log)...
      intializeRequestInfo(ctx, message);
      if (!USER_UNAUTH_STATES.contains(message)) {
        ctx.flash().put(JsonKey.USER_ID, message);
        ctx.flash().put(JsonKey.IS_AUTH_REQ, "false");
        for (String uri : RequestInterceptor.restrictedUriList) {
          if (ctx.request().path().contains(uri)) {
            ctx.flash().put(JsonKey.IS_AUTH_REQ, "true");
            break;
          }
        }
        result = delegate.call(ctx);
      } else if (JsonKey.UNAUTHORIZED.equals(message)) {
        result =
            onDataValidationError(
                ctx.request(), message, ResponseCode.UNAUTHORIZED.getResponseCode());
      } else {
        result = delegate.call(ctx);
      }
      return result;
    }
  }

  /**
   * This method will be called on application start up. it will be called only time in it's life
   * cycle.
   *
   * @param app Application
   */
  @Override
  public void onStart(Application app) {
    setEnvironment();
    ssoPublicKey = System.getenv(JsonKey.SSO_PUBLIC_KEY);
    ProjectLogger.log("Server started.. with environment: " + env.name(), LoggerEnum.INFO.name());
    SunbirdMWService.init();
  }

  /**
   * This method will be called on each request.
   *
   * @param request Request
   * @param actionMethod Method
   * @return Action
   */
  @Override
  @SuppressWarnings("rawtypes")
  public Action onRequest(Request request, Method actionMethod) {

    String messageId = request.getHeader(JsonKey.MESSAGE_ID);
    if (StringUtils.isBlank(messageId)) {
      UUID uuid = UUID.randomUUID();
      messageId = uuid.toString();
    }
    ExecutionContext.setRequestId(messageId);
    return new ActionWrapper(super.onRequest(request, actionMethod));
  }

  private void intializeRequestInfo(Context ctx, String userId) {
    Request request = ctx.request();
    String actionMethod = ctx.request().method();
    String messageId = ExecutionContext.getRequestId(); // request.getHeader(JsonKey.MESSAGE_ID);
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
    ctx.flash().put(JsonKey.SIGNUP_TYPE, signType);
    ctx.flash().put(JsonKey.REQUEST_SOURCE, source);
    ExecutionContext context = ExecutionContext.getCurrent();
    Map<String, Object> reqContext = new HashMap<>();
    // set env and channel to the
    String channel = request.getHeader(HeaderParam.CHANNEL_ID.getName());
    if (StringUtils.isBlank(channel)) {
      String custodianOrgHashTagid = getCustodianOrgHashTagId();
      channel =
          (StringUtils.isNotEmpty(custodianOrgHashTagid))
              ? custodianOrgHashTagid
              : JsonKey.DEFAULT_ROOT_ORG_ID;
    }
    reqContext.put(JsonKey.CHANNEL, channel);
    ctx.flash().put(JsonKey.CHANNEL, channel);
    reqContext.put(JsonKey.ENV, getEnv(request));
    reqContext.put(JsonKey.REQUEST_ID, ExecutionContext.getRequestId());
    String appId = request.getHeader(HeaderParam.X_APP_ID.getName());
    // check if in request header X-app-id is coming then that need to
    // be pass in search telemetry.
    if (StringUtils.isNotBlank(appId)) {
      ctx.flash().put(JsonKey.APP_ID, appId);
      reqContext.put(JsonKey.APP_ID, appId);
    }
    // checking device id in headers
    String deviceId = request.getHeader(HeaderParam.X_Device_ID.getName());
    if (StringUtils.isNotBlank(deviceId)) {
      ctx.flash().put(JsonKey.DEVICE_ID, deviceId);
      reqContext.put(JsonKey.DEVICE_ID, deviceId);
    }
    if (!USER_UNAUTH_STATES.contains(userId)) {
      reqContext.put(JsonKey.ACTOR_ID, userId);
      reqContext.put(JsonKey.ACTOR_TYPE, StringUtils.capitalize(JsonKey.USER));
      ctx.flash().put(JsonKey.ACTOR_ID, userId);
      ctx.flash().put(JsonKey.ACTOR_TYPE, JsonKey.USER);
    } else {
      String consumerId = request.getHeader(HeaderParam.X_Consumer_ID.getName());
      if (StringUtils.isBlank(consumerId)) {
        consumerId = JsonKey.DEFAULT_CONSUMER_ID;
      }
      reqContext.put(JsonKey.ACTOR_ID, consumerId);
      reqContext.put(JsonKey.ACTOR_TYPE, StringUtils.capitalize(JsonKey.CONSUMER));
      ctx.flash().put(JsonKey.ACTOR_ID, consumerId);
      ctx.flash().put(JsonKey.ACTOR_TYPE, JsonKey.CONSUMER);
    }
    context.setRequestContext(reqContext);
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.CONTEXT, TelemetryUtil.getTelemetryContext());
    Map<String, Object> additionalInfo = new HashMap<>();
    additionalInfo.put(JsonKey.URL, url);
    additionalInfo.put(JsonKey.METHOD, methodName);
    additionalInfo.put(JsonKey.START_TIME, startTime);

    // additional info contains info other than context info ...
    map.put(JsonKey.ADDITIONAL_INFO, additionalInfo);
    if (StringUtils.isBlank(messageId)) {
      messageId = JsonKey.DEFAULT_CONSUMER_ID;
    }
    ctx.flash().put(JsonKey.REQUEST_ID, messageId);
    if (requestInfo == null) {
      requestInfo = new HashMap<>();
    }
    requestInfo.put(messageId, map);
  }

  private String getEnv(Request request) {

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
    }  else if (uri.startsWith("/v1/role")) {
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

  /**
   * This method will do request data validation for GET method only. As a GET request user must
   * send some key in header.
   *
   * @param request Request
   * @param errorMessage String
   * @return Promise<Result>
   */
  public Promise<Result> onDataValidationError(
      Request request, String errorMessage, int responseCode) {
    ProjectLogger.log("Data error found--" + errorMessage);
    ResponseCode code = ResponseCode.getResponse(errorMessage);
    ResponseCode headerCode = ResponseCode.CLIENT_ERROR;
    Response resp = BaseController.createFailureResponse(request, code, headerCode);
    return Promise.<Result>pure(Results.status(responseCode, Json.toJson(resp)));
  }

  /**
   * This method will be used to send the request header missing error message.
   *
   * @param request Http.RequestHeader
   * @param t Throwable
   * @return Promise<Result>
   */
  @Override
  public Promise<Result> onError(Http.RequestHeader request, Throwable t) {
    ProjectLogger.log(
        "Global: onError called for path = " + request.path(), LoggerEnum.INFO.name());
    Response response = null;
    ProjectCommonException commonException = null;
    if (t instanceof ProjectCommonException) {
      ProjectLogger.log(
          "Global:onError: ProjectCommonException occurred for path = " + request.path(),
          LoggerEnum.INFO.name());
      commonException = (ProjectCommonException) t;
      response =
          BaseController.createResponseOnException(
              request.path(), request.method(), (ProjectCommonException) t);
    } else if (t instanceof akka.pattern.AskTimeoutException) {
      ProjectLogger.log(
          "Global:onError: AskTimeoutException occurred for path = " + request.path(),
          LoggerEnum.INFO.name());
      commonException =
          new ProjectCommonException(
              ResponseCode.actorConnectionError.getErrorCode(),
              ResponseCode.actorConnectionError.getErrorMessage(),
              ResponseCode.SERVER_ERROR.getResponseCode());
    } else {
      ProjectLogger.log(
          "Global:onError: Unknown exception occurred for path = " + request.path(),
          LoggerEnum.INFO.name());
      commonException =
          new ProjectCommonException(
              ResponseCode.internalError.getErrorCode(),
              ResponseCode.internalError.getErrorMessage(),
              ResponseCode.SERVER_ERROR.getResponseCode());
    }
    response =
        BaseController.createResponseOnException(request.path(), request.method(), commonException);
    return Promise.<Result>pure(Results.internalServerError(Json.toJson(response)));
  }

  /**
   * This method will identify the environment and update with enum.
   *
   * @return Environment
   */
  public Environment setEnvironment() {

    if (play.Play.isDev()) {
      return env = Environment.dev;
    } else if (play.Play.isTest()) {
      return env = Environment.qa;
    } else {
      return env = Environment.prod;
    }
  }

  /**
   * Method to get the response id on basis of request path.
   *
   * @param requestPath
   * @return
   */
  public static String getResponseId(String requestPath) {

    String path = requestPath;
    final String ver = "/" + version;
    final String ver2 = "/" + JsonKey.VERSION_2;
    path = path.trim();
    StringBuilder builder = new StringBuilder("");
    if (path.startsWith(ver) || path.startsWith(ver2)) {
      String requestUrl = (path.split("\\?"))[0];
      if (requestUrl.contains(ver)) {
        requestUrl = requestUrl.replaceFirst(ver, "api");
      } else if (requestUrl.contains(ver2)) {
        requestUrl = requestUrl.replaceFirst(ver2, "api");
      }

      String[] list = requestUrl.split("/");
      for (String str : list) {
        if (str.matches("[A-Za-z]+")) {
          builder.append(str).append(".");
        }
      }
      builder.deleteCharAt(builder.length() - 1);
    } else {
      if ("/health".equalsIgnoreCase(path)) {
        builder.append("api.all.health");
      }
    }
    return builder.toString();
  }

  private static void init() {

    Util.checkCassandraDbConnections(JsonKey.SUNBIRD);
    Util.checkCassandraDbConnections(JsonKey.SUNBIRD_PLUGIN);
    SchedulerManager.schedule();

    // Run quartz scheduler in a separate thread as it waits for 4 minutes
    // before scheduling various jobs.
    new Thread(() -> org.sunbird.common.quartz.scheduler.SchedulerManager.getInstance()).start();
  }

  private static String getCustodianOrgHashTagId() {
    synchronized (Global.class) {
      if (custodianOrgHashTagId == null) {
        try {
          // Get custodian org ID
          SystemSettingClient sysSettingClient = SystemSettingClientImpl.getInstance();
          ActorRef sysSettingActorRef =
              RequestRouter.getActor(ActorOperations.GET_SYSTEM_SETTING.getValue());
          SystemSetting systemSetting =
              sysSettingClient.getSystemSettingByField(
                  sysSettingActorRef, JsonKey.CUSTODIAN_ORG_ID);

          // Get hash tag ID of custodian org
          OrganisationClient orgClient = new OrganisationClientImpl();
          ActorRef orgActorRef = RequestRouter.getActor(ActorOperations.GET_ORG_DETAILS.getValue());
          custodianOrgHashTagId =
              orgClient.getOrgById(orgActorRef, systemSetting.getValue()).getHashTagId();
        } catch (ProjectCommonException e) {
          if (e.getResponseCode() == HttpStatus.SC_NOT_FOUND) custodianOrgHashTagId = "";
          else throw e;
        }
      }
    }
    return custodianOrgHashTagId;
  }

  public Promise<Result> checkForServiceHealth(Http.Context ctx) {
    if (Boolean.parseBoolean((ProjectUtil.getConfigValue(JsonKey.SUNBIRD_HEALTH_CHECK_ENABLE)))
        && !ctx.request().path().endsWith(JsonKey.HEALTH)) {
      if (!isServiceHealthy) {
        ResponseCode headerCode = ResponseCode.SERVICE_UNAVAILABLE;
        Response resp = BaseController.createFailureResponse(ctx.request(), headerCode, headerCode);
        return Promise.<Result>pure(
            Results.status(ResponseCode.SERVICE_UNAVAILABLE.getResponseCode(), Json.toJson(resp)));
      }
    }
    return null;
  }
}
