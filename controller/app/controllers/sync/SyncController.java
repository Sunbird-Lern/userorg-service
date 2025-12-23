/** */
package controllers.sync;

import org.apache.pekko.actor.ActorRef;
import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Named;
import org.sunbird.exception.ProjectCommonException;
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

  @Inject
  @Named("es_sync_actor")
  private ActorRef esSyncActor;

  /**
   * This method will do data Sync form Cassandra db to Elasticsearch.
   *
   * @return CompletionStage<Result>
   */
  public CompletionStage<Result> sync(Http.Request httpRequest) {
    Request reqObj = new Request();
    try {
      JsonNode requestData = httpRequest.body().asJson();
      reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
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
      return actorResponseHandler(esSyncActor, reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      ProjectCommonException exception =
          new ProjectCommonException(
              (ProjectCommonException) e,
              ActorOperations.getOperationCodeByActorOperation(reqObj.getOperation()));
      return CompletableFuture.completedFuture(
          createCommonExceptionResponse(exception, httpRequest));
    }
  }
}
