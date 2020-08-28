package controllers.metrics;

import controllers.BaseController;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;
import play.mvc.Http;
import play.mvc.Result;
import util.Attrs;
import util.Common;

public class OrganisationMetricsController extends BaseController {

  public CompletionStage<Result> orgCreation(String orgId, Http.Request httpRequest) {
    ProjectLogger.log("Start Org Metrics Creation Contoller");
    try {
      String periodStr = httpRequest.getQueryString(JsonKey.PERIOD);
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.ORG_ID, orgId);
      map.put(JsonKey.PERIOD, periodStr);
      Request request = new Request();
      request.setEnv(getEnvironment());
      request.setRequest(map);
      request.setOperation(ActorOperations.ORG_CREATION_METRICS.getValue());
      request.setRequest(map);
      request.setRequestId(Common.getFromRequest(httpRequest, Attrs.REQUEST_ID));
      ProjectLogger.log("Return from Org Metrics Creation Contoller");
      return actorResponseHandler(getActorRef(), request, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  public CompletionStage<Result> orgConsumption(String orgId, Http.Request httpRequest) {
    ProjectLogger.log("Start Org Metrics Consumption Contoller");
    try {
      String periodStr = httpRequest.getQueryString(JsonKey.PERIOD);
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.ORG_ID, orgId);
      map.put(JsonKey.PERIOD, periodStr);
      Request request = new Request();
      request.setEnv(getEnvironment());
      request.setRequest(map);
      request.setOperation(ActorOperations.ORG_CONSUMPTION_METRICS.getValue());
      request.setRequest(map);
      request.setRequestId(Common.getFromRequest(httpRequest, Attrs.REQUEST_ID));
      ProjectLogger.log("Return from Org Metrics Consumption Contoller");
      return actorResponseHandler(getActorRef(), request, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  public CompletionStage<Result> orgCreationReport(String orgId, Http.Request httpRequest) {
    ProjectLogger.log("Start Org Creation Report Contoller");
    try {
      String periodStr = httpRequest.getQueryString(JsonKey.PERIOD);
      String format = httpRequest.getQueryString(JsonKey.FORMAT);
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.ORG_ID, orgId);
      map.put(JsonKey.PERIOD, periodStr);
      map.put(JsonKey.FORMAT, format);
      map.put(JsonKey.REQUESTED_BY, Common.getFromRequest(httpRequest, Attrs.USER_ID));
      Request request = new Request();
      request.setEnv(getEnvironment());
      request.setOperation(ActorOperations.ORG_CREATION_METRICS_REPORT.getValue());
      request.setRequest(map);
      request.setRequestId(Common.getFromRequest(httpRequest, Attrs.REQUEST_ID));
      ProjectLogger.log("Return from Org Creation Report Contoller");
      return actorResponseHandler(getActorRef(), request, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  public CompletionStage<Result> orgConsumptionReport(String orgId, Http.Request httpRequest) {
    ProjectLogger.log("Start Org Consumption Report Contoller");
    try {
      String periodStr = httpRequest.getQueryString(JsonKey.PERIOD);
      String format = httpRequest.getQueryString(JsonKey.FORMAT);
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.ORG_ID, orgId);
      map.put(JsonKey.PERIOD, periodStr);
      map.put(JsonKey.FORMAT, format);
      map.put(JsonKey.REQUESTED_BY, Common.getFromRequest(httpRequest, Attrs.USER_ID));
      Request request = new Request();
      request.setEnv(getEnvironment());
      request.setOperation(ActorOperations.ORG_CONSUMPTION_METRICS_REPORT.getValue());
      request.setRequest(map);
      request.setRequestId(Common.getFromRequest(httpRequest, Attrs.REQUEST_ID));
      ProjectLogger.log("Return from Org Consumption Report Contoller");
      return actorResponseHandler(getActorRef(), request, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }
}
