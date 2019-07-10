package controllers.metrics;

import controllers.BaseController;
import java.util.HashMap;
import java.util.Map;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

public class OrganisationMetricsController extends BaseController {

  public Promise<Result> orgCreation(String orgId) {
    ProjectLogger.log("Start Org Metrics Creation Contoller");
    try {
      String periodStr = request().getQueryString(JsonKey.PERIOD);
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.ORG_ID, orgId);
      map.put(JsonKey.PERIOD, periodStr);
      Request request = new Request();
      request.setEnv(getEnvironment());
      request.setRequest(map);
      request.setOperation(ActorOperations.ORG_CREATION_METRICS.getValue());
      request.setRequest(map);
      request.setRequestId(ExecutionContext.getRequestId());
      ProjectLogger.log("Return from Org Metrics Creation Contoller");
      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  public Promise<Result> orgConsumption(String orgId) {
    ProjectLogger.log("Start Org Metrics Consumption Contoller");
    try {
      String periodStr = request().getQueryString(JsonKey.PERIOD);
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.ORG_ID, orgId);
      map.put(JsonKey.PERIOD, periodStr);
      Request request = new Request();
      request.setEnv(getEnvironment());
      request.setRequest(map);
      request.setOperation(ActorOperations.ORG_CONSUMPTION_METRICS.getValue());
      request.setRequest(map);
      request.setRequestId(ExecutionContext.getRequestId());
      ProjectLogger.log("Return from Org Metrics Consumption Contoller");
      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  public Promise<Result> orgCreationReport(String orgId) {
    ProjectLogger.log("Start Org Creation Report Contoller");
    try {
      String periodStr = request().getQueryString(JsonKey.PERIOD);
      String format = request().getQueryString(JsonKey.FORMAT);
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.ORG_ID, orgId);
      map.put(JsonKey.PERIOD, periodStr);
      map.put(JsonKey.FORMAT, format);
      map.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      Request request = new Request();
      request.setEnv(getEnvironment());
      request.setOperation(ActorOperations.ORG_CREATION_METRICS_REPORT.getValue());
      request.setRequest(map);
      request.setRequestId(ExecutionContext.getRequestId());
      ProjectLogger.log("Return from Org Creation Report Contoller");
      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  public Promise<Result> orgConsumptionReport(String orgId) {
    ProjectLogger.log("Start Org Consumption Report Contoller");
    try {
      String periodStr = request().getQueryString(JsonKey.PERIOD);
      String format = request().getQueryString(JsonKey.FORMAT);
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.ORG_ID, orgId);
      map.put(JsonKey.PERIOD, periodStr);
      map.put(JsonKey.FORMAT, format);
      map.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      Request request = new Request();
      request.setEnv(getEnvironment());
      request.setOperation(ActorOperations.ORG_CONSUMPTION_METRICS_REPORT.getValue());
      request.setRequest(map);
      request.setRequestId(ExecutionContext.getRequestId());
      ProjectLogger.log("Return from Org Consumption Report Contoller");
      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }
}
