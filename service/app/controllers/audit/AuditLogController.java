package controllers.audit;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;

import com.fasterxml.jackson.databind.JsonNode;

import akka.util.Timeout;
import controllers.BaseController;
import play.libs.F.Promise;
import play.mvc.Result;

/**
 * Controller class to provide APIs for searching Audit Logs
 *
 */
public class AuditLogController extends BaseController {

  /**
   * Method to search audit history logs
   * 
   * @return
   */
  public Promise<Result> searchAuditHistory() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("Request for Search Audit History: " + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setOperation(ActorOperations.SEARCH_AUDIT_LOG.getValue());
      reqObj.setRequest_id(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      innerMap.putAll(reqObj.getRequest());
      reqObj.setRequest(innerMap);
      Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
      return actorResponseHandler(getRemoteActor(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

}
