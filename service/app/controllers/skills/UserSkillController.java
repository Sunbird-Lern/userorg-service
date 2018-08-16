package controllers.skills;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import controllers.skills.validator.UserSkillRequestValidator;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

public class UserSkillController extends BaseController {

  public Promise<Result> updateSkill() {
    try {
      JsonNode bodyJson = request().body().asJson();
      Request reqObj = createAndInitRequest(ActorOperations.UPDATE_SKILL.getValue(), bodyJson);
      new UserSkillRequestValidator().validateUpdateSkillRequest(reqObj);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  public Promise<Result> getSkill() {
    try {

      Request reqObj = createAndInitRequest(ActorOperations.GET_SKILL.getValue());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  public Promise<Result> getSkillsList() {
    try {
      Request reqObj = createAndInitRequest(ActorOperations.GET_SKILLS_LIST.getValue());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }
}
