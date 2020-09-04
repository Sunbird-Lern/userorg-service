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
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.*;
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
    list.add("ekstep");
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
      reqObj.setRequestId(Common.getFromRequest(httpRequest, Attrs.REQUEST_ID));
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
    ProjectLogger.log("Call to get play service health api = " + val, LoggerEnum.INFO.name());
    Map<String, Object> finalResponseMap = new HashMap<>();
    List<Map<String, Object>> responseList = new ArrayList<>();
    if (list.contains(val) && !JsonKey.SERVICE.equalsIgnoreCase(val)) {
      if (ActorOperations.EKSTEP.name().equalsIgnoreCase(val)) {
        return getEkStepHealtCheck(httpRequest);
      } else {
        try {
          handleSigTerm();
          Request reqObj = new Request();
          reqObj.setOperation(val);
          reqObj.setRequestId(Common.getFromRequest(httpRequest, Attrs.REQUEST_ID));
          reqObj
              .getRequest()
              .put(JsonKey.CREATED_BY, Common.getFromRequest(httpRequest, Attrs.USER_ID));
          reqObj.setEnv(getEnvironment());
          return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
        } catch (Exception e) {
          return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
        }
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
        response.setTs(Common.getFromRequest(httpRequest, Attrs.REQUEST_ID));
        return CompletableFuture.completedFuture(ok(play.libs.Json.toJson(response)));
      } catch (Exception e) {
        return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
      }
    }
  }

  private void handleSigTerm() {
    if (signalHandler.isShuttingDown()) {
      ProjectLogger.log(
          "SIGTERM is "
              + signalHandler.isShuttingDown()
              + ", So play server will not allow any new request.",
          LoggerEnum.INFO.name());
      throw new ProjectCommonException(
          ResponseCode.serviceUnAvailable.getErrorCode(),
          ResponseCode.serviceUnAvailable.getErrorMessage(),
          ResponseCode.SERVICE_UNAVAILABLE.getResponseCode());
    }
  }

  public CompletionStage<Result> getEkStepHealtCheck(play.mvc.Http.Request request) {
    Map<String, Object> finalResponseMap = new HashMap<>();
    List<Map<String, Object>> responseList = new ArrayList<>();
    // check EKStep Util.
    try {
      String body = "{\"request\":{\"filters\":{\"identifier\":\"test\"}}}";
      Map<String, String> headers = new HashMap<>();
      headers.put(JsonKey.AUTHORIZATION, JsonKey.BEARER + System.getenv(JsonKey.AUTHORIZATION));
      if (StringUtils.isBlank((String) headers.get(JsonKey.AUTHORIZATION))) {
        headers.put(
            JsonKey.AUTHORIZATION,
            PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_AUTHORIZATION));
      }
      headers.put("Content-Type", "application/json");
      String ekStepBaseUrl = ProjectUtil.getConfigValue(JsonKey.SEARCH_SERVICE_API_BASE_URL);
      String response =
          HttpClientUtil.post(
              ekStepBaseUrl
                  + PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_CONTENT_SEARCH_URL),
              body,
              headers);
      if (response.contains("OK")) {
        responseList.add(ProjectUtil.createCheckResponse(JsonKey.EKSTEP_SERVICE, false, null));
        finalResponseMap.put(JsonKey.Healthy, true);
      } else {
        responseList.add(ProjectUtil.createCheckResponse(JsonKey.EKSTEP_SERVICE, true, null));
        finalResponseMap.put(JsonKey.Healthy, false);
      }
    } catch (Exception e) {
      responseList.add(ProjectUtil.createCheckResponse(JsonKey.EKSTEP_SERVICE, true, null));
      finalResponseMap.put(JsonKey.Healthy, false);
    }
    finalResponseMap.put(JsonKey.CHECKS, responseList);
    finalResponseMap.put(JsonKey.NAME, "EkStep service health");
    Response response = new Response();
    response.getResult().put(JsonKey.RESPONSE, finalResponseMap);
    response.setId("Ekstep.service.health.api");
    response.setVer(getApiVersion(request.path()));
    response.setTs(Common.getFromRequest(request, Attrs.REQUEST_ID));
    return CompletableFuture.completedFuture(ok(play.libs.Json.toJson(response)));
  }
}
