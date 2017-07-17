package util;
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
import org.sunbird.common.request.HeaderParam;
import org.sunbird.common.responsecode.ResponseCode;

import controllers.BaseController;
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
 * @author Manzarul
 *
 */
public class Global extends GlobalSettings {
    public static ProjectUtil.Environment env;
    private static ConcurrentHashMap<String , Short> apiHeaderIgnoreMap = new ConcurrentHashMap<>();
    public static Map<String,String> apiMap = new HashMap<>(); 
    
     private class ActionWrapper extends Action.Simple {
            public ActionWrapper(Action<?> action) {
              this.delegate = action;
            }

            @Override
            public Promise<Result> call(Http.Context ctx) throws java.lang.Throwable {
              //Promise<Result> result = this.delegate.call(ctx);
              Promise<Result> result = null;
              Http.Response response = ctx.response();
              response.setHeader("Access-Control-Allow-Origin", "*");
              String message = verifyRequestData(ctx.request(),RequestMethod.GET.name());
                if (!ProjectUtil.isStringNullOREmpty(message)) {
                    result = onDataValidationError(ctx.request(), message);
                } else {
                    result = delegate.call(ctx);
                }
              return result;
            }
          }

    
    /**
     * 
     * @author Manzarul
     *
     */
    public enum RequestMethod {
        GET,POST,PUT,DELETE;
    }
    /**
     * This method will be called on application start up.
     * it will be called only time in it's life cycle.
     * @param app Application
     */
    public void onStart(Application app) {
           setEnvironment();
           addApiListToMap();
           createApiMap();
           ProjectLogger.log("Server started.. with Environment --" + env.name(), LoggerEnum.INFO.name());
    }
    
    /**
     * This method will be called on each request.
     * @param request Request
     * @param actionMethod Method
     * @return Action
     */
    @SuppressWarnings("rawtypes")
    public Action onRequest(Request request, Method actionMethod) {
        String messageId = request.getHeader(JsonKey.MESSAGE_ID);
        ProjectLogger.log("method call start.." + request.path() + " " + actionMethod + " " + messageId, LoggerEnum.INFO.name());
        if (ProjectUtil.isStringNullOREmpty(messageId)) {
            UUID uuid = UUID.randomUUID();
            messageId = uuid.toString();
            ProjectLogger.log("message id is not provided by client.." + messageId);
        }
        ExecutionContext.setRequestId(messageId);
        return new ActionWrapper(super.onRequest(request, actionMethod));
    }
    
    
     /**
      *This method will do request data validation for GET method only.
     * As a GET request user must send some key in header.
     */
  public Promise<Result> onDataValidationError(Request request, String errorMessage) {
    ProjectLogger.log("Data error found--");
    ResponseCode code = ResponseCode.getResponse(errorMessage);
    ResponseCode headerCode = ResponseCode.CLIENT_ERROR;
    Response resp = BaseController.createFailureResponse(request, code, headerCode);
    return Promise.<Result>pure(Results.status(headerCode.getResponseCode(), Json.toJson(resp)));
  }
    
    
     /**
     * This method will be used to send the request header missing error message.
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
	 * This method will do the get request header mandatory value check
	 * it will check all the mandatory value under header , if any value is
	 * missing then it will send missing key name in response. 
	 * @param request Request
	 * @param method String 
	 * @return String
	 */
	@SuppressWarnings("deprecation")
	private String verifyRequestData(Request request, String method) {
		if (ProjectUtil.isStringNullOREmpty(request.getHeader(HeaderParam.X_Consumer_ID.getName().toLowerCase()))) {
			return ResponseCode.customerIdRequired.getErrorCode();
		} else if (ProjectUtil.isStringNullOREmpty(request.getHeader(HeaderParam.X_Device_ID.getName().toLowerCase()))) {
			return ResponseCode.deviceIdRequired.getErrorCode();
		} else if (ProjectUtil.isStringNullOREmpty(request.getHeader(HeaderParam.ts.getName()))) {
			return ResponseCode.timeStampRequired.getErrorCode();
		} else if (!apiHeaderIgnoreMap.containsKey(request.path())) {
			if (ProjectUtil.isStringNullOREmpty(request.getHeader(HeaderParam.X_Authenticated_Userid.getName()))) {
				return ResponseCode.authTokenRequired.getErrorCode();
			}
			String userId = AuthenticationHelper.verifyUserAccesToken(request.getHeader(HeaderParam.X_Authenticated_Userid.getName()));
			if (ProjectUtil.isStringNullOREmpty(userId)) {
				return ResponseCode.invalidAuthToken.getErrorCode();
			} 
		}
		return "";
	}
	
	/**
	 * This method will identify the environment and 
	 * update with enum.
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
	 * 
	 */
	private void addApiListToMap() {
		short var =1;
		apiHeaderIgnoreMap.put("/v1/user/create", var);
		apiHeaderIgnoreMap.put("/v1/user/login", var);
		apiHeaderIgnoreMap.put("/v1/user/getuser", var);
	}

 /**
  * This method will create api url and id for the url.
  * this id we will use while sending api response to client.   
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
    apiMap.put("/v1/user/getuser", "api.role.getuser");
  }
}
