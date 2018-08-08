package controllers.skills;

import controllers.BaseController;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

public class UserSkillController extends BaseController {

  public Promise<Result> updateSkill() {
    try {
      ProjectLogger.log("Endorse  skill request: ");
      Request reqObj = createAndInitRequest(ActorOperations.UPDATE_SKILL.getValue());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  public Promise<Result> getSkill() {
    try {
      ProjectLogger.log("get user skills=", LoggerEnum.INFO.name());
      Request reqObj = createAndInitRequest(ActorOperations.GET_SKILL.getValue());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  public Promise<Result> getSkillsList() {
    try {
      ProjectLogger.log("get list of skills ");
      Request reqObj = createAndInitRequest(ActorOperations.GET_SKILLS_LIST.getValue());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }
}
