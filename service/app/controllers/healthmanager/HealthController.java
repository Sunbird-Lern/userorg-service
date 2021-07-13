/** */
package controllers.healthmanager;

import controllers.BaseController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import modules.SignalHandler;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.operations.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import play.mvc.Http;
import play.mvc.Result;
import util.Attrs;
import util.Common;

/** @author Manzarul */
public class HealthController extends BaseController {
  private static List<String> list = new ArrayList<>();
  @Inject SignalHandler signalHandler;

  static {
    list.add("service");
    list.add("actor");
    list.add("cassandra");
    list.add("es");
  }

  /**
   * This method will do the complete health check
   *
   * @return CompletionStage<Result>
   */
  public CompletionStage<Result> getHealth(Http.Request httpRequest) {
    try {
      handleSigTerm();
      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.HEALTH_CHECK.getValue());
      reqObj.setRequestId(Common.getFromRequest(httpRequest, Attrs.X_REQUEST_ID));
      reqObj
          .getRequest()
          .put(JsonKey.CREATED_BY, Common.getFromRequest(httpRequest, Attrs.USER_ID));
      reqObj.setEnv(getEnvironment());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  /**
   * This method will do the health check for play service.
   *
   * @return CompletionStage<Result>
   */
  public CompletionStage<Result> getLearnerServiceHealth(String val, Http.Request httpRequest) {
    Map<String, Object> finalResponseMap = new HashMap<>();
    List<Map<String, Object>> responseList = new ArrayList<>();
    if (list.contains(val) && !JsonKey.SERVICE.equalsIgnoreCase(val)) {
      try {
        handleSigTerm();
        Request reqObj = new Request();
        reqObj.setOperation(val);
        reqObj.setRequestId(Common.getFromRequest(httpRequest, Attrs.X_REQUEST_ID));
        reqObj
            .getRequest()
            .put(JsonKey.CREATED_BY, Common.getFromRequest(httpRequest, Attrs.USER_ID));
        reqObj.setEnv(getEnvironment());
        return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
      } catch (Exception e) {
        return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
      }
    } else {
      try {
        handleSigTerm();
        responseList.add(ProjectUtil.createCheckResponse(JsonKey.LEARNER_SERVICE, false, null));
        finalResponseMap.put(JsonKey.CHECKS, responseList);
        finalResponseMap.put(JsonKey.NAME, "Learner service health");
        finalResponseMap.put(JsonKey.Healthy, true);
        Response response = new Response();
        response.getResult().put(JsonKey.RESPONSE, finalResponseMap);
        response.setId("learner.service.health.api");
        response.setVer(getApiVersion(httpRequest.path()));
        response.setTs(Common.getFromRequest(httpRequest, Attrs.X_REQUEST_ID));
        return CompletableFuture.completedFuture(ok(play.libs.Json.toJson(response)));
      } catch (Exception e) {
        return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
      }
    }
  }

  private void handleSigTerm() {
    if (signalHandler.isShuttingDown()) {
      throw new ProjectCommonException(
          ResponseCode.serviceUnAvailable.getErrorCode(),
          ResponseCode.serviceUnAvailable.getErrorMessage(),
          ResponseCode.SERVICE_UNAVAILABLE.getResponseCode());
    }
  }
}
