package controllers.skills;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

/** Created by arvind on 23/10/17. */
public class SkillController extends BaseController {

  public Promise<Result> addSkill() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("add/endorse user skills=" + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj =
          setExtraParam(
              reqObj,
              ExecutionContext.getRequestId(),
              ActorOperations.UPDATE_SKILL.getValue(),
              ctx().flash().get(JsonKey.USER_ID),
              getEnvironment());
      reqObj.getRequest().put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
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
      reqObj =
          setExtraParam(
              reqObj,
              ExecutionContext.getRequestId(),
              ActorOperations.UPDATE_SKILL.getValue(),
              ctx().flash().get(JsonKey.USER_ID),
              getEnvironment());
      reqObj.getRequest().put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  public Promise<Result> getSkillsList() {
    try {
      ProjectLogger.log("get list of skills ");
      Request reqObj = new Request();
      reqObj =
          setExtraParam(
              reqObj,
              ExecutionContext.getRequestId(),
              ActorOperations.UPDATE_SKILL.getValue(),
              ctx().flash().get(JsonKey.USER_ID),
              getEnvironment());
      reqObj.getRequest().put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  public Promise<Result> updateSkill() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("Update skill request: ");
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj =
          setExtraParam(
              reqObj,
              ExecutionContext.getRequestId(),
              ActorOperations.UPDATE_SKILL.getValue(),
              ctx().flash().get(JsonKey.USER_ID),
              getEnvironment());
      reqObj.getRequest().put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }
}
