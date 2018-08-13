package controllers.skills;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import org.apache.commons.lang.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import play.libs.F.Promise;
import play.mvc.Result;

public class UserSkillEndorsementController extends BaseController {

  public Promise<Result> addEndorsement() {
    try {
      JsonNode bodyJson = request().body().asJson();
      Request reqObj = createAndInitRequest(ActorOperations.ADD_USER_SKILL_ENDORSEMENT.getValue(), bodyJson);
      String userId = (String) reqObj.getRequest().get("userID");
      String endorseUserId = (String) reqObj.getRequest().get("endorseUserId");
      String skillId = (String) reqObj.getRequest().get("skillId");
      if(StringUtils.isBlank(userId) || StringUtils.isBlank(endorseUserId) || StringUtils.isBlank(skillId)){
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
}
