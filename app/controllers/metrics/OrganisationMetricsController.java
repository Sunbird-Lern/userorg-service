package controllers.metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;

import akka.util.Timeout;
import controllers.BaseController;
import play.libs.F.Promise;
import play.mvc.Result;

public class OrganisationMetricsController extends BaseController {

  public Promise<Result> orgCreation(String orgId) {
    ProjectLogger.log("Start Org Metrics Creation Contoller");
    try {
      String periodStr = request().getQueryString("period");
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.ORG_ID, orgId);
      map.put(JsonKey.PERIOD, periodStr);
      Request request = new Request();
      request.setEnv(getEnvironment());
      request.setRequest(map);
      request.setOperation(ActorOperations.ORG_CREATION_METRICS.getValue());
      request.setRequest(map);
      Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
      request.setRequest_id(ExecutionContext.getRequestId());
      Promise<Result> res =
          actorResponseHandler(getRemoteActor(), request, timeout, null, request());
      ProjectLogger.log("Return from Org Metrics Creation Contoller");
      return res;
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }
  
  public Promise<Result> orgConsumption(String orgId) {
    ProjectLogger.log("Start Org Metrics Consumption Contoller");
    try {
      String periodStr = request().getQueryString("period");
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.ORG_ID, orgId);
      map.put(JsonKey.PERIOD, periodStr);
      Request request = new Request();
      request.setEnv(getEnvironment());
      request.setRequest(map);
      request.setOperation(ActorOperations.ORG_CONSUMPTION_METRICS.getValue());
      request.setRequest(map);
      Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
      request.setRequest_id(ExecutionContext.getRequestId());
      Promise<Result> res =
          actorResponseHandler(getRemoteActor(), request, timeout, null, request());
      ProjectLogger.log("Return from Org Metrics Consumption Contoller");
      return res;
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  public Promise<Result> orgCreationReport(String orgId) {
    ProjectLogger.log("Start Org Metrics Creation Contoller");
    try {
      String periodStr = request().getQueryString("period");
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.ORG_ID, orgId);
      map.put(JsonKey.PERIOD, periodStr);
      Request request = new Request();
      request.setEnv(getEnvironment());
      request.setRequest(map);
      request.setOperation(ActorOperations.ORG_CREATION_METRICS_REPORT.getValue());
      request.setRequest(map);
      Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
      request.setRequest_id(ExecutionContext.getRequestId());
      Promise<Result> res =
          actorResponseHandler(getRemoteActor(), request, timeout, null, request());
      ProjectLogger.log("Return from Org Metrics Creation Contoller");
      return res;
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  public Promise<Result> orgConsumptionReport(String orgId) {
    ProjectLogger.log("Start Org Metrics Consumption Contoller");
    try {
      String periodStr = request().getQueryString("period");
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.ORG_ID, orgId);
      map.put(JsonKey.PERIOD, periodStr);
      Request request = new Request();
      request.setEnv(getEnvironment());
      request.setRequest(map);
      request.setOperation(ActorOperations.ORG_CONSUMPTION_METRICS_REPORT.getValue());
      request.setRequest(map);
      Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
      request.setRequest_id(ExecutionContext.getRequestId());
      Promise<Result> res =
          actorResponseHandler(getRemoteActor(), request, timeout, null, request());
      ProjectLogger.log("Return from Org Metrics Consumption Contoller");
      return res;
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

}
