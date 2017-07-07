package util;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LogHelper;
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
	private LogHelper logger = LogHelper.getInstance(Global.class.getName());
	public static ProjectUtil.Environment env;
	private static ConcurrentHashMap<String , Short> apiHeaderIgnoreMap = new ConcurrentHashMap<>();
	public static Map<String,String> apiMap = new HashMap<>(); 
	
	 private class ActionWrapper extends Action.Simple {
		    public ActionWrapper(Action<?> action) {
		      this.delegate = action;
		    }

			@Override
		    public Promise<Result> call(Http.Context ctx) throws java.lang.Throwable {
		      Promise<Result> result = this.delegate.call(ctx);
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
		logger.info("Server started.. with Environment --" + env.name());
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
		logger.info("method call start.." + request.path() + " " + actionMethod + " " + messageId);
		if (ProjectUtil.isStringNullOREmpty(messageId)) {
			UUID uuid = UUID.randomUUID();
			messageId = uuid.toString();
			logger.info("message id is not provided by client.." + messageId);
		}
		ExecutionContext.setRequestId(messageId);
		return new ActionWrapper(super.onRequest(request, actionMethod));
	}
	
	
	 /**
	  *This method will do request data validation for GET method only.
     * As a GET request user must send some key in header.
     */
      public Promise<Result> onDataValidationError(Request request,String errorMessage) {
    	  logger.info("Data error found--");
    	  ResponseCode code = ResponseCode.getResponse(errorMessage);
    	  ResponseCode headerCode = ResponseCode.CLIENT_ERROR;
    	  Response resp = BaseController.createFailureResponse(request, code,headerCode);
	  return   Promise.<Result>pure(Results.ok(Json.toJson(resp)));
      }
	
	
	 /**
     * This method will be used to send the request header missing error message.
     */
    @Override
    public Promise<Result> onError(Http.RequestHeader request, Throwable t) {
    	Response response = null;
    	if(t instanceof ProjectCommonException){
    		response = BaseController.createResponseOnException(request.path(),request.method(), (ProjectCommonException)t);
    	}else {
    	 ProjectCommonException	exception = new ProjectCommonException(ResponseCode.internalError.getErrorCode(),
    				ResponseCode.internalError.getErrorMessage(), ResponseCode.SERVER_ERROR.getResponseCode());
    	 response = BaseController.createResponseOnException(request.path(),request.method(), exception);
    	}
	  return   Promise.<Result>pure(Results.internalServerError(Json.toJson(response)));
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
    apiMap.put("/v1/organisation/create", "api.organisation.create");
    apiMap.put("/v1/organisation/update", "api.organisation.update");
    apiMap.put("/v1/organisation/getOrg", "api.organisation.details");
    apiMap.put("/v1/page/create", "api.page.create");
    apiMap.put("/v1/page/update", "api.page.update");
    apiMap.put("/v1/page", "api.page.get");
    apiMap.put("/v1/page/all/settings", "api.page.settings");
    apiMap.put("/v1/page/assemble", "api.page.assemble");
    apiMap.put("/v1/page/section/create", "api.page.section.create");
    apiMap.put("/v1/page/section/update", "api.page.section.update");
    apiMap.put("/v1/page/section/all/settings", "api.page.section.settings");
    apiMap.put("/v1/page/section", "api.page.section.get");
    apiMap.put("/v1/assessment/save", "api.assessment.save");
    apiMap.put("/v1/assessment/result", "api.assessment.result");
    apiMap.put("/v1/permission/data", "api.role.permission");
  }
}
