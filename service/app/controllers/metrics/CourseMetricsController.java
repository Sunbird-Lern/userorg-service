package controllers.metrics;

import controllers.BaseController;
import controllers.metrics.validator.CourseMetricsProgressValidator;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

public class CourseMetricsController extends BaseController {

  public Promise<Result> courseProgress(String batchId) {
    try {
      String periodStr = request().getQueryString("period");
      Map<String, Object> map = new HashMap<>();
      Request request = new Request();
      request.setEnv(getEnvironment());
      map.put(JsonKey.BATCH_ID, batchId);
      map.put(JsonKey.PERIOD, periodStr);
      map.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      request.setRequest(map);
      request.setOperation(ActorOperations.COURSE_PROGRESS_METRICS.getValue());
      request.setRequest(map);
      request.setRequestId(ExecutionContext.getRequestId());
      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  public Promise<Result> courseProgressV2(String batchId) {
    String limit = request().getQueryString(JsonKey.LIMIT);
    limit = StringUtils.isEmpty(limit) ? JsonKey.DEFAULT_LIMIT : limit;

    String offset = request().getQueryString(JsonKey.OFFSET);
    offset =
        StringUtils.isEmpty(offset) ? JsonKey.OFFSET : request().getQueryString(JsonKey.OFFSET);
    final int dataLimit = Integer.parseInt(limit);
    final int dataOffset = Integer.parseInt(offset);
    String sortBy = request().getQueryString(JsonKey.SORTBY);
    String userName = request().getQueryString(JsonKey.USERNAME);
    String sortOrder = request().getQueryString(JsonKey.SORT_ORDER);
    new CourseMetricsProgressValidator()
        .validateCourseProgressMetricsV2Request(limit, offset, sortOrder);
    return handleRequest(
        ActorOperations.COURSE_PROGRESS_METRICS_V2.getValue(),
        (request) -> {
          Request req = (Request) request;
          req.getContext().put(JsonKey.LIMIT, dataLimit);
          req.getContext().put(JsonKey.BATCH_ID, batchId);
          req.getContext().put(JsonKey.OFFSET, dataOffset);
          req.getContext().put(JsonKey.SORT_BY, sortBy);
          req.getContext().put(JsonKey.USERNAME, userName);
          return null;
        });
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
      map.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      request.setRequest(map);
      request.setRequestId(ExecutionContext.getRequestId());
      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  public Promise<Result> courseProgressReport(String batchId) {
    try {
      String periodStr = request().getQueryString(JsonKey.PERIOD);
      String reportType = request().getQueryString(JsonKey.FORMAT);
      Map<String, Object> map = new HashMap<>();
      Request request = new Request();
      request.setEnv(getEnvironment());
      map.put(JsonKey.BATCH_ID, batchId);
      map.put(JsonKey.PERIOD, periodStr);
      map.put(JsonKey.FORMAT, reportType);
      map.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      request.setRequest(map);
      request.setOperation(ActorOperations.COURSE_PROGRESS_METRICS_REPORT.getValue());
      request.setRequest(map);
      request.setRequestId(ExecutionContext.getRequestId());
      return actorResponseHandler(getActorRef(), request, timeout, null, request());
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
      request.setRequestId(ExecutionContext.getRequestId());
      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }
}
