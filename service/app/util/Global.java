
/**
 * 
 */
package util;

import controllers.BaseController;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.ProjectUtil.Environment;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.responsecode.ResponseCode;
import play.Application;
import play.GlobalSettings;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Results;

/**
 * This class will work as a filter.
 * 
 * @author Manzarul
 *
 */
public class Global extends GlobalSettings {

  public static ProjectUtil.Environment env;
 
  public static Map<String, String> apiMap = new HashMap<>();
  private static ConcurrentHashMap<String, Boolean> apiUserAccessToken = new ConcurrentHashMap<>();
  public static String ssoPublicKey = "";
  private class ActionWrapper extends Action.Simple {
    public ActionWrapper(Action<?> action) {
      this.delegate = action;
    }

    @Override
    public Promise<Result> call(Http.Context ctx) throws java.lang.Throwable {
      long startTime = System.currentTimeMillis();
      ProjectLogger.log("Learning Service Call start  for  api ==" + ctx.request().path()
          + " start time " + startTime, LoggerEnum.PERF_LOG);
      Promise<Result> result = null;
      Http.Response response = ctx.response();
      response.setHeader("Access-Control-Allow-Origin", "*");
      
      String message = RequestInterceptor.verifyRequestData(ctx.request());
      if (message.contains("{userId}")) {
        ctx.flash().put(JsonKey.USER_ID, message.replace("{userId}", ""));
        ctx.flash().put(JsonKey.IS_AUTH_REQ, "false");
        for(String uri : RequestInterceptor.restrictedUriList){
          if(ctx.request().path().contains(uri)){
            ctx.flash().put(JsonKey.IS_AUTH_REQ, "true");
            break;
          }
        }
        result = delegate.call(ctx);
      } else if (!ProjectUtil.isStringNullOREmpty(message)) {
        result = onDataValidationError(ctx.request(), message,ResponseCode.UNAUTHORIZED.getResponseCode());
      } else {
        result = delegate.call(ctx);
      }
      ProjectLogger.log("Learning Service Call Ended  for  api ==" + ctx.request().path()
          + " end time " + System.currentTimeMillis() + "  Time taken "
          + (System.currentTimeMillis() - startTime), LoggerEnum.PERF_LOG);
      return result;
    }
  }


  /**
   * 
   * @author Manzarul
   *
   */
  public enum RequestMethod {
    GET, POST, PUT, DELETE, PATCH;
  }

  /**
   * This method will be called on application start up. it will be called only time in it's life
   * cycle.
   * 
   * @param app Application
   */
  public void onStart(Application app) {

    setEnvironment();
    createApiMap();
    ssoPublicKey = System.getenv(JsonKey.SSO_PUBLIC_KEY);
    ProjectLogger.log("Server started.. with Environment --" + env.name(), LoggerEnum.INFO.name());
  }

  /**
   * This method will be called on each request.
   * 
   * @param request Request
   * @param actionMethod Method
   * @return Action
   */
  @SuppressWarnings("rawtypes")
  public Action onRequest(Request request, Method actionMethod) {

    String messageId = request.getHeader(JsonKey.MESSAGE_ID);
    ProjectLogger.log("method call start.." + request.path() + " " + actionMethod + " " + messageId,
        LoggerEnum.INFO.name());
    if (ProjectUtil.isStringNullOREmpty(messageId)) {
      UUID uuid = UUID.randomUUID();
      messageId = uuid.toString();
      ProjectLogger.log("message id is not provided by client.." + messageId);
    }
    ExecutionContext.setRequestId(messageId);
    return new ActionWrapper(super.onRequest(request, actionMethod));
  }


  /**
   * This method will do request data validation for GET method only. As a GET request user must
   * send some key in header.
   * 
   * @param request Request
   * @param errorMessage String
   * @return Promise<Result>
   */
  public Promise<Result> onDataValidationError(Request request, String errorMessage,int responseCode) {
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

    Response response = null;
    ProjectCommonException commonException = null;
    if (t instanceof ProjectCommonException) {
      commonException = (ProjectCommonException) t;
      response = BaseController.createResponseOnException(request.path(), request.method(),
          (ProjectCommonException) t);
    } else if (t instanceof akka.pattern.AskTimeoutException) {
      commonException = new ProjectCommonException(ResponseCode.actorConnectionError.getErrorCode(),
          ResponseCode.actorConnectionError.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    } else {
      commonException = new ProjectCommonException(ResponseCode.internalError.getErrorCode(),
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
   * This method will create api url and id for the url. this id we will use while sending api
   * response to client.
   */
  private static void createApiMap() {

    apiMap.put("/v1/user/courses/enroll", "api.course.enroll");
    apiMap.put("/v1/course/update", "api.course.update");
    apiMap.put("/v1/course/publish", "api.course.publish");
    apiMap.put("/v1/course/search", "api.course.search");
    apiMap.put("/v1/course/delete", "api.course.delete");
    apiMap.put("/v1/course", "api.course.getbycourseid");
    apiMap.put("/v1/course/recommended/courses", "api.recomend");
    apiMap.put("/v1/user/courses", "api.course.getbyuser");
    apiMap.put("/v1/user/content/state", "api.content.state");
    apiMap.put("/v1/user/create", "api.user.create");
    apiMap.put("/v1/user/update", "api.user.update");
    apiMap.put("/v1/user/login", "api.user.login");
    apiMap.put("/v1/user/logout", "api.user.logout");
    apiMap.put("/v1/user/changepassword", "api.user.cp");
    apiMap.put("/v1/user/getprofile", "api.user.profile");
    apiMap.put("/v1/course/create", "api.user.create");
    apiMap.put("/v1/org/create", "api.org.create");
    apiMap.put("/v1/org/update", "api.org.update");
    apiMap.put("/v1/org/status/update", "api.org.update.status");
    apiMap.put("/v1/org/member/join", "api.org.member.join");
    apiMap.put("/v1/org/member/approve", "api.org.member.approve");
    apiMap.put("/v1/org/member/reject", "api.org.member.reject");
    apiMap.put("/v1/org/member/add", "api.org.member.add");
    apiMap.put("/v1/org/member/remove", "api.org.member.remove");
    apiMap.put("/v1/org/approve", "api.org.approve");
    apiMap.put("/v1/org/read", "api.org.read");
    apiMap.put("/v1/page/create", "api.page.create");
    apiMap.put("/v1/page/update", "api.page.update");
    apiMap.put("/v1/page/read", "api.page.get");
    apiMap.put("/v1/page/all/settings", "api.page.settings");
    apiMap.put("/v1/page/assemble", "api.page.assemble");
    apiMap.put("/v1/page/section/create", "api.page.section.create");
    apiMap.put("/v1/page/section/update", "api.page.section.update");
    apiMap.put("/v1/page/section/list", "api.page.section.settings");
    apiMap.put("/v1/page/section/read", "api.page.section.get");
    apiMap.put("/v1/assessment/update", "api.assessment.save");
    apiMap.put("/v1/assessment/result/read", "api.assessment.result");
    apiMap.put("/v1/role/read", "api.role.read");
    apiMap.put("/v1/user/getuser", "api.user.getuser");
    apiMap.put("/v1/user/block", "api.user.inactive");
    apiMap.put("/v1/user/unblock", "api.user.active");
    apiMap.put("/v1/user/assign/role", "api.user.assign.role");
    apiMap.put("/v1/course/batch/create", "api.course.batch.create");
    apiMap.put("/v1/course/batch/update", "api.course.batch.update");
    apiMap.put("/v1/course/batch/users/add", "api.course.batch.user.add");
    apiMap.put("/v1/course/batch/users/remove", "api.course.batch.user.remove");
    apiMap.put("/v1/course/batch/read", "api.course.batch.read");
    apiMap.put("/v1/dashboard/creation/org", "api.sunbird.dashboard.org.creation");
    apiMap.put("/v1/dashboard/consumption/org", "api.sunbird.dashboard.org.consumption");
    apiMap.put("/v1/dashboard/progress/course", "api.sunbird.dashboard.course.admin");
    apiMap.put("/v1/dashboard/consumption/course", "api.sunbird.dashboard.course.consumption");
    apiMap.put("/v1/dashboard/creation/user", "api.sunbird.dashboard.user.creation");
    apiMap.put("/v1/dashboard/consumption/user", "api.sunbird.dashboard.user.consumption");
    apiMap.put("/v1/user/search", "api.user.search");
    apiMap.put("/v1/org/search", "api.org.search");
    apiMap.put("/v1/user/read", "api.user.read");
    apiMap.put("/v1/notification/email", "api.notification.email");
    apiMap.put("/v1/file/upload", "api.file.upload");
    apiMap.put("/v1/user/badges/add", "api.user.badge.add");
    apiMap.put("/v1/badges/list", "api.badge.list");
    apiMap.put("/v1/health", "all.service.health.api");
    apiMap.put("/v1/learner/health", "learner.service.health.api");
    apiMap.put("/v1/actor/health", "actor.service.health.api");
    apiMap.put("/v1/es/health", "es.service.health.api");
    apiMap.put("/v1/cassandra/health", "cassandra.service.health.api");
    apiMap.put("/v1/ekstep/health", "ekstep.service.health.api");
    apiMap.put("/v1/org/type/create", "api.sunbird.org.type.create");
    apiMap.put("/v1/org/type/update", "api.sunbird.org.type.update");
    apiMap.put("/v1/org/type/list", "api.sunbird.org.type.list");
    apiMap.put("/v1/note/create", "api.note.create");
    apiMap.put("/v1/note/read", "api.note.read");
    apiMap.put("/v1/note/update", "api.note.update");
    apiMap.put("/v1/note/search", "api.note.search");
    apiMap.put("/v1/note/delete", "api.note.delete");
    apiMap.put("/v1/upload/status/", "api.upload.status");
    apiMap.put("/v1/bulk/user/upload", "api.bulk.user.upload");
    apiMap.put("/v1/user/upload", "api.user.upload");
    apiMap.put("/v1/user/mediatype/list", "api.user.mediatypes.list");
    apiMap.put("/health", "api.all.health");
    apiMap.put("/v1/audit/history", "api.audit.history");
    apiMap.put("/v1/user/forgotpassword", "api.user.forgotpassword");
  }
}
