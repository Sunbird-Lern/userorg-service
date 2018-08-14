package controllers.skills;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import controllers.skills.validator.UserSkillEndorsementRequestValidator;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

public class UserSkillEndorsementController extends BaseController {

  public Promise<Result> addEndorseroment() {
    try {
      JsonNode bodyJson = request().body().asJson();
      Request reqObj =
          createAndInitRequest(ActorOperations.ADD_USER_SKILL_ENDORSEMENT.getValue(), bodyJson);
      new UserSkillEndorsementRequestValidator().validateSkillEndorsementRequest(reqObj);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }
}
