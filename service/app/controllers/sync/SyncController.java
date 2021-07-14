/** */
package controllers.sync;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.validator.RequestValidator;
import play.mvc.Http;
import play.mvc.Result;
import util.Attrs;
import util.Common;

/**
 * This controller will handle all the request related user and organization search.
 *
 * @author Manzarul
 */
public class SyncController extends BaseController {

  /**
   * This method will do data Sync form Cassandra db to Elasticsearch.
   *
   * @return CompletionStage<Result>
   */
  public CompletionStage<Result> sync(Http.Request httpRequest) {
    try {
      JsonNode requestData = httpRequest.body().asJson();
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      RequestValidator.validateSyncRequest(reqObj);
      reqObj.setOperation(ActorOperations.SYNC.getValue());
      reqObj.setRequestId(Common.getFromRequest(httpRequest, Attrs.X_REQUEST_ID));
      reqObj
          .getRequest()
          .put(JsonKey.CREATED_BY, Common.getFromRequest(httpRequest, Attrs.USER_ID));
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> map = new HashMap<>();
      map.put(JsonKey.DATA, reqObj.getRequest());
      reqObj.setRequest(map);
      setContextAndPrintEntryLog(httpRequest, reqObj);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }
}
