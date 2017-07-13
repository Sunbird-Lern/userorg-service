/**
 * 
 */
package controllers.usermanagement;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.LogHelper;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.HeaderParam;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestValidator;

import com.fasterxml.jackson.databind.JsonNode;

import akka.util.Timeout;
import controllers.BaseController;
import play.libs.F.Promise;
import play.mvc.Result;

/**
 * This controller will handle all the request and responses
 * for user management.
 * @author Manzarul
 */
public class UserController  extends BaseController{
	private LogHelper logger = LogHelper.getInstance(UserController.class.getName());
	
	/**
	 * This method will do the registration process.
	 * registered user data will be store inside cassandra db.
	 * @return Promise<Result>
	 */
	public Promise<Result> createUser() {
		try {
			JsonNode requestData = request().body().asJson();
			logger.info(" get user registration request data=" + requestData);
			ProjectLogger.log(" get user registration request data=" + requestData, LoggerEnum.INFO.name());
			Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
			RequestValidator.validateCreateUser(reqObj);
			reqObj.setOperation(ActorOperations.CREATE_USER.getValue());
	        reqObj.setRequest_id(ExecutionContext.getRequestId());
	        reqObj.setEnv(getEnvironment());
			HashMap<String, Object> innerMap = new HashMap<>();
			innerMap.put(JsonKey.USER, reqObj.getRequest());
			//reqObj.getRequest().put(JsonKey.USERNAME, reqObj.getRequest().get(JsonKey.EMAIL));
			reqObj.setRequest(innerMap);
			Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
			Promise<Result> res = actorResponseHandler(getRemoteActor(),reqObj,timeout,null,request());
			return res;
		} catch (Exception e) {
			return Promise.<Result> pure(createCommonExceptionResponse(e,request()));
		}
	}
	
	/**
	 * This method will update user profile data. user can update all the data 
	 * except email. 
	 * @return Promise<Result>
	 */
	public Promise<Result> updateUserProfile() {
		try {
			JsonNode requestData = request().body().asJson();
			logger.info(" get user update profile data=" + requestData);
			ProjectLogger.log(" get user update profile data=" + requestData, LoggerEnum.INFO.name());
			Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
			RequestValidator.validateUpdateUser(reqObj);
			reqObj.setOperation(ActorOperations.UPDATE_USER.getValue());
	        reqObj.setRequest_id(ExecutionContext.getRequestId());
	        reqObj.setEnv(getEnvironment());
			HashMap<String, Object> innerMap = new HashMap<>();
			innerMap.put(JsonKey.USER, reqObj.getRequest());
			innerMap.put(JsonKey.REQUESTED_BY,getUserIdByAuthToken(request().getHeader(HeaderParam.X_Authenticated_Userid.getName())));
			reqObj.setRequest(innerMap);
			Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
			Promise<Result> res = actorResponseHandler(getRemoteActor(),reqObj,timeout,null,request());
			return res;
		} catch (Exception e) {
			return Promise.<Result> pure(createCommonExceptionResponse(e,request()));
		}
	}
	
	
	/**
	 * This method will do the user authentication based on login type key.
	 * login can be done with following ways (simple login , Google plus login , Facebook login , Aadhaar login)
	 * @return Promise<Result>
	 */
	public Promise<Result> login() {
		try {
			JsonNode requestData = request().body().asJson();
			logger.info(" get user login data=" + requestData);
			ProjectLogger.log(" get user login data=" + requestData, LoggerEnum.INFO.name());
			Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
			RequestValidator.validateUserLogin(reqObj);
			reqObj.setOperation(ActorOperations.LOGIN.getValue());
	        reqObj.setRequest_id(ExecutionContext.getRequestId());
	        reqObj.setEnv(getEnvironment());
			HashMap<String, Object> innerMap = new HashMap<>();
			innerMap.put(JsonKey.USER, reqObj.getRequest());
			reqObj.setRequest(innerMap);
			Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
			Promise<Result> res = actorResponseHandler(getRemoteActor(),reqObj,timeout,null,request());
			return res;
		} catch (Exception e) {
			return Promise.<Result> pure(createCommonExceptionResponse(e,request()));
		}
	}
	
	/**
	 * This method will invalidate user auth token .
	 * @return Promise<Result>
	 */
	public Promise<Result> logout() {
		try {
			JsonNode requestData = request().body().asJson();
			logger.info(" get user logout data=" + requestData);
			ProjectLogger.log(" get user logout data=" + requestData, LoggerEnum.INFO.name());
			Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
			reqObj.setOperation(ActorOperations.LOGOUT.getValue());
	        reqObj.setRequest_id(ExecutionContext.getRequestId());
	        reqObj.setEnv(getEnvironment());
	        HashMap<String, Object> innerMap = new HashMap<>();
			innerMap.put(JsonKey.USER, reqObj.getRequest());
			innerMap.put(JsonKey.REQUESTED_BY,getUserIdByAuthToken(request().getHeader(HeaderParam.X_Authenticated_Userid.getName())));
			innerMap.put(JsonKey.AUTH_TOKEN,request().getHeader(HeaderParam.X_Authenticated_Userid.getName()));
	        reqObj.setRequest(innerMap);
			Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
			Promise<Result> res = actorResponseHandler(getRemoteActor(),reqObj,timeout,null,request());
			return res;
		} catch (Exception e) {
			return Promise.<Result> pure(createCommonExceptionResponse(e,request()));
		}
	}
	
	/**
	 * This method will allow user to change their password.
	 * @return Promise<Result>
	 */
	public Promise<Result> changePassword() {
		try {
			JsonNode requestData = request().body().asJson();
			logger.info(" get user change password data=" + requestData);
			ProjectLogger.log(" get user change password data=" + requestData, LoggerEnum.INFO.name());
			Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
			RequestValidator.validateChangePassword(reqObj);
			reqObj.setOperation(ActorOperations.CHANGE_PASSWORD.getValue());
	        reqObj.setRequest_id(ExecutionContext.getRequestId());
	        reqObj.setEnv(getEnvironment());
			HashMap<String, Object> innerMap = new HashMap<>();
			innerMap.put(JsonKey.USER, reqObj.getRequest());
			reqObj.getRequest().put(JsonKey.USER_ID,getUserIdByAuthToken(request().getHeader(HeaderParam.X_Authenticated_Userid.getName())));
			innerMap.put(JsonKey.REQUESTED_BY,getUserIdByAuthToken(request().getHeader(HeaderParam.X_Authenticated_Userid.getName())));
			reqObj.setRequest(innerMap);
			Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
			Promise<Result> res = actorResponseHandler(getRemoteActor(),reqObj,timeout,null,request());
			return res;
		} catch (Exception e) {
			return Promise.<Result> pure(createCommonExceptionResponse(e,request()));
		}
	}
	
	/**
	 * This method will provide user profile details based on requested userId.
	 * @return Promise<Result>
	 */
	public Promise<Result> getUserProfile(String userId) {
		try {
			JsonNode requestData = request().body().asJson();
			logger.info(" get user profile data by id = " + requestData);
			ProjectLogger.log(" get user profile data by id =" + requestData, LoggerEnum.INFO.name());
			Request reqObj = new Request();
			reqObj.setOperation(ActorOperations.GET_PROFILE.getValue());
	        reqObj.setRequest_id(ExecutionContext.getRequestId());
	        reqObj.setEnv(getEnvironment());
			HashMap<String, Object> innerMap = new HashMap<>();
			reqObj.getRequest().put(JsonKey.USER_ID, userId);
			innerMap.put(JsonKey.USER, reqObj.getRequest());
			innerMap.put(JsonKey.REQUESTED_BY,getUserIdByAuthToken(request().getHeader(HeaderParam.X_Authenticated_Userid.getName())));
			reqObj.setRequest(innerMap);
			Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
			Promise<Result> res = actorResponseHandler(getRemoteActor(),reqObj,timeout,null,request());
			return res;
		} catch (Exception e) {
			return Promise.<Result> pure(createCommonExceptionResponse(e,request()));
		}
	}
	
	/**
     * This method will provide complete role details list.
     * @return Promise<Result>
     */
  public Promise<Result> getRoles() {
    try {
      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.GET_ROLES.getValue());
      reqObj.setRequest_id(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      reqObj.setRequest(innerMap);
      Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
      Promise<Result> res =
          actorResponseHandler(getRemoteActor(), reqObj, timeout, null, request());
      return res;
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

	
	
	/**
     * Method to verify user existence in our db.
     * @return Promise<Result>
     */
    public Promise<Result> getUserDetailsByLoginId() {
        try {
            JsonNode requestData = request().body().asJson();
            logger.info(" verify user details by loginId data =" + requestData);
            ProjectLogger.log(" verify user details by loginId data =" + requestData, LoggerEnum.INFO.name());
            Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
            RequestValidator.validateVerifyUser(reqObj);
            reqObj.setOperation(ActorOperations.GET_USER_DETAILS_BY_LOGINID.getValue());
            reqObj.setRequest_id(ExecutionContext.getRequestId());
            reqObj.setEnv(getEnvironment());
            HashMap<String, Object> innerMap = new HashMap<>();
            innerMap.put(JsonKey.USER, reqObj.getRequest());
            innerMap.put(JsonKey.REQUESTED_BY,getUserIdByAuthToken(request().getHeader(HeaderParam.X_Authenticated_Userid.getName())));
            reqObj.setRequest(innerMap);
            Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
            Promise<Result> res = actorResponseHandler(getRemoteActor(),reqObj,timeout,null,request());
            return res;
        } catch (Exception e) {
            return Promise.<Result> pure(createCommonExceptionResponse(e,request()));
        }
    }
}
