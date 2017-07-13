/**
 * 
 */
package controllers.pagemanagement;

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
 * This controller will handle all the request related 
 * to page api's.
 * @author Manzarul
 */
public class PageController  extends BaseController{
private LogHelper logger = LogHelper.getInstance(PageController.class.getName());
	
	/**
	 * This method will allow admin to create a page for view.
	 * @return Promise<Result>
	 */
	public Promise<Result> createPage() {
		try {
			JsonNode requestData = request().body().asJson();
			logger.info("getting create page data request=" + requestData);
			Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
			RequestValidator.validateCreatePage(reqObj);
			reqObj.setOperation(ActorOperations.CREATE_PAGE.getValue());
	        reqObj.setRequest_id(ExecutionContext.getRequestId());
	        reqObj.getRequest().put(JsonKey.CREATED_BY,getUserIdByAuthToken(request().getHeader(HeaderParam.X_Authenticated_Userid.getName())));
	        reqObj.setEnv(getEnvironment());
			HashMap<String, Object> map = new HashMap<>();
			map.put(JsonKey.PAGE, reqObj.getRequest());
			reqObj.setRequest(map);
			Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
			Promise<Result> res = actorResponseHandler(getRemoteActor(),reqObj,timeout,null,request());
			return res;
		} catch (Exception e) {
			return Promise.<Result> pure(createCommonExceptionResponse(e,request()));
		}
	}
     
    
	/**
	 * This method will allow admin to update already created page data.
	 * @return Promise<Result>
	 */
	public Promise<Result> updatePage() {
		try {
			JsonNode requestData = request().body().asJson();
			logger.info("getting update page data request=" + requestData);
			Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
			RequestValidator.validateUpdatepage(reqObj);
			reqObj.setOperation(ActorOperations.UPDATE_PAGE.getValue());
	        reqObj.setRequest_id(ExecutionContext.getRequestId());
	        reqObj.getRequest().put(JsonKey.UPDATED_BY,getUserIdByAuthToken(request().getHeader(HeaderParam.X_Authenticated_Userid.getName())));
	        reqObj.setEnv(getEnvironment());
			HashMap<String, Object> map = new HashMap<>();
			map.put(JsonKey.PAGE, reqObj.getRequest());
			reqObj.setRequest(map);
			Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
			Promise<Result> res = actorResponseHandler(getRemoteActor(),reqObj,timeout,null,request());
			return res;
		} catch (Exception e) {
			return Promise.<Result> pure(createCommonExceptionResponse(e,request()));
		}
	}
	
	/**
	 * This method will provide particular page setting data.
	 * @return Promise<Result>
	 */
	public Promise<Result> getPageSetting(String pageId) {
		try {
			logger.info("getting data for particular page settings=" + pageId);
			Request reqObj = new Request();
			reqObj.setOperation(ActorOperations.GET_PAGE_SETTING.getValue());
	        reqObj.setRequest_id(ExecutionContext.getRequestId());
	        reqObj.setEnv(getEnvironment());
	        reqObj.getRequest().put(JsonKey.ID, pageId);
			Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
			Promise<Result> res = actorResponseHandler(getRemoteActor(),reqObj,timeout,null,request());
			return res;
		} catch (Exception e) {
			return Promise.<Result> pure(createCommonExceptionResponse(e,request()));
		}
	}

	
	/**
	 * This method will provide completed data for all pages which is saved in 
	 * cassandra dac.
	 * @return Promise<Result>
	 */
	public Promise<Result> getPageSettings() {
		try {
			logger.info("getting page settings api called=");
			Request reqObj = new Request();
			reqObj.setOperation(ActorOperations.GET_PAGE_SETTINGS.getValue());
	        reqObj.setRequest_id(ExecutionContext.getRequestId());
	        reqObj.setEnv(getEnvironment());
			Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
			Promise<Result> res = actorResponseHandler(getRemoteActor(),reqObj,timeout,null,request());
			return res;
		} catch (Exception e) {
			return Promise.<Result> pure(createCommonExceptionResponse(e,request()));
		}
	}
	
	/**
	 * This method will provide completed data for a particular page.
	 * @return Promise<Result>
	 */
	public Promise<Result> getPageData() {
		try {
		    JsonNode requestData = request().body().asJson();
			logger.info("requested data for get page  =" + requestData);
			Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
            RequestValidator.validateGetPageData(reqObj);
			reqObj.setOperation(ActorOperations.GET_PAGE_DATA.getValue());
	        reqObj.setRequest_id(ExecutionContext.getRequestId());
	        reqObj.setEnv(getEnvironment());
            reqObj.getRequest().put(JsonKey.CREATED_BY,getUserIdByAuthToken(request().getHeader(HeaderParam.X_Authenticated_Userid.getName())));
            HashMap<String, Object> map = new HashMap<>();
            map.put(JsonKey.PAGE, reqObj.getRequest());
            reqObj.setRequest(map);
			Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
			Promise<Result> res = actorResponseHandler(getRemoteActor(),reqObj,timeout,null,request());
			return res;
		} catch (Exception e) {
			return Promise.<Result> pure(createCommonExceptionResponse(e,request()));
		}
	}
	
	/**
	 * This method will allow admin to create sections for page view
	 * @return Promise<Result>
	 */
	public Promise<Result> createPageSection() {
		try {
			JsonNode requestData = request().body().asJson();
			logger.info("getting create page section data request=" + requestData);
			Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
			RequestValidator.validateCreateSection(reqObj);
			reqObj.setOperation(ActorOperations.CREATE_SECTION.getValue());
	        reqObj.setRequest_id(ExecutionContext.getRequestId());
	        reqObj.getRequest().put(JsonKey.CREATED_BY,getUserIdByAuthToken(request().getHeader(HeaderParam.X_Authenticated_Userid.getName())));
	        reqObj.setEnv(getEnvironment());
			HashMap<String, Object> map = new HashMap<>();
			map.put(JsonKey.SECTION, reqObj.getRequest());
			reqObj.setRequest(map);
			Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
			Promise<Result> res = actorResponseHandler(getRemoteActor(),reqObj,timeout,null,request());
			return res;
		} catch (Exception e) {
			return Promise.<Result> pure(createCommonExceptionResponse(e,request()));
		}
	}
     
    
	/**
	 * This method will allow admin to update already created page sections
	 * @return Promise<Result>
	 */
	public Promise<Result> updatePageSection() {
		try {
			JsonNode requestData = request().body().asJson();
			logger.info("getting update page section data request=" + requestData);
			Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
			RequestValidator.validateUpdateSection(reqObj);
			reqObj.setOperation(ActorOperations.UPDATE_SECTION.getValue());
	        reqObj.setRequest_id(ExecutionContext.getRequestId());
	        reqObj.setEnv(getEnvironment());
			HashMap<String, Object> innerMap = new HashMap<>();
			reqObj.getRequest().put(JsonKey.UPDATED_BY,getUserIdByAuthToken(request().getHeader(HeaderParam.X_Authenticated_Userid.getName())));
			innerMap.put(JsonKey.SECTION, reqObj.getRequest());
			reqObj.setRequest(innerMap);
			Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
			Promise<Result> res = actorResponseHandler(getRemoteActor(),reqObj,timeout,null,request());
			return res;
		} catch (Exception e) {
			return Promise.<Result> pure(createCommonExceptionResponse(e,request()));
		}
	}
	
	/**
	 * This method will provide particular page section data.
	 * @return Promise<Result>
	 */
	public Promise<Result> getSection(String sectionId) {
		try {
			logger.info("getting data for particular page section =" + sectionId);
			Request reqObj = new Request();
			reqObj.setOperation(ActorOperations.GET_SECTION.getValue());
	        reqObj.setRequest_id(ExecutionContext.getRequestId());
	        reqObj.setEnv(getEnvironment());
	        reqObj.getRequest().put(JsonKey.ID, sectionId);
			Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
			Promise<Result> res = actorResponseHandler(getRemoteActor(),reqObj,timeout,null,request());
			return res;
		} catch (Exception e) {
			return Promise.<Result> pure(createCommonExceptionResponse(e,request()));
		}
	}

	
	/**
	 * This method will provide completed data for all sections stored in
	 * cassandra dac.
	 * @return Promise<Result>
	 */
	public Promise<Result> getSections() {
		try {
			logger.info("get page all section method called =");
			Request reqObj = new Request();
			reqObj.setOperation(ActorOperations.GET_ALL_SECTION.getValue());
	        reqObj.setRequest_id(ExecutionContext.getRequestId());
	        reqObj.setEnv(getEnvironment());
			Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
			Promise<Result> res = actorResponseHandler(getRemoteActor(),reqObj,timeout,null,request());
			return res;
		} catch (Exception e) {
			return Promise.<Result> pure(createCommonExceptionResponse(e,request()));
		}
	}

}
