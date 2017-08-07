package controllers.metrics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;

import com.fasterxml.jackson.databind.JsonNode;

import akka.util.Timeout;
import controllers.BaseController;
import play.libs.F.Promise;
import play.mvc.Result;

public class CourseMetricsController extends BaseController {
  
  @SuppressWarnings("unchecked")
  public Promise<Result> courseProgress() {
    try {
      Map<String, Object> map = new HashMap<>();
      JsonNode requestData = request().body().asJson();
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      Request request = new Request();
      request.setEnv(getEnvironment());
      Map<String,Object> data = (Map<String,Object>)reqObj.getRequest().get(JsonKey.FILTER);
      List<Map<String,Object>> dataList = (List<Map<String,Object>>) data.get(JsonKey.COURSE);
      data = dataList.get(0);
      map.put(JsonKey.COURSE_ID, data.get(JsonKey.COURSE_ID));
      map.put(JsonKey.PERIOD, reqObj.getRequest().get(JsonKey.PERIOD));
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
  
  @SuppressWarnings("unchecked")
  public Promise<Result> courseCreation() {
    try {
      Map<String, Object> map = new HashMap<>();
      JsonNode requestData = request().body().asJson();
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      Request request = new Request();
      request.setEnv(getEnvironment());
      request.setOperation(ActorOperations.COURSE_CREATION_METRICS.getValue());
      Map<String,Object> data = (Map<String,Object>)reqObj.getRequest().get(JsonKey.FILTER);
      List<Map<String,Object>> dataList = (List<Map<String,Object>>) data.get(JsonKey.COURSE);
      data = dataList.get(0);
      map.put(JsonKey.COURSE_ID, data.get(JsonKey.COURSE_ID));
      map.put(JsonKey.PERIOD, reqObj.getRequest().get(JsonKey.PERIOD));
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
