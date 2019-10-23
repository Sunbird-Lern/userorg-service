package controllers.skills;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import controllers.skills.validator.UserSkillEndorsementRequestValidator;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class UserSkillEndorsementController extends BaseController {

  public CompletionStage<Result> addEndorsement(Http.Request httpRequest) {
    try {
      JsonNode bodyJson = httpRequest.body().asJson();
      Request reqObj =
          createAndInitRequest(ActorOperations.ADD_USER_SKILL_ENDORSEMENT.getValue(), bodyJson, httpRequest);
      new UserSkillEndorsementRequestValidator().validateSkillEndorsementRequest(reqObj);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }
}
