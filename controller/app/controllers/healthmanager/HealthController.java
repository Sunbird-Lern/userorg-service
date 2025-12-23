/** */
package controllers.healthmanager;

import org.apache.pekko.actor.ActorRef;
import controllers.BaseController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Named;
import modules.SignalHandler;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import org.sunbird.util.ProjectUtil;
import play.mvc.Http;
import play.mvc.Result;
import util.Attrs;
import util.Common;

/** @author Manzarul */
public class HealthController extends BaseController {
  private static List<String> list = new ArrayList<>();
  @Inject SignalHandler signalHandler;

  @Inject
  @Named("health_actor")
  private ActorRef healthActor;

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
  public CompletionStage<Result> health(Http.Request httpRequest) {
    Request reqObj = new Request();
    try {
      handleSigTerm();
      reqObj = new Request();
      reqObj.setOperation(ActorOperations.HEALTH_CHECK.getValue());
      reqObj.setRequestId(Common.getFromRequest(httpRequest, Attrs.X_REQUEST_ID));
      reqObj
          .getRequest()
          .put(JsonKey.CREATED_BY, Common.getFromRequest(httpRequest, Attrs.USER_ID));
      reqObj.setEnv(getEnvironment());
      return actorResponseHandler(healthActor, reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      ProjectCommonException exception =
          new ProjectCommonException(
              (ProjectCommonException) e,
              ActorOperations.getOperationCodeByActorOperation(reqObj.getOperation()));
      return CompletableFuture.completedFuture(
          createCommonExceptionResponse(exception, httpRequest));
    }
  }

  /**
   * This method will do the health check for play service.
   *
   * @return CompletionStage<Result>
   */
  public CompletionStage<Result> serviceHealth(String val, Http.Request httpRequest) {
    Map<String, Object> finalResponseMap = new HashMap<>();
    List<Map<String, Object>> responseList = new ArrayList<>();
    Request reqObj = new Request();
    if (list.contains(val) && !JsonKey.SERVICE.equalsIgnoreCase(val)) {
      try {
        reqObj.setOperation(val);
        handleSigTerm();
        reqObj.setRequestId(Common.getFromRequest(httpRequest, Attrs.X_REQUEST_ID));
        reqObj
            .getRequest()
            .put(JsonKey.CREATED_BY, Common.getFromRequest(httpRequest, Attrs.USER_ID));
        reqObj.setEnv(getEnvironment());
        return actorResponseHandler(healthActor, reqObj, timeout, null, httpRequest);
      } catch (Exception e) {
        ProjectCommonException exception =
            new ProjectCommonException(
                (ProjectCommonException) e,
                ActorOperations.getOperationCodeByActorOperation(reqObj.getOperation()));
        return CompletableFuture.completedFuture(
            createCommonExceptionResponse(exception, httpRequest));
      }
    } else {
      try {
        handleSigTerm();
        responseList.add(ProjectUtil.createCheckResponse(JsonKey.LEARNER_SERVICE, false, null));
        finalResponseMap.put(JsonKey.CHECKS, responseList);
        finalResponseMap.put(JsonKey.NAME, "UserOrg service health");
        finalResponseMap.put(JsonKey.Healthy, true);
        Response response = new Response();
        response.getResult().put(JsonKey.RESPONSE, finalResponseMap);
        response.setId("api.userorg.service.health");
        response.setVer(getApiVersion(httpRequest.path()));
        response.setTs(Common.getFromRequest(httpRequest, Attrs.X_REQUEST_ID));
        return CompletableFuture.completedFuture(ok(play.libs.Json.toJson(response)));
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

  private void handleSigTerm() {
    if (signalHandler.isShuttingDown()) {
      throw new ProjectCommonException(
          ResponseCode.serviceUnAvailable,
          ResponseCode.serviceUnAvailable.getErrorMessage(),
          ResponseCode.SERVICE_UNAVAILABLE.getResponseCode());
    }
  }
}
