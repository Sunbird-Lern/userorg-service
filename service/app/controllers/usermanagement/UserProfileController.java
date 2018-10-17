package controllers.usermanagement;

import controllers.BaseController;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.UserRequestValidator;
import org.sunbird.common.responsecode.ResponseCode;
import play.libs.F.Promise;
import play.mvc.Result;

public class UserProfileController extends BaseController {

  /**
   * Get all the social media types supported
   *
   * @return
   */
  public Promise<Result> getMediaTypes() {

    return handleRequest(ActorOperations.GET_MEDIA_TYPES.getValue(), null, null, null, null, false);
  }

  /**
   * This method will add or update user profile visibility control. User can make all field as
   * private except name. any private filed of user is not search-able.
   *
   * @return Promise<Result>
   */
  public Promise<Result> profileVisibility() {
    if (null != ctx().flash().get(JsonKey.IS_AUTH_REQ)
        && Boolean.parseBoolean(ctx().flash().get(JsonKey.IS_AUTH_REQ))) {
      String userId = (String) request().body().asJson().get(JsonKey.USER_ID).asText();
      if (!userId.equals(ctx().flash().get(JsonKey.USER_ID))) {
        throw new ProjectCommonException(
            ResponseCode.unAuthorized.getErrorCode(),
            ResponseCode.unAuthorized.getErrorMessage(),
            ResponseCode.UNAUTHORIZED.getResponseCode());
      }
    }

    return handleRequest(
        ActorOperations.PROFILE_VISIBILITY.getValue(),
        request().body().asJson(),
        (req) -> {
          Request request = (Request) req;
          UserRequestValidator.validateProfileVisibility(request);
          return null;
        },
        null,
        null,
        true);
  }
}
