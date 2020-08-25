/** */
package controllers.search;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestValidator;
import play.mvc.Http;
import play.mvc.Result;
import util.Attrs;
import util.Common;

/**
 * This controller will handle all the request related user and organization search.
 *
 * @author Manzarul
 */
public class SearchController extends BaseController {

  /**
   * This method will do data search for user and organization. Search type will be decide based on
   * request object type coming with filter if objectType key is not coming then we need to do the
   * search for all the types.
   *
   * @return CompletionStage<Result>
   */
  public CompletionStage<Result> compositeSearch(Http.Request httpRequest) {
    try {
      JsonNode requestData = httpRequest.body().asJson();
      ProjectLogger.log("getting search request data = " + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setOperation(ActorOperations.COMPOSITE_SEARCH.getValue());
      reqObj.setRequestId(Common.getFromRequest(httpRequest, Attrs.REQUEST_ID));
      reqObj.getRequest().put(JsonKey.CREATED_BY, Common.getFromRequest(httpRequest, Attrs.USER_ID));
      reqObj.setEnv(getEnvironment());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  /**
   * This method will do data Sync form Cassandra db to Elasticsearch.
   *
   * @return CompletionStage<Result>
   */
  public CompletionStage<Result> sync(Http.Request httpRequest) {
    try {
      JsonNode requestData = httpRequest.body().asJson();
      ProjectLogger.log("making a call to data synch api = " + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      RequestValidator.validateSyncRequest(reqObj);
      String operation = (String) reqObj.getRequest().get(JsonKey.OPERATION_FOR);
      if ("keycloak".equalsIgnoreCase(operation)) {
        reqObj.setOperation(ActorOperations.SYNC_KEYCLOAK.getValue());
        reqObj.setRequestId(Common.getFromRequest(httpRequest, Attrs.REQUEST_ID));
        reqObj.getRequest().put(JsonKey.CREATED_BY, Common.getFromRequest(httpRequest, Attrs.USER_ID));
        reqObj.setEnv(getEnvironment());
        HashMap<String, Object> map = new HashMap<>();
        map.put(JsonKey.DATA, reqObj.getRequest());
        reqObj.setRequest(map);
        return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
      } else {
        reqObj.setOperation(ActorOperations.SYNC.getValue());
        reqObj.setRequestId(Common.getFromRequest(httpRequest, Attrs.REQUEST_ID));
        reqObj.getRequest().put(JsonKey.CREATED_BY, Common.getFromRequest(httpRequest, Attrs.USER_ID));
        reqObj.setEnv(getEnvironment());
        HashMap<String, Object> map = new HashMap<>();
        map.put(JsonKey.DATA, reqObj.getRequest());
        reqObj.setRequest(map);
        return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
      }

    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }
}
