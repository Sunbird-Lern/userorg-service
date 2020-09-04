package controllers.metrics;

import controllers.BaseController;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import play.mvc.Http;
import play.mvc.Result;
import util.Attrs;
import util.Common;

public class UserMetricsController extends BaseController {

  public CompletionStage<Result> userCreation(String userId, Http.Request httpRequest) {
    try {
      Map<String, Object> map = new HashMap<>();
      Request request = new Request();
      request.setEnv(getEnvironment());
      request.setRequest(map);
      request.setOperation(ActorOperations.USER_CREATION_METRICS.getValue());
      request.setRequest(map);
      request.setRequestId(Common.getFromRequest(httpRequest, Attrs.REQUEST_ID));
      return actorResponseHandler(getActorRef(), request, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  public CompletionStage<Result> userConsumption(String userId, Http.Request httpRequest) {
    try {
      Map<String, Object> map = new HashMap<>();
      Request request = new Request();
      request.setEnv(getEnvironment());
      request.setRequest(map);
      request.setOperation(ActorOperations.USER_CONSUMPTION_METRICS.getValue());
      request.setRequest(map);
      request.setRequestId(Common.getFromRequest(httpRequest, Attrs.REQUEST_ID));
      return actorResponseHandler(getActorRef(), request, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }
}
