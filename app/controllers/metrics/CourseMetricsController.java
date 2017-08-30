package controllers.metrics;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.HeaderParam;
import org.sunbird.common.request.Request;

import akka.util.Timeout;
import controllers.BaseController;
import play.libs.F.Promise;
import play.mvc.Result;

public class CourseMetricsController extends BaseController {

  public Promise<Result> courseProgress(String courseId) {
    try {
      String periodStr = request().getQueryString("period");
      Map<String, Object> map = new HashMap<>();
      Request request = new Request();
      request.setEnv(getEnvironment());
      map.put(JsonKey.COURSE_ID, courseId);
      map.put(JsonKey.PERIOD, periodStr);
      request.setRequest(map);
      request.setOperation(ActorOperations.COURSE_PROGRESS_METRICS.getValue());
      request.setRequest(map);
      Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
      request.setRequest_id(ExecutionContext.getRequestId());
      Promise<Result> res =
          actorResponseHandler(getRemoteActor(), request, timeout, null, request());
      return res;
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  public Promise<Result> courseCreation(String courseId) {
    try {
      String periodStr = request().getQueryString("period");
      Map<String, Object> map = new HashMap<>();
      Request request = new Request();
      request.setEnv(getEnvironment());
      request.setOperation(ActorOperations.COURSE_CREATION_METRICS.getValue());
      map.put(JsonKey.COURSE_ID, courseId);
      map.put(JsonKey.PERIOD, periodStr);
      request.setRequest(map);
      Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
      request.setRequest_id(ExecutionContext.getRequestId());
      Promise<Result> res =
          actorResponseHandler(getRemoteActor(), request, timeout, null, request());
      return res;
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  public Promise<Result> courseProgressReport(String batchId) {
    try {
      String periodStr = request().getQueryString(JsonKey.PERIOD);
      String reportType = request().getQueryString(JsonKey.TYPE);
      Map<String, Object> map = new HashMap<>();
      Request request = new Request();
      request.setEnv(getEnvironment());
      map.put(JsonKey.BATCH_ID, batchId);
      map.put(JsonKey.PERIOD, periodStr);
      map.put(JsonKey.TYPE , reportType);
      map.put(JsonKey.REQUESTED_BY,
          getUserIdByAuthToken(request().getHeader(HeaderParam.X_Authenticated_Userid.getName())));
      request.setRequest(map);
      request.setOperation(ActorOperations.COURSE_PROGRESS_METRICS_REPORT.getValue());
      request.setRequest(map);
      Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
      request.setRequest_id(ExecutionContext.getRequestId());
      Promise<Result> res =
          actorResponseHandler(getRemoteActor(), request, timeout, null, request());
      return res;
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  public Promise<Result> courseCreationReport(String courseId) {
    try {
      String periodStr = request().getQueryString("period");
      Map<String, Object> map = new HashMap<>();
      Request request = new Request();
      request.setEnv(getEnvironment());
      request.setOperation(ActorOperations.COURSE_CREATION_METRICS_REPORT.getValue());
      map.put(JsonKey.COURSE_ID, courseId);
      map.put(JsonKey.PERIOD, periodStr);
      request.setRequest(map);
      Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
      request.setRequest_id(ExecutionContext.getRequestId());
      Promise<Result> res =
          actorResponseHandler(getRemoteActor(), request, timeout, null, request());
      return res;
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }



}
