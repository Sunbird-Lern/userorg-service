/**
 * 
 */
package controllers.assessment;

import akka.util.Timeout;

import com.fasterxml.jackson.databind.JsonNode;

import controllers.BaseController;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.HeaderParam;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestValidator;

import play.libs.F.Promise;
import play.mvc.Result;

/**
 * This controller will handle all the API related to Assessment
 * 
 * @author Manzarul
 */
public class AssessmentController extends BaseController {


  /**
   * This method will add assessment entry into cassandra DB.
   * 
   * @return Promise<Result>
   */
  public Promise<Result> saveAssessment() {

    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("add new assessment data=" + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      RequestValidator.validateSaveAssessment(reqObj);
      reqObj.setOperation(ActorOperations.SAVE_ASSESSMENT.getValue());
      reqObj.setRequest_id(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.ASSESSMENT, reqObj.getRequest());
      innerMap.put(JsonKey.REQUESTED_BY,ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
      return actorResponseHandler(getRemoteActor(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will provide user assessment details based on userid and course id. if only course
   * id is coming then it will provide all the user assessment for that course. if course id and
   * user id's both coming then it will provide only those users assessment for that course.
   * 
   * @return Promise<Result>
   */
  public Promise<Result> getAssessment() {

    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("get assessment request=" + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      RequestValidator.validateGetAssessment(reqObj);
      reqObj.setOperation(ActorOperations.GET_ASSESSMENT.getValue());
      reqObj.setRequest_id(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.ASSESSMENT, reqObj.getRequest());
      innerMap.put(JsonKey.REQUESTED_BY,ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
      return actorResponseHandler(getRemoteActor(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }


}
