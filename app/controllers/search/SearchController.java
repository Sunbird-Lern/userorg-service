/**
 * 
 */
package controllers.search;

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
 * user and organization search.
 * @author Manzarul
 */
public class SearchController  extends BaseController{
private LogHelper logger = LogHelper.getInstance(SearchController.class.getName());
	
	/**
	 * This method will do data search for user and organization.
	 * Search type will be decide based on request object type coming with filter
	 * if objectType key is not coming then we need to do the search for all 
	 * the types.
	 * @return Promise<Result>
	 */
	public Promise<Result> compositeSearch() {
		try {
			JsonNode requestData = request().body().asJson();
			logger.info("getting search request data=" + requestData);
			Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
			RequestValidator.validateCompositeSearch(reqObj);
			reqObj.setOperation(ActorOperations.COMPOSITE_SEARCH.getValue());
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
}
