package controllers.courseenrollment;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import controllers.courseenrollment.validator.RequestValidator;

import com.fasterxml.jackson.databind.JsonNode;

import controllers.BaseController;
import play.libs.F.Promise;
import play.mvc.Result;

public class CourseEnrollmentController extends BaseController{
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
	      request.setRequestId(ExecutionContext.getRequestId());
	      return actorResponseHandler(getActorRef(), request, timeout, JsonKey.COURSES, request());
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
	      reqObj.setRequestId(ExecutionContext.getRequestId());
	      reqObj.setOperation(ActorOperations.ENROLL_COURSE.getValue());
	      reqObj.setEnv(getEnvironment());
	      HashMap<String, Object> innerMap = new HashMap<>();
	      innerMap.put(JsonKey.COURSE, reqObj.getRequest());
	      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
	      innerMap.put(JsonKey.HEADER, getAllRequestHeaders(request()));
	      reqObj.setRequest(innerMap);
	      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
	    } catch (Exception e) {
	      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
	    }
	  }
	  
	  /**
	   * This method will be called when user will unenroll for course.
	   *
	   * @return Result
	   */
	  public Promise<Result> unenrollCourse() {
	    try {
	      JsonNode requestData = request().body().asJson();
	      ProjectLogger.log(" get course request data=" + requestData, LoggerEnum.INFO.name());
	      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
	      RequestValidator.validateUnenrollCourse(reqObj);
	      reqObj.setRequestId(ExecutionContext.getRequestId());
	      reqObj.setOperation(ActorOperations.UNENROLL_COURSE.getValue());
	      reqObj.setEnv(getEnvironment());
	      HashMap<String, Object> innerMap = new HashMap<>();
	      innerMap.put(JsonKey.COURSE, reqObj.getRequest());
	      innerMap.put(JsonKey.BATCH,  reqObj.getRequest());
	      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
	      innerMap.put(JsonKey.HEADER, getAllRequestHeaders(request()));
	      reqObj.setRequest(innerMap);
	      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
	    } catch (Exception e) {
	      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
	    }
	  }

	  /**
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


	}
