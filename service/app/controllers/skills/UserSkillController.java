package controllers.skills;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import controllers.skills.validator.UserSkillRequestValidator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import play.mvc.Http;
import play.mvc.Result;
import util.Attrs;
import util.Common;

public class UserSkillController extends BaseController {

  public CompletionStage<Result> addSkill(Http.Request httpRequest) {
    try {
      JsonNode bodyJson = httpRequest.body().asJson();
      Request reqObj =
          createAndInitRequest(ActorOperations.ADD_SKILL.getValue(), bodyJson, httpRequest);
      reqObj.put(JsonKey.REQUESTED_BY, Common.getFromRequest(httpRequest, Attrs.USER_ID));
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  public CompletionStage<Result> updateSkill(Http.Request httpRequest) {
    try {
      JsonNode bodyJson = httpRequest.body().asJson();
      Request reqObj =
          createAndInitRequest(ActorOperations.UPDATE_SKILL.getValue(), bodyJson, httpRequest);
      new UserSkillRequestValidator().validateUpdateSkillRequest(reqObj);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  public CompletionStage<Result> getSkill(Http.Request httpRequest) {
    try {
      JsonNode bodyJson = httpRequest.body().asJson();
      Request reqObj =
          createAndInitRequest(ActorOperations.GET_SKILL.getValue(), bodyJson, httpRequest);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  public CompletionStage<Result> getSkillsList(Http.Request httpRequest) {
    try {
      Request reqObj =
          createAndInitRequest(ActorOperations.GET_SKILLS_LIST.getValue(), httpRequest);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, httpRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }
}
