package controllers.skills;

import controllers.BaseController;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

public class UserSkillEndorsementController extends BaseController {

  public Promise<Result> endorseUserSkill() {
    try {
      ProjectLogger.log("Endorse user skill request: ");
      Request reqObj = createAndInitRequest(ActorOperations.ENDORSE_USER_SKILL.getValue());
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }
}
