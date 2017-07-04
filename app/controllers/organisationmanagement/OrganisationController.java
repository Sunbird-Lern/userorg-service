package controllers.organisationmanagement;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LogHelper;
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
 * This controller will handle all the api
 * related to course , add course , published course,
 * update course , search course.
 * @author Amit Kumar
 */
public class OrganisationController extends BaseController{

private LogHelper logger = LogHelper.getInstance(OrganisationController.class.getName());


	/**
	 * This method will do the registration process.
	 * registered org data will be store inside cassandra db.
	 * @return Promise<Result>
	 */
	public Promise<Result> createOrg() {
		try {
			JsonNode requestData = request().body().asJson();
			logger.info(" get organisation request data=" + requestData);
			Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
			RequestValidator.validateCreateOrganisation(reqObj);
			reqObj.setOperation(ActorOperations.CREATE_ORG.getValue());
	        reqObj.setRequest_id(ExecutionContext.getRequestId());
	        reqObj.setEnv(getEnvironment());
			HashMap<String, Object> innerMap = new HashMap<>();
			innerMap.put(JsonKey.ORGANISATION, reqObj.getRequest());
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
	 * This method will update org data.  
	 * @return Promise<Result>
	 */
	public Promise<Result> updateOrg() {
		try {
			JsonNode requestData = request().body().asJson();
			logger.info(" get user update profile data=" + requestData);
			Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
			RequestValidator.validateUpdateUser(reqObj);
			reqObj.setOperation(ActorOperations.UPDATE_ORG.getValue());
	        reqObj.setRequest_id(ExecutionContext.getRequestId());
	        reqObj.setEnv(getEnvironment());
			HashMap<String, Object> innerMap = new HashMap<>();
			innerMap.put(JsonKey.ORGANISATION, reqObj.getRequest());
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
	public Promise<Result> getOrgDetails() {
		try {
			JsonNode requestData = request().body().asJson();
			logger.info(" get user change password data=" + requestData);
			Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
			reqObj.setOperation(ActorOperations.GET_ORG_DETAILS.getValue());
	        reqObj.setRequest_id(ExecutionContext.getRequestId());
	        reqObj.setEnv(getEnvironment());
			HashMap<String, Object> innerMap = new HashMap<>();
			innerMap.put(JsonKey.ORGANISATION, reqObj.getRequest());
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
