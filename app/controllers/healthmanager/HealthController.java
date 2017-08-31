/**
 * 
 */
package controllers.healthmanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.HeaderParam;
import org.sunbird.common.request.Request;

import akka.util.Timeout;
import controllers.BaseController;
import play.libs.F.Promise;
import play.mvc.Result;

/**
 * @author Manzarul
 */
public class HealthController extends BaseController {
  
  /**
   * This method will do the complete health check
   * @return Promise<Result>
   */
  public Promise<Result> getHealth() {
    try {
      ProjectLogger.log("Call to get all server health api = " , LoggerEnum.INFO.name());
      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.HEALTH_CHECK.getValue());
      reqObj.setRequest_id(ExecutionContext.getRequestId());
      reqObj.getRequest().put(JsonKey.CREATED_BY,
          getUserIdByAuthToken(request().getHeader(HeaderParam.X_Authenticated_Userid.getName())));
      reqObj.setEnv(getEnvironment());
      Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
      return actorResponseHandler(getRemoteActor(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }
  
  
  /**
   * This method will do the health check for play service.
   * @return Promise<Result>
   */
  public Result getLearnerServiceHealth() {
      ProjectLogger.log("Call to get play service health api = " , LoggerEnum.INFO.name());
      Map<String,Object> finalResponseMap = new HashMap<>();
      List<Map<String,Object>> responseList  =  new ArrayList<> ();
      responseList.add(ProjectUtil.createCheckResponse(JsonKey.LEARNER_SERVICE, false, null));
      finalResponseMap.put(JsonKey.CHECKS, responseList);
      finalResponseMap.put(JsonKey.NAME, "Learner service health");
      finalResponseMap.put(JsonKey.Healthy, true);
      Response response = new  Response();
      response.getResult().put(JsonKey.RESPONSE, finalResponseMap);
      response.setId("learner.service.health.api");
      response.setVer(getApiVersion(request().path()));
      response.setTs(ExecutionContext.getRequestId());
      return ok (play.libs.Json.toJson(response));
  }
}
