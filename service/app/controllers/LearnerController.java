/**
 * 
 */
package controllers;

import akka.util.Timeout;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestValidator;
import org.sunbird.common.responsecode.ResponseCode;
import play.libs.F.Promise;
import play.mvc.Result;
import play.mvc.Results;

/**
 * This controller will handler all the request related to learner state.
 * 
 * @author Manzarul
 */
public class LearnerController extends BaseController {
  /**
   * This method will provide list of enrolled courses for a user. User courses are stored in
   * Cassandra db.
   * 
   * @param uid user id for whom we need to collect all the courses.
   * @return Result
   */
  public Promise<Result> getEnrolledCourses(String uid) {
    try {
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.USER_ID, uid);
      map.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      Request request = new Request();
      request.setEnv(getEnvironment());
      request.setRequest(map);
      request.setOperation(ActorOperations.GET_COURSE.getValue());
      request.setRequest(map);
      Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
      request.setRequest_id(ExecutionContext.getRequestId());
      return   actorResponseHandler(getRemoteActor(), request, timeout, JsonKey.COURSES, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will be called when user will enroll for a new course.
   * 
   * @return Result
   */
  public Promise<Result> enrollCourse() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log(" get course request data=" + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      RequestValidator.validateEnrollCourse(reqObj);
      reqObj.setRequest_id(ExecutionContext.getRequestId());
      reqObj.setOperation(ActorOperations.ENROLL_COURSE.getValue());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.COURSE, reqObj.getRequest());
      innerMap.put(JsonKey.REQUESTED_BY,ctx().flash().get(JsonKey.USER_ID));
      innerMap.put(JsonKey.HEADER, getAllRequestHeaders(request()));
      reqObj.setRequest(innerMap);
      Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
      return  actorResponseHandler(getRemoteActor(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * 
   * @param request
   * @return Map<String, String>
   */
  private Map<String, String> getAllRequestHeaders(play.mvc.Http.Request request) {
    Map<String, String> map = new HashMap<>();
    Map<String, String[]> headers = request.headers();
    Iterator<Entry<String, String[]>> itr = headers.entrySet().iterator();
    while (itr.hasNext()) {
      Entry<String, String[]> entry = itr.next();
      map.put(entry.getKey(), entry.getValue()[0]);
    }
    return map;
  }


  /**
   * This method will provide list of user content state. Content refer user activity {started,half
   * completed ,completed} against TOC (table of content).
   * 
   * @return Result
   */
  public Promise<Result> getContentState() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log(" get course request data=" + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setRequest_id(ExecutionContext.getRequestId());
      reqObj.setOperation(ActorOperations.GET_CONTENT.getValue());
      reqObj.setEnv(getEnvironment());
      Map<String, Object> innerMap = createRequest(reqObj);
      reqObj.setRequest(innerMap);
      Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
      return actorResponseHandler(getRemoteActor(), reqObj, timeout, JsonKey.CONTENT_LIST, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }

  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> createRequest(Request reqObj) {

    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.REQUESTED_BY,ctx().flash().get(JsonKey.USER_ID));
    innerMap.put(JsonKey.USER_ID, reqObj.getRequest().get(JsonKey.USER_ID));

    if ((null != reqObj.getRequest().get(JsonKey.CONTENT_IDS))
        && (null == reqObj.getRequest().get(JsonKey.COURSE_IDS))) {
      innerMap.put(JsonKey.CONTENT_IDS, reqObj.getRequest().get(JsonKey.CONTENT_IDS));
      return innerMap;
    } else if ((null == reqObj.getRequest().get(JsonKey.CONTENT_IDS))
        && (null != reqObj.getRequest().get(JsonKey.COURSE_IDS))) {
      innerMap.put(JsonKey.COURSE_IDS, reqObj.getRequest().get(JsonKey.COURSE_IDS));
      return innerMap;
    } else if ((null != reqObj.getRequest().get(JsonKey.CONTENT_IDS))
        && (null != reqObj.getRequest().get(JsonKey.COURSE_IDS))) {
      if ((((List<String>) reqObj.getRequest().get(JsonKey.COURSE_IDS)).size() == 1)
          && ((List<String>) reqObj.getRequest().get(JsonKey.CONTENT_IDS)).size() >= 0) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(JsonKey.COURSE_ID,
            ((List<String>) reqObj.getRequest().get(JsonKey.COURSE_IDS)).get(0));
        map.put(JsonKey.CONTENT_IDS, (reqObj.getRequest().get(JsonKey.CONTENT_IDS)));
        innerMap.put(JsonKey.COURSE, map);
        return innerMap;
      } else {
        throw new ProjectCommonException(ResponseCode.invalidRequestData.getErrorCode(),
            ResponseCode.invalidRequestData.getErrorMessage(),
            ResponseCode.invalidRequestData.getResponseCode());
      }
    }
    return null;
  }

  /**
   * This method will update learner current state with last store state.
   * 
   * @return Result
   */
  public Promise<Result> updateContentState() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log(" get content request data=" + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      RequestValidator.validateUpdateContent(reqObj);
      reqObj.setOperation(ActorOperations.ADD_CONTENT.getValue());
      reqObj.setRequest_id(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.CONTENTS, reqObj.getRequest().get(JsonKey.CONTENTS));
      innerMap.put(JsonKey.REQUESTED_BY,ctx().flash().get(JsonKey.USER_ID));
      innerMap.put(JsonKey.USER_ID, reqObj.getRequest().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
      return actorResponseHandler(getRemoteActor(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  public Result getHealth() {
    return Results.ok("ok");
  }

  /**
   * 
   * @param all
   * @return
   */
  public Result preflight(String all) {
    response().setHeader("Access-Control-Allow-Origin", "*");
    response().setHeader("Allow", "*");
    response().setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS");
    response().setHeader("Access-Control-Allow-Headers",
        "Origin, X-Requested-With, Content-Type, Accept, Referer, User-Agent,X-Consumer-ID,cid,ts,X-Device-ID,X-Authenticated-Userid,X-msgid,id,X-Access-TokenId");
    return ok();
  }

}
