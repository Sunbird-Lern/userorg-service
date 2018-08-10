package controllers.skills;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import play.libs.F.Promise;
import play.mvc.Result;

public class UserSkillController extends BaseController {

  public Promise<Result> updateSkill() {
    try {
      JsonNode bodyJson = request().body().asJson();
      Request reqObj = createAndInitRequest(ActorOperations.UPDATE_SKILL.getValue(), bodyJson);
      String userId = (String) reqObj.getRequest().get("userID");
      if (StringUtils.isBlank(userId)) {
        throw new ProjectCommonException(
            ResponseCode.mandatoryParamsMissing.getErrorCode(),
            ResponseCode.mandatoryParamsMissing.getErrorMessage(),
            ResponseCode.mandatoryParamsMissing.getResponseCode());
      }
      if (!userId.equals(ctx().flash().get(JsonKey.USER_ID))) {
        throw new ProjectCommonException(
            ResponseCode.invalidParameterValue.getErrorCode(),
            ResponseCode.invalidParameterValue.getErrorMessage(),
            ResponseCode.invalidParameterValue.getResponseCode());
      }
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
