package controllers.skills;

import akka.util.Timeout;
import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestValidator;
import play.libs.F.Promise;
import play.mvc.Result;

/**
 * Created by arvind on 23/10/17.
 */
public class SkillController extends BaseController {

  public Promise<Result> addSkill() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("add/endorse user skills=" + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setOperation(ActorOperations.ADD_SKILL.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      Map<String, Object> innerMap = reqObj.getRequest();
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  public Promise<Result> getSkill() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("get user skills=" + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setOperation(ActorOperations.GET_SKILL.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      Map<String, Object> innerMap = reqObj.getRequest();
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  public Promise<Result> getSkillsList() {
    try {
      ProjectLogger.log("get list of skills ");
      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.GET_SKILLS_LIST.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      Map<String, Object> innerMap = reqObj.getRequest();
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

}
